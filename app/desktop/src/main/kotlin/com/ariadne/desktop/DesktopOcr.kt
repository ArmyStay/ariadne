package com.ariadne.desktop

import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

/** Desktop OCR via a bounded Tesseract subprocess; failure still leaves the capture editable. */
internal class TesseractTextRecognizer : TextRecognizer {
    override suspend fun recognize(imagePath: String): OcrResult = runInterruptible(Dispatchers.IO) {
        val file = File(imagePath)
        if (!file.exists()) return@runInterruptible OcrResult.Failure("Photo file missing")
        if (file.length() > MAX_COMPRESSED_IMAGE_BYTES || !file.hasSafeImageBounds()) {
            return@runInterruptible OcrResult.Failure("Image is too large or has unsafe dimensions")
        }
        // stdout avoids Tesseract's output-base + ".txt" convention and orphaned scratch files.
        runOcrProcess(listOf("tesseract", file.absolutePath, "stdout", "--psm", "6"))
    }
}

/**
 * Drain both pipes concurrently with the timed wait: waiting before reading can
 * deadlock even a healthy process. Unlike redirectErrorStream(true), separate drains
 * keep diagnostic numbers out of receipt parsing. Limit *all* output, including diagnostics, rather
 * than retaining an unbounded string. A failed/oversized run never returns partial OCR.
 * The deadline also covers pipe EOF (a child could inherit the pipe after its parent exits).
 */
internal fun runOcrProcess(
    command: List<String>,
    timeoutSeconds: Long = 8,
    maxOutputBytes: Int = 1024 * 1024,
): OcrResult {
    require(timeoutSeconds > 0 && maxOutputBytes > 0)
    val process = try {
        ProcessBuilder(command).start()
    } catch (error: IOException) {
        return OcrResult.Failure("desktop OCR needs the tesseract command line tool installed")
    }
    val reader = Executors.newFixedThreadPool(2) { task ->
        Thread(task, "ariadne-ocr-output").apply { isDaemon = true }
    }
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
    val remaining = AtomicInteger(maxOutputBytes)
    val exceeded = AtomicBoolean(false)
    fun drain(stream: InputStream, retain: Boolean) = reader.submit<ByteArray?> {
        stream.use { input ->
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (remaining.addAndGet(-count) < 0) {
                    exceeded.set(true)
                    process.destroyForcibly()
                    return@submit null
                }
                if (retain) bytes.write(buffer, 0, count)
            }
            bytes.toByteArray()
        }
    }
    val output = drain(process.inputStream, retain = true)
    val diagnostics = drain(process.errorStream, retain = false)
    return try {
        process.outputStream.close() // OCR never reads stdin.
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            OcrResult.Failure("Text recognition timed out")
        } else {
            val bytes = output.get((deadline - System.nanoTime()).coerceAtLeast(0), TimeUnit.NANOSECONDS)
            diagnostics.get((deadline - System.nanoTime()).coerceAtLeast(0), TimeUnit.NANOSECONDS)
            when {
                exceeded.get() || bytes == null -> OcrResult.Failure("Text recognition output is too large")
                process.exitValue() != 0 -> OcrResult.Failure("tesseract exited with code ${process.exitValue()}")
                else -> OcrResult.Text(bytes.toString(Charsets.UTF_8).lineSequence()
                    .map { it.trim() }.filter { it.isNotEmpty() }.toList())
            }
        }
    } catch (error: TimeoutException) {
        OcrResult.Failure("Text recognition timed out")
    } catch (error: InterruptedException) {
        // runInterruptible translates this to cancellation; don't turn it into OCR failure.
        throw error
    } catch (error: Exception) {
        OcrResult.Failure(if (exceeded.get()) "Text recognition output is too large"
            else "Could not read text recognition output")
    } finally {
        process.destroyForcibly()
        output.cancel(true)
        diagnostics.cancel(true)
        reader.shutdownNow()
        // The readers own their streams. Closing a pipe here can itself block
        // behind a reader if a descendant inherited it; never block the extraction queue.
        runCatching { process.outputStream.close() }
    }
}
