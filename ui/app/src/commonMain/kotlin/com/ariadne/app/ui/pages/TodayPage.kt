package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.ariadne.design.BudgetBar
import com.ariadne.design.EmptyState
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.Paper
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.QuietAction
import com.ariadne.design.RailFigure
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.BudgetPlan
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.Money
import com.ariadne.model.Trip
import com.ariadne.model.entryStatus
import kotlin.math.roundToInt

/**
 * Home — the landing page, and the page the app opens on.
 *
 * The brief for this page changed twice, and the second change is the one worth
 * writing down. It used to be "what have I spent today": one figure, a budget line,
 * the day's photographs. Honest, but thin — and it left the person opening the app
 * on a trip they could not see: the register was behind a link, the trip's total was
 * on another page, the settings were two taps deep in a corner.
 *
 * Now it opens on the trip **and** the day, in the order the questions actually get
 * asked when someone unlocks their phone in a shop:
 *
 *   1. **Which trip, and how am I doing in it** — the name, the share of the budget
 *      spent, what is left. One line, always true, never scolding.
 *   2. **What today has cost** — the largest figure on the page, on ink, because it
 *      is the one number that moves several times a day.
 *   3. **What the trip has cost, against the fence** — the same plan as Trip and
 *      Stats draw, with the pace tick, so the three pages cannot disagree.
 *   4. **What is waiting** — the photographs still owed a figure, with the
 *      photographs themselves, because "2 waiting" is not a sentence anyone acts on.
 *   5. **The record itself** — the whole trip as a feed of days, filterable, newest
 *      first, today open. Days past collapse to a line, so the page stays short.
 *   6. **Everything else** — the trips, search, and settings, on the page rather than
 *      behind a menu. The old Home was criticised for being mostly links; this is the
 *      other failure mode, and the answer is a footer, not a menu.
 */
@Composable
public fun TodayPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip
    // The library is a real second way in, and it is its own picker call.
    val importPhotos = rememberPhotoImportMany(
        onBytes = { model.importPhotos(it) },
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

        val currency = trip.baseCurrency
        val spent = state.categoryTotals.sumOf { it.baseMinor }
        val days = state.dayCards.size.coerceAtLeast(1)
        val closed = state.dayCards.count { it.isClosed }
        // One plan, one arithmetic: the strip, the card and the pace tick all come
        // from the tested value type, so Home cannot round differently from Trip.
        val plan = BudgetPlan.of(
            budgetMinor = trip.budgetMinor,
            days = days,
            spentMinor = spent,
            daysClosed = closed,
        )
        val snaps = state.dayCapturesAll.values.sumOf { it.size }

        TripHead(state, trip, model, paper, wide)

        Spacer(Modifier.height(18.dp))
        InsightStrip(plan, spent, currency, paper) { model.go(Route.Stats) }

        Spacer(Modifier.height(14.dp))
        TodayCard(state, model, paper, wide)

        Spacer(Modifier.height(14.dp))
        TripTotalCard(plan, spent, currency, snaps, paper) { model.go(Route.Stats) }

        Spacer(Modifier.height(20.dp))
        Action(
            "Snap a receipt",
            paper,
            { model.openCapture() },
            accent = true,
            icon = Glyph.Shutter,
        )
        if (pickerAvailable()) {
            Spacer(Modifier.height(6.dp))
            QuietAction(
                "or bring some in from the library",
                paper,
                importPhotos,
                color = paper.inkSoft,
            )
        }

        Waiting(state, model, paper)

        Spacer(Modifier.height(24.dp))
        Feed(state, model, paper)

        Spacer(Modifier.height(26.dp))
        MoreStrip(model, paper)

        AriadneBarSpacer()
    }
}

// ── 1. which trip, and how it is going ──────────────────────────────────────────

