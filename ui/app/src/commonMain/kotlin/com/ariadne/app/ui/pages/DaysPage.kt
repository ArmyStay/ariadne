package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.EmptyState
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.PageAction
import com.ariadne.design.Paper
import com.ariadne.design.RailFigure
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.DaySummary
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.dayRange
import com.ariadne.model.entryStatus

/**
 * Days: the trip as a run of dates.
 *
 * A strip of dates across the top, the day you have picked underneath it with its
 * total and its photographs, and the rest of the trip below that. Tapping a date
 * changes what the page shows; tapping the day's name opens it.
 *
 * The old Timeline drew a 24-hour tick chart under every day — a bar per hour with
 * the captures as ticks — which was beautiful, unreadable, and answered a question
 * ("when in the day did I spend?") that no traveller asks. It is gone; the day's
 * total and its photographs are what the page is for.
 */
@Composable
public fun DaysPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip
    val today = state.today

    TabPage(
        paper,
        wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            val cards = state.dayCards
            RailFigure("closed", "${cards.count { it.isClosed }}", paper, 26.sp)
            Spacer(Modifier.height(22.dp))
            RailFigure("open", "${cards.count { !it.isClosed }}", paper, 26.sp)
            Spacer(Modifier.height(22.dp))
            RailFigure("waiting", "${state.tripPending.size}", paper, 26.sp)
        },
    ) {
        if (trip == null) {
            Spacer(Modifier.height(26.dp))
            Title("Days", paper, size = 32.sp, italic = true)
            Spacer(Modifier.height(10.dp))
            Body("A trip has no days until it has dates.", paper, color = paper.inkSoft, size = 16.sp)
            Spacer(Modifier.height(22.dp))
            Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
            AriadneBarSpacer()
            return@TabPage
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Title("Days", paper, size = if (wide) 40.sp else 32.sp, italic = true)
                Spacer(Modifier.height(4.dp))
                Label(
                    "${trip.startDate.iso} → ${trip.endDate.iso}",
                    paper,
                    color = paper.inkFaint,
                    size = 10.sp,
                    tracking = 0.4.sp,
                )
            }
            if (today != null) {
                PageAction("Close day", paper, { model.openCloseDay(today) })
            }
        }

        Spacer(Modifier.height(18.dp))
        WeekStrip(state, model, paper, today)
        Spacer(Modifier.height(6.dp))
        Rule(paper)

        // The day in view.
        if (today != null) {
            val captures = state.dayCaptures
            val summary = DaySummary.of(captures)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Title(spokenDay(today.iso), paper, size = 24.sp, italic = true)
                    Spacer(Modifier.height(3.dp))
                    Label(
                        when {
                            summary.total == 0 -> "nothing kept"
                            summary.isSettled -> "${summary.total} kept · settled"
                            else -> "${summary.total} kept · ${summary.pending} to settle"
                        },
                        paper,
                        color = paper.inkFaint,
                        size = 9.sp,
                        tracking = 0.6.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Numeral(
                    Money.ofMinor(state.dayTotalMinor).formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
                    paper,
                    size = 21.sp,
                    weight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(4.dp))
            ConversionNote(state.dayRateDates, state.dayUnconverted, paper)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlineAction("Open the day", paper, { model.openRecord(today) })
                }
                Box(Modifier.weight(1f)) {
                    OutlineAction("Close day", paper, { model.openCloseDay(today) })
                }
            }

            Spacer(Modifier.height(18.dp))
            if (captures.isEmpty()) {
                EmptyState(
                    title = "Nothing kept on this day.",
                    body = "An empty day is still part of the trip — it is the one you spent " +
                        "walking. Photographs can be added to it until the day is closed.",
                    paper = paper,
                )
            } else {
                captures.forEach { capture ->
                    DayEntry(capture, model.titleOf(capture), state.dayCurrency, paper) {
                        model.openDetail(capture.id)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        // The rest of the trip, so a day can be reached without reading the strip.
        Rule(paper)
        Spacer(Modifier.height(14.dp))
        Label("all days", paper, color = paper.inkFaint, size = 9.sp, tracking = 0.6.sp)
        Spacer(Modifier.height(6.dp))
        state.dayCards.forEach { card ->
            DayLine(
                card = card,
                figure = state.dayFigures[card.localDate.iso] ?: 0L,
                currency = state.dayCurrency,
                selected = card.localDate == today,
                summary = state.daySummary(card.localDate.iso),
                paper = paper,
                onSelect = { model.viewDay(card.localDate) },
                onOpen = { model.openRecord(card.localDate) },
            )
        }

        AriadneBarSpacer()
    }
}

/**
 * The week strip: seven (or so) dates in a row, scrolled to the day in view.
 *
 * Every day of the trip is in the strip — a trip is short enough — so picking one
 * is a single horizontal gesture rather than a date picker. The selected day is
 * marked with a rule under it and ink in it, not with a coloured pill: this is a
 * date, not a filter chip.
 */
@Composable
private fun WeekStrip(state: AppUiState, model: AppModel, paper: Paper, today: LocalDay?) {
    val trip = state.trip ?: return
    val days = remember(trip.startDate, trip.endDate) { dayRange(trip.startDate, trip.endDate) }
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            val selected = day == today
            val figure = state.dayFigures[day.iso]
            Column(
                Modifier
                    .widthIn(min = 46.dp)
                    .pointerInput(day) { detectTapGestures(onTap = { model.viewDay(day) }) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Label(
                    weekdayName(day).take(2),
                    paper,
                    color = if (selected) paper.ink else paper.inkFaint,
                    size = 9.sp,
                    tracking = 0.4.sp,
                )
                Spacer(Modifier.height(3.dp))
                Numeral(
                    day.iso.takeLast(2),
                    paper,
                    size = 17.sp,
                    weight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) paper.ink else paper.inkMid,
                )
                Spacer(Modifier.height(3.dp))
                // A tick for a day that holds something, and nothing for one that
                // does not: a day's emptiness is information, not a gap to fill.
                Box(
                    Modifier
                        .height(2.dp)
                        .width(if (selected) 18.dp else if (figure != null && figure > 0) 10.dp else 4.dp)
                        .background(
                            when {
                                selected -> paper.ink
                                figure != null && figure > 0 -> paper.edgeStrong
                                else -> paper.edge
                            },
                        ),
                )
            }
        }
    }
}

