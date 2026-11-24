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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.LinkAction
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.PageHeader
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


/**
 * The four destinations in the bottom bar, as single views.
 *
 * They are not "screens" in the routed sense — the routed pages (a trip, a day,
 * the card) still sit on top of these. Each one is a scroller with a headline, a
 * figure and a list, in the reference's language: white panels on paper, hairline
 * rules, tracked-out labels, one blue action.
 */

/** Wraps every tab view in the same page furniture so the app reads as one book. */
@Composable
private fun TabPage(
    paper: Paper,
    wide: Boolean,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(paper.bg), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = if (wide) Bone.Measure + 180.dp else Dp.Unspecified)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (wide) Bone.MarginWide else Bone.Margin,
                    vertical = 24.dp,
                ),
        ) { content() }
    }
}

// ── 1. Home ─────────────────────────────────────────────────────────────────

@Composable
public fun HomePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    TabPage(paper, wide) {
        PageHeader(
            eyebrow = "Ariadne",
            title = state.trip?.name ?: "No trips yet",
            paper = paper,
            italicTitle = false,
            trailing = {
                QuietAction("Prefs", paper, { model.go(Route.Settings) })
            },
        )

        Spacer(Modifier.height(24.dp))

        val trip = state.trip
        if (trip == null) {
            Spacer(Modifier.height(40.dp))
            Title("Nothing kept yet.", paper, size = 26.sp, italic = true)
            Spacer(Modifier.height(10.dp))
            Body(
                "A trip starts on a date and ends on a date. Everything in between is a photograph " +
                    "you take now and finish tonight.",
                paper,
                color = paper.inkSoft,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(26.dp))
            Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
            Spacer(Modifier.height(10.dp))
            OutlineAction("Begin with the demo roll", paper, { model.startTripDemo() })
            return@TabPage
        }

        // Today's state: the one number that matters, set as type.
        Panel(paper) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Label("Spent today", paper, color = paper.inkSoft)
                        Spacer(Modifier.height(8.dp))
                        Numeral(
                            Money.ofMinor(state.dayTotalMinor)
                                .formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
                            paper,
                            size = 38.sp,
                            weight = androidx.compose.ui.text.font.FontWeight.Medium,
                        )
                    }
                    Pill(
                        if (state.stack.isEmpty()) "Closed" else "${state.stack.size} waiting",
                        paper,
                        if (state.stack.isEmpty()) PillTone.Quiet else PillTone.Blue,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Rule(paper)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Label("${trip.startDate.iso} → ${trip.endDate.iso}", paper, color = paper.inkSoft)
                    Label("${dayRange(trip.startDate, trip.endDate).size} days", paper, color = paper.inkSoft)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // The one thing to do, in blue.
        Action(
            if (state.stack.isEmpty()) "Begin tonight's tally" else
                "Finish ${state.stack.size} photograph${if (state.stack.size == 1) "" else "s"}",
            paper,
            { model.openNightfall(state.today) },
            accent = true,
        )

        Spacer(Modifier.height(26.dp))
        Label("The trip", paper, color = paper.inkSoft)
        Spacer(Modifier.height(12.dp))

        NavRow("Timeline", "${state.dayCards.count { it.isClosed }} days closed of ${state.dayCards.size}", paper) {
            model.go(Route.Record(state.today))
        }
        NavRow("The record", "${state.dayCaptures.size} kept today", paper) {
            model.go(Route.Record(state.today))
        }
        NavRow("Search what you kept", "By shop, price, day", paper) {
            model.go(Route.Search)
        }
        NavRow("The trip's page", "Dates, currencies, the card", paper) {
            model.go(Route.Trip)
        }

        Spacer(Modifier.height(24.dp))
        Body(
            "No account. No sync. Nothing leaves this device — a record of what you spent should " +
                "not need permission to exist.",
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun NavRow(label: String, detail: String, paper: Paper, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Title(label, paper, size = 19.sp)
            Label(detail, paper, color = paper.inkFaint, tracking = 1.2.sp)
        }
        Rule(paper)
    }
}