@Composable
private fun TripHead(
    state: AppUiState,
    trip: Trip,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
) {
    val waiting = state.tripPending.size
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            // The trip is the container of everything on this page, so switching it
            // is a real control and sits where the name is — not in a menu.
            Row(
                Modifier.rowTap { model.go(Route.Trips) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label("active trip", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
                Spacer(Modifier.width(7.dp))
                Chevron(paper, up = false, tint = paper.inkFaint, box = 7.dp)
            }
            Spacer(Modifier.height(5.dp))
            Title(trip.name, paper, size = if (wide) 40.sp else 32.sp, italic = true)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(
                    spokenDay(state.today?.iso.orEmpty()),
                    paper,
                    color = paper.inkSoft,
                    size = 10.sp,
                    tracking = 0.5.sp,
                )
                Dot(paper)
                Label(
                    "base ${trip.baseCurrency.code}",
                    paper,
                    color = paper.inkFaint,
                    size = 10.sp,
                    tracking = 0.5.sp,
                )
                if (waiting > 0) {
                    Dot(paper)
                    Label(
                        "$waiting waiting",
                        paper,
                        color = paper.blue,
                        size = 10.sp,
                        tracking = 0.5.sp,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        RoundButton(Glyph.Gear, "Settings", paper, paper.inkMid) { model.go(Route.Preferences) }
    }
}

// ── 2. the trip in one line ─────────────────────────────────────────────────────

@Composable
private fun InsightStrip(
    plan: BudgetPlan,
    spent: Long,
    currency: CurrencyCode,
    paper: Paper,
    onOpen: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(paper.raised, RoundedCornerShape(2.dp))
            .border(Bone.Hairline, paper.edgeStrong, RoundedCornerShape(2.dp))
            .rowTap(onOpen)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label("insights", paper, color = paper.inkSoft, size = 9.sp, tracking = 1.8.sp)
        Spacer(Modifier.width(12.dp))
        if (plan.leftMinor == null) {
            Label("no budget set", paper, color = paper.inkFaint, size = 9.sp, tracking = 0.8.sp)
            Spacer(Modifier.weight(1f))
            Numeral(
                Money.ofMinor(spent).formatWithSymbol(currency),
                paper,
                color = paper.ink,
                size = 13.sp,
            )
        } else {
            val used = (plan.spendFraction * 100f).roundToInt()
            Label("$used% used", paper, color = paper.inkMid, size = 9.sp, tracking = 1.1.sp)
            Spacer(Modifier.weight(1f))
            Numeral(
                if (plan.isOver) {
                    "${Money.ofMinor(-(plan.leftMinor ?: 0L)).formatWithSymbol(currency)} over"
                } else {
                    "${Money.ofMinor(plan.leftMinor ?: 0L).formatWithSymbol(currency)} left"
                },
                paper,
                color = if (plan.isOver) paper.blue else paper.ink,
                size = 14.sp,
            )
        }
        Spacer(Modifier.width(9.dp))
        Chevron(paper, up = false, tint = paper.inkFaint, box = 9.dp)
    }
}

// ── 3. today, on ink ────────────────────────────────────────────────────────────

@Composable
private fun TodayCard(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val currency = CurrencyCode.of(state.dayCurrency)
    val snapsToday = state.dayCaptures.size
    val pendingToday = state.dayCaptures.count { it.entryStatus.isPending }
    val rate = if (state.dayRateDates.isNotEmpty()) "≈" else ""
    Column(
        Modifier
            .fillMaxWidth()
            .background(paper.ink, RoundedCornerShape(2.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(
                "spent today",
                paper,
                color = paper.onBlue.copy(alpha = 0.62f),
                size = 9.sp,
                tracking = 1.8.sp,
            )
            Spacer(Modifier.width(10.dp))
            Tag(currency.code, paper, onInk = true)
            Spacer(Modifier.weight(1f))
            Tag(
                if (snapsToday == 1) "1 snap" else "$snapsToday snaps",
                paper,
                onInk = true,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            if (rate.isNotEmpty()) {
                Numeral(
                    rate,
                    paper,
                    color = paper.onBlue.copy(alpha = 0.6f),
                    size = 17.sp,
                )
                Spacer(Modifier.width(5.dp))
            }
            Numeral(
                Money.ofMinor(state.dayTotalMinor).formatWithSymbol(currency),
                paper,
                color = paper.onBlue,
                size = if (wide) 46.sp else 36.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pendingToday > 0) {
                // A blue mark on ink is the one place the accent is legible in the
                // dark, so the waiting count gets it rather than a colour of its own.
                Box(Modifier.size(5.dp).background(paper.blue, CircleShape))
                Spacer(Modifier.width(7.dp))
                Label(
                    "$pendingToday still to settle",
                    paper,
                    color = paper.onBlue.copy(alpha = 0.78f),
                    size = 9.sp,
                    tracking = 0.9.sp,
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .rowTap { model.openCloseDay(state.today) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                ) {
                    Label(
                        "finish",
                        paper,
                        color = paper.onBlue,
                        size = 9.sp,
                        tracking = 0.9.sp,
                    )
                }
            } else {
                Label(
                    if (snapsToday == 0) "nothing yet today" else "all settled",
                    paper,
                    color = paper.onBlue.copy(alpha = 0.62f),
                    size = 9.sp,
                    tracking = 0.9.sp,
                )
            }
        }
    }
}

// ── 4. the trip against the fence ───────────────────────────────────────────────

@Composable
private fun TripTotalCard(
    plan: BudgetPlan,
    spent: Long,
    currency: CurrencyCode,
    snaps: Int,
    paper: Paper,
    onOpen: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(paper.raised, RoundedCornerShape(2.dp))
            .border(Bone.Hairline, paper.edge, RoundedCornerShape(2.dp))
            .rowTap(onOpen)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("trip total", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.8.sp)
            Spacer(Modifier.weight(1f))
            Tag(if (snaps == 1) "1 snap" else "$snaps snaps", paper)
        }
        Spacer(Modifier.height(10.dp))
        Numeral(
            Money.ofMinor(spent).formatWithSymbol(currency),
            paper,
            color = paper.ink,
            size = 26.sp,
        )
        Spacer(Modifier.height(14.dp))
        if (!plan.hasBudget) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(
                    "No budget on this trip. It is a record either way.",
                    paper,
                    color = paper.inkSoft,
                    size = 12.sp,
                )
                Spacer(Modifier.weight(1f))
                Label("set one", paper, color = paper.blue, size = 9.sp, tracking = 1.1.sp)
            }
            return@Column
        }
        val pct = (plan.spendFraction * 100f).roundToInt()
        val pacePct = (plan.paceFraction * 100f).roundToInt()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(
                "budget ${Money.ofMinor(plan.budgetMinor ?: 0L).formatWithSymbol(currency)}",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.1.sp,
            )
            Spacer(Modifier.weight(1f))
            Numeral("$pct%", paper, color = paper.ink, size = 13.sp)
        }
        Spacer(Modifier.height(9.dp))
        BudgetBar(
            fraction = plan.spendFraction,
            paper = paper,
            pace = plan.paceFraction,
            thickness = 8.dp,
            over = plan.isOver,
        )
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(
                "$pacePct% of the trip gone",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 0.7.sp,
            )
            Spacer(Modifier.weight(1f))
            Pill(
                if (plan.isAheadOfPace) "ahead of the days" else "behind the days",
                paper,
                PillTone.Quiet,
            )
        }
    }
}