/** One day in the full list: date, figure, state, and two ways in. */
@Composable
private fun DayLine(
    card: DayCard,
    figure: Long,
    currency: String,
    selected: Boolean,
    summary: DaySummary,
    paper: Paper,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .rowTap(onSelect)
            .background(if (selected) paper.sunk else paper.bg)
            .padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(56.dp)) {
                Numeral(
                    card.localDate.iso.takeLast(2),
                    paper,
                    size = 20.sp,
                    color = if (selected) paper.ink else paper.inkMid,
                )
                Label(spokenDay(card.localDate.iso).substringAfter(" "), paper, color = paper.inkFaint, size = 9.sp, tracking = 0.4.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Title(weekdayName(card.localDate), paper, size = 15.sp)
                Spacer(Modifier.height(2.dp))
                Label(
                    when {
                        summary.total == 0 -> "nothing kept"
                        card.isClosed -> "closed · ${summary.total} kept"
                        summary.isSettled -> "${summary.total} kept"
                        else -> "${summary.pending} to settle"
                    },
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 0.4.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            if (summary.total > 0) {
                Numeral(
                    Money.ofMinor(figure).formatWithSymbol(CurrencyCode.of(currency)),
                    paper,
                    size = 15.sp,
                    weight = FontWeight.Medium,
                )
            } else {
                Label("—", paper, color = paper.inkFaint)
            }
            Spacer(Modifier.width(8.dp))
            Pill(
                if (card.isClosed) "closed" else if (summary.isSettled) "settled" else "open",
                paper,
                when {
                    card.isClosed -> PillTone.Ink
                    summary.isSettled -> PillTone.Quiet
                    else -> PillTone.Blue
                },
            )
        }
        if (selected) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(start = 56.dp)) {
                PageAction("Open the day", paper, onOpen)
            }
        }
    }
    Rule(paper)
}

/** A photograph in a day's list: a thumbnail, its name, its figure. */
@Composable
private fun DayEntry(
    capture: Capture,
    title: String,
    baseCurrency: String,
    paper: Paper,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .rowTap(onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(52.dp)
                .aspectRatio(1f)
                .border(Bone.Hairline, paper.edge),
        ) {
            PhotoPreview(
                photoPath = capture.photoPath,
                caption = title,
                paper = paper,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Title(title, paper, size = 15.sp)
            Spacer(Modifier.height(2.dp))
            Label(
                if (capture.amountMinor != null && capture.currency != null) {
                    capture.currency.code
                } else {
                    capture.entryStatus.label
                },
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 0.4.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Numeral(
            capture.amountMinor?.let {
                Money.ofMinor(it).formatWithSymbol(capture.currency ?: CurrencyCode.of(baseCurrency))
            } ?: "—",
            paper,
            color = if (capture.amountMinor == null) paper.inkFaint else paper.ink,
            size = 15.sp,
        )
    }
}
