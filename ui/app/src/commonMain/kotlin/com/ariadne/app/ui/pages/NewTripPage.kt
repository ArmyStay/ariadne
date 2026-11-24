package com.ariadne.app.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Masthead
import com.ariadne.design.Optical
import com.ariadne.design.OutlineAction
import com.ariadne.design.Page
import com.ariadne.design.Paper
import com.ariadne.design.Rule
import com.ariadne.design.SectionHead
import com.ariadne.design.Title

/**
 * 01b — A new trip.
 *
 * This is the front door. On a first run there is no trip, so there is nothing to
 * show and nothing to photograph until one exists: the record has to be scoped to
 * something before a single frame can be hung on it.
 *
 * Five fields, set as a form in a ledger would be set — a stack of labelled
 * hairlines, no boxes, no borders, nothing that looks like a web form. The names
 * are set in the serif at reading size so the page reads as a title page being
 * filled in rather than as data entry.
 *
 * Everything here is optional except the name and the dates, because a traveller
 * who does not know what they will spend should not have to invent a number to
 * get past the first screen.
 */
@Composable
public fun NewTripPage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
) {
    val d = state.draft

    Page(paper = paper, wide = wide) {
        Masthead(
            number = "01",
            eyebrow = if (state.trip == null) "Ariadne" else "another trip",
            title = "Where to",
            paper = paper,
            italic = true,
            wide = wide,
            lede = "A name, the dates, and a figure to stay under. The fence is optional.",
        )

        Spacer(Modifier.height(26.dp))

        DraftField(
            label = "the trip",
            placeholder = "Bangkok & Home",
            value = d.name,
            paper = paper,
            wide = wide,
            serif = true,
            onChange = { model.editDraft(name = it) },
        )

        DraftField(
            label = "where",
            placeholder = "Bangkok — or a coast, or a train",
            value = d.place,
            paper = paper,
            wide = wide,
            onChange = { model.editDraft(place = it) },
        )

        // Dates side by side: they are read as a pair, so they are set as one.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (wide) 32.dp else 18.dp)) {
            Box(Modifier.weight(1f)) {
                DraftField(
                    label = "from",
                    placeholder = "2026-11-24",
                    value = d.startIso,
                    paper = paper,
                    wide = false,
                    numeric = true,
                    onChange = { model.editDraft(startIso = it) },
                )
            }
            Box(Modifier.weight(1f)) {
                DraftField(
                    label = "to",
                    placeholder = "2026-11-30",
                    value = d.endIso,
                    paper = paper,
                    wide = false,
                    numeric = true,
                    onChange = { model.editDraft(endIso = it) },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (wide) 32.dp else 18.dp)) {
            Box(Modifier.weight(1f)) {
                DraftField(
                    label = "currency",
                    placeholder = "INR",
                    value = d.currency,
                    paper = paper,
                    wide = false,
                    onChange = { model.editDraft(currency = it.uppercase().take(3)) },
                )
            }
            Box(Modifier.weight(1f)) {
                DraftField(
                    label = "budget, if you have one",
                    placeholder = "25000",
                    value = d.budget,
                    paper = paper,
                    wide = false,
                    numeric = true,
                    onChange = { model.editDraft(budget = it) },
                )
            }
        }

        if (d.error != null) {
            Spacer(Modifier.height(14.dp))
            Body(d.error, paper, color = paper.blue, size = 13.sp, lineHeight = 19.sp)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth().widthIn(max = if (wide) 460.dp else 10_000.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                Action("Open the trip", paper, { model.commitDraft() }, accent = true)
            }
            QuietOpen(paper, model)
        }

        Spacer(Modifier.height(28.dp))
        SectionHead("II", "or start with something in it", paper)

        Body(
            "Five photographs of one Bangkok afternoon, already taken — Nightfall with a " +
                "stack in front of you. " +
                "reading about it.",
            paper,
            color = paper.inkFaint,
            size = 13.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))
        // Stacked on a phone, side by side on a wide page: two outlined bars in a row
        // at 440dp leaves the second one hanging off the measure.
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineAction("Demo roll", paper, { model.commitDraft(withDemoRoll = true) })
                OutlineAction("Prefill from demo", paper, { model.fillDraftFromDemo() })
            }
        } else {
            Column {
                OutlineAction("Demo roll", paper, { model.commitDraft(withDemoRoll = true) })
                Spacer(Modifier.height(10.dp))
                OutlineAction("Prefill from demo", paper, { model.fillDraftFromDemo() })
            }
        }

        Spacer(Modifier.height(30.dp))
        Rule(paper, color = paper.edge)
        Spacer(Modifier.height(10.dp))
        Label(
            "no account · no server · only the photograph leaves the camera",
            paper,
            color = paper.inkFaint,
            size = 9.sp,
            tracking = 1.5.sp,
        )
    }
}

/** Kept separate so the primary action stays the only blue thing on the page. */
@Composable
private fun QuietOpen(paper: Paper, model: AppModel) {
    val state = model.state.value
    if (state.trip != null) {
        com.ariadne.design.QuietAction("Go back", paper, { model.go(com.ariadne.app.Route.Home) })
    }
}

/**
 * A labelled hairline that happens to accept typing.
 *
 * The rule under the value is the whole control: there is no box, because a box
 * draws a border around a field that already has a line under it, and two edges
 * around one piece of text is what every generated form looks like.
 */
@Composable
private fun DraftField(
    label: String,
    placeholder: String,
    value: String,
    paper: Paper,
    wide: Boolean,
    serif: Boolean = false,
    numeric: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Label(label, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Title(placeholder, paper, size = if (serif) 26.sp else 19.sp, color = paper.inkFaint)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = if (serif) Bone.Serif else Bone.Sans,
                    fontSize = if (serif) (if (wide) 30.sp else 26.sp) else 19.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = if (serif) Optical.displayTracking else Optical.bodyTracking,
                    color = paper.ink,
                ),
                cursorBrush = SolidColor(paper.blue),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label },
            )
        }
        Spacer(Modifier.height(9.dp))
        Rule(paper, color = if (numeric) paper.edgeStrong else paper.edge)
        Spacer(Modifier.height(18.dp))
    }
}
