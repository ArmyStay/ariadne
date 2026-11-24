package com.ariadne.desktop

import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import java.io.File
import java.io.IOException

/**
 * Desktop OCR via the Tesseract command line tool.
 *
 * Tesseract is not a Kotlin library in this module's dependency set and the
 * sandbox has no network route to Maven Central, so the desktop target binds
 * OCR by shelling out: this recognizer asks the installed `tesseract` to read
 * the photo file and turns its text output into the shared
 * [OcrResult] contract. On machines without the tool it reports
 * [OcrResult.Failure] — the same "no text read" outcome as the Wasm target —
 * so the capture still exists and still reaches Nightfall.
 *
 * Lines are reordered the way every other recognizer returns them: not in
 * page coordinates but in reading order top to bottom, because the shared
 * [com.ariadne.extract.ReceiptParser] parses the line list.
 */
internal class TesseractTextRecognizer : TextRecognizer {

    override suspend fun recognize(imagePath: String): OcrResult = runCatching {
        val file = File(imagePath)
        if (!file.exists()) return@runCatching OcrResult.Failure("Photo file missing")
        // A scratch text file beside the system temp dir; deleted in a finally.
        val out = File.createTempFile("ariadne-ocr-", ".txt")
        try {
            val process = ProcessBuilder(
                "tesseract",
                file.absolutePath,
                out.absolutePath,
                "--psm", "6", // sparse text: a receipt photograph, not a full page
            ).start()
            val code = process.waitFor()
            if (code != 0) {
                OcrResult.Failure("tesseract exited with code $code")
            } else {
                val lines = out.readText().lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                OcrResult.Text(lines)
            }
        } finally {
            out.delete()
        }
    }.getOrElse { error ->
        when (error) {
            is IOException -> OcrResult.Failure(
                "desktop OCR needs the tesseract command line tool installed",
            )

            else -> OcrResult.Failure(error.message ?: "desktop OCR failed")
        }
    }
}
