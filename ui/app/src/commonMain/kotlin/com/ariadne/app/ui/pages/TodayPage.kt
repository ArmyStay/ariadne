package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Glyph
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.PageAction
import com.ariadne.design.Paper
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.QuietAction
import com.ariadne.design.RailFigure
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DaySummary
import com.ariadne.model.Money
import com.ariadne.model.dayRange
import com.ariadne.model.entryStatus

/**
 * Today: the page the app opens on, and the only one most days need.
 *
 * It answers three questions in the order they are asked — what have I spent
 * today, how am I doing against the money I set aside, and what is still waiting
 * for me — and then shows the day as photographs, because the photographs are the
 * record.
 *
 * What it no longer does is act as a contents page. The old Home listed five rows
 * — Trips, Timeline, The record, Search, The trip's page — every one of which is
 * now either a destination in the bar or reachable from the page it belongs to.
 * A home screen that is mostly links to other screens is a menu, not a home.
 */
@Composable
public fun TodayPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip
    // The library is a real second way in, and it is its own picker call.
    val importPhoto = rememberPhotoImport(
        onBytes = { model.importPhoto(it) },
        onUnavailable = { model.openCapture() },
    )

    TabPage(
        paper,
        wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            if (trip != null) {
                RailFigure("today", state.today?.iso ?: "—", paper)
                Spacer(Modifier.height(22.dp))
                RailFigure("waiting", "${state.tripPending.size}", paper)
                Spacer(Modifier.height(4.dp))
                Label(
                    if (state.tripPending.isEmpty()) "nothing" else "to settle",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 0.6.sp,
                )
            }
        },
    ) {
        if (trip == null) {
            NoTrip(state, model, paper)
            return@TabPage
        }
        if (trip.isClosed) {
            ClosedTrip(trip.name, trip.id, model, paper)
            return@TabPage
        }

        // The header is a name and a date. No plate number, no "01 — ARIADNE":
        // the numbering was furniture from the first design and it made every page
        // look like a chapter of something rather than a day of a trip.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Title("Today", paper, size = if (wide) 40.sp else 32.sp, italic = true)
                Spacer(Modifier.height(4.dp))
                Label(
                    spokenDay(state.today?.iso.orEmpty()) + " · " + trip.name,
                    paper,
                    color = paper.inkFaint,
                    size = 10.sp,
                    tracking = 0.4.sp,
                )
            }
            PageAction("Trip", paper, { model.go(Route.Trip) })
        }

        Spacer(Modifier.height(26.dp))

        // The figure. Hung on the paper, no card: a number this size is its own
        // container.
        Row(verticalAlignment = Alignment.Bottom) {
            if (state.dayRateDates.isNotEmpty()) {
                Numeral("≈", paper, color = paper.inkFaint, size = 20.sp)
                Spacer(Modifier.width(4.dp))
            }
            Numeral(
                CurrencyCode.of(state.dayCurrency).symbol(),
                paper,
                color = paper.inkFaint,
                size = 20.sp,
            )
            Spacer(Modifier.width(6.dp))
            Numeral(
                Money.ofMinor(state.dayTotalMinor).format(CurrencyCode.of(state.dayCurrency)),
                paper,
                size = if (wide) 60.sp else 46.sp,
                weight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Label("spent today", paper, color = paper.inkSoft, size = 10.sp, tracking = 0.4.sp)
        Spacer(Modifier.height(6.dp))
        ConversionNote(state.dayRateDates, state.dayUnconverted, paper)

        Spacer(Modifier.height(16.dp))
        BudgetPace(state, trip.budgetMinor, paper)

        Spacer(Modifier.height(22.dp))

        // The one blue thing on the page, always — not only on days when a stack
        // happens to exist.
        Action(
            "Take a photo",
            paper,
            { model.openCapture() },
            accent = true,
            icon = Glyph.Shutter,
        )
        if (pickerAvailable()) {
            Spacer(Modifier.height(6.dp))
            QuietAction(
                "or bring one in from the library",
                paper,
                importPhoto,
                color = paper.inkSoft,
            )
        }

        NeedsAnAmount(state, model, paper)

        Spacer(Modifier.height(26.dp))
        Rule(paper)
        Spacer(Modifier.height(14.dp))

        val captures = state.dayCaptures
        if (captures.isEmpty()) {
            Body(
                "Nothing yet today. Photograph the receipt now and put the figure in later — " +
                    "that is the whole idea.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 16.sp,
                lineHeight = 24.sp,
            )
        } else {
            val summary = DaySummary.of(captures)
            Label(
                "${summary.total} ${if (summary.total == 1) "photograph" else "photographs"}",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 0.6.sp,
            )
            Spacer(Modifier.height(10.dp))
            captures.forEach { capture ->
                TodayEntry(capture, model.titleOf(capture), state.dayCurrency, paper) {
                    model.openDetail(capture.id)
                }
            }
        }

        AriadneBarSpacer()
    }
}

/**
 * The day's photographs, as photo cards.
 *
 * A photograph on the left at a size you can recognise a receipt from, the name
 * and the state beside it, the figure on the right — the shape a contact sheet
 * has, not the shape a settings list has.
 */
