package com.ariadne.android

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariadne.extract.ExtractionOutcome
import com.ariadne.extract.OcrResult
import com.ariadne.extract.ReceiptParser
import com.ariadne.model.CurrencyCode
import java.io.File
import kotlin.math.max
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device fixture test: real receipt image bytes -> MlKitTextRecognizer -> ReceiptParser.
 *
 * Add real photographed/scanned receipts under androidTest assets at
 * `recogniser-fixtures/android/<name>.(jpg|jpeg|png|webp)` with either `<name>.expected.txt`
 * (expected OCR text, used for fuzzy/normalized text assertions) or `<name>.meta.json`
 * (optional parser expectations and similarity threshold).
 *
 * Intended to be run on a Play-Services-less emulator/device to validate bundled ML Kit behavior.
 */
@RunWith(AndroidJUnit4::class)
class AndroidReceiptFixtureInstrumentedTest {

    private val fixturesDir = "recogniser-fixtures/android"

    @Test
    fun recognizerAndParser_handleAvailableFixtures() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testContext = instrumentation.context
        val targetContext = instrumentation.targetContext
        val assets = testContext.assets
        val entries = assets.list(fixturesDir)?.toList().orEmpty()
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
        val images = entries.filter { name ->
            val extension = name.substringAfterLast('.', "").lowercase()
            val base = name.substringBeforeLast('.')
            extension in imageExtensions &&
                (entries.contains("$base.expected.txt") || entries.contains("$base.meta.json"))
        }.sorted()

        assumeTrue(
            "No Android fixture images with matching .expected.txt or .meta.json assets found in $fixturesDir",
            images.isNotEmpty(),
        )

        val recognizer = MlKitTextRecognizer(targetContext)
        val parser = ReceiptParser(CurrencyCode.of("INR"))
        val failures = mutableListOf<String>()
        var exercised = 0

        for (imageName in images) {
            val base = imageName.substringBeforeLast('.')
            val metaName = "$base.meta.json"
            val expectedTextName = "$base.expected.txt"
            val meta = if (entries.contains(metaName)) readAssetText(assets, "$fixturesDir/$metaName") else null
            val expectedText = if (entries.contains(expectedTextName)) readAssetText(assets, "$fixturesDir/$expectedTextName") else null
            if (meta == null && expectedText == null) {
                failures += "$imageName: missing $metaName or $expectedTextName"
                continue
            }

            val imageFile = copyAssetToCache(testContext, "$fixturesDir/$imageName", "android-fixture-$imageName")
            val ocr = recognizer.recognize(imageFile.absolutePath)
            val lines = when (ocr) {
                is OcrResult.Text -> ocr.lines
                is OcrResult.Failure -> {
                    failures += "$imageName: OCR failed (${ocr.reason})"
                    continue
                }
            }
            val parsed = parser.parse(lines)
            exercised++

            if (meta != null) {
                assertMeta(base, meta, lines, parsed, failures)
            } else if (expectedText != null) {
                assertExpectedText(base, expectedText, lines, parsed, failures)
            }
        }

        assertTrue(exercised > 0, "No fixtures were exercised: ${failures.joinToString("; ")}")
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun assertMeta(
        fixtureId: String,
        metaJson: String,
        lines: List<String>,
        parsed: ExtractionOutcome,
        failures: MutableList<String>,
    ) {
        val meta = JSONObject(metaJson)
        val minSimilarity = meta.optDouble("minTextSimilarity", 0.6)
        val expectedText = meta.optString("expectedText").takeIf { it.isNotBlank() }
            ?: meta.optString("expectedOcrText").takeIf { it.isNotBlank() }
        if (expectedText != null) {
            val sim = similarity(normalize(expectedText), normalize(lines.joinToString("\n")))
            if (sim < minSimilarity) failures += "$fixtureId: OCR text similarity $sim < $minSimilarity"
        }

        if (meta.has("expectedAmountMinor")) {
            val expectedAmount = meta.optLong("expectedAmountMinor")
            val actualAmount = amountMinorOf(parsed)
            if (actualAmount != expectedAmount) failures += "$fixtureId: expected amountMinor=$expectedAmount but was $actualAmount"
        }
        if (meta.has("expectedCurrency")) {
            val expectedCurrency = CurrencyCode.of(meta.getString("expectedCurrency"))
            val actualCurrency = currencyOf(parsed)
            if (actualCurrency != expectedCurrency) failures += "$fixtureId: expected currency=$expectedCurrency but was $actualCurrency"
        }
        val merchantContains = meta.optString("expectedMerchantContains").takeIf { it.isNotBlank() }
        if (merchantContains != null) {
            val merchant = merchantOf(parsed).orEmpty()
            if (!normalize(merchant).contains(normalize(merchantContains))) {
                failures += "$fixtureId: expected merchant containing '$merchantContains' but was '$merchant'"
            }
        }
        val expectedMerchant = meta.optString("expectedMerchant").takeIf { it.isNotBlank() }
        if (expectedMerchant != null) {
            val merchant = merchantOf(parsed).orEmpty()
            if (!normalize(merchant).contains(normalize(expectedMerchant)) && !normalize(lines.joinToString(" ")).contains(normalize(expectedMerchant))) {
                failures += "$fixtureId: expected merchant '$expectedMerchant' to survive in parse/OCR"
            }
        }
        val mustContain = meta.optJSONArray("mustContainTokens")
        if (mustContain != null) {
            val haystack = normalize(lines.joinToString("\n"))
            for (i in 0 until mustContain.length()) {
                val token = normalize(mustContain.getString(i))
                if (token.isNotEmpty() && !haystack.contains(token)) {
                    failures += "$fixtureId: OCR text missing token '$token'"
                }
            }
        }
        if (meta.optBoolean("expectParsedReceipt", false) &&
            (parsed is ExtractionOutcome.Nothing || parsed is ExtractionOutcome.Failed)
        ) {
            failures += "$fixtureId: expected parsed receipt but parser returned ${parsed::class.simpleName}"
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

        // Key survival checks derived from expected text: amount-like and merchant-like tokens.
        val amountToken = Regex("""\d+[.,]\d{2}|\d+""").find(expectedText)?.value
        if (amountToken != null) {
            val normalizedAmount = normalize(amountToken)
            val amountSurvived = normalizedActual.contains(normalizedAmount) || amountMinorOf(parsed) != null
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

    private fun readAssetText(assets: android.content.res.AssetManager, path: String): String =
        assets.open(path).bufferedReader().use { it.readText() }

    private fun copyAssetToCache(context: Context, assetPath: String, name: String): File {
        val out = File(context.cacheDir, name)
        context.assets.open(assetPath).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun amountMinorOf(parsed: ExtractionOutcome): Long? = parsed.amountMinorOrNull

    private fun currencyOf(parsed: ExtractionOutcome): CurrencyCode? = parsed.currencyOrNull

    private fun merchantOf(parsed: ExtractionOutcome): String? = parsed.merchantOrNull

    private fun normalize(s: String): String = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    // Simple normalized Levenshtein similarity in [0,1].
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
}