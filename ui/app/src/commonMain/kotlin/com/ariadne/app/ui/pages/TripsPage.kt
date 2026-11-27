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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.design.EmptyState
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Masthead
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.ariadne.design.ScrollRail
import com.ariadne.design.Paper
import com.ariadne.design.Reveal
import com.ariadne.design.Rule
import com.ariadne.design.Tick
import com.ariadne.design.Title
import com.ariadne.model.Money
import com.ariadne.model.Trip
import com.ariadne.design.tap

/**
 * The register.
 *
 * Every trip this device holds, open and closed, newest first. One of them is
 * *active* - the one a photograph taken now belongs to - and that is marked by
 * the single blue element on the page rather than by a badge saying "active".
 *
 * The page is set as a register in the paper sense: numbered entries, a rule
 * between them, dates in the margin, and a figure on the right. No cards, because
 * a trip is not a card.
 */
@Composable
public fun TripsPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 22.dp),
    ) {
        // No plate number: the register is the shelf the plates sit on, not one
        // of them. 01..09 belong to the pages of the trip itself.
        Masthead(
            number = "",
            eyebrow = "The register",
            title = "Trips",
            paper = paper,
            italic = true,
            wide = wide,
            lede = "Every journey this device holds. One of them is open.",
        )

        Spacer(Modifier.height(10.dp))
        Rule(paper, color = paper.ink)

        Row(Modifier.fillMaxWidth().weight(1f)) {
        val scroll = rememberScrollState()
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(scroll)) {
            Spacer(Modifier.height(8.dp))

            if (state.trips.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                EmptyState(
                    title = "No trips yet.",
                    body = "A trip is the container the whole record hangs from: its dates, its " +
                        "money, and every photograph you take during it.",
                    paper = paper,
                )
            }

            state.trips.forEachIndexed { index, trip ->
                Reveal(index) {
                    TripEntry(
                        trip = trip,
                        index = index + 1,
                        active = trip.id == state.activeTripId,
                        paper = paper,
                        onOpen = { model.switchTrip(trip.id) },
                    )
                }
                Rule(paper, color = paper.edge.copy(alpha = 0.7f))
            }

            Spacer(Modifier.height(30.dp))
            // Not blue: the single blue element on this page is the open trip.
            // An outlined action is how this language says "available, not now".
            OutlineAction(
                "Begin a new trip",
                paper,
                { model.startNewTrip() },
            )
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.height(26.dp))
            Rule(paper, color = paper.edge)

            // The space below the register is not padding: it holds the facts the
            // device can vouch for. An empty page under a list is the tell of a
            // layout that assumed more rows than it got.
            val closed = state.trips.count { it.isClosed }
            val open = state.trips.size - closed
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(34.dp)) {
                ShelfFigure("journeys", state.trips.size.toString(), paper)
                ShelfFigure("open", open.toString(), paper)
                ShelfFigure("closed", closed.toString(), paper)
            }
            Spacer(Modifier.height(30.dp))
            Body(
                "One at a time. A photograph taken now belongs to the open journey.",
                paper,
                serif = true,
                color = paper.inkFaint,
                size = 15.sp,
            )
            Spacer(Modifier.height(40.dp))
        }
        ScrollRail(scroll, paper, Modifier.fillMaxHeight())
        }
    }
}

/** A labelled figure on the register's lower rule. */
@Composable
private fun ShelfFigure(label: String, value: String, paper: Paper) {
    Column {
        Numeral(value, paper, color = paper.inkSoft, size = 26.sp, weight = FontWeight.Light)
        Spacer(Modifier.height(3.dp))
        Label(label, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
    }
}

private val Months = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/**
 * A trip's dates, the way a register writes them: "24 Nov — 30 Nov".
 *
 * The first attempt used `iso.drop(2)`, which produced "26-11-24 — 26-11-30" -
 * a range that reads as arithmetic. A year is added only when the trip is not in
 * the current one, because repeating this year on every line is noise.
 */
private fun Trip.dates(): String {
    fun end(day: com.ariadne.model.LocalDay): String =
        "${day.day} ${Months[day.month - 1]}"
    return "${end(startDate)} — ${end(endDate)}"
}

/**
 * One line of the register.
 *
 * Numbered, with the place under the name, the dates in the margin and the trip's
 * own total on the right. Tapping opens it; the active one is the only thing on
 * the page carrying blue.
 */
@Composable
private fun TripEntry(
    trip: Trip,
    index: Int,
    active: Boolean,
    paper: Paper,
    onOpen: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // The whole row is the target, and the whole row is one accessibility
        // node: name, dates and state in one sentence, so a screen reader does not
        // have to assemble a trip out of four labels.
        val state = when {
            trip.isClosed -> "closed"
            active -> "open, the one photographs go into"
            else -> "open"
        }
        Row(
            Modifier
                .fillMaxWidth()
                .tap("Open this trip") { onOpen() }
                .semantics {
                    contentDescription = "${trip.name}, ${trip.dates()}, $state"
                }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // The entry number, set in the figure face so the register aligns as
            // a column of numbers rather than a column of words.
            Box(Modifier.width(34.dp)) {
                Numeral(
                    index.toString().padStart(2, '0'),
                    paper,
                    color = if (active) paper.blue else paper.inkFaint,
                    size = 13.sp,
                )
            }
            Column(Modifier.weight(1f)) {
                Title(
                    trip.name,
                    paper,
                    size = 21.sp,
                    italic = false,
                    color = if (active) paper.ink else paper.inkMid,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Glyph.Place, paper, size = 13.dp, color = paper.inkFaint)
                    Spacer(Modifier.width(7.dp))
                    Label(
                        trip.place ?: "no place",
                        paper,
                        color = paper.inkFaint,
                        size = 9.sp,
                        tracking = 1.3.sp,
                    )
                    Spacer(Modifier.width(14.dp))
                    Label(
                        trip.dates(),
                        paper,
                        color = paper.inkFaint,
                        size = 9.sp,
                        tracking = 1.3.sp,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Numeral(
                    trip.budgetMinor?.let { Money.ofMinor(it).format(trip.baseCurrency) } ?: "—",
                    paper,
                    color = if (trip.budgetMinor == null) paper.inkFaint else paper.inkMid,
                    size = 15.sp,
                )
                Spacer(Modifier.height(5.dp))
                if (active) {
                    // The one blue thing on this page.
                    Tick(paper, width = 22.dp, color = paper.blue)
                } else {
                    // An open trip that is simply not the active one is not
                    // "closed"; only a closed trip says so.
                    Label(
                        if (trip.isClosed) "closed" else "open",
                        paper,
                        color = paper.inkFaint,
                        size = 8.sp,
                        tracking = 1.2.sp,
                    )
                }
            }
        }
    }
}
