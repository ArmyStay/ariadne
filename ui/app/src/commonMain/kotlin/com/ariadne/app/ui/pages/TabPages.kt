package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import androidx.compose.ui.text.font.FontWeight
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.ScrollRail
import com.ariadne.design.Label
import com.ariadne.design.Motion
import com.ariadne.design.NavRow
import com.ariadne.design.Reveal
import com.ariadne.design.SectionBreak
import com.ariadne.design.Rule
import com.ariadne.design.Numeral
import com.ariadne.design.motionSpec
import com.ariadne.design.OutlineAction
import com.ariadne.design.Masthead
import com.ariadne.design.Page
import com.ariadne.design.RailFigure
import com.ariadne.design.SectionHead
import com.ariadne.design.GlobeSky
import com.ariadne.design.Glyph
import com.ariadne.design.MarkHead
import com.ariadne.design.StatusTag
import com.ariadne.design.animatedLong
import com.ariadne.design.Grid
import com.ariadne.design.Icon
import com.ariadne.design.PageAction
import com.ariadne.design.Paper
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.QuietAction
import com.ariadne.design.Tick
import com.ariadne.design.Title
import com.ariadne.design.TravelGlobeArtwork
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DaySummary
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.Settings
import com.ariadne.model.dayRange
import com.ariadne.model.symbol

/**
 * Wraps every tab view in the same page furniture.
 *
 * The shell is the grid from `:core:design`: a hanging rail on the left for dates
 * and figures, the measure-bounded column on the right. On a phone the rail folds
 * away and the same content is one column at 24dp margins.
 */
@Composable
private fun TabPage(
    paper: Paper,
    wide: Boolean,
    railDocked: Boolean,
    onToggleRail: () -> Unit,
    rail: (@Composable ColumnScope.() -> Unit)? = null,
    backdrop: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Page(
        paper,
        wide,
        railDocked = railDocked,
        onToggleRail = onToggleRail,
        rail = rail,
        backdrop = backdrop,
    ) {
        val scroll = rememberScrollState()
        if (wide) {
            Row(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(bottom = 40.dp),
                ) { content() }
                Box(Modifier.fillMaxHeight().padding(start = 10.dp)) {
                    ScrollRail(scroll, paper, Modifier.fillMaxHeight().offset(x = 4.dp))
                }
            }
        } else {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(bottom = 40.dp),
                ) { content() }
                ScrollRail(
                    scroll,
                    paper,
                    Modifier.align(Alignment.TopEnd).fillMaxHeight().offset(x = Bone.Margin),
                )
            }
        }
    }
}


/**
 * The four destinations in the bottom bar, as single views.
 *
 * They are not "screens" in the routed sense — the routed pages (a trip, a day,
 * the card) still sit on top of these. Each one is a scroller with a headline, a
 * figure and a list, in the reference's language: white panels on paper, hairline
 * rules, tracked-out labels, one blue action.
 */

// ── 1. Home ─────────────────────────────────────────────────────────────────

