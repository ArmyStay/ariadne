package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.PageHeader
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title

/**
 * Page 1 — Trips.
 *
 * The shelf the whole record hangs off. A closed trip is a bound volume; the open
 * one is the only thing you can add to. Deliberately the first thing a visitor
 * sees: one trip, one line, no dashboard.
 */
@Composable
public fun TripsPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 26.dp),
    ) {
        PageHeader(
            eyebrow = "Ariadne",
            title = if (state.trip == null) "No trips yet" else "Trips",
            paper = paper,
            trailing = { QuietAction("Settings", paper, { model.go(Route.Settings) }) },
        )

        Spacer(Modifier.height(28.dp))

        val trip = state.trip
        if (trip == null) {
            Body(
                "A trip is the whole of what you keep. Start one and every photograph you " +
                    "take belongs to it, until the day it closes.",
                paper,
                serif = true,
                size = 18.sp(),
            )
            Spacer(Modifier.height(26.dp))
            Action("Begin with the demo roll", paper, { model.startSeedTrip() }, accent = true)
            Spacer(Modifier.height(14.dp))
            Action("Begin empty", paper, { model.startEmptyTrip() })
            return@Column
        }

        // The open trip: the one thing that wants attention.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Label("Open", paper, color = paper.thread)
                Spacer(Modifier.height(8.dp))
                Title(trip.name, paper, size = 30.sp())
                Spacer(Modifier.height(6.dp))
                Label("${trip.startDate.iso} — ${trip.endDate.iso}", paper, color = paper.inkFaint)
            }
            Column(horizontalAlignment = Alignment.End) {
                Label("Days", paper, color = paper.inkFaint)
                Spacer(Modifier.height(6.dp))
                Numeral(state.dayCards.size.toString(), paper, size = 22.sp())
            }
        }

        Spacer(Modifier.height(18.dp))
        Rule(paper)
        Spacer(Modifier.height(8.dp))

        TripRow("Today's tally", "${state.stack.size} waiting", paper) { model.openNightfall(state.today) }
        TripRow("The record", "${state.dayCards.count { it.isClosed }} days closed", paper) {
            model.go(Route.Record(state.today))
        }
        TripRow("Search", "by shop, price, day", paper) { model.go(Route.Search) }
        TripRow("Capture", "photograph a spend", paper) { model.go(Route.Capture) }

        Spacer(Modifier.height(30.dp))
        Label("Closed", paper, color = paper.inkFaint)
        Spacer(Modifier.height(12.dp))
        Body(
            "Nothing here yet. When this trip ends it closes into a card, and the card is the point.",
            paper,
            serif = true,
            color = paper.inkSoft,
        )

        Spacer(Modifier.height(26.dp))
        TickedBlock(paper)

        Spacer(Modifier.height(24.dp))
        Body(
            "No account. No sync. Nothing leaves this device — a record of what you spent " +
                "should not need permission to exist.",
            paper,
            color = paper.inkFaint,
            size = 13.sp(),
        )
    }
}

@Composable
private fun TripRow(
    title: String,
    detail: String,
    paper: Paper,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box3(paper)
                Spacer(Modifier.width(14.dp))
                Title(title, paper, size = 18.sp())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(detail, paper, color = paper.inkFaint)
                Spacer(Modifier.width(14.dp))
                QuietAction("Open", paper, onClick, color = paper.inkMid)
            }
        }
        Rule(paper)
    }
}

/** A small square of manuscript paper, the visual signature of the shelf. */
@Composable
private fun Box3(paper: Paper) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .width(10.dp)
            .height(10.dp)
            .background(paper.edge),
    )
}

@Composable
private fun TickedBlock(paper: Paper) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(7.dp)) {
        val step = 9f
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = paper.edge,
                start = androidx.compose.ui.geometry.Offset(x, size.height),
                end = androidx.compose.ui.geometry.Offset(x, size.height - 5f),
                strokeWidth = 1f,
            )
            x += step
        }
        drawLine(
            color = paper.edge,
            start = androidx.compose.ui.geometry.Offset(0f, size.height),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
            strokeWidth = 1f,
        )
    }
}

/** Local sp helper so the page reads as sizes, not types. */
private fun Int.sp() = this.sp
