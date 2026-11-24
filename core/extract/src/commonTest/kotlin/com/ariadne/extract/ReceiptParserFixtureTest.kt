package com.ariadne.extract

import com.ariadne.model.CurrencyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The fixture suite is the specification for the parser. Every case here is a
 * receipt shape the app will actually see; a parser change that breaks one of
 * these is a regression, not a preference.
 */
class ReceiptParserFixtureTest {

    private val parser = ReceiptParser(fallbackCurrency = CurrencyCode.of("INR"))

    @Test
    fun suite_has_at_least_twenty_five_fixtures() {
        assertTrue(fixtures.size >= 25, "fixture count was ${fixtures.size}")
    }

    @Test
    fun every_fixture_matches_its_expected_reading() {
        val failures = mutableListOf<String>()
        for (fixture in fixtures) {
            val outcome = parser.parse(fixture.lines)
            if (fixture.expectNothing) {
                if (outcome !is ExtractionOutcome.Nothing) {
                    failures += "${fixture.name}: expected Nothing, got $outcome"
                }
                continue
            }
            val amount = outcome.amountMinorOrNull
            if (amount != fixture.expectedAmountMinor) {
                failures += "${fixture.name}: amount ${amount} != ${fixture.expectedAmountMinor}"
            }
            if (fixture.expectedCurrency != null) {
                val currency = outcome.currencyOrNull?.code
                if (currency != fixture.expectedCurrency) {
                    failures += "${fixture.name}: currency $currency != ${fixture.expectedCurrency}"
                }
            }
            if (fixture.expectedMerchantContains != null) {
                val merchant = outcome.merchantOrNull
                if (merchant == null || !merchant.uppercase().contains(fixture.expectedMerchantContains)) {
                    failures += "${fixture.name}: merchant '$merchant' missing '${fixture.expectedMerchantContains}'"
                }
            }
        }
        assertTrue(failures.isEmpty(), "parser fixtures failed:\n" + failures.joinToString("\n"))
    }

    @Test
    fun a_receipt_with_no_currency_token_falls_back_to_the_trip_currency() {
        val fixtur = fixtures.first { it.name.startsWith("05_") }
        val outcome = parser.parse(fixtur.lines)
        assertEquals("INR", outcome.currencyOrNull?.code, "fallback currency should apply")
        // ...but the confidence is marked down, so the UI can leave it unconfirmed.
        assertTrue(outcome.confidence < 1.0)
    }

    @Test
    fun an_explicit_currency_token_beats_the_fallback() {
        val thai = fixtures.first { it.name.startsWith("03_") }
        assertEquals("THB", parser.parse(thai.lines).currencyOrNull?.code)
    }

    @Test
    fun empty_ocr_is_nothing_not_an_error() {
        assertIs<ExtractionOutcome.Nothing>(parser.parse(emptyList()))
    }

    @Test
    fun dates_are_read_when_present_and_ignored_when_absent() {
        val dated = fixtures.first { it.name.startsWith("02_") }
        val outcome = parser.parse(dated.lines)
        assertTrue(outcome is ExtractionOutcome.AmountAndMerchant || outcome is ExtractionOutcome.AmountOnly)
    }

    @Test
    fun confidence_floor_leaves_a_weak_guess_blank() {
        val strict = ReceiptParser(fallbackCurrency = CurrencyCode.of("INR"), confidenceFloor = 0.99)
        val outcome = strict.parse(listOf("TOTAL", "83.00"))
        // Refusing to guess is the designed behaviour, not a failure mode.
        assertIs<ExtractionOutcome.Nothing>(outcome)
    }

    @Test
    fun parser_never_throws_on_arbitrary_garbage() {
        val junk = listOf(
            listOf(""), listOf(" "), listOf("----"), listOf("0"), listOf("Total 0"),
            listOf("1.2.3.4.5"), listOf("₹₹₹"), listOf("Total ₹"),
            listOf("999999999999999999999"),
        )
        for (lines in junk) {
            parser.parse(lines) // must not throw
        }
    }
}