@Composable
public fun HomePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip
    val closedTrip = trip?.takeIf { it.isClosed }
    TabPage(
        paper,
        wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            if (trip != null) {
                RailFigure("today", state.today?.iso ?: "—", paper, 13.sp)
                Spacer(Modifier.height(22.dp))

                RailFigure("days", "${dayRange(trip.startDate, trip.endDate).size}", paper, 26.sp)
                Spacer(Modifier.height(4.dp))
                Label("closed ${state.dayCards.count { it.isClosed }}", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
                Spacer(Modifier.height(26.dp))
                RailFigure("waiting today", "${state.stack.size}", paper, 26.sp)
                Spacer(Modifier.height(4.dp))
                Label("on this day", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            }
        },
    ) {
        HomeHero(
            title = trip?.name?.lowercase()
                ?: if (state.trips.isEmpty()) "somewhere new" else "choose a trip",
            paper = paper,
            wide = wide,
        )

        if (trip == null) {
            Spacer(Modifier.height(28.dp))
            Body(
                if (state.trips.isEmpty()) {
                    "A journey, from its first day to its last. Photographs now; figures at nightfall."
                } else {
                    "Every trip on this device is closed. Open one from the register, or begin another."
                },
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 18.sp,
                lineHeight = 27.sp,
            )
            Spacer(Modifier.height(30.dp))
            Action("Create a trip", paper, { model.startEmptyTrip() }, accent = true)
            Spacer(Modifier.height(12.dp))
            if (state.trips.isEmpty()) {
                QuietAction("Open the trip register", paper, { model.go(Route.Trips) }, color = paper.inkSoft)
            }
            return@TabPage
        }

        // A finished trip is read, not photographed. Home says so and offers the
        // two real places: its card, and the register where another trip begins.
        if (closedTrip != null) {
            Spacer(Modifier.height(26.dp))
            Body(
                "This trip is closed. Nothing new can be filed against it; the record " +
                    "and the card stay exactly as they were.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 18.sp,
                lineHeight = 27.sp,
            )
            Spacer(Modifier.height(28.dp))
            Action("See the card", paper, { model.go(Route.TripCard(closedTrip.id)) }, accent = true)
            Spacer(Modifier.height(12.dp))
            OutlineAction("The register", paper, { model.go(Route.Trips) })
            return@TabPage
        }

        HomeBalancePanel(
            amount = Money.ofMinor(state.dayTotalMinor)
                .formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
            status = if (state.stack.isEmpty()) "Finished today" else "${state.stack.size} waiting today",
            paper = paper,
            wide = wide,
            onSettle = { model.openCloseDay(state.today) },
        )
        Spacer(Modifier.height(14.dp))
        HomeManualEntry(paper) { model.go(Route.ManualEntry) }
        Spacer(Modifier.height(18.dp))
        HomeTripHeading(
            start = trip.startDate,
            end = trip.endDate,
            paper = paper,
        )
        Spacer(Modifier.height(8.dp))

        // The shelf, first: everything else on this page is about the trip you are
        // in, and this is the way out of it.
        Reveal(0) {
            HomeShortcut(
                glyph = Glyph.Trip,
                title = "Trips",
                detail = if (state.trips.size <= 1) "the register" else "${state.trips.size} in the register",
                paper = paper,
                onClick = { model.go(Route.Trips) },
            )
        }
        Rule(paper, color = paper.edge.copy(alpha = 0.7f))

        // The index settles in, one row behind the next. Each row is a Reveal, so
        // the page arrives as a composed set rather than as a block that appears.
        Reveal(1) {
            HomeShortcut(
                glyph = Glyph.Date,
                title = "Timeline",
                detail = "${state.dayCards.count { it.isClosed }} of ${state.dayCards.size} days closed",
                paper = paper,
                onClick = { model.go(Route.Days) },
            )
        }
        Rule(paper, color = paper.edge.copy(alpha = 0.7f))
        Reveal(2) {
            HomeShortcut(
                glyph = Glyph.Record,
                title = "The record",
                detail = "${state.dayCaptures.size} kept today",
                paper = paper,
                onClick = { model.go(Route.Record(state.today)) },
            )
        }
        Rule(paper, color = paper.edge.copy(alpha = 0.7f))
        Reveal(3) {
            HomeShortcut(
                glyph = Glyph.Search,
                title = "Search what you kept",
                detail = "by shop, price, day",
                paper = paper,
                onClick = { model.go(Route.Search) },
            )
        }
        Spacer(Modifier.height(24.dp))
        SectionHead("III", "recent records", paper, trailing = "across this trip")
        val recent = state.dayCapturesAll.values.flatten()
            .sortedByDescending { it.createdAt }
            .take(4)
        if (recent.isEmpty()) {
            Body(
                "Photographs and manual expenses will appear here as you keep them.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
        } else {
            recent.forEach { capture ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(capture.id) {
                            detectTapGestures(onTap = { model.openDetail(capture.id) })
                        }
                        .semantics {
                            contentDescription = "Open ${model.titleOf(capture)}, ${capture.localDate.iso}"
                            role = Role.Button
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .border(Bone.Hairline, paper.edge),
                    ) {
                        PhotoPreview(
                            capture.photoPath,
                            model.titleOf(capture),
                            paper,
                            Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Title(model.titleOf(capture), paper, size = 15.sp)
                        Spacer(Modifier.height(3.dp))
                        Label(capture.localDate.iso, paper, color = paper.inkFaint, size = 9.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Numeral(
                        capture.amountMinor?.let {
                            Money.ofMinor(it).format(capture.currency ?: trip.baseCurrency)
                        } ?: "—",
                        paper,
                        color = if (capture.amountMinor == null) paper.inkFaint else paper.ink,
                        size = 15.sp,
                    )
                }
                Rule(paper, color = paper.edge.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(22.dp))
        // One line, no argument. The old sentence argued with an imaginary
        // competitor; a guest book does not do that.
        Body(
            "No account. No sync. The record stays here.",
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun HomeHero(title: String, paper: Paper, wide: Boolean) {
    val titleSize = when {
        title.length > 18 -> if (wide) 48.sp else 38.sp
        title.length > 12 -> if (wide) 56.sp else 46.sp
        else -> if (wide) 68.sp else 56.sp
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (wide) 218.dp else 190.dp)
            .clipToBounds(),
    ) {
        TravelGlobeArtwork(
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = if (wide) (-36).dp else (-26).dp)
                .width(if (wide) 340.dp else 300.dp)
                .aspectRatio(0.5333f),
            opacity = if (paper.isNight) 0.3f else 0.92f,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            paper.bg.copy(alpha = 0.98f),
                            paper.bg.copy(alpha = 0.78f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("01", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .width(18.dp)
                        .height(Bone.Hairline)
                        .background(paper.edgeStrong),
                )
                Spacer(Modifier.width(10.dp))
                Label("Ariadne", paper, color = paper.inkSoft, size = 9.sp, tracking = 1.6.sp)
            }
            Spacer(Modifier.height(if (wide) 18.dp else 14.dp))
            Title(
                title,
                paper,
                size = titleSize,
                italic = true,
                lineHeight = titleSize * 1.02f,
            )
            Spacer(Modifier.height(5.dp))
            Label(
                "Plan · Track · Spend",
                paper,
                color = paper.inkFaint,
                size = 10.sp,
                tracking = 1.5.sp,
            )
        }
    }
}

@Composable
private fun HomeBalancePanel(
    amount: String,
    status: String,
    paper: Paper,
    wide: Boolean,
    onSettle: () -> Unit,
) {
    val navy = Color(0xFF13243E)
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape)
            .clip(shape)
            .background(navy),
    ) {
        TravelGlobeArtwork(
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.matchParentSize(),
            opacity = 0.16f,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Color.White, CircleShape))
                Spacer(Modifier.width(9.dp))
                Body("Spent today", paper, color = Color.White, size = 13.sp)
            }
            Spacer(Modifier.height(3.dp))
            Title(
                amount,
                paper,
                color = Color.White,
                size = if (amount.length > 14) 34.sp else if (wide) 48.sp else 44.sp,
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(status, paper, color = Color.White.copy(alpha = 0.68f), size = 12.sp)
                Spacer(Modifier.weight(1f))
                HomeSettleAction(paper, onSettle)
            }
        }
    }
}

@Composable
private fun HomeSettleAction(paper: Paper, onClick: () -> Unit) {
    val outline = Color.White.copy(alpha = 0.72f)
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .border(1.dp, outline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Body("Settle today", paper, color = Color.White, size = 12.sp)
        Spacer(Modifier.width(7.dp))
        Body("→", paper, color = Color.White, size = 16.sp)
    }
}

@Composable
private fun HomeManualEntry(paper: Paper, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(paper.raised)
            .border(1.dp, paper.edgeStrong, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(21.dp).background(paper.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Body("+", paper, color = paper.onBlue, size = 18.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Body("Add an expense manually", paper, color = paper.inkMid, size = 13.sp)
    }
}

@Composable
private fun HomeTripHeading(start: LocalDay, end: LocalDay, paper: Paper) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label("II", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
                Spacer(Modifier.width(10.dp))
                Title("The Trip", paper, size = 19.sp)
            }
            Spacer(Modifier.width(10.dp))
            Label(homeTripDateRange(start, end), paper, color = paper.inkSoft, size = 9.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .padding(start = 22.dp)
                .width(68.dp)
                .height(Bone.Hairline)
                .background(paper.edgeStrong),
        )
    }
}

private fun homeTripDateRange(start: LocalDay, end: LocalDay): String {
    fun month(day: LocalDay): String = when (day.month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
    }

    val startYear = if (start.year == end.year) "" else " ${start.year}"
    return "${start.day} ${month(start)}$startYear — ${end.day} ${month(end)} ${end.year}"
}

@Composable
private fun HomeShortcut(
    glyph: Glyph,
    title: String,
    detail: String,
    paper: Paper,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, paper, size = 19.dp, color = paper.inkFaint)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Title(title, paper, size = 16.sp)
            Spacer(Modifier.height(2.dp))
            Label(detail, paper, color = paper.inkFaint, size = 8.sp, tracking = 0.8.sp)
        }
        Body("›", paper, color = paper.inkFaint, size = 22.sp)
    }
}

