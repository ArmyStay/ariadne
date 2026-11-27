package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Bone
import com.ariadne.design.Body
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.LinkAction
import com.ariadne.design.Masthead
import com.ariadne.design.Numeral
import com.ariadne.design.Paper
import com.ariadne.design.Rule
import com.ariadne.design.SectionHead
import com.ariadne.design.Title
import com.ariadne.design.animatedLong
import com.ariadne.design.tap
import com.ariadne.model.CategoryTotal
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.symbol
import kotlin.math.abs

/**
 * What the trip came to, on cards.
 *
 * Four readings of the same finished entries, in the order a person asks for them:
 * the total and its fence, how it arrived day by day, what it was for, and what it
 * was paid in. Each is a card with a disc, a tracked label and one figure — the
 * page a traveller photographs from across a table, not a dashboard to operate.
 *
 * Three rules the cards keep, because the page is a ledger and not a coach:
 *
 * - **Only finished entries are counted**, and the qualifier sits inside the card
 *   that owns the figure ("finished entries only"), not three sections below it.
 * - **The chart is drawn from this trip's days**, one bar per day, scaled to the
 *   largest of them. No illustrative series, no comparison to other people.
 * - **Nothing is invented.** No budget, no rate, no currency, no category is shown
 *   that the record does not have; what is absent says so quietly.
 */
@Composable
public fun StatsPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    TabPage(
        paper,
        wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            RailFigure(
                "the trip",
                Money.ofMinor(state.tripTotalMinor)
                    .formatWithSymbol(state.trip?.baseCurrency ?: CurrencyCode.of(state.dayCurrency)),
                paper,
                16.sp,
            )
            Spacer(Modifier.height(26.dp))
            RailFigure("entries", "${state.categoryTotals.sumOf { it.captures }}", paper, 26.sp)
            Spacer(Modifier.height(26.dp))
            Label("finished only", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        },
    ) {
        // The trip's figure, not the open day's: this page is about the whole thing,
        // and a headline that disagreed with the breakdown below it would be a bug
        // wearing a design.
        val base = state.trip?.baseCurrency ?: CurrencyCode.of(state.dayCurrency)
        val tripTotal = state.tripTotalMinor

        Masthead(
            number = "03",
            eyebrow = "Stats",
            title = "Your travel stats",
            paper = paper,
            italic = true,
            wide = wide,
            lede = "A quick look at this trip's spending, and what it went on.",
        )

        Spacer(Modifier.height(if (wide) 26.dp else 20.dp))
        TripTotalCard(state, base, tripTotal, paper, wide)

        if (state.dayFigures.isNotEmpty()) {
            SectionHead("II", "daily spending", paper)
            Spacer(Modifier.height(14.dp))
            SpendCard(state, base, paper, wide)
        }

        if (state.categoryTotals.isNotEmpty()) {
            SectionHead("III", "what it was", paper)
            Spacer(Modifier.height(14.dp))
            CategoryCard(state.categoryTotals, base, tripTotal, paper)
            PatternLine(state.categoryTotals, base, paper)
        }

        if (state.tripPerCurrency.isNotEmpty()) {
            // The same finished photographs, seen from the other side. The two
            // columns must always add to the same figure.
            SectionHead("IV", "by currency, before conversion", paper)
            Spacer(Modifier.height(14.dp))
            CurrencyCard(state.tripPerCurrency, paper)
        } else if (state.categoryTotals.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            Body(
                "Nothing has been tallied yet. The figures appear here the first time you confirm an entry.",
                paper,
                color = paper.inkSoft,
                size = 15.sp,
                lineHeight = 22.sp,
            )
        }

        Spacer(Modifier.height(26.dp))
        // Nothing to settle on a day already filed: the bar goes inert rather than
        // opening a ritual with an empty stack.
        val todayClosed = state.dayCards.firstOrNull { it.localDate == state.today }?.isClosed == true
        val tripClosed = state.trip?.isClosed == true
        Action(
            "Close today",
            paper,
            { model.openCloseDay(state.today) },
            accent = true,
            enabled = state.stack.isNotEmpty() && !todayClosed && !tripClosed,
            corner = 12.dp,
        )

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinkAction("Privacy policy", paper) { model.go(Route.PrivacyPolicy) }
            Box(
                Modifier
                    .padding(horizontal = 12.dp)
                    .width(Bone.Hairline)
                    .height(12.dp)
                    .background(paper.edgeStrong),
            )
            LinkAction("Terms of use", paper) { model.go(Route.Terms) }
        }

        Spacer(Modifier.height(96.dp))
    }
}

