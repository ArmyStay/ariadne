package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.font.FontWeight
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.ScrollRail
import com.ariadne.design.Label
import com.ariadne.design.LinkAction
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
import com.ariadne.design.PageHeader
import com.ariadne.design.RailFigure
import com.ariadne.design.SectionHead
import com.ariadne.design.Glyph
import com.ariadne.design.animatedLong
import com.ariadne.design.Grid
import com.ariadne.design.Icon
import com.ariadne.design.PageAction
import com.ariadne.design.Paper
import com.ariadne.design.Panel
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.Plate
import com.ariadne.design.QuietAction
import com.ariadne.design.Subject
import com.ariadne.design.Tick
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.DaySummary
import com.ariadne.model.Settings
import com.ariadne.model.dayRange
import com.ariadne.model.symbol


/**
 * The four destinations in the bottom bar, as single views.
 *
 * They are not "screens" in the routed sense — the routed pages (a trip, a day,
 * the card) still sit on top of these. Each one is a scroller with a headline, a
 * figure and a list, in the reference's language: white panels on paper, hairline
 * rules, tracked-out labels, one blue action.
 */

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
    content: @Composable ColumnScope.() -> Unit,
) {
    Page(
        paper,
        wide,
        railDocked = railDocked,
        onToggleRail = onToggleRail,
        rail = rail,
    ) {
        Row(Modifier.fillMaxWidth()) {
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(bottom = 40.dp),
            ) { content() }
            // The bar hangs in the margin, level with the text it measures and
            // never on top of it.
            Box(Modifier.fillMaxHeight().padding(start = 10.dp)) {
                ScrollRail(scroll, paper, Modifier.fillMaxHeight().offset(x = 4.dp))
            }
        }
    }
}

// ── 1. Home ─────────────────────────────────────────────────────────────────

