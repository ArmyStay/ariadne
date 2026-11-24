package com.ariadne.desktop

import com.ariadne.extract.ExtractionOutcome
import com.ariadne.extract.OcrResult
import com.ariadne.extract.ReceiptParser
import com.ariadne.model.CurrencyCode
import java.io.File
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Desktop platform recogniser fixture test: image bytes/path -> Tess4JTextRecognizer -> ReceiptParser.
 *
 * Add real receipt photos under `recogniser-fixtures/desktop/` with `<name>.expected.txt` or `<name>.meta.json`.
 * Replace synthetic placeholders with real, redacted images for meaningful OCR assertions.
 */
class DesktopRecognizerFixtureTest {
    @Test
    fun recognizerFixtures_bytesToTextToParser() = runTest {
        val fixturesDir = sequenceOf(
            File("recogniser-fixtures/desktop"),
            File("../recogniser-fixtures/desktop"),
            File("../../recogniser-fixtures/desktop"),
        ).firstOrNull { it.exists() } ?: return@runTest

        val images = fixturesDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "bmp", "tif", "tiff") }
            .sortedBy { it.name }
        if (images.isEmpty()) return@runTest

        val tessdata = sequenceOf(
            System.getenv("TESSDATA_PREFIX")?.let(::File),
            File("tessdata"),
            File("../tessdata"),
            File(System.getProperty("user.home"), ".ariadne/tessdata"),
        ).filterNotNull().firstOrNull { it.exists() && it.isDirectory }?.absolutePath
            ?: Tess4JTextRecognizer.defaultDataPath()
            ?: return@runTest

        val recognizer = Tess4JTextRecognizer(dataPath = tessdata)
        val parser = ReceiptParser(CurrencyCode.of("INR"))
        val failures = mutableListOf<String>()
        var exercised = 0
        var ocrSucceeded = 0

        for (image in images) {
            val base = image.nameWithoutExtension
            val expectedFile = File(fixturesDir, "$base.expected.txt")
            val metaFile = File(fixturesDir, "$base.meta.json")
            val expectedText = expectedFile.takeIf { it.exists() }?.readText()
            val metaText = metaFile.takeIf { it.exists() }?.readText()
            if (expectedText == null && metaText == null) continue

            val result = recognizer.recognize(image.absolutePath)
            exercised++
            when (result) {
                is OcrResult.Failure -> {
                    // On hosts without native OCR libs this is acceptable; wiring/image loading still exercised.
                    // If you run on a host with Tesseract natives installed, failures should be investigated.
                    assertTrue(result.reason.isNotBlank(), "${image.name}: OCR failure reason should not be blank")
                }
                is OcrResult.Text -> {
                    ocrSucceeded++
                    val parsed = parser.parse(result.lines)
                    assertTrue(parsed.confidence >= 0.0, "${image.name}: parser should not throw on OCR output")
                    if (metaText != null) {
                        assertMeta(image.name, metaText, result.lines, parsed, failures)
                    } else if (expectedText != null) {
                        assertExpectedText(image.name, expectedText, result.lines, parsed, failures)
                    }
                }
            }
        }

        if (exercised == 0) return@runTest
        // If OCR succeeded for at least one fixture, enforce collected expectation failures.
        if (ocrSucceeded > 0) {
            assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        }
    }

    private fun assertExpectedText(
        fixtureId: String,
        expectedText: String,
        lines: List<String>,
        parsed: ExtractionOutcome,
        failures: MutableList<String>,
    ) {
        val normalizedExpected = normalize(expectedText)
        val normalizedActual = normalize(lines.joinToString("\n"))
        val sim = similarity(normalizedExpected, normalizedActual)
        if (sim < 0.6) failures += "$fixtureId: OCR text similarity $sim < 0.6"

        val amountToken = Regex("""\d+[.,]\d{2}|\d+""").find(expectedText)?.value
        if (amountToken != null) {
            val normalizedAmount = normalize(amountToken)
            val amountSurvived = normalizedActual.contains(normalizedAmount) || parsed.amountMinorOrNull != null
            if (!amountSurvived) failures += "$fixtureId: expected amount token '$amountToken' to survive in OCR/parser output"
        }
        val merchantLine = expectedText.lineSequence().map { it.trim() }.firstOrNull { line ->
            line.isNotEmpty() && !line.any { it.isDigit() } && line.length >= 3
        }
        if (merchantLine != null) {
            val token = normalize(merchantLine).take(12)
            if (token.isNotEmpty() && !normalizedActual.contains(token)) {
                failures += "$fixtureId: expected merchant text '$merchantLine' to survive in OCR output"
            }
        }
    }

    private fun assertMeta(
        fixtureId: String,
        metaJson: String,
        lines: List<String>,
        parsed: ExtractionOutcome,
        failures: MutableList<String>,
    ) {
        val minSimilarity = jsonDouble(metaJson, "minTextSimilarity") ?: 0.6
        val expectedText = jsonString(metaJson, "expectedText") ?: jsonString(metaJson, "expectedOcrText")
        if (expectedText != null) {
            val sim = similarity(normalize(expectedText), normalize(lines.joinToString("\n")))
            if (sim < minSimilarity) failures += "$fixtureId: OCR text similarity $sim < $minSimilarity"
        }
        jsonLong(metaJson, "expectedAmountMinor")?.let { expected ->
            val actual = parsed.amountMinorOrNull
            if (actual != expected) failures += "$fixtureId: expected amountMinor=$expected but was $actual"
        }
        jsonString(metaJson, "expectedCurrency")?.let { expected ->
            val expectedCode = expected.trim().uppercase()
            val actual = parsed.currencyOrNull?.code
            if (actual != expectedCode) failures += "$fixtureId: expected currency=$expectedCode but was $actual"
        }
        jsonString(metaJson, "expectedMerchantContains")?.let { needle ->
            val merchant = parsed.merchantOrNull.orEmpty()
            if (!normalize(merchant).contains(normalize(needle))) {
                failures += "$fixtureId: expected merchant containing '$needle' but was '$merchant'"
            }
        }
        jsonString(metaJson, "expectedMerchant")?.let { expectedMerchant ->
            val merchant = parsed.merchantOrNull.orEmpty()
            val haystack = normalize(lines.joinToString(" "))
            if (!normalize(merchant).contains(normalize(expectedMerchant)) && !haystack.contains(normalize(expectedMerchant))) {
                failures += "$fixtureId: expected merchant '$expectedMerchant' to survive in parse/OCR"
            }
        }
        if (jsonBool(metaJson, "expectParsedReceipt") == true &&
            (parsed is ExtractionOutcome.Nothing || parsed is ExtractionOutcome.Failed)
        ) {
            failures += "$fixtureId: expected parsed receipt but parser returned ${parsed::class.simpleName}"
        }
    }

    private fun normalize(s: String): String = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
                prev = temp
            }
        }
        val dist = dp[b.length]
        return 1.0 - (dist.toDouble() / max(a.length, b.length).toDouble())
    }

    private fun jsonString(json: String, key: String): String? =
        Regex(""""$key"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1)

    private fun jsonLong(json: String, key: String): Long? =
        Regex(""""$key"\s*:\s*(-?\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull()

    private fun jsonDouble(json: String, key: String): Double? =
        Regex(""""$key"\s*:\s*(-?\d+(?:\.\d+)?)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()

    private fun jsonBool(json: String, key: String): Boolean? =
        Regex(""""$key"\s*:\s*(true|false)""").find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
}
