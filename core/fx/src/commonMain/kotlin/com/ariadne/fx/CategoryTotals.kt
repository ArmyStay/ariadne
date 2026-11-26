package com.ariadne.fx

import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CategoryTotal
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money

/**
 * What the money was for, converted by the same rules as everything else.
 *
 * The breakdown used to be a `GROUP BY category, currency` in SQL — which produced
 * one row per currency for the same category, so an uncategorised rupee and an
 * uncategorised baht printed as two separate "Not said" lines, and the conversion
 * above them used the trip's start date while the day pages used the capture's own.
 * Stats therefore disagreed with Home about the same trip, in both directions: the
 * rows were duplicated *and* the figures were converted differently.
 *
 * So the aggregation happens here, in Kotlin, over the same captures the totals are
 * computed from, with the same [Tally] rules: one row per category, no category
 * twice, snapshot-first conversion, and money without a rate left out of the figure
 * rather than added to it.
 */
public object CategoryTotals {

    /**
     * One row per category — including a single row for everything with no category
     * — largest first.
     *
     * [captures] is expected to be the finished ones (the caller filters by state,
     * exactly as it does for the trip total); anything without an amount is skipped
     * because it contributes no money, and counting it would inflate the "n
     * photographs" beside each row.
     */
    public fun of(
        captures: List<Capture>,
        fx: FxTable,
        baseCurrency: CurrencyCode,
    ): List<CategoryTotal> {
        val sums = linkedMapOf<Category?, Long>()
        val counts = mutableMapOf<Category?, Int>()
        for (capture in captures) {
            val minor = capture.amountMinor ?: continue
            val code = capture.currency ?: continue
            val converted = convert(capture, minor, code, fx, baseCurrency) ?: continue
            val key = capture.category
            sums[key] = (sums[key] ?: 0L) + converted
            counts[key] = (counts[key] ?: 0) + 1
        }
        return sums
            .map { (category, minor) -> CategoryTotal(category = category, captures = counts[category] ?: 0, baseMinor = minor) }
            .sortedWith(compareByDescending<CategoryTotal> { it.baseMinor }.thenBy { it.category?.name ?: "\uFFFF" })
    }

    /**
     * One capture's amount in the base currency, or null when it cannot be converted.
     *
     * A base-currency amount needs no rate. Anything else uses the rate pinned to the
     * row, falling back to the table on that capture's own day — never the trip's
     * start date, and never the day a page happens to be showing.
     */
    private fun convert(
        capture: Capture,
        minor: Long,
        code: CurrencyCode,
        fx: FxTable,
        baseCurrency: CurrencyCode,
    ): Long? {
        if (code == baseCurrency) return minor
        val amount = Money.ofMinor(minor)
        val snapshot = capture.rateScaled
        val conversion = if (snapshot != null) {
            fx.convertAt(amount, code, baseCurrency, snapshot)
        } else {
            fx.convert(amount, code, baseCurrency, capture.localDate)
        }
        return if (conversion.applied) conversion.amount.minorUnits else null
    }
}
