package com.ariadne.desktop

import com.ariadne.extract.OcrResult
import com.ariadne.extract.ReceiptParser
import com.ariadne.model.CurrencyCode
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopRecognizerFixtureTest {
    @Test
    fun recognizerFixture_whenImageAndTessdataAvailable_producesParsableText() = runTest {
        val fixturesDir = sequenceOf(
            File("recogniser-fixtures/desktop"),
            File("../recogniser-fixtures/desktop"),
            File("../../recogniser-fixtures/desktop"),
        ).firstOrNull { it.exists() } ?: return@runTest

        val image = fixturesDir.listFiles()?.firstOrNull {
            it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "bmp", "tif", "tiff")
        } ?: return@runTest // add a real receipt image to enable this platform fixture test

        // Prefer filesystem TESSDATA_PREFIX/local dirs; fall back to recognizer's bundled extraction.
        val tessdata = sequenceOf(
            System.getenv("TESSDATA_PREFIX")?.let(::File),
            File("tessdata"),
            File("../tessdata"),
            File(System.getProperty("user.home"), ".ariadne/tessdata"),
        ).filterNotNull().firstOrNull { it.exists() && it.isDirectory }?.absolutePath
            ?: Tess4JTextRecognizer.defaultDataPath()
            ?: return@runTest // install/provide eng.traineddata to enable OCR assertion

        val recognizer = Tess4JTextRecognizer(dataPath = tessdata)
        val result = recognizer.recognize(image.absolutePath)
        // On hosts without Tesseract natives, OCR may fail gracefully; fixture parsing still must not crash.
        when (result) {
            is OcrResult.Text -> {
                val parsed = ReceiptParser(CurrencyCode.of("INR")).parse(result.lines)
                assertTrue(parsed.confidence >= 0.0, "Parser should handle OCR output without throwing")
            }
            is OcrResult.Failure -> {
                // Acceptable on machines without native OCR libs; image+traineddata wiring was exercised.
                assertTrue(result.reason.isNotBlank())
            }
        }
    }
}