// ── 2. Timeline ─────────────────────────────────────────────────────────────

/**
 * The journey, and every day in it.
 *
 * Three blocks in the order a traveller reads them: *which* trip this is (the
 * masthead, and the trip's own name under it), what it has cost so far, and what
 * each day holds.
 *
 * The days are a ledger spine, not a list of cards: a hairline down the margin
 * with a ring for every date, the date set as a printed block, and the state of
 * the day — `IN THE RECORD`, `SETTLED`, `2 TO SETTLE` — stamped beside it. That
 * is the one shape that makes a run of dates read as one continuous record; the
 * same rows stacked as cards read as a feed.
 *
 * The globe behind the masthead is the only picture in the book. It is drawn
 * outside the scroller (see `Page`'s `backdrop`), so it does not slide under the
 * days as they scroll: a watermark that moves is a banner, one that stays is
 * paper.
 */
@Composable
public fun TimelinePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val days = state.dayCards
    val trip = state.trip
    val closed = days.count { it.isClosed }
    // `tripTotalMinor` and every `dayFigures` entry come out of the same pass in
    // `loadTripDays`, each capture converted at its own pinned rate, so the total
    // is one currency's money rather than a sum of currencies.
    val currency = trip?.baseCurrency ?: CurrencyCode.of(state.dayCurrency)

    TabPage(
        paper,
        wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            RailFigure("closed", "$closed", paper, 26.sp)
            Spacer(Modifier.height(26.dp))
            RailFigure("open", "${days.size - closed}", paper, 26.sp)
            Spacer(Modifier.height(26.dp))
            Label("the trip as a run of days", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        },
        backdrop = {
            // Full width so the wash can be hung off the *page* edge rather than
            // the text column's, which is the difference between a plate and a
            // picture dropped into a column.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                GlobeSky(
                    paper,
                    Modifier
                        .offset(x = if (wide) 96.dp else 52.dp, y = if (wide) (-44).dp else (-24).dp)
                        .width(if (wide) 470.dp else 322.dp)
                        .height(if (wide) 633.dp else 434.dp),
                )
            }
        },
    ) {
        Masthead(
            number = "02",
            eyebrow = trip?.name ?: "Timeline",
            title = "Timeline",
            paper = paper,
            wide = wide,
            // The headline is the page here, so it is set larger than the book's
            // display scale and left upright: an italic headline over a
            // watercolour reads as a magazine cover, and this is a ledger.
            titleSize = if (wide) 76.sp else 50.sp,
            lede = "The journey and every day in it.",
            trailing = {
                QuietAction("Record", paper, { model.go(Route.Record(state.today)) }, color = paper.ink)
            },
        )

        if (days.isEmpty() || trip == null) {
            Spacer(Modifier.height(18.dp))
            Body("A trip has no days until it has dates.", paper, color = paper.inkSoft, size = 16.sp)
            Spacer(Modifier.height(22.dp))
            Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
            Spacer(Modifier.height(120.dp))
            return@TabPage
        }

        // ── the journey ──────────────────────────────────────────────────────
        MarkHead("The journey", paper)
        Spacer(Modifier.height(2.dp))
        Title(
            trip.name,
            paper,
            size = if (wide) 46.sp else 34.sp,
            lineHeight = if (wide) 48.sp else 36.sp,
        )
        Spacer(Modifier.height(10.dp))
        Label(
            // Where the trip is, if it was named, then the dates. Repeating the
            // headline above would be a line that says nothing twice.
            listOfNotNull(
                trip.place ?: trip.name,
                "${trip.startDate.iso} — ${trip.endDate.iso}",
            ).joinToString(" · "),
            paper,
            color = paper.inkFaint,
            size = 9.sp,
            tracking = 1.3.sp,
        )

        Spacer(Modifier.height(28.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                // A closed trip has no "so far" left in it: the figure is final,
                // and the label says so rather than pretending the trip is moving.
                Label(
                    if (trip.isClosed) "Trip total" else "Spent so far",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 0.4.sp,
                )
                Spacer(Modifier.height(6.dp))
                Numeral(
                    Money.ofMinor(state.tripTotalMinor).formatWithSymbol(currency),
                    paper,
                    size = if (wide) 44.sp else 33.sp,
                    serif = true,
                )
                trip.budgetMinor?.let { budget ->
                    Spacer(Modifier.height(6.dp))
                    Label(
                        "budget ${Money.ofMinor(budget).formatWithSymbol(currency)}",
                        paper,
                        color = paper.inkFaint,
                        size = 9.sp,
                        tracking = 0.4.sp,
                    )
                }
            }
            Label(
                "$closed of ${days.size} days closed",
                paper,
                color = paper.inkSoft,
                size = 9.sp,
                tracking = 1.3.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
        Rule(paper, color = paper.edgeStrong)

        // ── the days ─────────────────────────────────────────────────────────
        MarkHead("The days", paper)

        days.forEach { card ->
            // Every day, loaded in one pass — never "no photographs" for a day
            // the app has simply not looked at.
            val captures = state.dayCapturesAll[card.localDate.iso].orEmpty()
            val minor = state.dayFigures[card.localDate.iso] ?: 0L
            TimelineDayRow(
                day = card.localDate,
                closed = card.isClosed,
                pending = DaySummary.of(captures).pending,
                kept = captures.size,
                figure = if (captures.isEmpty()) null else {
                    Money.ofMinor(minor).formatWithSymbol(currency)
                },
                paper = paper,
            ) { model.openRecord(card.localDate) }
        }

        Spacer(Modifier.height(120.dp))
    }
}

/**
 * One day of the journey.
 *
 * A ring on the spine, the date as a printed block (number, month, weekday), the
 * day's state stamped beside it, and one line saying what was kept. The whole row
 * is the tap target, and it carries a description of all four facts, because a
 * screen reader gets nothing from a hairline.
 */
@Composable
private fun TimelineDayRow(
    day: LocalDay,
    closed: Boolean,
    pending: Int,
    kept: Int,
    figure: String?,
    paper: Paper,
    onClick: () -> Unit,
) {
    val word = when {
        closed -> "In the record"
        pending == 0 -> "Settled"
        else -> "$pending to settle"
    }
    val line = when {
        kept == 0 -> "Nothing kept"
        figure != null -> "$kept kept · $figure"
        else -> "$kept kept"
    }
    val spoken = "${day.iso}, ${word.lowercase()}, ${line.lowercase()}"

    Row(
        // The row's height comes from its tallest *content* — the date block —
        // and the spine and the rule between the columns fill it. A minimum on
        // the date block, not a fixed height on the row: at 200% text the row
        // grows and the spine grows with it instead of the date clipping.
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .rowTap(onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.Top,
    ) {
        // The spine: a hairline down the margin with a ring for each day. The ring
        // is filled with the page, so the line reads as passing behind it.
        Box(Modifier.width(22.dp).fillMaxHeight()) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(Bone.Hairline)
                    .fillMaxHeight()
                    .background(paper.edgeStrong),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 17.dp)
                    .size(7.dp)
                    .background(paper.bg, CircleShape)
                    .border(Bone.Hairline, paper.edgeStrong, CircleShape),
            )
        }

        Column(
            Modifier
                .widthIn(min = 52.dp)
                .heightIn(min = 84.dp)
                .padding(start = 6.dp, top = 8.dp, bottom = 14.dp),
        ) {
            Numeral(day.iso.takeLast(2), paper, size = 30.sp, serif = true)
            Spacer(Modifier.height(4.dp))
            Label(localMonth(day), paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
            Label(weekdayName(day), paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
        }

        // The rule between the date and the day's content: the same upright
        // hairline the section head uses, run the height of the row.
        Box(Modifier.width(Bone.Hairline).fillMaxHeight().background(paper.edge))

        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 16.dp, top = 14.dp, bottom = 14.dp),
                ) {
                    StatusTag(word, paper)
                    Spacer(Modifier.height(10.dp))
                    Label(line, paper, color = paper.inkMid, size = 10.sp, tracking = 1.5.sp)
                }
                Box(
                    Modifier.fillMaxHeight().padding(end = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Glyph.Chevron, paper, size = 14.dp, color = paper.inkFaint)
                }
            }
            Rule(paper, color = paper.edge)
        }
    }
}

