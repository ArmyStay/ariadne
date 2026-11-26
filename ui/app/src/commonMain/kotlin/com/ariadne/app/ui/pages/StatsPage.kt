package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.EmptyState
import com.ariadne.design.Body
import com.ariadne.design.BudgetBar
import com.ariadne.model.CategoryTotal
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.RailFigure
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money
import com.ariadne.model.dayRange

/**
 * Stats — what the trip came to, and what that means per day.
 *
 * The old page answered "what did each category take" and then, at the bottom,
 * offered to close the day — a control that belonged to another page and that made
 * the last thing on a page of figures an unrelated action.
 *
 * This page answers three questions and stops:
 *
 *   1. **What it came to** — the finished total, and the budget it is measured
 *      against, with the daily share of that budget named explicitly, because "₹3,400
 *      a day" is the number a traveller can actually act on.
 *   2. **What it was** — the category breakdown, drawn to scale rather than only
 *      tabulated.
 *   3. **Day by day** — every day of the trip as a bar, with the daily budget as a
 *      line across the chart. The shape of the trip, not a table of it.
 *
 * Nothing here is sorted for the person: a category is a claim they made on a
 * photograph, and most photographs never need one.
 */
@Composable
public fun StatsPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip
    val base = trip?.baseCurrency ?: CurrencyCode.of(state.dayCurrency)
    val total = state.categoryTotals.sumOf { it.baseMinor }
    val days = trip?.let { dayRange(it.startDate, it.endDate).size.coerceAtLeast(1) } ?: 1
    val dailyShare = trip?.budgetMinor?.let { it / days }

    TabPage(
        paper = paper,
        wide = wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            RailFigure("the trip", Money.ofMinor(total).formatWithSymbol(base), paper, 16.sp)
            Spacer(Modifier.height(22.dp))
            RailFigure("photographs", "${state.categoryTotals.sumOf { it.captures }}", paper, 20.sp)
            Spacer(Modifier.height(22.dp))
            Label("finished only", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        },
    ) {
        Label("statistics", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(8.dp))
        Title("What it came to", paper, size = if (wide) 42.sp else 32.sp, italic = true)

        Spacer(Modifier.height(22.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Numeral(
                Money.ofMinor(total).formatWithSymbol(base),
                paper,
                size = if (wide) 58.sp else 44.sp,
                weight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Label(
            "finished photographs only · anything still waiting is not counted as zero",
            paper,
            color = paper.inkFaint,
            size = 9.sp,
            tracking = 0.5.sp,
        )
        Spacer(Modifier.height(6.dp))
        ConversionNote(emptyMap(), state.tripUnconverted, paper)

        // ── the fence, and the daily share of it ─────────────────────────────
        val budget = trip?.budgetMinor
        if (budget != null) {
            Spacer(Modifier.height(20.dp))
            Rule(paper, color = paper.edgeStrong)
            Spacer(Modifier.height(14.dp))
            val left = budget - total
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Label("budget", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
                    Spacer(Modifier.height(4.dp))
                    Numeral(
                        Money.ofMinor(budget).formatWithSymbol(base),
                        paper,
                        size = 15.sp,
                        color = paper.inkSoft,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Label(
                        if (left >= 0) "left" else "over",
                        paper,
                        color = paper.inkFaint,
                        size = 9.sp,
                        tracking = 1.4.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Numeral(
                        Money.ofMinor(if (left >= 0) left else -left).formatWithSymbol(base),
                        paper,
                        size = 18.sp,
                        weight = FontWeight.Medium,
                        color = if (left >= 0) paper.ink else paper.blue,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Spent against budget, with the pace tick at where the trip's days have
            // got to — the same measure the Trip page draws, from the same code.
            BudgetBar(
                fraction = if (budget > 0) total.toFloat() / budget.toFloat() else 0f,
                paper = paper,
                pace = state.dayCards.count { it.isClosed }.toFloat() / days.toFloat(),
                over = left < 0,
            )
            if (dailyShare != null) {
                Spacer(Modifier.height(10.dp))
                Label(
                    "a day of it: ${Money.ofMinor(dailyShare).formatWithSymbol(base)} over $days days",
                    paper,
                    color = paper.inkSoft,
                    size = 10.sp,
                    tracking = 0.4.sp,
                )
            }
        }

        // ── what it was ─────────────────────────────────────────────────────
        if (state.categoryTotals.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Rule(paper)
            Spacer(Modifier.height(20.dp))
            Label("what it was", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(14.dp))
            Breakdown(state.categoryTotals, total, base, paper)
            Spacer(Modifier.height(12.dp))
            Body(
                "Nothing here is sorted for you. A category is a claim you make on the " +
                    "photograph, and most photographs never need one.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
        }

        // ── day by day ──────────────────────────────────────────────────────
        if (state.dayFigures.isNotEmpty() && trip != null) {
            val tripDays = dayRange(trip.startDate, trip.endDate)
            Spacer(Modifier.height(24.dp))
            Rule(paper)
            Spacer(Modifier.height(20.dp))
            Label("day by day", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(6.dp))
            Label(
                if (dailyShare != null) {
                    "the line is a day of the budget"
                } else {
                    "each bar is a day's finished total"
                },
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 0.5.sp,
            )
            Spacer(Modifier.height(12.dp))
            DayChart(
                days = tripDays.map { it.iso },
                figures = state.dayFigures,
                dailyShare = dailyShare,
                paper = paper,
            )
        }

        // ── before conversion ───────────────────────────────────────────────
        if (state.perCurrency.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Rule(paper)
            Spacer(Modifier.height(20.dp))
            Label(
                "by currency, before conversion",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.6.sp,
            )
            Spacer(Modifier.height(10.dp))
            state.perCurrency.forEach { (code, minor) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Label(code, paper, color = paper.inkSoft, size = 11.sp, tracking = 0.6.sp)
                    Spacer(Modifier.weight(1f))
                    Numeral(Money.ofMinor(minor).format(CurrencyCode.of(code)), paper, size = 15.sp)
                }
                Rule(paper)
            }
            Spacer(Modifier.height(8.dp))
            Label(
                "these are the amounts as they were paid, not converted",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 0.5.sp,
            )
        }

        if (state.categoryTotals.isEmpty() && state.perCurrency.isEmpty()) {
            Spacer(Modifier.height(22.dp))
            EmptyState(
                title = "Nothing to count yet.",
                body = "These figures appear the first time you put an amount on a photograph — " +
                    "and the day it was spent stays the day it counts for.",
                paper = paper,
            )
        }

        Spacer(Modifier.height(24.dp))
        QuietAction("the trip's page", paper, { model.go(Route.Trip) }, color = paper.inkMid)
        Spacer(Modifier.height(20.dp))
    }
}

/**
 * The breakdown, drawn to scale.
 *
 * A table of figures tells you the order; it does not tell you the shape. Each row
 * is a hairline track with the category's share of the total inked into it, so a trip
 * where food is two thirds reads that way at a glance. The uncategorised row is
 * always last and always named "not said", never folded into a category.
 */
@Composable
private fun Breakdown(totals: List<CategoryTotal>, total: Long, base: CurrencyCode, paper: Paper) {
    val ordered = totals.sortedByDescending { it.baseMinor }
    val biggest = ordered.maxOfOrNull { it.baseMinor }?.coerceAtLeast(1) ?: 1
    ordered.forEach { row ->
        val name = row.category?.name?.lowercase() ?: "not said"
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(name, paper, color = paper.ink, size = 12.sp, tracking = 0.3.sp)
                Spacer(Modifier.width(8.dp))
                Label("${row.captures}", paper, color = paper.inkFaint, size = 9.sp, tracking = 0.4.sp)
                Spacer(Modifier.weight(1f))
                Numeral(Money.ofMinor(row.baseMinor).formatWithSymbol(base), paper, size = 14.sp)
                Spacer(Modifier.width(8.dp))
                Numeral(
                    if (total <= 0) "0%" else "${(row.baseMinor * 100 / total)}%",
                    paper,
                    color = paper.inkFaint,
                    size = 11.sp,
                )
            }
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).background(paper.sunk)) {
                Box(
                    Modifier
                        .fillMaxWidth((row.baseMinor.toFloat() / biggest.toFloat()).coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(if (row.category == null) paper.inkFaint else paper.ink),
                )
            }
        }
    }
}

/**
 * The trip as a chart.
 *
 * One bar per day, scaled against the busiest day, with the daily budget drawn as a
 * hairline across the whole chart rather than repeated per bar — a line you can
 * follow with your eye beats a legend. Days with nothing on them are still drawn, as
 * an empty slot: a gap in a trip is information too.
 */
@Composable
private fun DayChart(
    days: List<String>,
    figures: Map<String, Long>,
    dailyShare: Long?,
    paper: Paper,
) {
    val values = days.map { figures[it] ?: 0L }
    // The chart's ceiling is the busiest day, or the daily share if that is higher:
    // a chart where every day is under the budget line would otherwise draw the line
    // off the top and say nothing.
    val ceiling = maxOf(values.maxOrNull() ?: 0L, dailyShare ?: 0L).coerceAtLeast(1L)
    val chartHeight = 92.dp
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(chartHeight),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { index, _ ->
                val value = values[index]
                val fraction = (value.toFloat() / ceiling.toFloat()).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = 1.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (fraction <= 0f) 0.02f else fraction)
                            .background(if (value == 0L) paper.edge else paper.ink),
                    )
                }
            }
        }
        if (dailyShare != null) {
            // The line sits at the height the bars would show for one budgeted day:
            // same fraction, same chart, so the comparison is one glance.
            val fraction = (dailyShare.toFloat() / ceiling.toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = chartHeight * (1f - fraction))
                    .height(1.dp)
                    .background(paper.blue),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (days.isNotEmpty()) {
            Label(days.first().takeLast(5), paper, color = paper.inkFaint, size = 9.sp, tracking = 0.4.sp)
            Label(days.last().takeLast(5), paper, color = paper.inkFaint, size = 9.sp, tracking = 0.4.sp)
        }
    }
}
