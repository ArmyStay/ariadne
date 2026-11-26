package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.Paper
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CategoryTotal
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DaySummary
import com.ariadne.model.Money
import com.ariadne.model.isoWeekday
import kotlin.math.abs

/**
 * Two pieces of the ledger that earn their space: the day band and the
 * breakdown.
 *
 * Both are readings of data the app already has, not new features. Neither
 * invents a target, a streak, or a verdict — the band shows *when* the day
 * happened, the breakdown shows *what* it was, and both stop there. That is the
 * difference between a record and a coach.
 */

/**
 * The day as a run of hours.
 *
 * Each capture is a mark at the hour it was taken. Recorded ones are filled ink,
 * still-open ones are outlined, so the shape of the day is legible at a glance
 * without reading a single figure. Empty hours stay empty: an hour with no
 * photograph is not a gap to be ashamed of, it is an hour you were living.
 *
 * Honest about its own resolution: `createdAt` is an instant, and the hour it is
 * drawn at comes from the [Clock] in the device's zone — never from the instant's
 * own remainder, which is UTC and wrong everywhere else. Nothing is interpolated
 * between two marks, and marks that share an hour are stacked rather than hidden.
 *
 * The clock is passed in as `hourOf` rather than looked up here, so a preview or a
 * test can pin the zone by pinning the clock.
 */
@Composable
internal fun DayBand(
    captures: List<Capture>,
    paper: Paper,
    hourOf: (Long) -> Int,
    modifier: Modifier = Modifier,
) {
    val line = paper.edgeStrong
    val ink = paper.ink
    val open = paper.blue

    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(54.dp)) {
            val left = 0f
            val right = size.width
            // The 24-hour rail.
            drawLine(
                color = line,
                start = Offset(left, size.height),
                end = Offset(right, size.height),
                strokeWidth = 1f,
            )
            // Hours that carry something get a tick; the marks sit on the rail.
            // Two photographs in one hour land on the same x, so each further
            // mark in that hour is lifted one step: a collision is visible
            // instead of one dot hiding another.
            val taken = captures.groupBy { hourOf(it.createdAt).coerceIn(0, 23) }
            taken.keys.sorted().forEach { hour ->
                taken.getValue(hour).forEachIndexed { index, capture ->
                    val x = left + (right - left) * (hour / 23f)
                    val confirmed = capture.state.name == "CONFIRMED" || capture.state.name == "ARCHIVED"
                    val markColor: Color = if (confirmed) ink else open
                    val top = size.height - 30f - index.coerceAtMost(3) * 7f
                    if (!confirmed) {
                        // Outlined: a hairline rectangle rather than a filled dot.
                        drawCircle(
                            color = markColor,
                            radius = 4.2f,
                            center = Offset(x, top),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f),
                        )
                    } else {
                        drawCircle(color = markColor, radius = 3.4f, center = Offset(x, top))
                    }
                    drawLine(
                        color = if (confirmed) ink else open.copy(alpha = 0.55f),
                        start = Offset(x, top + 6f),
                        end = Offset(x, size.height - 1f),
                        strokeWidth = if (confirmed) 1.4f else 1f,
                        cap = StrokeCap.Butt,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Only the edges are numbered; the middle would be noise.
            listOf("00", "06", "12", "18", "23").forEach { hour ->
                Label(hour, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Legend(ink, "Recorded", paper)
            Legend(open, "Still open", paper)
        }
    }
}

@Composable
private fun Legend(color: Color, text: String, paper: Paper) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(6.dp).height(6.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(Modifier.width(6.dp))
        Label(text, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.2.sp)
    }
}


/**
 * The breakdown: what the money was for, in the trip's base currency.
 *
 * Rendered as a ruled column rather than the usual donut. A list can be read to
 * the rupee; a donut can only be glanced at, and this is a ledger. The bar beside
 * each figure is proportional to the largest row, so the shape is still visible
 * without pretending to be a chart.
 */
@Composable
internal fun Breakdown(
    totals: List<CategoryTotal>,
    baseCurrency: CurrencyCode,
    totalBaseMinor: Long,
    paper: Paper,
    modifier: Modifier = Modifier,
) {
    val largest = totals.maxOfOrNull { abs(it.baseMinor) }?.coerceAtLeast(1L) ?: 1L

    Column(modifier.fillMaxWidth()) {
        totals.forEachIndexed { index, row ->
            val proportion = (abs(row.baseMinor).toDouble() / largest.toDouble()).toFloat().coerceIn(0.02f, 1f)
            Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Title(
                            row.category?.label ?: "Uncategorised",
                            paper,
                            size = 17.sp,
                            italic = row.category == null,
                            color = if (row.category == null) paper.inkSoft else paper.ink,
                        )
                        Spacer(Modifier.height(3.dp))
                        Label(
                            "${row.captures} photograph${if (row.captures == 1) "" else "s"}",
                            paper,
                            color = paper.inkFaint,
                            tracking = 1.2.sp,
                            size = 9.sp,
                        )
                    }
                    Numeral(
                        Money.ofMinor(row.baseMinor).formatWithSymbol(baseCurrency),
                        paper,
                        size = 16.sp,
                        weight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(10.dp))
                // The bar is drawn in a wash, never a colour: the page has one
                // chromatic value and it belongs to the actions.
                Box(Modifier.fillMaxWidth().height(3.dp).background(paper.sunk)) {
                    Box(
                        Modifier
                            .fillMaxWidth(proportion)
                            .height(3.dp)
                            .background(if (row.category == null) paper.inkGhost else paper.ink),
                    )
                }
            }
            if (index < totals.lastIndex) Rule(paper)
        }

        if (totals.isNotEmpty() && totalBaseMinor > 0) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label("of", paper, color = paper.inkSoft)
                Numeral(
                    Money.ofMinor(totalBaseMinor).formatWithSymbol(baseCurrency),
                    paper,
                    size = 13.sp,
                )
            }
        }
    }
}

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
            "$share% of it, across ${top.captures} photograph${if (top.captures == 1) "" else "s"}. " +
            "Everything else is in the column above.",
        paper,
        serif = true,
        color = paper.inkSoft,
        size = 15.sp,
        lineHeight = 23.sp,
    )
}

