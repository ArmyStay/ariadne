package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {

    private val inr = CurrencyCode.of("INR")
    private val jpy = CurrencyCode.of("JPY")
    private val kwd = CurrencyCode.of("KWD")

    @Test
    fun parses_whole_rupees_into_minor_units() {
        assertEquals(Money.ofMinor(120_000), Money.parseOrNull("1200", inr))
    }

    @Test
    fun parses_decimals_at_currency_scale() {
        assertEquals(Money.ofMinor(125_050), Money.parseOrNull("1250.50", inr))
        assertEquals(Money.ofMinor(125_050), Money.parseOrNull("1,250.50", inr))
        assertEquals(Money.ofMinor(5), Money.parseOrNull(".05", inr))
    }

    @Test
    fun zero_scale_currency_rejects_fraction_digits() {
        assertEquals(Money.ofMinor(1200), Money.parseOrNull("1200", jpy))
        assertNull(Money.parseOrNull("1200.5", jpy))
    }

    @Test
    fun three_scale_currency_keeps_three_digits() {
        assertEquals(Money.ofMinor(1_250), Money.parseOrNull("1.250", kwd))
        assertNull(Money.parseOrNull("1.2501", kwd))
    }

    @Test
    fun rejects_junk_instead_of_throwing() {
        assertNull(Money.parseOrNull("", inr))
        assertNull(Money.parseOrNull("12a", inr))
        assertNull(Money.parseOrNull("1.2.3", inr))
        assertNull(Money.parseOrNull("--5", inr))
    }

    @Test
    fun formats_with_indian_grouping_for_rupees() {
        assertEquals("₹1,20,000.00", Money.ofMinor(12_000_000).formatWithSymbol(inr))
        assertEquals("1,250.50", Money.ofMinor(125_050).format(inr))
    }

    @Test
    fun formats_with_western_grouping_elsewhere() {
        assertEquals("12,000.00", Money.ofMinor(1_200_000).format(CurrencyCode.of("USD")))
        assertEquals("1,200", Money.ofMinor(1200).format(jpy))
    }

    @Test
    fun addition_is_exact_no_floating_point_drift() {
        var total = Money.ofMinor(0)
        repeat(100) { total += Money.ofMinor(10) }
        assertEquals(Money.ofMinor(1_000), total)
        // The canonical float failure: 0.1 + 0.2 != 0.3.
        val one = Money.ofMinor(10)
        val two = Money.ofMinor(20)
        assertEquals(Money.ofMinor(30), one + two)
    }

    @Test
    fun comparisons_and_zero() {
        assertTrue(Money.ofMinor(5) < Money.ofMinor(6))
        assertTrue(Money.ofMinor(0).isZero)
        assertEquals(Money.ofMinor(1), Money.ofMinor(6) - Money.ofMinor(5))
    }

    @Test
    fun currency_scale_is_derived_from_the_code() {
        assertEquals(2, inr.scale)
        assertEquals(0, jpy.scale)
        assertEquals(3, kwd.scale)
        assertEquals(0, CurrencyCode.of("VND").scale)
    }
}
