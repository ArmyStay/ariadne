package com.ariadne.fx

import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.Rate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The two failures this file exists to prevent, stated as tests.
 *
 * 1. THB 2,555 and INR 50 printed as "₹2,605". One total, two currencies, no rate
 *    in between: the Thai amount was added to the rupee amount because the lookup
 *    missed and the miss was counted as a zero.
 *
 * 2. The same capture converted at three different dates on three different pages,
 *    so the trip total depended on which page was open.
 */
class FxHonestyTest {

    private val inr = CurrencyCode.of("INR")
    private val thb = CurrencyCode.of("THB")
    private val usd = CurrencyCode.of("USD")

    private val bangkokDay = LocalDay.of(2026, 11, 20)
    private val laterDay = LocalDay.of(2026, 11, 24)

    private val table = FxTable(
        listOf(
            Rate(thb, inr, bangkokDay, 2_600_000L), // 1 THB = 2.60 INR
        ),
    )

    @Test
    fun unconverted_currency_is_never_added_to_the_base_total() {
        // The exact pair from the bug report: 2,555.00 THB and 50.00 INR, with no
        // THB rate on file at all.
        val empty = FxTable(emptyList())
        val tally = Tally(empty, inr).total(
            listOf(
                capture("thb", 255_500, thb, bangkokDay),
                capture("inr", 5_000, inr, bangkokDay),
            ),
        )
        // Not 260,500. The rupee amount alone, because the baht cannot be counted.
        assertEquals(5_000L, tally.baseTotal.minorUnits)
        assertEquals(mapOf(thb to 255_500L), tally.unconverted)
        assertEquals(1, tally.unconvertedCount)
        assertFalse(tally.isComplete)
        // And the money is still shown, in its own currency, not dropped.
        assertEquals(255_500L, tally.perCurrency[thb])
    }

    @Test
    fun a_rate_makes_the_same_two_captures_add_up_correctly() {
        val tally = Tally(table, inr).total(
            listOf(
                capture("thb", 255_500, thb, bangkokDay),
                capture("inr", 5_000, inr, bangkokDay),
            ),
        )
        // 2,555.00 THB × 2.60 = 6,643.00 INR, plus 50.00 = 6,693.00.
        assertEquals(669_300L, tally.baseTotal.minorUnits)
        assertTrue(tally.isComplete)
        assertEquals(bangkokDay, tally.rateDates[thb])
    }

    @Test
    fun a_pinned_rate_wins_over_a_later_table() {
        // The amount was confirmed at 2.60; the table has since been updated to 3.10.
        val moved = FxTable(
            listOf(
                Rate(thb, inr, bangkokDay, 2_600_000L),
                Rate(thb, inr, laterDay, 3_100_000L),
            ),
        )
        val pinned = capture("thb", 100_000, thb, bangkokDay).copy(
            rateScaled = 2_600_000L,
            rateDate = bangkokDay,
        )
        val tally = Tally(moved, inr).total(listOf(pinned))
        // Still 2,600.00 INR — the figure the traveller confirmed, not today's rate.
        assertEquals(260_000L, tally.baseTotal.minorUnits)
    }

    @Test
    fun a_stale_row_without_a_snapshot_converts_at_its_own_day_not_the_trips_start() {
        val onlyOnLaterDay = FxTable(listOf(Rate(thb, inr, laterDay, 3_000_000L)))
        // Captured on the 20th; the only rate on file is from the 24th, so there is
        // no rate that was true on the day of the capture and nothing may be assumed.
        val tally = Tally(onlyOnLaterDay, inr).total(listOf(capture("thb", 100_000, thb, bangkokDay)))
        assertEquals(0L, tally.baseTotal.minorUnits)
        assertEquals(1, tally.unconvertedCount)
    }

    @Test
    fun the_rate_date_is_reported_so_the_ui_can_print_it() {
        val tally = Tally(table, inr).total(listOf(capture("thb", 100_000, thb, bangkokDay)))
        assertEquals(bangkokDay, tally.rateDates[thb])
        assertEquals(1, tally.convertedCount)
    }

    @Test
    fun a_base_currency_amount_needs_no_rate_and_is_never_called_unconverted() {
        val tally = Tally(FxTable(emptyList()), inr).total(listOf(capture("inr", 12_345, inr, bangkokDay)))
        assertEquals(12_345L, tally.baseTotal.minorUnits)
        assertTrue(tally.isComplete)
        assertTrue(tally.rateDates.isEmpty())
    }

    @Test
    fun two_unconvertible_currencies_are_counted_and_named_separately() {
        val tally = Tally(FxTable(emptyList()), inr).total(
            listOf(
                capture("a", 1_000, thb, bangkokDay),
                capture("b", 2_000, thb, bangkokDay),
                capture("c", 300, usd, bangkokDay),
                capture("d", 400, inr, bangkokDay),
            ),
        )
        assertEquals(400L, tally.baseTotal.minorUnits)
        assertEquals(mapOf(thb to 3_000L, usd to 300L), tally.unconverted)
        assertEquals(3, tally.unconvertedCount)
    }

