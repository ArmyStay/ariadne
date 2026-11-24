package com.ariadne.fx

import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.Rate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FxTest {

    private val inr = CurrencyCode.of("INR")
    private val thb = CurrencyCode.of("THB")
    private val jpy = CurrencyCode.of("JPY")
    private val usd = CurrencyCode.of("USD")
    private val day = LocalDay.of(2026, 11, 23)

    private val table = FxTable(
        listOf(
            // 1 THB = 2.64 INR → scaled 2_640_000
            Rate(thb, inr, day, 2_640_000L),
            // 1 USD = 88.25 INR
            Rate(usd, inr, day, 88_250_000L),
            // 1 JPY = 0.55 INR
            Rate(jpy, inr, day, 550_000L),
        ),
    )

    @Test
    fun identity_conversion_is_lossless_and_marks_not_applied() {
        val result = table.convert(Money.ofMinor(12_345), inr, inr, day)
        assertEquals(12_345L, result.amount.minorUnits)
        assertFalse(result.applied)
    }

    @Test
    fun thai_baht_converts_to_rupees_with_half_up_rounding() {
        // 115.00 THB = 11_500 minor → × 2.64 = 30_360 minor exactly = ₹303.60
        val result = table.convert(Money.ofMinor(11_500), thb, inr, day)
        assertEquals(30_360L, result.amount.minorUnits)
        assertTrue(result.applied)
    }

    @Test
    fun rounding_happens_once_at_the_boundary() {
        // 1 THB = 2.64 INR; 33 minor THB → 87.12 minor INR, no drift.
        val result = table.convert(Money.ofMinor(33), thb, inr, day)
        assertEquals(87L, result.amount.minorUnits)
    }

    @Test
    fun half_up_rounds_a_true_half_upward() {
        // 0.01 USD = 0.8825 INR → 88.25 minor → rounds to 88; 0.005 ties go up.
        val tie = table.convert(Money.ofMinor(1), usd, inr, day)
        assertEquals(88L, tie.amount.minorUnits)
        val half = table.convert(Money.ofMinor(50), usd, inr, day)
        // 50 × 88.25 = 4412.5 → HALF_UP → 4413
        assertEquals(4_413L, half.amount.minorUnits)
    }

    @Test
    fun zero_scale_target_currency_rescales_minor_units() {
        // 100.00 INR (10_000 minor) → JPY has no minor unit: 10_000 × 1/0.55 …
        val result = table.convert(Money.ofMinor(10_000), inr, jpy, day)
        assertTrue(result.applied)
        // 10_000 minor INR = ₹100 → ÷ 0.55 = ¥181.8 → ¥182 (0 minor digits)
        assertTrue(result.amount.minorUnits in 181L..182L)
    }

    @Test
    fun inverse_pair_is_derived_when_only_one_direction_is_stored() {
        val rate = table.rateOrNull(inr, thb, day)
        assertTrue(rate != null && rate < FxTable.SCALE)
    }

    @Test
    fun a_missing_rate_never_zeroes_a_record() {
        val missing = FxTable(emptyList()).convert(Money.ofMinor(5_000), thb, inr, day)
        assertEquals(5_000L, missing.amount.minorUnits)
        assertFalse(missing.applied)
    }

    @Test
    fun the_most_recent_earlier_snapshot_wins() {
        val older = Rate(thb, inr, LocalDay.of(2026, 11, 20), 2_500_000L)
        val newer = Rate(thb, inr, LocalDay.of(2026, 11, 22), 2_600_000L)
        val future = Rate(thb, inr, LocalDay.of(2026, 11, 30), 9_000_000L)
        val fx = FxTable(listOf(older, newer, future))
        val result = fx.convert(Money.ofMinor(100), thb, inr, day)
        // 100 × 2.6 = 260 minor
        assertEquals(260L, result.amount.minorUnits)
    }

    @Test
    fun rate_from_decimal_string_is_exact_at_six_places() {
        assertEquals(11_200L, FxTable.rateFromDecimal("0.0112"))
        assertEquals(2_640_000L, FxTable.rateFromDecimal("2.64"))
        assertEquals(1_000_000L, FxTable.rateFromDecimal("1"))
    }

    @Test
    fun tally_separates_currencies_and_converts_the_total() {
        val captures = listOf(
            capture("a", 68_25, inr),
            capture("b", 11_500, thb),
            capture("c", 12_000, inr),
        )
        val tally = Tally(table, inr).total(captures, day)
        assertEquals(2, tally.perCurrency.size)
        assertTrue(tally.isMultiCurrency)
        // 68.25 + 120.00 (INR) + 303.60 (THB converted) = 491.85
        assertEquals(49_185L, tally.baseTotal.minorUnits)
    }

    @Test
    fun tally_counts_captures_with_no_amount_without_treating_them_as_zero_rupees() {
        val noAmount = capture("d", null, null)
        val tally = Tally(table, inr).total(listOf(capture("a", 100, inr), noAmount), day)
        assertEquals(1, tally.noAmountCount)
        assertEquals(100L, tally.baseTotal.minorUnits)
    }

    @Test
    fun two_currencies_and_a_non_zero_total_are_reachable_in_one_step() {
        // Acceptance criterion 7, expressed as a unit test.
        val tally = Tally(table, inr).total(
            listOf(capture("chai", 4_000, inr), capture("kiosk", 8_000, thb)),
            day,
        )
        assertTrue(tally.isMultiCurrency)
        assertTrue(tally.baseTotal.minorUnits > 0)
        assertEquals(2, tally.perCurrency.keys.size)
    }

    private fun capture(id: String, minor: Long?, code: CurrencyCode?) = Capture(
        id = id,
        tripId = "trip-1",
        localDate = day,
        photoPath = "$id.jpg",
        createdAt = 0,
        state = CaptureState.CONFIRMED,
        amountMinor = minor,
        currency = code,
    )
}
