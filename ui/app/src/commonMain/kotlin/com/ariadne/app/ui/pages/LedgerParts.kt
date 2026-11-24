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
import com.ariadne.model.Money
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
 * Honest about its own resolution: `createdAt` is an instant, converted to the
 * hour of the day it happened, so no timezone is invented and nothing is
 * interpolated between two marks.
 */
@Composable
internal fun DayBand(
    captures: List<Capture>,
    paper: Paper,
    modifier: Modifier = Modifier,
    onHourTap: ((Int) -> Unit)? = null,
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
            captures.forEach { capture ->
                val hour = hourOf(capture.createdAt)
                val x = left + (right - left) * (hour.coerceIn(0, 23) / 23f)
                val confirmed = capture.state.name == "CONFIRMED" || capture.state.name == "ARCHIVED"
                val markColor: Color = if (confirmed) ink else open
                val top = size.height - 30f
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

/** Hour of the day from an epoch instant, in the device's zone, without a formatter. */
private fun hourOf(epochMillis: Long): Int {
    val hours = ((epochMillis / 3_600_000L) % 24L)
    return hours.toInt().let { if (it < 0) it + 24 else it }
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
                            row.category?.label ?: "Not said",
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

/** A day card in the timeline: the date, the day's figure, and its band. */
@Composable
internal fun DayPlate(
    day: com.ariadne.model.LocalDay,
    closed: Boolean,
    selected: Boolean,
    captures: List<Capture>,
    figure: String,
    paper: Paper,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .pointerInput(day) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Label(if (closed) "in the record" else "still open", paper, color = paper.inkFaint, tracking = 1.2.sp)
                Spacer(Modifier.height(4.dp))
                Title(day.iso, paper, size = 20.sp, color = if (selected) paper.ink else paper.inkMid)
            }
            Numeral(figure, paper, size = 17.sp, weight = FontWeight.Medium)
        }
        if (captures.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(Bone.Hairline, paper.edge)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                DayBand(captures, paper)
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Body("No photographs on this day.", paper, color = paper.inkFaint, size = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Rule(paper)
    }
}

/** Every category, whether or not it has anything in it. */
internal fun Category.all(): List<Category> = Category.entries.toList()