    @Test
    fun convert_at_refuses_a_nonsense_rate_instead_of_dividing_by_zero() {
        val result = table.convertAt(Money.ofMinor(1_000), thb, inr, 0L)
        assertEquals(1_000L, result.amount.minorUnits)
        assertFalse(result.applied)
    }

    @Test
    fun convert_at_matches_convert_when_the_rate_is_the_tables() {
        val a = table.convert(Money.ofMinor(77_700), thb, inr, bangkokDay)
        val b = table.convertAt(Money.ofMinor(77_700), thb, inr, 2_600_000L)
        assertEquals(a.amount.minorUnits, b.amount.minorUnits)
    }

    @Test
    fun an_inverse_pair_still_converts_when_the_table_only_holds_one_direction() {
        // Rates arrive as base → quote, but a capture can be in the quote currency.
        val table = FxTable(listOf(Rate(inr, thb, bangkokDay, 384_615L))) // 1 INR = 0.384615 THB
        val tally = Tally(table, inr).total(listOf(capture("thb", 100_000, thb, bangkokDay)))
        assertEquals(1, tally.convertedCount)
        assertTrue(tally.baseTotal.minorUnits > 250_000L)
    }

    private fun capture(id: String, minor: Long?, code: CurrencyCode?, day: LocalDay) = Capture(
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

class RatesTest {

    private val inr = CurrencyCode.of("INR")
    private val thb = CurrencyCode.of("THB")
    private val usd = CurrencyCode.of("USD")

    private val body = """
        {"amount":1.0,"base":"THB","date":"2026-11-20","rates":{"INR":2.5873,"JPY":4.4211}}
    """.trimIndent()

    @Test
    fun the_url_asks_for_every_quote_at_once() {
        assertEquals(
            "https://api.frankfurter.app/latest?from=THB&to=INR,USD",
            Frankfurter.url(thb, listOf(inr, usd)),
        )
    }

    @Test
    fun the_url_skips_the_base_currency_and_a_request_with_nothing_to_ask_for() {
        assertEquals("https://api.frankfurter.app/latest?from=THB&to=USD", Frankfurter.url(thb, listOf(thb, usd)))
        assertNull(Frankfurter.url(thb, listOf(thb)))
    }

    @Test
    fun a_historical_url_names_the_day() {
        val day = LocalDay.of(2026, 11, 20)
        assertEquals(
            "https://api.frankfurter.app/2026-11-20?from=THB&to=INR",
            Frankfurter.urlForDay(thb, listOf(inr), day),
        )
    }

    @Test
    fun a_response_parses_into_one_row_per_quote_on_the_published_day() {
        val parsed = Frankfurter.parse(body, thb)
        val rates = parsed?.rates.orEmpty()
        assertEquals(LocalDay.of(2026, 11, 20), parsed?.day)
        assertEquals(2, rates.size)
        // 2.5873 scaled by 1e6, exactly, with nothing in between.
        assertEquals(2_587_300L, rates.first { it.quote == inr }.rateScaled)
        assertTrue(rates.all { it.base == thb })
        assertTrue(rates.all { it.date == LocalDay.of(2026, 11, 20) })
    }

    @Test
    fun the_base_currency_is_never_echoed_as_a_rate() {
        val echo = """{"amount":1.0,"base":"THB","date":"2026-11-20","rates":{"THB":1.0,"INR":2.5873}}"""
        val parsed = Frankfurter.parse(echo, thb)
        assertEquals(listOf(inr), parsed?.rates?.map { it.quote })
    }

    @Test
    fun an_unreadable_body_yields_no_rates_rather_than_wrong_ones() {
        assertNull(Frankfurter.parse(null, thb))
        assertNull(Frankfurter.parse("", thb))
        assertNull(Frankfurter.parse("<html>captive portal</html>", thb))
        assertNull(Frankfurter.parse("""{"base":"THB","rates":{"INR":2.5}}""", thb)) // no date
        assertNull(Frankfurter.parse("""{"date":"2026-11-20"}""", thb)) // no rates
        assertNull(Frankfurter.parse("""{"date":"2026-11-20","rates":{}}""", thb)) // empty
        assertNull(Frankfurter.parse("""{"date":"not-a-day","rates":{"INR":2.5}}""", thb))
    }

    @Test
    fun a_zero_or_negative_rate_is_dropped_rather_than_dividing_by_zero() {
        val odd = """{"date":"2026-11-20","rates":{"INR":0,"USD":-1.5}}"""
        assertNull(Frankfurter.parse(odd, thb))
    }

    @Test
    fun a_rate_is_fresh_for_two_days_and_stale_after() {
        val day = LocalDay.of(2026, 11, 20)
        assertFalse(RatePolicy.isStale(day, day))
        assertFalse(RatePolicy.isStale(day, LocalDay.of(2026, 11, 21)))
        assertTrue(RatePolicy.isStale(day, LocalDay.of(2026, 11, 22)))
        assertTrue(RatePolicy.isStale(null, day))
        // A rate dated in the future is not trusted either — a clock can be wrong.
        assertTrue(RatePolicy.isStale(LocalDay.of(2026, 11, 25), day))
    }

    @Test
    fun one_stale_quote_is_enough_to_justify_one_request() {
        val today = LocalDay.of(2026, 11, 20)
        val fresh = mapOf(inr to today, usd to today)
        assertFalse(RatePolicy.shouldFetch(fresh, listOf(inr, usd), today))
        assertTrue(RatePolicy.shouldFetch(fresh, listOf(inr, thb), today))
        assertFalse(RatePolicy.shouldFetch(fresh, emptyList(), today))
    }
}

/**
 * The breakdown that printed "Not said" twice.
 *
 * Category totals were a SQL `GROUP BY category, currency`, so an uncategorised
 * rupee receipt and an uncategorised baht receipt were two rows with the same
 * title — and each was converted at the trip's start date while the day pages used
 * the capture's own. These tests pin the two properties that were missing: one row
 * per category, and the same money as the totals.
 */
class CategoryTotalsTest {

    private val inr = CurrencyCode.of("INR")
    private val thb = CurrencyCode.of("THB")
    private val day = LocalDay.of(2026, 11, 20)

    private val table = FxTable(listOf(Rate(thb, inr, day, 2_600_000L)))

    @Test
    fun two_uncategorised_currencies_are_one_row_not_two() {
        val rows = CategoryTotals.of(
            listOf(
                capture("a", 5_000, inr, category = null),
                capture("b", 100_000, thb, category = null),
            ),
            table,
            inr,
        )
        assertEquals(1, rows.size)
        val row = rows.single()
        assertNull(row.category)
        assertEquals(2, row.captures)
        // 50.00 INR + 1,000.00 THB × 2.60 = 2,650.00 INR
        assertEquals(265_000L, row.baseMinor)
    }

    @Test
    fun one_row_per_category_and_largest_first() {
        val rows = CategoryTotals.of(
            listOf(
                capture("a", 1_000, inr, category = Category.Food),
                capture("b", 9_000, inr, category = Category.Transport),
                capture("c", 2_000, inr, category = Category.Food),
            ),
            table,
            inr,
        )
        assertEquals(listOf(Category.Transport, Category.Food), rows.map { it.category })
        assertEquals(3_000L, rows.first { it.category == Category.Food }.baseMinor)
        assertEquals(2, rows.first { it.category == Category.Food }.captures)
    }

    @Test
    fun the_breakdown_adds_up_to_the_tally_of_the_same_captures() {
        val captures = listOf(
            capture("a", 12_345, inr, category = Category.Food),
            capture("b", 500_000, thb, category = Category.Lodging),
            capture("c", 3_000, inr, category = null),
        )
        val rows = CategoryTotals.of(captures, table, inr)
        val tally = Tally(table, inr).total(captures)
        assertEquals(tally.baseTotal.minorUnits, rows.sumOf { it.baseMinor })
    }

    @Test
    fun a_pinned_rate_is_used_here_too() {
        val moved = FxTable(
            listOf(
                Rate(thb, inr, day, 2_600_000L),
                Rate(thb, inr, LocalDay.of(2026, 11, 24), 3_100_000L),
            ),
        )
        val pinned = capture("a", 100_000, thb, category = Category.Lodging)
            .copy(rateScaled = 2_600_000L, rateDate = day)
        val rows = CategoryTotals.of(listOf(pinned), moved, inr)
        assertEquals(260_000L, rows.single().baseMinor)
    }

    @Test
    fun money_without_a_rate_is_left_out_rather_than_added() {
        val rows = CategoryTotals.of(
            listOf(
                capture("a", 5_000, inr, category = Category.Food),
                capture("b", 100_000, thb, category = Category.Food),
            ),
            FxTable(emptyList()),
            inr,
        )
        assertEquals(5_000L, rows.single().baseMinor)
        assertEquals(1, rows.single().captures)
    }

    @Test
    fun captures_with_no_amount_do_not_inflate_the_count() {
        val rows = CategoryTotals.of(
            listOf(
                capture("a", 5_000, inr, category = Category.Food),
                capture("b", null, null, category = Category.Food),
            ),
            table,
            inr,
        )
        assertEquals(1, rows.single().captures)
    }

    @Test
    fun no_finished_captures_is_an_empty_breakdown_not_a_zero_row() {
        assertTrue(CategoryTotals.of(emptyList(), table, inr).isEmpty())
    }

    private fun capture(id: String, minor: Long?, code: CurrencyCode?, category: Category?) = Capture(
        id = id,
        tripId = "trip-1",
        localDate = day,
        photoPath = "$id.jpg",
        createdAt = 0,
        state = CaptureState.CONFIRMED,
        amountMinor = minor,
        currency = code,
        category = category,
    )
}
