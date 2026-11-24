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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ariadne.design.Label
import com.ariadne.design.LinkAction
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.Masthead
import com.ariadne.design.Page
import com.ariadne.design.PageHeader
import com.ariadne.design.RailFigure
import com.ariadne.design.SectionHead
import com.ariadne.design.Glyph
import com.ariadne.design.Grid
import com.ariadne.design.Icon
import com.ariadne.design.Paper
import com.ariadne.design.Panel
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.Plate
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Subject
import com.ariadne.design.Tick
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
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
    rail: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Page(paper, wide, rail = rail) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
        ) { content() }
    }
}

// ── 1. Home ─────────────────────────────────────────────────────────────────

@Composable
public fun HomePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val trip = state.trip
    TabPage(
        paper,
        wide,
        rail = {
            if (trip != null) {
                RailFigure("today", state.today?.iso ?: "—", paper, 13.sp)
                Spacer(Modifier.height(26.dp))
                RailFigure("days", "${dayRange(trip.startDate, trip.endDate).size}", paper, 26.sp)
                Spacer(Modifier.height(4.dp))
                Label("closed ${state.dayCards.count { it.isClosed }}", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
                Spacer(Modifier.height(26.dp))
                RailFigure("waiting", "${state.stack.size}", paper, 26.sp)
                Spacer(Modifier.height(4.dp))
                Label("photographs", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            }
        },
    ) {
        Masthead(
            number = "01",
            eyebrow = "Ariadne",
            title = trip?.name ?: "No trips yet",
            paper = paper,
            italic = true,
            wide = wide,
            trailing = { QuietAction("Prefs", paper, { model.go(Route.Settings) }) },
        )

        if (trip == null) {
            Spacer(Modifier.height(28.dp))
            Body(
                "A trip starts on a date and ends on a date. Everything in between is a photograph " +
                    "you take now and finish tonight.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 18.sp,
                lineHeight = 27.sp,
            )
            Spacer(Modifier.height(30.dp))
            Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
            Spacer(Modifier.height(12.dp))
            OutlineAction("Begin with the demo roll", paper, { model.startTripDemo() })
            return@TabPage
        }

        // The figure, hung directly on the paper with no card around it. A number
        // this large does not need a box; the box was only ever decoration.
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.Bottom) {
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
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Label("finished today", paper, color = paper.inkSoft)
            Pill(
                if (state.stack.isEmpty()) "all settled" else "${state.stack.size} waiting",
                paper,
                if (state.stack.isEmpty()) PillTone.Quiet else PillTone.Blue,
            )
        }

        Spacer(Modifier.height(26.dp))
        // A bar, not a wall: on a wide page the action keeps a button's width and
        // sits under the figure it acts on.
        Action(
            if (state.stack.isEmpty()) "Begin tonight's tally" else
                "Finish ${state.stack.size} photograph${if (state.stack.size == 1) "" else "s"}",
            paper,
            { model.openNightfall(state.today) },
            accent = true,
            modifier = Modifier.widthIn(max = if (wide) 340.dp else Dp.Unspecified),
        )

        SectionHead("II", "the trip", paper, trailing = "${trip.startDate.iso} → ${trip.endDate.iso}")

        NavRow(Glyph.Date, "Timeline", "${state.dayCards.count { it.isClosed }} of ${state.dayCards.size} days closed", paper) {
            model.go(Route.Timeline)
        }
        NavRow(Glyph.Record, "The record", "${state.dayCaptures.size} kept today", paper) {
            model.go(Route.Record(state.today))
        }
        NavRow(Glyph.Search, "Search what you kept", "by shop, price, day", paper) {
            model.go(Route.Search)
        }
        NavRow(Glyph.Trip, "The trip's page", "dates, currencies, the card", paper) {
            model.go(Route.Trip)
        }

        Spacer(Modifier.height(30.dp))
        Body(
            "No account. No sync. Nothing leaves this device — a record of what you spent should " +
                "not need permission to exist.",
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun NavRow(
    glyph: Glyph,
    label: String,
    detail: String,
    paper: Paper,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A glyph in the hanging margin, so the index scans as a list of places
            // rather than four lines of serif that happen to be different words.
            Box(Modifier.width(26.dp)) {
                Icon(glyph, paper, size = 16.dp, color = paper.inkSoft)
            }
            Title(label, paper, size = 19.sp)
            Spacer(Modifier.weight(1f))
            Label(detail, paper, color = paper.inkFaint, tracking = 1.2.sp)
        }
        Rule(paper)
    }
}

// ── 2. Timeline ─────────────────────────────────────────────────────────────

@Composable
public fun TimelinePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val days = state.dayCards
    TabPage(
        paper,
        wide,
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
            lede = "Every day you kept something, and the hours it happened in.",
            trailing = { QuietAction("Record", paper, { model.go(Route.Record(state.today)) }) },
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
            Numeral(
                Money.ofMinor(tripTotal).format(base),
                paper,
                size = if (wide) 64.sp else 50.sp,
                weight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(12.dp))
        Rule(paper, color = paper.ink)
        Spacer(Modifier.height(10.dp))

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
        Action(
            "Close today",
            paper,
            { model.openNightfall(state.today) },
            accent = true,
            enabled = state.stack.isNotEmpty(),
        )

        Spacer(Modifier.height(96.dp))
    }
}