/**
 * A day in the timeline.
 *
 * The date hangs in its own narrow column — day number set large, month and
 * weekday tracked underneath — and the band sits in the column beside it. That
 * left-hand date column is what makes a run of days read as a ledger spine; the
 * same content stacked in one column reads as a feed, which is the shape every
 * generic app has.
 */
@Composable
internal fun DayPlate(
    day: com.ariadne.model.LocalDay,
    closed: Boolean,
    selected: Boolean,
    captures: List<Capture>,
    figure: String,
    paper: Paper,
    hourOf: (Long) -> Int,
    onClick: () -> Unit,
) {
    val summary = DaySummary.of(captures)
    Row(
        Modifier
            .fillMaxWidth()
            .pointerInput(day) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // The spine. A minimum, not a width: at 200% text the day number is
        // twice as wide, and the spine grows instead of slicing the numeral.
        Column(Modifier.widthIn(min = 64.dp)) {
            Numeral(
                day.iso.takeLast(2),
                paper,
                size = 30.sp,
                weight = FontWeight.Medium,
                color = if (selected) paper.ink else paper.inkMid,
            )
            Spacer(Modifier.height(2.dp))
            Label(localMonthName(day), paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
            Label(weekdayName(day), paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
        }

        Spacer(Modifier.width(18.dp))

        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(
                    // "Still open" was a page-level guess. The day now says what it
                    // actually owes, counted by the same rule Home counts with.
                    when {
                        closed -> "in the record"
                        summary.isSettled -> "settled"
                        summary.pending == 1 -> "1 to settle"
                        else -> "${summary.pending} to settle"
                    },
                    paper,
                    color = if (closed) paper.inkFaint else paper.blue,
                    tracking = 1.3.sp,
                    size = 9.sp,
                )
                Spacer(Modifier.weight(1f))
                if (captures.isNotEmpty()) {
                    Numeral(figure, paper, size = 15.sp, weight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (captures.isNotEmpty()) {
                DayBand(captures, paper, hourOf)
            } else {
                Box(Modifier.fillMaxWidth().height(Bone.Hairline).background(paper.edge))
                Spacer(Modifier.height(9.dp))
                Label("nothing kept", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            }
        }
    }
    Rule(paper)
}

private fun localMonthName(day: com.ariadne.model.LocalDay): String = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)[(day.month - 1).coerceIn(0, 11)]

private fun weekdayName(day: com.ariadne.model.LocalDay): String = listOf(
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
