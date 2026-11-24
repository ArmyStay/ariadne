package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money

/**
 * Page 7 — Trip card.
 *
 * The payoff, and the end of the product. Everything the app has been collecting
 * arrives here as one column of type you could print: name, dates, the days, the
 * currencies, one total, and a line saying the record is yours.
 *
 * A trip that has not finished still shows this page — it just says so, plainly,
 * without a warning colour.
 */
@Composable
public fun TripCardPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip ?: return
    val closed = state.dayCards.count { it.isClosed }

    Column(
        Modifier
            .fillMaxSize()
            .background(paper.sunk)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 96.dp else 20.dp, vertical = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            QuietAction("Back to the record", paper, { model.go(Route.Record(state.today)) })
            // Share copies the card as text, built from the same state the page is
            // showing, so the shared numbers cannot disagree with the printed ones.
            Row(
                Modifier.androidTap { model.copyCardToClipboard() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Glyph.Share, paper, size = 14.dp, color = paper.inkFaint)
                Spacer(Modifier.width(7.dp))
                Label(
                    if (state.cardCopied) "Copied" else "Share",
                    paper,
                    color = if (state.cardCopied) paper.blue else paper.inkFaint,
                    size = 10.sp,
                    tracking = 1.5.sp,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // The card itself: paper within paper.
        Column(
            Modifier
                .fillMaxWidth()
                .background(paper.bg)
                .border(Bone.Hairline, paper.edge)
                .padding(horizontal = if (wide) 56.dp else 26.dp, vertical = 40.dp),
        ) {
            Label("A trip, kept", paper, color = paper.inkFaint)
            Spacer(Modifier.height(14.dp))
            Title(trip.name, paper, size = 38.sp)
            Spacer(Modifier.height(10.dp))
            Label("${trip.startDate.iso} — ${trip.endDate.iso}", paper, color = paper.inkSoft)

            Spacer(Modifier.height(30.dp))
            Rule(paper, color = paper.ink)
            Spacer(Modifier.height(24.dp))

            // The days, listed as a run rather than a table.
            state.dayCards.forEach { card ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Numeral(card.localDate.iso, paper, size = 13.sp, color = paper.inkMid)
                    Label(
                        if (card.isClosed) "closed" else "open",
                        paper,
                        color = if (card.isClosed) paper.inkMid else paper.inkFaint,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Rule(paper)
            Spacer(Modifier.height(18.dp))

            if (state.perCurrency.isEmpty()) {
                Body(
                    "No figures recorded yet. The photographs are still the record.",
                    paper,
                    serif = true,
                    color = paper.inkSoft,
                )
            } else {
                state.perCurrency.forEach { (code, minor) ->
                    FigureRow(code, Money.ofMinor(minor).format(CurrencyCode.of(code)), paper)
                }
                Spacer(Modifier.height(6.dp))
                Rule(paper, color = paper.ink)
                FigureRow(
                    "Total",
                    Money.ofMinor(state.dayTotalMinor).formatWithSymbol(trip.baseCurrency),
                    paper,
                    emphasised = true,
                )
            }

            Spacer(Modifier.height(30.dp))
            Rule(paper)
            Spacer(Modifier.height(16.dp))
            Body(
                if (closed == state.dayCards.size) {
                    "Every day closed. This is the whole of the trip: no account, no sync, " +
                        "and nothing about it left this device."
                } else {
                    "$closed of ${state.dayCards.size} days closed. The card keeps filling itself in " +
                        "until the trip is done."
                },
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
        }

        Spacer(Modifier.height(22.dp))
        Action("Copy as text", paper, { model.copyCardToClipboard() })
        Spacer(Modifier.height(8.dp))
        QuietAction("Back to the trip", paper, { model.go(Route.Trip) })
    }
}

/** A stamp-like block used only by the onboarding page. */
@Composable
internal fun Stamp(paper: Paper, text: String) {
    Box(
        Modifier
            .border(Bone.Hairline, paper.ink)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Label(text, paper, color = paper.ink)
    }
}

/** Small centred column helper for onboarding. */
@Composable
internal fun CenteredWidth(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().width(520.dp), horizontalAlignment = Alignment.Start) { content() }
}