// ── 4. Prefs ────────────────────────────────────────────────────────────────

@Composable
public fun PrefsPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    TabPage(
        paper,
        wide,
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
            trailing = { QuietAction("Done", paper, { model.go(Route.Home) }) },
        )

        Spacer(Modifier.height(14.dp))

        // Every row below is a read-out of something real, and the two that can be
        // changed are the two that are actually changeable. A row that says
        // "Date format — 2026-11-23" with no way to set it would be decoration
        // pretending to be a setting; the ones that cannot change say so instead.
        SectionHead("I", "the record", paper)

        PrefChoice(
            glyph = Glyph.Budget,
            label = "Base currency",
            value = state.trip?.baseCurrency?.code ?: "INR",
            note = "Every total is stated in this one",
            paper = paper,
            options = listOf("INR", "THB", "USD", "EUR", "GBP", "JPY"),
            onChoose = { model.editTripCurrency(it) },
        )
        PrefChoice(
            glyph = Glyph.Trip,
            label = "Home currency",
            value = state.trip?.homeCurrency?.code ?: "INR",
            note = "What a foreign amount is converted back to",
            paper = paper,
            options = listOf("INR", "THB", "USD", "EUR", "GBP", "JPY"),
            onChoose = { model.editTripHomeCurrency(it) },
        )
        PrefReadout(Glyph.Date, "Dates", "2026-11-23, ISO order", paper)
        PrefReadout(Glyph.Paper, "Paper", "Follows the system", paper)

        Spacer(Modifier.height(26.dp))
        SectionHead("II", "what leaves this device", paper)

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

        Spacer(Modifier.height(28.dp))
        SectionHead("III", "this trip", paper)

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
        IconAction(Glyph.Done, "Close the trip into a card", paper) { model.closeTripIntoCard() }

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
 * A setting you can change: the value is the control.
 *
 * Not a dropdown, not a switch. The options are laid out under the row when it is
 * open, at the same tracked size as the label, because a currency is a word and
 * six words fit on one line.
 */
@Composable
private fun PrefChoice(
    glyph: Glyph,
    label: String,
    value: String,
    note: String,
    paper: Paper,
    options: List<String>,
    onChoose: (String) -> Unit,
) {
    var open by remember(label) { mutableStateOf(false) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp)
                .androidTap { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(glyph, paper, size = 16.dp, color = paper.inkSoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Title(label, paper, size = 17.sp)
                Spacer(Modifier.height(3.dp))
                Label(note, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.1.sp)
            }
            Numeral(value, paper, size = 18.sp, weight = FontWeight.Medium, color = paper.ink)
            Spacer(Modifier.width(8.dp))
            Icon(Glyph.Edit, paper, size = 14.dp, color = paper.inkFaint)
        }
        if (open) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { code ->
                    val chosen = code == value
                    Box(
                        Modifier
                            .border(
                                Bone.Hairline,
                                if (chosen) paper.blue else paper.edge,
                            )
                            .androidTap { onChoose(code); open = false }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Label(
                            code,
                            paper,
                            color = if (chosen) paper.blue else paper.inkSoft,
                            size = 9.sp,
                            tracking = 1.4.sp,
                        )
                    }
                }
            }
        }
        Rule(paper)
    }
}

/** A setting that is a fact, not a choice. It says so rather than pretending. */
@Composable
private fun PrefReadout(glyph: Glyph, label: String, value: String, paper: Paper) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, paper, size = 16.dp, color = paper.inkFaint)
        Spacer(Modifier.width(12.dp))
        Title(label, paper, size = 17.sp)
        Spacer(Modifier.weight(1f))
        Label(value, paper, color = paper.inkSoft, tracking = 1.2.sp)
    }
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

