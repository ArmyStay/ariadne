package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.BudgetBar
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.MeasuredRule
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.Paper
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.QuietAction
import com.ariadne.design.RailFigure
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.design.Glyph
import com.ariadne.model.BudgetPlan
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money

/**
 * Trip — the journey as an object, in the bar rather than behind a link.
 *
 * The old page was a contents list for the trip (its dates, its progress, its
 * currencies) with the register and the card reachable from it. Now that the trip
 * has its own place in the bar, the page can be about one thing: what this journey
 * is, what it has cost, and how far through it you are.
 *
 * Four blocks, in the order the questions get asked:
 *
 *   1. **What it is** — the name, the place, the dates.
 *   2. **What it has cost** — spent against the budget, in the base currency, with
 *      the share of the budget drawn rather than described.
 *   3. **How far through it** — days closed of days in the trip.
 *   4. **What it was spent in** — the currencies actually touched, each with its own
 *      subtotal, and then the one figure they add up to.
 *
 * The other trips on the device appear only when there is more than one: a switcher
 * with a single row is a list of one thing you already know.
 */
@Composable
public fun TripPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean = false) {
    val trip = state.trip ?: return
    val currency = trip.baseCurrency
    val place = trip.place
    val spent = state.categoryTotals.sumOf { it.baseMinor }
    val days = state.dayCards.size
    val closed = state.dayCards.count { it.isClosed }
    // One plan, one arithmetic: the bar, the pace tick and the day's share all come
    // from the tested value type, so this page cannot round differently from Stats.
    val plan = BudgetPlan.of(
        budgetMinor = trip.budgetMinor,
        days = days,
        spentMinor = spent,
        daysClosed = closed,
    )

    TabShellPage(
        paper = paper,
        wide = wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            RailFigure("spent", Money.ofMinor(spent).formatWithSymbol(currency), paper)
            Spacer(Modifier.height(22.dp))
            RailFigure("days closed", "$closed of $days", paper)
            Spacer(Modifier.height(22.dp))
            RailFigure("since", trip.startDate.iso, paper)
        },
    ) {
        // ── 1. what it is ───────────────────────────────────────────────────
        Label("trip", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(8.dp))
        Title(trip.name, paper, size = if (wide) 44.sp else 34.sp, italic = true)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (place != null) {
                Icon(Glyph.Place, paper, size = 13.dp, color = paper.inkFaint)
                Spacer(Modifier.width(7.dp))
                Label(place, paper, color = paper.inkSoft, size = 11.sp, tracking = 0.4.sp)
                Spacer(Modifier.width(14.dp))
            }
            Label(
                "${trip.startDate.iso} — ${trip.endDate.iso} · $days days",
                paper,
                color = paper.inkFaint,
                size = 10.sp,
                tracking = 0.5.sp,
            )
        }
        if (trip.isClosed) {
            Spacer(Modifier.height(10.dp))
            Pill("closed", paper, PillTone.Quiet)
        }

        Spacer(Modifier.height(26.dp))

        // ── 2. what it has cost ─────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Numeral(
                Money.ofMinor(spent).formatWithSymbol(currency),
                paper,
                size = if (wide) 52.sp else 40.sp,
                weight = FontWeight.Medium,
            )
            Spacer(Modifier.width(10.dp))
            Label("spent", paper, color = paper.inkFaint, size = 10.sp, tracking = 1.2.sp)
        }

        if (plan.hasBudget) {
            val left = plan.leftMinor ?: 0L
            Spacer(Modifier.height(10.dp))
            Label(
                "of ${Money.ofMinor(plan.budgetMinor ?: 0L).formatWithSymbol(currency)} · " +
                    if (left >= 0) {
                        "${Money.ofMinor(left).formatWithSymbol(currency)} left"
                    } else {
                        "${Money.ofMinor(-left).formatWithSymbol(currency)} over"
                    },
                paper,
                color = if (left >= 0) paper.inkSoft else paper.blue,
                size = 11.sp,
                tracking = 0.3.sp,
            )
            Spacer(Modifier.height(8.dp))
            // The tick is the trip's own pace: how much of the trip's time has
            // passed. Being past the bar and behind the tick is the one thing a bar
            // alone cannot tell you.
            BudgetBar(
                fraction = plan.spendFraction,
                paper = paper,
                pace = plan.paceFraction,
                over = plan.isOver,
            )
            Spacer(Modifier.height(10.dp))
            if (plan.isAheadOfPace) {
                Label(
                    "ahead of the trip's own pace",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 0.6.sp,
                )
                Spacer(Modifier.height(6.dp))
            }
            QuietAction("change the budget", paper, { model.go(Route.Preferences) }, color = paper.inkFaint)
        } else {
            Spacer(Modifier.height(10.dp))
            QuietAction("set a budget", paper, { model.go(Route.Preferences) }, color = paper.inkFaint)
        }

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        // ── 3. how far through it ───────────────────────────────────────────
        Label("how far", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(8.dp))
        Title("$closed of $days days closed", paper, size = 22.sp)
        Spacer(Modifier.height(10.dp))
        MeasuredRule(closed, days, paper)
        Spacer(Modifier.height(12.dp))
        QuietAction("see the days", paper, { model.go(Route.Days) }, color = paper.inkMid)

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        // ── 4. what it was spent in ─────────────────────────────────────────
        Label("what it was spent in", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(12.dp))

        if (state.perCurrency.isEmpty()) {
            Body(
                "Nothing tallied yet. The first capture is a photograph, not a form.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
            )
        } else {
            state.perCurrency.forEach { (code, minor) ->
                CurrencyLine(code, Money.ofMinor(minor).format(CurrencyCode.of(code)), paper)
            }
            Spacer(Modifier.height(8.dp))
            Rule(paper, color = paper.edgeStrong)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label("all of it", paper, color = paper.inkSoft, size = 11.sp, tracking = 0.4.sp)
                Spacer(Modifier.weight(1f))
                Numeral(
                    Money.ofMinor(spent).formatWithSymbol(currency),
                    paper,
                    size = 17.sp,
                    weight = FontWeight.Medium,
                )
            }
            if (state.tripUnconverted.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ConversionNote(emptyMap(), state.tripUnconverted, paper)
            }
        }

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        Label("currency", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(8.dp))
        Body(
            "Base ${currency.code} · home ${trip.homeCurrency.code}. The original amount is never " +
                "converted away — it stays under the figure it became.",
            paper,
            color = paper.inkSoft,
            size = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(10.dp))
        QuietAction("the rates", paper, { model.go(Route.Preferences) }, color = paper.inkFaint)

        // ── other trips, only when there are some ───────────────────────────
        val others = state.trips.filter { it.id != trip.id }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Rule(paper)
            Spacer(Modifier.height(20.dp))
            Label("also on this device", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(6.dp))
            others.forEach { other ->
                OtherTripRow(other.name, "${other.startDate.iso} — ${other.endDate.iso}", paper) {
                    model.switchTrip(other.id)
                }
                Rule(paper)
            }
        }

        Spacer(Modifier.height(26.dp))
        if (trip.isClosed) {
            Action("The card", paper, { model.go(Route.TripCard(trip.id)) }, accent = true)
        } else {
            Action("Close the trip into a card", paper, { model.closeTripIntoCard() }, accent = true)
        }
        Spacer(Modifier.height(12.dp))
        if (others.isNotEmpty()) {
            OutlineAction("Start another trip", paper, { model.startNewTrip() })
            Spacer(Modifier.height(10.dp))
        }
        QuietAction("the register", paper, { model.go(Route.Trips) }, color = paper.inkMid)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CurrencyLine(code: String, value: String, paper: Paper) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(code, paper, color = paper.inkSoft, size = 11.sp, tracking = 0.6.sp)
        Spacer(Modifier.weight(1f))
        Numeral(value, paper, size = 15.sp)
    }
}

@Composable
private fun OtherTripRow(name: String, dates: String, paper: Paper, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .rowTap(onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Title(name, paper, size = 17.sp)
            Spacer(Modifier.height(2.dp))
            Label(dates, paper, color = paper.inkFaint, size = 9.sp, tracking = 0.5.sp)
        }
        Label("open", paper, color = paper.inkMid, size = 10.sp, tracking = 1.2.sp)
    }
}