// ── 2. Timeline ─────────────────────────────────────────────────────────────

@Composable
public fun TimelinePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val days = state.dayCards
    TabPage(paper, wide) {
        PageHeader(
            eyebrow = state.trip?.name ?: "Timeline",
            title = "Timeline",
            paper = paper,
            italicTitle = true,
            trailing = { QuietAction("Record", paper, { model.go(Route.Record(state.today)) }) },
        )

        Spacer(Modifier.height(22.dp))

        if (days.isEmpty()) {
            Body("A trip has no days until it has dates.", paper, color = paper.inkSoft, size = 16.sp)
            Spacer(Modifier.height(22.dp))
            Action("Start a trip", paper, { model.startEmptyTrip() }, accent = true)
            return@TabPage
        }

        Label(
            "${days.count { it.isClosed }} closed · ${days.count { !it.isClosed }} open",
            paper,
            color = paper.inkSoft,
        )
        Spacer(Modifier.height(14.dp))

        days.forEach { card ->
            DayRow(
                day = card.localDate,
                closed = card.isClosed,
                selected = card.localDate == state.today,
                paper = paper,
            ) { model.go(Route.Record(card.localDate)) }
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
    TabPage(paper, wide) {
        PageHeader(
            eyebrow = state.trip?.name ?: "Stats",
            title = "What it came to",
            paper = paper,
            italicTitle = true,
        )

        Spacer(Modifier.height(26.dp))

        Panel(paper) {
            Column {
                Label("Tallied", paper, color = paper.inkSoft)
                Spacer(Modifier.height(8.dp))
                Numeral(
                    Money.ofMinor(state.dayTotalMinor)
                        .formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
                    paper,
                    size = 44.sp,
                    weight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
                Spacer(Modifier.height(6.dp))
                Body(
                    "Only the cards you finished at nightfall. Open photographs are not in this " +
                        "figure, and are not counted as zero.",
                    paper,
                    color = paper.inkFaint,
                    size = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        if (state.perCurrency.isNotEmpty()) {
            Label("By what was on the receipt", paper, color = paper.inkSoft)
            Spacer(Modifier.height(8.dp))
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
        } else {
            Body(
                "Nothing has been tallied yet. The figures appear here the first time you close a day.",
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
    TabPage(paper, wide) {
        PageHeader(
            eyebrow = "Ariadne",
            title = "Preferences",
            paper = paper,
            trailing = { QuietAction("Done", paper, { model.go(Route.Home) }) },
        )

        Spacer(Modifier.height(22.dp))

        PrefRow("Base currency", state.trip?.baseCurrency?.code ?: "INR", paper)
        PrefRow("Home currency", state.trip?.homeCurrency?.code ?: "INR", paper)
        PrefRow("Date format", "2026-11-23", paper)
        PrefRow("Paper", "Follows the system", paper)
        PrefRow("Reading", "On device only", paper)

        Spacer(Modifier.height(26.dp))

        Label("What leaves this device", paper, color = paper.inkSoft)
        Spacer(Modifier.height(10.dp))
        Body(
            "Nothing. There is no account to sign into and no server to sign into it from. " +
                "Photographs are stored in the app's private folder, the record lives in a local " +
                "database, and the only network call in the whole product is an optional one you " +
                "would have to configure yourself.",
            paper,
            color = paper.inkMid,
            size = 14.sp,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(26.dp))
        Label("This trip", paper, color = paper.inkSoft)
        Spacer(Modifier.height(10.dp))
        LinkAction("Close the trip into a card", paper, { model.go(Route.TripCard("0")) })
        Spacer(Modifier.height(6.dp))
        LinkAction("See the trip's page", paper, { model.go(Route.Trip) })

        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun PrefRow(label: String, value: String, paper: Paper) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Title(label, paper, size = 17.sp)
            Label(value, paper, color = paper.inkSoft, tracking = 1.2.sp)
        }
        Rule(paper)
    }
}