@Composable
public fun HomePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip
    val closedTrip = trip?.takeIf { it.isClosed }
    // "Bring one in from the library" opens the picker itself. Routing to the
    // shutter first and asking for a second tap made a real control feel dead.
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
                RailFigure("today", state.today?.iso ?: "—", paper, 13.sp)
                Spacer(Modifier.height(22.dp))

                RailFigure("days", "${dayRange(trip.startDate, trip.endDate).size}", paper, 26.sp)
                Spacer(Modifier.height(4.dp))
                Label("closed ${state.dayCards.count { it.isClosed }}", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
                Spacer(Modifier.height(26.dp))
                RailFigure("waiting", "${DaySummary.of(state.dayCaptures).pending}", paper, 26.sp)
                Spacer(Modifier.height(4.dp))
                Label("photographs", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            }
        },
    ) {
        Masthead(
            number = "01",
            eyebrow = "Ariadne",
            title = trip?.name ?: if (state.trips.isEmpty()) "No trips yet" else "No trip open",
            paper = paper,
            italic = true,
            wide = wide,
            // The visible word stays PREFS — it is what a printed index would put
            // in this corner — but it opens the Preferences route.
            trailing = { PageAction("Prefs", paper, { model.go(Route.Preferences) }) },
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
            Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
            Spacer(Modifier.height(12.dp))
            if (state.trips.isEmpty()) {
                OutlineAction("Begin with the demo roll", paper, { model.startTripDemo() })
            } else {
                OutlineAction("The register", paper, { model.go(Route.Trips) })
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

        // The figure, hung directly on the paper with no card around it. A number
        // this large does not need a box; the box was only ever decoration.
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            // The "≈" hangs beside the symbol, on the same line, when any part of
            // this figure came from a conversion — the figure is an approximation
            // and says so where the number is, not in a footnote.
            if (state.dayRateDates.isNotEmpty()) {
                Numeral("≈", paper, color = paper.inkFaint, size = 22.sp)
                Spacer(Modifier.width(4.dp))
            }
            Numeral(
                CurrencyCode.of(state.dayCurrency).symbol(),
                paper,
                color = paper.inkFaint,
                size = 22.sp,
            )
            Spacer(Modifier.width(6.dp))
            Numeral(
                Money.ofMinor(state.dayTotalMinor).format(CurrencyCode.of(state.dayCurrency)),
                paper,
                size = if (wide) 68.sp else 52.sp,
                weight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(8.dp))
        ConversionNote(state.dayRateDates, state.dayUnconverted, paper)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Label("finished today", paper, color = paper.inkSoft)
            // Counted from the entry state of the day this row is about, which is
            // the same count the timeline prints for that day. The pill used to read
            // the pending stack instead, so it could say "all settled" over a day
            // the timeline was calling open.
            val waiting = DaySummary.of(state.dayCaptures).pending
            Pill(
                if (waiting == 0) "all settled" else "$waiting waiting",
                paper,
                if (waiting == 0) PillTone.Quiet else PillTone.Blue,
            )
        }

        Spacer(Modifier.height(24.dp))

        // The shutter is the product, so it gets the page's one blue - always,
        // not only on the days a stack happens to exist. Before this, a fresh day
        // showed "Begin tonight's tally" and no way to take a photograph at all,
        // which is the wrong hierarchy for an app whose whole claim is the photo.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1.5f)) {
                Action(
                    // Short enough to fit beside Finish at 440dp without the
                    // word photograph being clipped to "A".
                    "Take a photo",
                    paper,
                    { model.openCapture() },
                    accent = true,
                    icon = Glyph.Shutter,
                )
            }
            if (state.stack.isNotEmpty()) {
                Box(Modifier.weight(1f)) {
                    OutlineAction(
                        "Finish ${state.stack.size}",
                        paper,
                        { model.openNightfall(state.today) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // The library is the second way in - a real action, not a label.
        // Only where a picker exists. A link that opens nothing is worse than
        // no link: the shutter above is already the primary way in.
        if (pickerAvailable()) {
            QuietAction(
                "or bring one in from the library",
                paper,
                importPhoto,
                color = paper.inkSoft,
            )
        }

        SectionHead("II", "the trip", paper, trailing = "${trip.startDate.iso} → ${trip.endDate.iso}")

        // The shelf, first: everything else on this page is about the trip you are
        // in, and this is the way out of it.
        Reveal(0) {
            NavRow(
                Glyph.Trip,
                "Trips",
                if (state.trips.size <= 1) "the register" else "${state.trips.size} in the register",
                paper,
            ) {
                model.go(Route.Trips)
            }
        }
        Rule(paper)

        // The index settles in, one row behind the next. Each row is a Reveal, so
        // the page arrives as a composed set rather than as a block that appears.
        Reveal(1) {
            NavRow(Glyph.Date, "Timeline", "${state.dayCards.count { it.isClosed }} of ${state.dayCards.size} days closed", paper) {
                model.go(Route.Timeline)
            }
        }
        Rule(paper)
        Reveal(2) {
            NavRow(Glyph.Record, "The record", "${state.dayCaptures.size} kept today", paper) {
                model.go(Route.Record(state.today))
            }
        }
        Rule(paper)
        Reveal(3) {
            NavRow(Glyph.Search, "Search what you kept", "by shop, price, day", paper) {
                model.go(Route.Search)
            }
        }
        Rule(paper)
        Reveal(4) {
            NavRow(Glyph.Trip, "The trip's page", "dates, currencies, the card", paper) {
                model.go(Route.Trip)
            }
        }
        Rule(paper)

        Spacer(Modifier.height(30.dp))
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

// ── 2. Timeline ─────────────────────────────────────────────────────────────

@Composable
public fun TimelinePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val days = state.dayCards
    TabPage(
        paper,
        wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            RailFigure("closed", "${days.count { it.isClosed }}", paper, 26.sp)
            Spacer(Modifier.height(26.dp))
            RailFigure("open", "${days.count { !it.isClosed }}", paper, 26.sp)
            Spacer(Modifier.height(26.dp))
            Label("the trip as a run of days", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        },
    ) {
        Masthead(
            number = "02",
            eyebrow = state.trip?.name ?: "Timeline",
            title = "Timeline",
            paper = paper,
            italic = true,
            wide = wide,
            lede = "Each day, and the hours it was spent in.",
            trailing = { PageAction("Record", paper, { model.go(Route.Record(state.today)) }) },
        )

        Spacer(Modifier.height(10.dp))

        if (days.isEmpty()) {
            Body("A trip has no days until it has dates.", paper, color = paper.inkSoft, size = 16.sp)
            Spacer(Modifier.height(22.dp))
            Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
            return@TabPage
        }

        days.forEach { card ->
            // Every day, loaded in one pass — never "no photographs" for a day the
            // app simply has not looked at.
            val captures = state.dayCapturesAll[card.localDate.iso].orEmpty()
            val minor = state.dayFigures[card.localDate.iso] ?: 0L
            val figure = if (captures.isEmpty()) "—" else {
                Money.ofMinor(minor).formatWithSymbol(CurrencyCode.of(state.dayCurrency))
            }
            DayPlate(
                day = card.localDate,
                closed = card.isClosed,
                selected = card.localDate == state.today,
                captures = captures,
                figure = figure,
                paper = paper,
                hourOf = model::hourOf,
            ) { model.openRecord(card.localDate) }
        }

        Spacer(Modifier.height(96.dp))
    }
}

private fun localMonth(day: LocalDay): String = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)[(day.month - 1).coerceIn(0, 11)]

@Composable
private fun DayRow(
    day: LocalDay,
    closed: Boolean,
    selected: Boolean,
    paper: Paper,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pointerInput(day) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The date, as a small printed block.
        Column(Modifier.width(52.dp)) {
            Numeral(day.iso.takeLast(2), paper, size = 22.sp, weight = androidx.compose.ui.text.font.FontWeight.Medium)
            Label(localMonth(day), paper, color = paper.inkFaint, size = 9.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Title(day.iso, paper, size = 17.sp, color = if (selected) paper.ink else paper.inkMid)
            Spacer(Modifier.height(3.dp))
            Label(
                if (closed) "In the record" else "Still open",
                paper,
                color = paper.inkFaint,
                tracking = 1.2.sp,
            )
        }
        Pill(if (closed) "Closed" else "Open", paper, if (closed) PillTone.Ink else PillTone.Quiet)
    }
    Rule(paper)
}

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
                Money.ofMinor(state.categoryTotals.sumOf { it.baseMinor })
                    .formatWithSymbol(state.trip?.baseCurrency ?: CurrencyCode.of(state.dayCurrency)),
                paper,
                16.sp,
            )
            Spacer(Modifier.height(26.dp))
            RailFigure("photographs", "${state.categoryTotals.sumOf { it.captures }}", paper, 26.sp)
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
        val tripTotal = state.categoryTotals.sumOf { it.baseMinor }
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
        Label("finished photographs only", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.3.sp)
        // Money that could not be converted is named here, under the figure it is
        // missing from, rather than folded into it.
        Spacer(Modifier.height(6.dp))
        ConversionNote(emptyMap(), state.tripUnconverted, paper)
        Spacer(Modifier.height(14.dp))

        // The fence, if one was set. Over budget is stated plainly and in the same
        // voice as under it: this is a record, not a scold.
        val budget = state.trip?.budgetMinor
        if (budget != null) {
            val remaining = budget - tripTotal
            // Two columns, not five siblings in one row.
            //
            // This used to be a `Row` of labels with a weighted spacer between them,
            // and at a large font the spacer collapsed to nothing: the budget and the
            // remaining figure came out as "₹10,000.00LEFT". Splitting the row into
            // a side that may shrink and a side that may not, with a real 12dp gap
            // between them, means the words wrap and the money never moves.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Label("budget", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
                    Spacer(Modifier.height(3.dp))
                    // Money inside a shrinkable column: it stays on one line and is
                    // the last thing to give up space.
                    Numeral(
                        Money.ofMinor(budget).formatWithSymbol(base),
                        paper,
                        size = 13.sp,
                        color = paper.inkSoft,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Label(
                        if (remaining >= 0) "left" else "over",
                        paper,
                        color = paper.inkFaint,
                        size = 9.sp,
                        tracking = 1.4.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Numeral(
                        Money.ofMinor(if (remaining >= 0) remaining else -remaining).formatWithSymbol(base),
                        paper,
                        size = 15.sp,
                        weight = FontWeight.Medium,
                        color = if (remaining >= 0) paper.ink else paper.blue,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Body(
            "Only photographs that were finished. Anything still waiting tonight is not in " +
                "this figure, and is not counted as zero.",
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            lineHeight = 18.sp,
        )

        if (state.categoryTotals.isNotEmpty()) {
            SectionHead("II", "what it was", paper)
            Breakdown(
                totals = state.categoryTotals,
                baseCurrency = base,
                totalBaseMinor = state.categoryTotals.sumOf { it.baseMinor },
                paper = paper,
            )
            PatternLine(state.categoryTotals, base, paper)
            Spacer(Modifier.height(10.dp))
            Body(
                "Nothing here is sorted for you. A category is a claim you make on the photograph, " +
                    "and most photographs never need one.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(26.dp))
        }

        if (state.perCurrency.isNotEmpty()) {
            // The same finished photographs, seen from the other side. The two
            // columns must always add to the same figure.
            SectionHead("III", "by currency, before conversion", paper)
            state.perCurrency.forEach { (code, minor) ->
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
                "Nothing has been tallied yet. The figures appear here the first time you finish a " +
                    "photograph at nightfall.",
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
            { model.openNightfall(state.today) },
            accent = true,
            enabled = state.stack.isNotEmpty() && !todayClosed && !tripClosed,
        )

        Spacer(Modifier.height(96.dp))
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
            title = "Preferences",
            paper = paper,
            wide = wide,
            trailing = { PageAction("Done", paper, { model.go(Route.Home) }) },
        )

        Spacer(Modifier.height(14.dp))

        // Every row below is a read-out of something real, and the two that can be
        // changed are the two that are actually changeable. A row that says
        // "Date format — 2026-11-23" with no way to set it would be decoration
        // pretending to be a setting; the ones that cannot change say so instead.
        SectionHead("I", "the record", paper)

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
        PrefReadout(Glyph.Date, "Dates", "${state.today?.iso ?: "2026-11-23"}, ISO order", paper)
        PrefPaper(paper, state.theme) { model.setTheme(it) }

        // ── The exchange rate ────────────────────────────────────────────────
        //
        // Not decoration: without a rate a foreign amount cannot be converted,
        // and every total silently under-reports. The figures are exact scaled
        // integers all the way down; this is the only place a human types one.
        Spacer(Modifier.height(26.dp))
        SectionHead("II", "the exchange", paper)

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
        SectionHead("III", "what leaves this device", paper)

        PrefReadout(Glyph.Ledger, "Reading", "On device only", paper)
        Spacer(Modifier.height(12.dp))
        Body(
            "Nothing leaves. There is no account to sign into and no server to sign into it " +
                "from. Photographs are stored in the app's private folder, the record lives in " +
                "a local database, and there is no network call in the product at all.",
            paper,
            color = paper.inkMid,
            size = 14.sp,
            lineHeight = 21.sp,
        )

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
        SectionHead("IV", "this trip", paper)

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
                "Deletes its photographs and its record. Nothing is kept anywhere else, so this " +
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
                    modifier = Modifier.weight(1f),
                )
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

