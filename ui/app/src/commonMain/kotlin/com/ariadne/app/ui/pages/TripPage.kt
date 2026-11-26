package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.FigureRow
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.Masthead
import com.ariadne.design.MeasuredRule
import com.ariadne.design.PageAction
import com.ariadne.design.ScrollingMargin
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money

/**
 * Page 3 — Trip.
 *
 * The trip as an object rather than a list: dates, the running state of the days,
 * the currencies actually touched, and the one action that closes it. This is
 * where "the trip is the container" becomes visible in one screen.
 */
@Composable
public fun TripPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean = false) {
    val trip = state.trip ?: return
    val closed = state.dayCards.count { it.isClosed }

    ScrollingMargin(paper, wide, vertical = 26.dp) { _ ->
        Masthead(
            number = "09",
            eyebrow = "Trip",
            title = trip.name,
            paper = paper,
            trailing = { PageAction("Back", paper, { model.go(Route.Home) }) },
        )

        Spacer(Modifier.height(22.dp))

        // Dates, set like a title page: ruled column, not a table.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DateBlock("From", trip.startDate.iso, paper)
            DateBlock("To", trip.endDate.iso, paper)
            DateBlock("Days", state.dayCards.size.toString(), paper, alignEnd = true)
        }

        if (trip.place != null || trip.budgetMinor != null) {
            Spacer(Modifier.height(20.dp))
            Rule(paper, color = paper.edge)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Glyph.Place, paper, size = 15.dp, color = paper.inkFaint)
                    Spacer(Modifier.width(9.dp))
                    Label(trip.place ?: "no place set", paper, color = paper.inkSoft, tracking = 1.3.sp)
                }
                trip.budgetMinor?.let { budget ->
                    val spent = state.categoryTotals.sumOf { it.baseMinor }
                    val left = budget - spent
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Glyph.Budget, paper, size = 15.dp, color = paper.inkFaint)
                        Spacer(Modifier.width(9.dp))
                        Label("fence", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
                        Spacer(Modifier.width(8.dp))
                        Numeral(
                            Money.ofMinor(budget).formatWithSymbol(trip.baseCurrency),
                            paper,
                            size = 13.sp,
                            color = paper.inkSoft,
                        )
                        Spacer(Modifier.width(10.dp))
                        Label(
                            if (left >= 0) "${Money.ofMinor(left).formatWithSymbol(trip.baseCurrency)} left"
                            else "${Money.ofMinor(-left).formatWithSymbol(trip.baseCurrency)} over",
                            paper,
                            color = paper.inkFaint,
                            size = 9.sp,
                            tracking = 1.2.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(18.dp))

        Label("Progress", paper, color = paper.inkFaint)
        Spacer(Modifier.height(10.dp))
        Title("$closed of ${state.dayCards.size} days closed", paper, size = 22.sp)
        Spacer(Modifier.height(8.dp))
        MeasuredRule(closed, state.dayCards.size, paper)

        Spacer(Modifier.height(26.dp))
        Label("So far", paper, color = paper.inkFaint)
        Spacer(Modifier.height(10.dp))

        if (state.perCurrency.isEmpty()) {
            Body("Nothing tallied yet. The first capture is a photograph, not a form.", paper, serif = true, color = paper.inkSoft)
        } else {
            state.perCurrency.forEach { (code, minor) ->
                FigureRow(code, Money.ofMinor(minor).format(CurrencyCode.of(code)), paper)
            }
            Spacer(Modifier.height(6.dp))
            Rule(paper)
            FigureRow(
                "In ${trip.baseCurrency.code}",
                Money.ofMinor(state.dayTotalMinor).formatWithSymbol(trip.baseCurrency),
                paper,
                emphasised = true,
            )
        }

        Spacer(Modifier.height(28.dp))
        Label("Currency", paper, color = paper.inkFaint)
        Spacer(Modifier.height(10.dp))
        Body(
            "Base ${trip.baseCurrency.code} · home ${trip.homeCurrency.code}. The original amount is " +
                "never converted away — it stays under the figure it became.",
            paper,
            color = paper.inkSoft,
        )

        Spacer(Modifier.height(30.dp))
        if (trip.isClosed) {
            // A closed trip is not closed twice: the card already exists.
            Action("See the card", paper, { model.go(Route.TripCard(trip.id)) }, accent = true)
        } else {
            Action("Close the trip into a card", paper, { model.closeTripIntoCard() }, accent = true)
        }
        Spacer(Modifier.height(12.dp))
        QuietAction("Every day of it", paper, { model.go(Route.Record(state.today)) }, color = paper.inkMid)
    }
}

@Composable
private fun DateBlock(label: String, value: String, paper: Paper, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Label(label, paper, color = paper.inkFaint)
        Spacer(Modifier.height(7.dp))
        Numeral(value, paper, size = 14.sp)
    }
}

/** Small helper so callers can put a rule between sections without a spacer dance. */
@Composable
internal fun SectionBreak(paper: Paper, space: Int = 24) {
    Spacer(Modifier.height(space.dp))
    Rule(paper)
    Spacer(Modifier.height(space.dp))
}

/** A framed column, used for the day strip and the receipt stack. */
@Composable
internal fun FramedColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) { Column { content() } }
}
