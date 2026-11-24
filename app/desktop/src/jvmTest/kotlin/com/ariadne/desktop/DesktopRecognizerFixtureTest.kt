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

        val tessdata = sequenceOf(
            System.getenv("TESSDATA_PREFIX")?.let(::File),
            File("tessdata"),
            File("../tessdata"),
            File(System.getProperty("user.home"), ".ariadne/tessdata"),
        ).filterNotNull().firstOrNull { it.exists() && it.isDirectory }
            ?: return@runTest // install/provide eng.traineddata to enable OCR assertion

        val recognizer = Tess4JTextRecognizer(dataPath = tessdata.absolutePath)
        val result = recognizer.recognize(image.absolutePath)
        assertTrue(result is OcrResult.Text, "Expected OCR text result for fixture ${image.name}, got $result")
        val parsed = ReceiptParser(CurrencyCode.of("INR")).parse(result.lines)
        assertTrue(parsed.confidence >= 0.0, "Parser should handle OCR output without throwing")
    }
}