/** "Nov" from a local day, for the date block on the timeline. */
private fun localMonth(day: LocalDay): String = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)[(day.month - 1).coerceIn(0, 11)]

// ── 3. Stats ────────────────────────────────────────────────────────────────

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
        Masthead(
            number = "03",
            eyebrow = state.trip?.name ?: "Stats",
            title = "What it came to",
            paper = paper,
            italic = true,
            wide = wide,
        )

        Spacer(Modifier.height(20.dp))

        // The trip's figure, not the open day's: this page is about the whole thing,
        // and a headline that disagreed with the breakdown below it would be a bug
        // wearing a design.
        val tripTotal = state.tripTotalMinor
        val base = state.trip?.baseCurrency ?: CurrencyCode.of(state.dayCurrency)

        // The figure, set on the paper itself. No card: a number this size carries
        // its own emphasis, and a white box around it is decoration that also
        // happens to be the default container in every generated layout.
        Label("the trip so far", paper, color = paper.inkSoft)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral(CurrencyCode.of(base.code).symbol(), paper, color = paper.inkFaint, size = 22.sp)
            Spacer(Modifier.width(6.dp))
            // Counts to the new total rather than snapping: this figure is read
            // against a fence, and seeing which way it moved is the point.
            val shown = animatedLong(tripTotal)
            Numeral(
                Money.ofMinor(shown).format(base),
                paper,
                size = if (wide) 64.sp else 50.sp,
                weight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(12.dp))
        Rule(paper, color = paper.ink)
        Spacer(Modifier.height(7.dp))
        // The qualifier belongs to the figure, not to the note three sections
        // below it: a total is read before anything under it.
        Label("finished entries only", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.3.sp)
        Spacer(Modifier.height(14.dp))

        // The fence, if one was set. Over budget is stated plainly and in the same
        // voice as under it: this is a record, not a scold.
        val budget = state.trip?.budgetMinor
        if (budget != null) {
            val remaining = budget - tripTotal
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label("against a budget of", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
                Spacer(Modifier.width(8.dp))
                Label(
                    Money.ofMinor(budget).formatWithSymbol(base),
                    paper,
                    color = paper.inkSoft,
                    size = 10.sp,
                    tracking = 1.2.sp,
                )
                Spacer(Modifier.weight(1f))
                Label(
                    if (remaining >= 0) "left" else "over",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 1.4.sp,
                )
                Spacer(Modifier.width(8.dp))
                Numeral(
                    Money.ofMinor(if (remaining >= 0) remaining else -remaining).format(base),
                    paper,
                    size = 15.sp,
                    weight = FontWeight.Medium,
                    color = if (remaining >= 0) paper.ink else paper.blue,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        Body(
            "Only entries that were finished. Anything still waiting across this trip is not " +
                "in this figure, and is not counted as zero.",
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            lineHeight = 18.sp,
        )

        if (state.dayFigures.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionHead("II", "daily spending", paper)
            Label(
                "finished entries · ${base.code}",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.2.sp,
            )
            Spacer(Modifier.height(10.dp))
            DailySpendChart(state, paper)
            Spacer(Modifier.height(22.dp))
        }

        if (state.categoryTotals.isNotEmpty()) {
            SectionHead("III", "what it was", paper)
            Breakdown(
                totals = state.categoryTotals,
                baseCurrency = base,
                totalBaseMinor = state.tripTotalMinor,
                paper = paper,
            )
            PatternLine(state.categoryTotals, base, paper)
            Spacer(Modifier.height(10.dp))
            Body(
                "Nothing here is sorted for you. A category is a claim you make on an entry, " +
                    "and most entries never need one.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(26.dp))
        }

        if (state.tripPerCurrency.isNotEmpty()) {
            // The same finished photographs, seen from the other side. The two
            // columns must always add to the same figure.
            SectionHead("IV", "by currency, before conversion", paper)
            state.tripPerCurrency.forEach { (code, minor) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tick(paper, 16.dp, paper.inkGhost)
                        Spacer(Modifier.width(12.dp))
                        Title(code, paper, size = 18.sp)
                    }
                    Numeral(Money.ofMinor(minor).format(CurrencyCode.of(code)), paper, size = 17.sp)
                }
                Rule(paper)
            }
            Label(
                "these are the amounts as they were paid, not converted",
                paper,
                color = paper.inkFaint,
                tracking = 1.2.sp,
                size = 9.sp,
            )
        } else if (state.categoryTotals.isEmpty()) {
            Body(
                "Nothing has been tallied yet. The figures appear here the first time you confirm an entry.",
                paper,
                color = paper.inkSoft,
                size = 15.sp,
                lineHeight = 22.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
        // Nothing to settle on a day already filed: the bar goes inert rather
        // than opening a ritual with an empty stack.
        val todayClosed = state.dayCards.firstOrNull { it.localDate == state.today }?.isClosed == true
        val tripClosed = state.trip?.isClosed == true
        Action(
            "Close today",
            paper,
            { model.openCloseDay(state.today) },
            accent = true,
            enabled = state.stack.isNotEmpty() && !todayClosed && !tripClosed,
        )

        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun DailySpendChart(state: AppUiState, paper: Paper) {
    val values = state.dayCards.map { it.localDate }.takeLast(7).map { day ->
        day to (state.dayFigures[day.iso] ?: 0L)
    }
    val maximum = values.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    Canvas(Modifier.fillMaxWidth().height(108.dp)) {
        val columnWidth = size.width / (values.size * 2f)
        val chartHeight = size.height - 2f
        values.forEachIndexed { index, (_, value) ->
            val barHeight = (value.toDouble() / maximum.toDouble() * chartHeight).toFloat()
            drawRect(
                color = if (index == values.lastIndex) paper.blue else paper.inkSoft.copy(alpha = 0.62f),
                topLeft = Offset(columnWidth * (index * 2 + 0.5f), chartHeight - barHeight),
                size = Size(columnWidth, barHeight),
            )
        }
        drawLine(
            color = paper.edgeStrong,
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx(),
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        values.forEach { (day, _) ->
            Label(day.iso.takeLast(2), paper, color = paper.inkFaint, size = 8.sp)
        }
    }
}

// ── 4. Preferences ──────────────────────────────────────────────────────────

@Composable
public fun PreferencesPage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    lastFailure: String? = null,
) {
    TabPage(
        paper,
        wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            Label("on device only", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            Spacer(Modifier.height(10.dp))
            Label("no account · no sync", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        },
    ) {
        Masthead(
            number = "04",
            eyebrow = "Ariadne",
            title = "Settings",
            paper = paper,
            wide = wide,
        )

        Spacer(Modifier.height(14.dp))
        SectionHead("I", "your profile", paper)
        LineField(
            label = "display name · kept on this device",
            value = state.displayName,
            placeholder = "Your name",
            paper = paper,
            onChange = model::editProfileName,
            serif = false,
        )
        Spacer(Modifier.height(10.dp))

        // Every row below is a read-out of something real, and the two that can be
        // changed are the two that are actually changeable. A row that says
        // "Date format — 2026-11-23" with no way to set it would be decoration
        // pretending to be a setting; the ones that cannot change say so instead.
        SectionHead("II", "the record", paper)

        // Both currencies come from the same catalogue the New trip page uses, so
        // there is one vocabulary for choosing money in the whole app.
        CurrencyField(
            label = "Base currency",
            code = state.trip?.baseCurrency?.code ?: "INR",
            note = "Every total is stated in this one",
            glyph = Glyph.Budget,
            paper = paper,
            onChoose = { model.editTripCurrency(it) },
        )
        CurrencyField(
            label = "Home currency",
            code = state.trip?.homeCurrency?.code ?: "INR",
            note = "What a foreign amount is converted back to",
            glyph = Glyph.Trip,
            paper = paper,
            onChoose = { model.editTripHomeCurrency(it) },
        )
        PrefReadout(Glyph.Date, "Dates", "${state.today?.iso ?: "—"}, ISO order", paper)
        PrefPaper(paper, state.theme) { model.setTheme(it) }

        // ── The exchange rate ────────────────────────────────────────────────
        //
        // Not decoration: without a rate a foreign amount cannot be converted,
        // and every total silently under-reports. The figures are exact scaled
        // integers all the way down; this is the only place a human types one.
        Spacer(Modifier.height(26.dp))
        SectionHead("III", "the exchange", paper)

        val inUse = model.currenciesInUse()
        val foreign = (inUse + listOf("THB", "USD", "EUR", "GBP", "JPY"))
            .filter { it != state.trip?.baseCurrency?.code }
            .distinct()

        // One open field at a time. Two live keyboards' worth of ruled boxes on
        // one page is a form; the page is a ledger with one line you can edit.
        var openCode by remember { mutableStateOf<String?>(null) }

        if (foreign.isEmpty()) {
            Body("Everything so far is already in the base currency.", paper, color = paper.inkFaint, size = 12.sp)
        } else {
            foreign.forEachIndexed { index, code ->
                // Keyed so an opened row's typed text belongs to that currency and
                // does not drift to the next one down when the list changes.
                key(code) {
                    RateRow(
                        code = code,
                        label = model.rateLabel(code),
                        effectiveDate = model.rateDateLabel(code),
                        used = inUse.contains(code),
                        paper = paper,
                        base = state.trip?.baseCurrency?.code ?: "INR",
                        open = openCode == code,
                        onOpen = { openCode = code },
                        onDone = { openCode = null },
                        onSet = { text ->
                            val accepted = model.setFxRate(code, text)
                            if (accepted) openCode = null
                            accepted
                        },
                        onClear = {
                            model.clearFxRate(code)
                            openCode = null
                        },
                    )
                }
                if (index == foreign.lastIndex) Rule(paper)
            }
        }

        if (state.rateError != null) {
            Spacer(Modifier.height(8.dp))
            Body(state.rateError, paper, color = paper.inkMid, size = 12.sp)
        }

        Spacer(Modifier.height(26.dp))
        SectionHead("IV", "what leaves this device", paper)

        PrefReadout(Glyph.Ledger, "Storage", "Local to this build", paper)
        Spacer(Modifier.height(12.dp))
        Body(
            state.storageDisclosure,
            paper,
            color = paper.inkMid,
            size = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(12.dp))
        Body(
            state.rateDisclosure,
            paper,
            color = paper.inkMid,
            size = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(16.dp))
        IconAction(Glyph.Ledger, "Export all data as a ZIP archive", paper) {
            model.go(Route.DataExport)
        }

        // The honest diagnostic. If anything threw and was swallowed - which is
        // exactly how a dead button happens - it is written here rather than
        // living only in a console nobody has attached to.
        if (lastFailure != null) {
            Spacer(Modifier.height(22.dp))
            SectionBreak("something failed", paper, top = 0.dp)
            Label(lastFailure, paper, color = paper.inkSoft, size = 9.sp, tracking = 0.6.sp)
            Spacer(Modifier.height(6.dp))
            Body(
                "This is the last error the app saw. Nothing was lost; it is written here " +
                    "because a failure that only happens in a log is a failure nobody fixes.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHead("V", "this trip", paper)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Trip, paper, size = 16.dp)
            Spacer(Modifier.width(10.dp))
            Title(state.trip?.name ?: "No trip", paper, size = 17.sp)
            Spacer(Modifier.weight(1f))
            Label(
                state.trip?.place ?: "no place set",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.3.sp,
            )
        }
        Spacer(Modifier.height(14.dp))

        IconAction(Glyph.Ledger, "See the trip's page", paper) { model.go(Route.Trip) }
        if (state.trip?.isClosed == true) {
            // Closing is not a thing a closed trip can do again; the card is where
            // the finished record lives.
            IconAction(Glyph.Done, "See the card", paper) {
                state.trip?.let { model.go(Route.TripCard(it.id)) }
            }
        } else {
            IconAction(Glyph.Done, "Close the trip into a card", paper) { model.closeTripIntoCard() }
        }

        Spacer(Modifier.height(22.dp))
        Rule(paper, color = paper.edge)
        Spacer(Modifier.height(14.dp))

        if (!state.deleteArmed) {
            IconAction(Glyph.Remove, "Delete this trip", paper, color = paper.inkMid) {
                model.armTripDeletion()
            }
            Spacer(Modifier.height(6.dp))
            Body(
                "Deletes its entries and photographs. Nothing is kept anywhere else, so this " +
                    "is the one action in the app you cannot take back.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
        } else {
            // Armed. The copy says exactly what will go, and the only way out that
            // is not deletion is the quieter of the two controls.
            Label("this deletes everything, now", paper, color = paper.blue, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Action("Delete for good", paper, { model.deleteTripNow() }, enabled = !state.busy)
                OutlineAction("Keep it", paper, { model.armTripDeletion() })
            }
        }

        Spacer(Modifier.height(96.dp))
    }
}

/**
 * The stock the book is printed on.
 *
 * Three choices, as chips — not a switch, because "System" is a real third answer
 * and a switch has only two. The note under the label says what the current one
 * means, which is the difference between a setting and a riddle.
 */
@Composable
private fun PrefPaper(
    paper: Paper,
    theme: Settings.Theme,
    onChoose: (Settings.Theme) -> Unit,
) {
    val options = listOf(
        Settings.Theme.LIGHT to "Light",
        Settings.Theme.SYSTEM to "System",
        Settings.Theme.DARK to "Night",
    )
    val note = when (theme) {
        Settings.Theme.LIGHT -> "Light paper, always"
        Settings.Theme.SYSTEM -> "Follows this device's light or dark setting"
        Settings.Theme.DARK -> "Night paper: the same design on dark stock"
    }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Paper, paper, size = 16.dp, color = paper.inkSoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Title("Paper", paper, size = 17.sp)
                Spacer(Modifier.height(3.dp))
                Label(note, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.1.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, text) ->
                val chosen = value == theme
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .border(Bone.Hairline, if (chosen) paper.blue else paper.edge)
                        .androidTap { onChoose(value) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Label(
                        text,
                        paper,
                        color = if (chosen) paper.blue else paper.inkSoft,
                        size = 9.sp,
                        tracking = 1.2.sp,
                    )
                }
            }
        }
        Rule(paper)
    }
}

/**
 * One exchange rate, as a sentence.
 *
 * "1 THB = [ 2.5 ] INR" is the form a traveller can check against a receipt or a
 * board at the airport. The alternative - two abstract numbers - is what an
 * engineer would write and what nobody can verify.
 *
 * When a rate is on file the field shows it and the row carries the pair; when
 * one is not, the row says so plainly, because a missing rate is the difference
 * between a total that is right and a total that is quietly wrong.
 */
@Composable
private fun RateRow(
    code: String,
    label: String?,
    effectiveDate: String?,
    used: Boolean,
    paper: Paper,
    base: String,
    open: Boolean,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    onSet: (String) -> Boolean,
    onClear: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var refused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    // Opening the row is what asks for the keyboard; closing it throws away a
    // half-typed rate rather than leaving it to look like a saved one.
    LaunchedEffect(open) {
        if (open) focus.requestFocus() else {
            text = ""
            refused = false
        }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(26.dp)) {
                Icon(Glyph.Budget, paper, size = 15.dp, color = paper.inkSoft)
            }
            Title(code, paper, size = 17.sp)
            Spacer(Modifier.width(10.dp))
            if (used) {
                // Marks the currencies this trip actually holds, so the list is
                // not four speculative rows.
                Tick(paper, width = 14.dp, color = paper.blue)
            }
            Spacer(Modifier.weight(1f))
            if (label != null) {
                QuietAction("Clear", paper, onClear, color = paper.inkFaint)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!open) {
            // A closed row still says what is true; only the field is put away.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(
                        label ?: if (used) {
                            "no rate on file — this currency is not being converted"
                        } else {
                            "no rate on file"
                        },
                        paper,
                        color = paper.inkSoft,
                        size = 9.5.sp,
                        tracking = 1.2.sp,
                    )
                    if (label != null && effectiveDate != null) {
                        Spacer(Modifier.height(3.dp))
                        Label(
                            "rate dated $effectiveDate",
                            paper,
                            color = paper.inkFaint,
                            size = 9.sp,
                            tracking = 0.8.sp,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                QuietAction(if (label == null) "Set rate" else "Change", paper, onOpen, color = paper.blue)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("1 $code =", paper, color = paper.inkSoft, size = 10.sp, tracking = 1.2.sp)
                Spacer(Modifier.width(12.dp))
                // The field is the thing you have to find and hit, so it is a ruled
                // box with a visible baseline rather than a line of floating type.
                Box(
                    Modifier
                        .width(104.dp)
                        .background(paper.sunk)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { v ->
                            text = v.filter { it.isDigit() || it == '.' }.take(12)
                            refused = false
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Bone.Sans,
                            fontSize = 16.sp,
                            color = paper.ink,
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(paper.ink),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                    if (text.isEmpty()) {
                        Label("rate", paper, color = paper.inkFaint, size = 10.sp, tracking = 1.2.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Label(base, paper, color = paper.inkSoft, size = 10.sp, tracking = 1.2.sp)
                Spacer(Modifier.weight(1f))
                if (text.isNotEmpty()) {
                    QuietAction("Set", paper, {
                        val accepted = onSet(text)
                        refused = !accepted
                        if (accepted) text = ""
                    }, color = paper.blue)
                }
                QuietAction("Cancel", paper, onDone, color = paper.inkFaint)
            }
            Spacer(Modifier.height(6.dp))
            Label(
                if (refused) "not a number" else label ?: "no rate on file",
                paper,
                // Every state of a rate row is information, so every state is legible.
                // inkGhost is for line work, never for a sentence.
                color = paper.inkSoft,
                size = 9.5.sp,
                tracking = 1.2.sp,
            )
        }
    }
}

/** A setting that is a fact, not a choice. It says so rather than pretending. */
@Composable
private fun PrefReadout(glyph: Glyph, label: String, value: String, paper: Paper) {
    // Same block as the index rows on the front page: glyph in the hanging
    // margin, title, a figure set in the sans face. A read-out is simply a row
    // that does nothing when tapped, which is why it is the shared one with a
    // no-op rather than a second copy that looks almost the same.
    NavRow(glyph, label, value, paper, onClick = {})
    Rule(paper)
}

/** An icon and a tracked label that is a control. Nothing about it is a link blue. */
@Composable
private fun IconAction(
    glyph: Glyph,
    text: String,
    paper: Paper,
    color: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .androidTap(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, paper, size = 16.dp, color = color ?: paper.inkSoft)
        Spacer(Modifier.width(12.dp))
        Label(text, paper, color = color ?: paper.ink, size = 10.sp, tracking = 1.5.sp)
    }
}

