package com.ariadne.app.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.design.Body
import com.ariadne.design.Paper
import com.ariadne.model.Category
import com.ariadne.model.CategoryTotal
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money
import com.ariadne.model.isoWeekday

/**
 * Two pieces of the ledger that earn their space: the breakdown, and the sentence
 * it is allowed to say.
 *
 * Both are readings of data the app already has, not new features. Neither
 * invents a target, a streak, or a verdict — the breakdown shows *what* the money
 * was for, the sentence says which row was the largest, and both stop there. That
 * is the difference between a record and a coach.
 */

/**
 * The one sentence the breakdown is allowed to say about you.
 *
 * It states a fact about this trip's own numbers and never compares the traveller
 * to other people, to last month, or to a budget they did not set. The reference
 * screen this replaces announced "5660% more than usual"; that is a machine
 * grading a person it has never met, and it does not belong in a record.
 */
@Composable
internal fun PatternLine(totals: List<CategoryTotal>, baseCurrency: CurrencyCode, paper: Paper) {
    val top = totals.firstOrNull { it.category != null && it.baseMinor > 0 } ?: return
    val counted = totals.filter { it.category != null }
    val sum = counted.sumOf { it.baseMinor }.coerceAtLeast(1L)
    val share = (top.baseMinor * 100.0 / sum).toInt()
    if (share < 30) return

    Spacer(Modifier.height(18.dp))
    Body(
        "Most of this trip so far went on ${top.category?.label?.lowercase()} — " +
            "$share% of it, across ${top.captures} entr${if (top.captures == 1) "y" else "ies"}. " +
            "Everything else is in the column above.",
        paper,
        serif = true,
        color = paper.inkSoft,
        size = 15.sp,
        lineHeight = 23.sp,
    )
}

private fun localMonthName(day: com.ariadne.model.LocalDay): String = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)[(day.month - 1).coerceIn(0, 11)]

/** "Mon", "Tue" … — one definition for the whole package. */
internal fun weekdayName(day: com.ariadne.model.LocalDay): String = listOf(
    "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun",
)[(day.isoWeekday() - 1).coerceIn(0, 6)]

/** Every category, whether or not it has anything in it. */
internal fun Category.all(): List<Category> = Category.entries.toList()

/**
 * What a converted figure is worth trusting, printed under it.
 *
 * The brief asks for two marks wherever money has been converted: an "≈" on the
 * figure, and the date of the rate behind it. This is that sentence — and it
 * carries the third fact the old app hid, which is money it could *not* convert.
 * "฿2,555.00 not counted" is a smaller claim than a wrong total and a truer one.
 *
 * Deliberately not a `Row` of tracked capitals in the margin: the sentence has to
 * survive 200% font sizes, so it is a `Body` that wraps.
 */
@Composable
internal fun ConversionNote(
    rateDates: Map<String, String>,
    unconverted: Map<String, Long>,
    paper: Paper,
    modifier: Modifier = Modifier,
) {
    if (rateDates.isEmpty() && unconverted.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        if (rateDates.isNotEmpty()) {
            val days = rateDates.values.distinct().sorted()
            Body(
                if (days.size == 1) "≈ converted at the rate for ${spokenDay(days.first())}" else
                    "≈ converted at the rates in force ${spokenDay(days.first())} – ${spokenDay(days.last())}",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
        }
        unconverted.forEach { (code, minor) ->
            val currency = CurrencyCode.parseOrNull(code)
            if (currency != null) {
                Spacer(Modifier.height(4.dp))
                Body(
                    // The money is named in its own currency and never added to the
                    // total above it. That addition was the ₹2,605 bug.
                    "${Money.ofMinor(minor).formatWithSymbol(currency)} not counted — no ${currency.code} rate yet",
                    paper,
                    color = paper.inkFaint,
                    size = 12.sp,
                )
            }
        }
    }
}

/** "20 Nov" from an ISO day, for a sentence rather than a column. */
internal fun spokenDay(iso: String): String {
    val day = com.ariadne.model.LocalDay.parseOrNull(iso) ?: return iso
    return "${day.day} ${localMonthName(day)}"
}