// ── 5. what is still owed a figure ──────────────────────────────────────────────

@Composable
private fun Waiting(state: AppUiState, model: AppModel, paper: Paper) {
    val pending = state.tripPending
    if (pending.isEmpty()) return
    Spacer(Modifier.height(22.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .background(paper.sunk, RoundedCornerShape(2.dp))
            .border(Bone.Hairline, paper.edge, RoundedCornerShape(2.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Label("waiting for an amount", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(4.dp))
            Title(
                if (pending.size == 1) "One photograph" else "${pending.size} photographs",
                paper,
                size = 18.sp,
                italic = true,
            )
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Three is enough to recognise the tray; the count carries the rest.
                pending.take(3).forEach { capture ->
                    Box(
                        Modifier
                            .width(40.dp)
                            .aspectRatio(1f)
                            .border(Bone.Hairline, paper.edge)
                            .rowTap { model.openDetail(capture.id) },
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
                    Box(Modifier.width(40.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Numeral("+${pending.size - 3}", paper, color = paper.inkFaint, size = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        RoundButton(Glyph.Done, "Settle the waiting photographs", paper, paper.onBlue, filled = true) {
            model.openCloseDay(state.today)
        }
    }
}

// ── 6. the record, as a feed of days ────────────────────────────────────────────

private enum class FeedFilter(val label: String) {
    All("All"),
    Pending("Pending"),
    Today("Today"),
}

@Composable
private fun Feed(state: AppUiState, model: AppModel, paper: Paper) {
    val pending = state.tripPending
    val days = remember(state.dayCards, state.dayCapturesAll) {
        state.dayCards
            .sortedByDescending { it.localDate.iso }
            .map { it to state.dayCapturesAll[it.localDate.iso].orEmpty() }
            .filter { (_, captures) -> captures.isNotEmpty() }
    }
    var filter by remember(state.trip?.id) { mutableStateOf(FeedFilter.All) }
    var open by remember(state.trip?.id) {
        mutableStateOf(setOfNotNull(state.today?.iso).toSet())
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label("activity", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.8.sp)
        Spacer(Modifier.weight(1f))
        Label(
            "newest first",
            paper,
            color = paper.inkGhost,
            size = 9.sp,
            tracking = 0.7.sp,
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FeedFilter.entries.forEach { chip ->
            val count = when (chip) {
                FeedFilter.All -> days.sumOf { it.second.size }
                FeedFilter.Pending -> pending.size
                FeedFilter.Today -> state.dayCaptures.size
            }
            Chip(
                text = chip.label,
                count = count,
                selected = filter == chip,
                paper = paper,
            ) { filter = chip }
        }
    }
    Spacer(Modifier.height(12.dp))

    when (filter) {
        FeedFilter.All -> {
            if (days.isEmpty()) {
                EmptyState(
                    title = "Nothing yet today.",
                    body = "Photograph the receipt now and put the figure in later — that is the " +
                        "whole idea. Nothing here asks for a form.",
                    paper = paper,
                )
            } else {
                days.forEachIndexed { index, (day, captures) ->
                    if (index > 0) Spacer(Modifier.height(4.dp))
                    DayGroup(
                        day = day,
                        captures = captures,
                        figure = state.dayFigures[day.localDate.iso]
                            ?: captures.sumOf { it.amountMinor ?: 0L },
                        currency = CurrencyCode.of(state.dayCurrency),
                        isToday = day.localDate.iso == state.today?.iso,
                        expanded = open.contains(day.localDate.iso),
                        paper = paper,
                        onOpenDay = { model.openRecord(day.localDate) },
                        onToggle = {
                            open = if (open.contains(day.localDate.iso)) {
                                open - day.localDate.iso
                            } else {
                                open + day.localDate.iso
                            }
                        },
                        onEntry = { model.openDetail(it.id) },
                    )
                }
            }
        }

        FeedFilter.Pending -> {
            if (pending.isEmpty()) {
                EmptyState(
                    title = "Nothing waiting.",
                    body = "Every photograph in this trip has a figure, or was deliberately left " +
                        "without one. That is a finished record.",
                    paper = paper,
                )
            } else {
                pending
                    .sortedWith(compareByDescending<Capture> { it.localDate.iso }.thenByDescending { it.createdAt })
                    .forEach { capture ->
                        Entry(
                            capture = capture,
                            title = model.titleOf(capture),
                            currency = CurrencyCode.of(state.dayCurrency),
                            trailing = spokenDay(capture.localDate.iso),
                            paper = paper,
                        ) { model.openDetail(capture.id) }
                    }
            }
        }

        FeedFilter.Today -> {
            if (state.dayCaptures.isEmpty()) {
                EmptyState(
                    title = "Nothing yet today.",
                    body = "The shutter is the round button at the foot of the page. Ten seconds, " +
                        "and the receipt is safe.",
                    paper = paper,
                )
            } else {
                state.dayCaptures.forEach { capture ->
                    Entry(
                        capture = capture,
                        title = model.titleOf(capture),
                        currency = CurrencyCode.of(state.dayCurrency),
                        trailing = capture.entryStatus.label,
                        paper = paper,
                    ) { model.openDetail(capture.id) }
                }
            }
        }
    }
}

@Composable
private fun DayGroup(
    day: DayCard,
    captures: List<Capture>,
    figure: Long,
    currency: CurrencyCode,
    isToday: Boolean,
    expanded: Boolean,
    paper: Paper,
    onOpenDay: () -> Unit,
    onToggle: () -> Unit,
    onEntry: (Capture) -> Unit,
) {
    val pending = captures.count { it.entryStatus.isPending }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).rowTap(onOpenDay).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Label(
                        weekdayName(day.localDate) + (if (isToday) " · today" else ""),
                        paper,
                        color = if (isToday) paper.ink else paper.inkSoft,
                        size = 9.sp,
                        tracking = 1.5.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Numeral(
                            Money.ofMinor(figure).formatWithSymbol(currency),
                            paper,
                            color = paper.ink,
                            size = 14.sp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Label(
                            "${captures.size} " +
                                (if (captures.size == 1) "photograph" else "photographs") +
                                if (pending > 0) " · $pending waiting" else "",
                            paper,
                            color = if (pending > 0) paper.blue else paper.inkFaint,
                            size = 9.sp,
                            tracking = 0.6.sp,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .semantics {
                        contentDescription =
                            if (expanded) "Collapse ${spokenDay(day.localDate.iso)}" else
                                "Expand ${spokenDay(day.localDate.iso)}"
                    }
                    .rowTap(onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Chevron(paper, up = expanded, tint = paper.inkFaint, box = 10.dp)
            }
        }
        Rule(paper)
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            captures.forEach { capture ->
                Entry(
                    capture = capture,
                    title = null,
                    currency = currency,
                    trailing = null,
                    paper = paper,
                ) { onEntry(capture) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/**
 * One photograph in the feed.
 *
 * A photograph on the left at a size you can recognise a receipt from, the name and
 * the state beside it, the figure on the right — the shape a contact sheet has, not
 * the shape a settings list has. [title] is passed only where the model's naming
 * would be a lookup per row; [trailing] replaces the state word when the day matters
 * more than the state.
 */
@Composable
private fun Entry(
    capture: Capture,
    title: String?,
    currency: CurrencyCode,
    trailing: String?,
    paper: Paper,
    onClick: () -> Unit,
) {
    val status = capture.entryStatus
    Row(
        Modifier
            .fillMaxWidth()
            .rowTap(onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(56.dp)
                .aspectRatio(1f)
                .border(Bone.Hairline, paper.edge),
        ) {
            PhotoPreview(
                photoPath = capture.photoPath,
                caption = title ?: capture.merchant ?: "A photograph",
                paper = paper,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Title(title ?: capture.merchant ?: "A photograph", paper, size = 15.sp)
            Spacer(Modifier.height(3.dp))
            Label(
                if (capture.amountMinor != null && capture.currency != null) {
                    capture.currency.code
                } else {
                    status.label
                },
                paper,
                color = if (status.isPending) paper.blue else paper.inkFaint,
                size = 9.sp,
                tracking = 0.6.sp,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Label(trailing, paper, color = paper.inkFaint, size = 9.sp, tracking = 0.6.sp)
        }
        Spacer(Modifier.width(10.dp))
        Numeral(
            capture.amountMinor?.let {
                Money.ofMinor(it).formatWithSymbol(capture.currency ?: currency)
            } ?: "—",
            paper,
            color = if (capture.amountMinor == null) paper.inkFaint else paper.ink,
            size = 15.sp,
        )
    }
}

// ── 7. everything else, on the page ─────────────────────────────────────────────

@Composable
private fun MoreStrip(model: AppModel, paper: Paper) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(Bone.Hairline, paper.edge, RoundedCornerShape(2.dp)),
    ) {
        MoreItem(Glyph.Trip, "trips", paper, Modifier.weight(1f)) { model.go(Route.Trips) }
        Box(Modifier.width(Bone.Hairline).height(56.dp).background(paper.edge))
        MoreItem(Glyph.Search, "search", paper, Modifier.weight(1f)) { model.go(Route.Search) }
        Box(Modifier.width(Bone.Hairline).height(56.dp).background(paper.edge))
        MoreItem(Glyph.Gear, "settings", paper, Modifier.weight(1f)) {
            model.go(Route.Preferences)
        }
    }
}

@Composable
private fun MoreItem(
    glyph: Glyph,
    text: String,
    paper: Paper,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .rowTap(onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, paper, size = 14.dp, color = paper.inkSoft)
        Spacer(Modifier.width(8.dp))
        Label(text, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.4.sp)
    }
}

// ── small parts ────────────────────────────────────────────────────────────────

/** The chips over the feed: square, ink when chosen, a hairline when not. */
@Composable
private fun Chip(text: String, count: Int, selected: Boolean, paper: Paper, onClick: () -> Unit) {
    Row(
        Modifier
            .background(if (selected) paper.ink else Color.Transparent, RoundedCornerShape(1.dp))
            .border(
                Bone.Hairline,
                if (selected) paper.ink else paper.edgeStrong,
                RoundedCornerShape(1.dp),
            )
            .rowTap(onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(
            text,
            paper,
            color = if (selected) paper.onBlue else paper.inkMid,
            size = 9.sp,
            tracking = 1.2.sp,
        )
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier
                .background(
                    if (selected) paper.onBlue.copy(alpha = 0.18f) else paper.sunk,
                    CircleShape,
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Numeral(
                "$count",
                paper,
                color = if (selected) paper.onBlue else paper.inkSoft,
                size = 10.sp,
            )
        }
    }
}

/** A tag on the ink or on paper: the currency, a count. Square, bordered, quiet. */
@Composable
private fun Tag(text: String, paper: Paper, onInk: Boolean = false) {
    val edge = if (onInk) paper.onBlue.copy(alpha = 0.28f) else paper.edgeStrong
    val ink = if (onInk) paper.onBlue.copy(alpha = 0.82f) else paper.inkSoft
    Box(
        Modifier
            .border(Bone.Hairline, edge, RoundedCornerShape(1.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Label(text, paper, color = ink, size = 8.sp, tracking = 1.1.sp)
    }
}

/** A round control: the settings gear, the settle button. Circle, because it is not a row. */
@Composable
private fun RoundButton(
    glyph: Glyph,
    description: String,
    paper: Paper,
    ink: Color,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(46.dp)
            .semantics { contentDescription = description }
            .rowTap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .then(
                    if (filled) {
                        Modifier.background(paper.ink, CircleShape)
                    } else {
                        Modifier.border(Bone.Hairline, paper.edgeStrong, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                glyph,
                paper,
                size = 17.dp,
                color = if (filled) ink else paper.inkMid,
            )
        }
    }
}

/** A drawn chevron. There is no chevron in the glyph set, and one is not an icon. */
@Composable
private fun Chevron(paper: Paper, up: Boolean, tint: Color, box: Dp) {
    Canvas(Modifier.size(box)) {
        val w = this.size.width
        val h = this.size.height
        val top = if (up) h * 0.68f else h * 0.32f
        val bottom = if (up) h * 0.32f else h * 0.68f
        drawLine(tint, Offset(w * 0.08f, top), Offset(w * 0.5f, bottom), 1.6f)
        drawLine(tint, Offset(w * 0.5f, bottom), Offset(w * 0.92f, top), 1.6f)
    }
}

/** The separator between two facts on one line. */
@Composable
private fun Dot(paper: Paper) {
    Spacer(Modifier.width(9.dp))
    Box(Modifier.size(3.dp).background(paper.inkGhost, CircleShape))
    Spacer(Modifier.width(9.dp))
}

// ── the two states a home page can also be in ───────────────────────────────────

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
    Spacer(Modifier.height(10.dp))
    QuietAction("Settings", paper, { model.go(Route.Preferences) }, color = paper.inkSoft)
    AriadneBarSpacer()
}