@Composable
private fun TodayEntry(
    capture: Capture,
    title: String,
    baseCurrency: String,
    paper: Paper,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pointerInput(capture.id) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(64.dp)
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
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Title(title, paper, size = 16.sp)
            Spacer(Modifier.height(3.dp))
            Label(
                if (capture.amountMinor != null && capture.currency != null) {
                    capture.currency.code
                } else {
                    capture.entryStatus.label
                },
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 0.6.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Numeral(
            capture.amountMinor?.let {
                Money.ofMinor(it).formatWithSymbol(capture.currency ?: CurrencyCode.of(baseCurrency))
            } ?: "—",
            paper,
            color = if (capture.amountMinor == null) paper.inkFaint else paper.ink,
            size = 16.sp,
        )
    }
}

/**
 * "Needs an amount (3)" — the tray, on the page where you would notice it.
 *
 * This is the strip the brief asks for and the one the old Home did not have: the
 * cards that are waiting were only visible behind a link labelled "Finish 3", and
 * the figure beside them counted the wrong thing. It appears only when there is
 * something in it, carries the photographs themselves, and one tap finishes them.
 */
@Composable
private fun NeedsAnAmount(state: AppUiState, model: AppModel, paper: Paper) {
    val pending = state.tripPending
    if (pending.isEmpty()) return
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Title(
            "Needs an amount (${pending.size})",
            paper,
            size = 17.sp,
            italic = true,
        )
        Spacer(Modifier.weight(1f))
        PageAction("Finish", paper, { model.openCloseDay(state.today) })
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Three is enough to recognise the tray; the count carries the rest.
        pending.take(3).forEach { capture ->
            Box(
                Modifier
                    .width(52.dp)
                    .aspectRatio(1f)
                    .border(Bone.Hairline, paper.edge)
                    .pointerInput(capture.id) { detectTapGestures(onTap = { model.openDetail(capture.id) }) },
            ) {
                PhotoPreview(
                    photoPath = capture.photoPath,
                    caption = model.titleOf(capture),
                    paper = paper,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
        }
        if (pending.size > 3) {
            Box(Modifier.width(52.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Numeral("+${pending.size - 3}", paper, color = paper.inkFaint, size = 15.sp)
            }
        }
    }
}

/**
 * Today against the daily share of the trip's budget.
 *
 * The budget is a whole-trip number, so the honest daily comparison is the trip's
 * budget spread over its days. It is a pace, not a rule: "over today" is stated in
 * the same voice as "under today", because this is a record and not a scold.
 */
@Composable
private fun BudgetPace(state: AppUiState, budgetMinor: Long?, paper: Paper) {
    val trip = state.trip ?: return
    if (budgetMinor == null) {
        QuietAction("Set a budget", paper, { /* Trip page owns the field */ }, color = paper.inkFaint)
        return
    }
    val days = remember(trip.startDate, trip.endDate) {
        dayRange(trip.startDate, trip.endDate).size.coerceAtLeast(1)
    }
    val perDay = budgetMinor / days
    val today = state.dayTotalMinor
    val left = perDay - today
    val currency = CurrencyCode.of(state.dayCurrency)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label("today against", paper, color = paper.inkFaint, size = 9.sp, tracking = 0.6.sp)
            Spacer(Modifier.height(2.dp))
            Numeral(
                "${Money.ofMinor(perDay).formatWithSymbol(currency)} a day",
                paper,
                color = paper.inkSoft,
                size = 13.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Pill(
            if (left >= 0) {
                "${Money.ofMinor(left).formatWithSymbol(currency)} left"
            } else {
                "${Money.ofMinor(-left).formatWithSymbol(currency)} over"
            },
            paper,
            if (left >= 0) PillTone.Quiet else PillTone.Blue,
        )
    }
}

@Composable
private fun NoTrip(state: AppUiState, model: AppModel, paper: Paper) {
    Spacer(Modifier.height(30.dp))
    Title("No trip open", paper, size = 32.sp, italic = true)
    Spacer(Modifier.height(12.dp))
    Body(
        "A journey, from its first day to its last. Photographs as you go; figures " +
            "whenever you have a moment.",
        paper,
        serif = true,
        color = paper.inkSoft,
        size = 17.sp,
        lineHeight = 26.sp,
    )
    Spacer(Modifier.height(26.dp))
    Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
    Spacer(Modifier.height(10.dp))
    if (state.trips.isEmpty()) {
        OutlineAction("Begin with the demo roll", paper, { model.startTripDemo() })
    } else {
        OutlineAction("See the trips", paper, { model.go(Route.Trips) })
    }
    AriadneBarSpacer()
}

@Composable
private fun ClosedTrip(name: String, tripId: String, model: AppModel, paper: Paper) {
    Spacer(Modifier.height(30.dp))
    Title(name, paper, size = 32.sp, italic = true)
    Spacer(Modifier.height(12.dp))
    Body(
        "This trip is closed. Nothing new can be filed against it; the record and the " +
            "card stay exactly as they were.",
        paper,
        serif = true,
        color = paper.inkSoft,
        size = 17.sp,
        lineHeight = 26.sp,
    )
    Spacer(Modifier.height(24.dp))
    Action("See the card", paper, { model.go(Route.TripCard(tripId)) }, accent = true)
    Spacer(Modifier.height(10.dp))
    OutlineAction("See the trips", paper, { model.go(Route.Trips) })
    AriadneBarSpacer()
}