// ── I. the trip so far ─────────────────────────────────────────────────────

@Composable
private fun TripTotalCard(state: AppUiState, base: CurrencyCode, total: Long, paper: Paper, wide: Boolean) {
    StatCard(paper) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DiscBadge(paper, glyph = Glyph.Trip)
                    Spacer(Modifier.width(10.dp))
                    Label("the trip so far", paper, color = paper.inkMid, size = 9.sp, tracking = 1.3.sp)
                }
                Spacer(Modifier.height(if (wide) 18.dp else 14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Numeral(
                        base.symbol(),
                        paper,
                        color = paper.inkFaint,
                        size = if (wide) 24.sp else 18.sp,
                    )
                    Spacer(Modifier.width(4.dp))
                    // Counts to the new total rather than snapping: this figure is
                    // read against a fence, and seeing which way it moved is the point.
                    Numeral(
                        Money.ofMinor(animatedLong(total)).format(base),
                        paper,
                        size = if (wide) 48.sp else 36.sp,
                        weight = FontWeight.Medium,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Label("finished entries only", paper, color = paper.inkFaint, size = 8.sp, tracking = 1.1.sp)
                val budget = state.trip?.budgetMinor
                if (budget != null) {
                    val remaining = budget - total
                    Spacer(Modifier.height(if (wide) 14.dp else 10.dp))
                    Label("against a budget of", paper, color = paper.inkFaint, size = 8.sp, tracking = 1.1.sp)
                    Spacer(Modifier.height(3.dp))
                    // The fence and which side of it, in one line: the number under
                    // it is the distance, and it never wears red.
                    Label(
                        Money.ofMinor(budget).formatWithSymbol(base) + if (remaining >= 0) " left" else " over",
                        paper,
                        color = paper.inkMid,
                        size = 9.sp,
                        tracking = 1.0.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Numeral(
                        Money.ofMinor(abs(remaining)).format(base),
                        paper,
                        color = paper.blue,
                        size = if (wide) 20.sp else 17.sp,
                        weight = FontWeight.Medium,
                    )
                } else {
                    Spacer(Modifier.height(if (wide) 14.dp else 10.dp))
                    Label("no budget set", paper, color = paper.inkFaint, size = 8.sp, tracking = 1.1.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Rule(paper, color = paper.edge)
        Spacer(Modifier.height(12.dp))
        InfoNote(
            "Only entries that were finished. Anything still waiting across this trip is not in " +
                "this figure, and is not counted as zero.",
            paper,
        )
    }
}

// ── II. daily spending ─────────────────────────────────────────────────────

@Composable
private fun SpendCard(state: AppUiState, base: CurrencyCode, paper: Paper, wide: Boolean) {
    // The whole trip by default — this page answers "what did it come to", and a
    // window that hid the early days would answer a different question. The pill
    // offers the shorter reading rather than imposing it.
    val lastSeven = remember { mutableStateOf(false) }
    val days: List<LocalDay> = state.dayCards.map { it.localDate }
    val shown = if (lastSeven.value) days.takeLast(7) else days
    val values = shown.map { day -> day to (state.dayFigures[day.iso] ?: 0L) }

    StatCard(paper) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DiscBadge(paper, glyph = Glyph.Stats)
            Spacer(Modifier.width(10.dp))
            Label("daily spending", paper, color = paper.inkMid, size = 9.sp, tracking = 1.3.sp)
            Spacer(Modifier.weight(1f))
            RangePill(paper, lastSeven.value) { lastSeven.value = !lastSeven.value }
        }
        Spacer(Modifier.height(18.dp))
        SpendChart(values, base, paper)
        Spacer(Modifier.height(14.dp))
        Rule(paper, color = paper.edge)
        Spacer(Modifier.height(12.dp))
        InfoNote(
            "One bar per day of the trip, in the base currency. A day with nothing finished " +
                "is a day with no bar, not a day with a zero.",
            paper,
        )
    }
}

/** A pill that says which slice of the trip the chart is drawn from, and changes it. */
@Composable
private fun RangePill(paper: Paper, lastSeven: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .tap(if (lastSeven) "Draw the chart from the whole trip" else "Draw the chart from the last seven days") {
                onToggle()
            }
            .clip(shape)
            .border(Bone.Hairline, paper.edgeStrong, shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(
            if (lastSeven) "last 7 days" else "this trip",
            paper,
            color = paper.inkMid,
            size = 9.sp,
            tracking = 1.0.sp,
        )
        Spacer(Modifier.width(6.dp))
        Icon(Glyph.Chevron, paper, size = 11.dp, color = paper.inkFaint)
    }
}

@Composable
private fun SpendChart(values: List<Pair<LocalDay, Long>>, base: CurrencyCode, paper: Paper) {
    if (values.isEmpty()) return
    val maxMinor = values.maxOfOrNull { it.second } ?: 0L
    val step = niceStep(((maxMinor + 3) / 4).coerceAtLeast(1))
    val top = step * 4
    val ticks = (0..4).map { it * step }
    val peak = values.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    val chartHeight = if (values.size > 14) 116.dp else 132.dp

    Row(Modifier.fillMaxWidth()) {
        // The money, on the left, read top-down like every other figure in the app.
        Column(
            Modifier.width(40.dp).height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            ticks.reversed().forEach { tick ->
                Label(axisLabel(tick, base), paper, color = paper.inkFaint, size = 8.sp, tracking = 0.4.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().height(chartHeight)) {
                Plot(values, top, paper, Modifier.fillMaxSize())
                if (peak != null) {
                    // The largest day, named. A chart without one number on it makes
                    // the reader estimate, and this page exists to avoid estimating.
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val slot = maxWidth / values.size
                        val centre = slot * (values.indexOf(peak) + 0.5f)
                        val barTop = chartHeight * (1f - (peak.second.toDouble() / top).toFloat())
                        val bubbleWidth = 104.dp
                        Box(
                            Modifier
                                .offset(
                                    x = (centre - bubbleWidth / 2).coerceIn(0.dp, (maxWidth - bubbleWidth).coerceAtLeast(0.dp)),
                                    y = (barTop - 32.dp).coerceIn(0.dp, chartHeight - 26.dp),
                                )
                                .widthIn(min = bubbleWidth)
                                .background(paper.raised, RoundedCornerShape(9.dp))
                                .border(Bone.Hairline, paper.edge, RoundedCornerShape(9.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Numeral(
                                Money.ofMinor(peak.second).formatWithSymbol(base),
                                paper,
                                color = paper.blue,
                                size = 10.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                values.forEach { (day, _) ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Label(day.iso.takeLast(2), paper, color = paper.inkFaint, size = 8.sp, tracking = 0.4.sp)
                    }
                }
            }
        }
    }
}

/** Gridlines and bars. No fills, no legend, no axes drawn as furniture. */
@Composable
private fun Plot(values: List<Pair<LocalDay, Long>>, top: Long, paper: Paper, modifier: Modifier) {
    Canvas(modifier) {
        val height = size.height
        val width = size.width
        if (values.isEmpty()) return@Canvas

        // Four ruled intervals, the same four the labels name.
        for (i in 1..3) {
            val y = height * i / 4f
            drawLine(
                color = paper.edge.copy(alpha = 0.7f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx() * 0.75f,
            )
        }
        drawLine(
            color = paper.edgeStrong,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1.dp.toPx(),
        )

        val slot = width / values.size
        val barWidth = (slot * 0.42f).coerceIn(5.dp.toPx(), 18.dp.toPx())
        values.forEachIndexed { index, (_, value) ->
            if (value <= 0L) return@forEachIndexed
            val barHeight = (value.toDouble() / top.toDouble()).toFloat() * height
            drawRoundRect(
                color = paper.navy,
                topLeft = Offset(slot * index + (slot - barWidth) / 2f, height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            )
        }
    }
}

/**
 * 0 → "0", 400 → "400", 25 000 → "25k", 1 00 000 → "1L".
 *
 * The axis names money in the brief's own shorthand, because an axis is read at a
 * glance and "1,00,000.00" four times is not.
 */
private fun axisLabel(minor: Long, base: CurrencyCode): String {
    val whole = Money.ofMinor(minor).format(base, grouping = false).substringBefore('.').toLongOrNull() ?: 0L
    return when {
        whole >= 100_000L && whole % 100_000L == 0L -> "${whole / 100_000L}L"
        whole >= 1_000L -> {
            val thousands = whole / 1_000L
            val hundreds = (whole % 1_000L) / 100L
            if (hundreds == 0L) "${thousands}k" else "$thousands.${hundreds}k"
        }
        else -> whole.toString()
    }
}

/** A step that is 1, 2 or 5 times a power of ten — the widths an axis divides by. */
private fun niceStep(raw: Long): Long {
    if (raw <= 1L) return 1L
    var magnitude = 1L
    while (magnitude * 10L <= raw) magnitude *= 10L
    val n = raw / magnitude
    val stepped = when {
        n <= 1L -> 1L
        n <= 2L -> 2L
        n <= 5L -> 5L
        else -> 10L
    }
    return magnitude * stepped
}

// ── III. what it was ───────────────────────────────────────────────────────

@Composable
private fun CategoryCard(totals: List<CategoryTotal>, base: CurrencyCode, totalBaseMinor: Long, paper: Paper) {
    StatCard(paper) {
        totals.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    // A category you chose is set straight; the column that holds
                    // the rest is italic, because it is the absence of a claim.
                    Title(
                        row.category?.label ?: "Uncategorised",
                        paper,
                        size = 19.sp,
                        italic = row.category == null,
                        color = if (row.category == null) paper.inkSoft else paper.ink,
                    )
                    Spacer(Modifier.height(3.dp))
                    Label(
                        "${row.captures} entr${if (row.captures == 1) "y" else "ies"}",
                        paper,
                        color = paper.inkFaint,
                        size = 8.sp,
                        tracking = 1.2.sp,
                    )
                }
                Numeral(
                    Money.ofMinor(row.baseMinor).formatWithSymbol(base),
                    paper,
                    size = 17.sp,
                    weight = FontWeight.Medium,
                )
            }
            Rule(paper, color = paper.edge)
        }

        if (totals.isNotEmpty() && totalBaseMinor > 0) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label("of", paper, color = paper.inkSoft, size = 9.sp, tracking = 1.3.sp)
                Numeral(
                    Money.ofMinor(totalBaseMinor).formatWithSymbol(base),
                    paper,
                    size = 15.sp,
                    weight = FontWeight.Medium,
                )
            }
            Rule(paper, color = paper.edge)
        }

        Spacer(Modifier.height(12.dp))
        InfoNote(
            "Nothing here is sorted for you. A category is a claim you make on an entry, " +
                "and most entries never need one.",
            paper,
        )
    }
}

// ── IV. by currency ────────────────────────────────────────────────────────

@Composable
private fun CurrencyCard(perCurrency: Map<String, Long>, paper: Paper) {
    val entries = perCurrency.entries.toList()
    StatCard(paper) {
        entries.forEachIndexed { index, (code, minor) ->
            val currency = CurrencyCode.of(code)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiscBadge(paper, text = currency.symbol())
                Spacer(Modifier.width(12.dp))
                Title(code, paper, size = 17.sp)
                Spacer(Modifier.weight(1f))
                Numeral(
                    Money.ofMinor(minor).formatWithSymbol(currency),
                    paper,
                    size = 17.sp,
                    weight = FontWeight.Medium,
                )
            }
            if (index < entries.lastIndex) Rule(paper, color = paper.edge)
        }
        Spacer(Modifier.height(12.dp))
        InfoNote("These are the amounts as they were paid, not converted.", paper)
    }
}

// ── the card itself ────────────────────────────────────────────────────────

private val CardRadius: Dp = 18.dp

/**
 * A white card on the paper: a hairline, a small lift, 18dp corners.
 *
 * The page before this drew its figures straight onto the paper, which was right
 * for a book of plates and wrong for a page read at arm's length. What has not
 * changed: the card is a *container*, not a material — no fill colour, no tonal
 * wash, no elevation that pretends to be a sheet of glass.
 */
@Composable
private fun StatCard(paper: Paper, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(CardRadius)
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (paper.isNight) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = paper.ink.copy(alpha = 0.05f),
                        spotColor = paper.ink.copy(alpha = 0.08f),
                    )
                }
            )
            .clip(shape)
            .background(paper.raised)
            .border(Bone.Hairline, paper.edge, shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        content = content,
    )
}

/** A disc of wash with a glyph or a code inside it: the card's one mark. */
@Composable
private fun DiscBadge(paper: Paper, glyph: Glyph? = null, text: String? = null) {
    Box(
        Modifier
            .size(34.dp)
            .background(paper.blueWash, CircleShape)
            .border(Bone.Hairline, paper.blueEdge, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            Icon(glyph, paper, size = 16.dp, color = paper.blue)
        } else {
            Numeral(text.orEmpty(), paper, color = paper.blue, size = if (text.orEmpty().length > 1) 11.sp else 14.sp, weight = FontWeight.Medium)
        }
    }
}

/** The card's footnote: an i in a ring, and a sentence that qualifies the figure. */
@Composable
private fun InfoNote(text: String, paper: Paper) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(16.dp).border(Bone.Hairline, paper.edgeStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Numeral("i", paper, color = paper.inkFaint, size = 9.sp)
        }
        Spacer(Modifier.width(10.dp))
        Body(
            text,
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
