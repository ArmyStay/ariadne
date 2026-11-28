package com.ariadne.desktop

import com.ariadne.extract.OcrResult
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout

/** Real child processes, without requiring an installed Tesseract or shell. */
class DesktopOcrProcessTest {
    private fun command(mode: String, vararg args: String): List<String> {
        // Gradle workers may load tests outside java.class.path.
        val classpath = listOf(
            System.getProperty("java.class.path"),
            File(OcrProcessFixture::class.java.protectionDomain.codeSource.location.toURI()).path,
            File(Unit::class.java.protectionDomain.codeSource.location.toURI()).path,
        ).joinToString(File.pathSeparator)
        return listOf(File(System.getProperty("java.home"), "bin/java").path,
            "-cp", classpath, OcrProcessFixture::class.java.name, mode) + args
    }

    @Test
    fun successful_output_is_trimmed_in_reading_order() {
        assertEquals(OcrResult.Text(listOf("Shop", "TOTAL 123.45")), runOcrProcess(command("success")))
    }

    @Test
    fun stderr_larger_than_a_pipe_buffer_is_drained_while_waiting() {
        val result = runOcrProcess(command("stderr"))
        assertEquals(OcrResult.Text(listOf("TOTAL 123.45")), result)
    }

    @Test
    fun excessive_stdout_or_stderr_fails_without_returning_partial_text() {
        for (mode in listOf("stdout-flood", "stderr-flood")) {
            assertEquals(OcrResult.Failure("Text recognition output is too large"),
                runOcrProcess(command(mode), maxOutputBytes = 32768))
        }
    }

    @Test
    fun output_limit_is_inclusive_and_applies_to_both_streams_together() {
        assertTrue(runOcrProcess(command("both"), maxOutputBytes = 200) is OcrResult.Text)
        assertEquals(OcrResult.Failure("Text recognition output is too large"),
            runOcrProcess(command("both"), maxOutputBytes = 199))
    }

    @Test
    fun hung_process_is_killed_and_the_next_job_can_run() {
        val pidFile = Files.createTempFile("ocr-test-", ".pid").toFile()
        try {
            val start = System.nanoTime()
            assertEquals(OcrResult.Failure("Text recognition timed out"),
                runOcrProcess(command("hang", pidFile.path), timeoutSeconds = 2))
            assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 6)
            val pid = pidFile.readText().toLong()
            ProcessHandle.of(pid).ifPresent { handle ->
                handle.onExit().get(3, TimeUnit.SECONDS)
                assertTrue(!handle.isAlive)
            }
            successful_output_is_trimmed_in_reading_order()
        } finally {
            pidFile.delete()
        }
    }

    @Test
    fun cancellation_interrupts_the_wait_and_kills_the_process() = runBlocking {
        val pidFile = Files.createTempFile("ocr-cancel-", ".pid").toFile()
        val job = launch(Dispatchers.Default) {
            runInterruptible(Dispatchers.IO) {
                runOcrProcess(command("hang", pidFile.path), timeoutSeconds = 30)
            }
        }
        try {
            withTimeout(5000) {
                while (pidFile.readText().toLongOrNull() == null) delay(10)
            }
            withTimeout(5000) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
            ProcessHandle.of(pidFile.readText().toLong()).ifPresent { handle ->
                handle.onExit().get(3, TimeUnit.SECONDS)
                assertTrue(!handle.isAlive)
            }
        } finally {
            job.cancelAndJoin()
            pidFile.delete()
        }
    }

    @Test
    fun nonzero_exit_is_a_failure_even_with_text() {
        assertEquals(OcrResult.Failure("tesseract exited with code 7"), runOcrProcess(command("failure")))
    }

    @Test
    fun missing_executable_is_editable_failure() {
        assertTrue(runOcrProcess(listOf("ariadne-nonexistent-ocr-test-command")) is OcrResult.Failure)
    }
}

/** Small, portable executable fixture used by [DesktopOcrProcessTest]. */
object OcrProcessFixture {
    @JvmStatic
    fun main(args: Array<String>) {
        when (args[0]) {
            "success" -> print("  Shop  \n\n TOTAL 123.45 \n")
            "stderr" -> {
                System.err.println("x".repeat(256 * 1024))
                println("TOTAL 123.45")
            }
            "stdout-flood", "stderr-flood" -> {
                val stream = if (args[0] == "stdout-flood") System.out else System.err
                val block = ByteArray(8192) { 65 }
                while (true) stream.write(block)
            }
            "both" -> {
                System.out.print("a".repeat(100))
                System.err.print("b".repeat(100))
            }
            "hang" -> {
                File(args[1]).writeText(ProcessHandle.current().pid().toString())
                Thread.sleep(60_000)
            }
            "failure" -> {
                println("TOTAL 123.45")
                kotlin.system.exitProcess(7)
            }
        }
    }
}
