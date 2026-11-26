package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.OutlineAction
import com.ariadne.design.Paper
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Settings

/**
 * Preferences — the things that are actually decisions.
 *
 * The old page was a numbered form: "I — the record", "II — the exchange",
 * "III — what leaves this device", "IV — this trip", with a Masthead plate number
 * above it. Four sections of a form is a form, and a form is what this page is not:
 * it holds two real choices (the two currencies), one reading preference (which
 * paper), a set of exchange rates that are typed rather than chosen, and the trip's
 * own closing actions.
 *
 * So it is now five short blocks with names and rules between them, and every row
 * says whether it can be changed:
 *
 *   - **Money** — base and home currency, the only two settings with real
 *     consequences for the figures everywhere else.
 *   - **Paper** — light, night, or the device's own answer.
 *   - **The exchange** — one line per currency in use, each a sentence the person
 *     can check against a board at the airport: "1 THB = 2.5 INR".
 *   - **This device** — what leaves it (nothing) and the last failure, if any.
 *   - **This trip** — its page, its card, and the two-tap deletion.
 *
 * A row that cannot be changed is printed as a read-out and says so. A row that
 * looks like a setting and does nothing is decoration wearing a control's clothes.
 */
@Composable
public fun PreferencesPage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    lastFailure: String? = null,
) {
    TabPage(
        paper = paper,
        wide = wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            Label("on device only", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            Spacer(Modifier.height(10.dp))
            Label("no account · no sync", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Label("ariadne", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
                Spacer(Modifier.height(8.dp))
                Title("Preferences", paper, size = if (wide) 40.sp else 30.sp, italic = true)
            }
            // The bar is hidden on this page (it is not one of the four), so Back
            // lives here. The system gesture does the the same thing.
            // Back through the stack, and to Today if this page was somehow
            // arrived at first: a page with no exit is worse than a page with a
            // guess for one.
            QuietAction("Back", paper, {
                if (!model.back()) model.go(Route.Home)
            }, color = paper.inkSoft)
        }

        Spacer(Modifier.height(24.dp))
        Rule(paper, color = paper.edgeStrong)
        Spacer(Modifier.height(20.dp))

        // ── money ───────────────────────────────────────────────────────────
        Label("money", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(6.dp))
        Body(
            "Two currencies, and every figure in the book follows them.",
            paper,
            serif = true,
            color = paper.inkSoft,
            size = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(14.dp))

        // Both come from the same catalogue the New trip page uses, so there is one
        // vocabulary for choosing money in the whole app.
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

        Spacer(Modifier.height(8.dp))
        Readout(
            glyph = Glyph.Date,
            label = "Dates",
            value = "${state.today?.iso ?: "—"}, ISO order",
            note = "Fixed on purpose: a date that can be reordered is a date that can be misread",
            paper = paper,
        )

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        // ── paper ───────────────────────────────────────────────────────────
        Label("paper", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(14.dp))
        PaperChoice(paper, state.theme) { model.setTheme(it) }

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        // ── the exchange ────────────────────────────────────────────────────
        //
        // Not decoration: without a rate a foreign amount cannot be converted, and
        // every total silently under-reports. The figures are exact scaled integers
        // all the way down; this is the only place a human types one.
        Label("the exchange", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(6.dp))
        Body(
            "A rate is true on a day. The rate a figure was converted at is pinned to that " +
                "photograph, so changing one here never rewrites a total you have already read.",
            paper,
            serif = true,
            color = paper.inkSoft,
            size = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(6.dp))

        val inUse = model.currenciesInUse()
        val foreign = (inUse + listOf("THB", "USD", "EUR", "GBP", "JPY"))
            .filter { it != state.trip?.baseCurrency?.code }
            .distinct()

        // One open field at a time. Two live keyboards' worth of ruled boxes on one
        // page is a form; this page is a ledger with one line you can edit.
        var openCode by remember { mutableStateOf<String?>(null) }

        if (foreign.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Body(
                "Everything so far is already in the base currency.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
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

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        // ── this device ─────────────────────────────────────────────────────
        Label("this device", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(12.dp))
        Readout(
            glyph = Glyph.Ledger,
            label = "Reading",
            value = "On device only",
            note = null,
            paper = paper,
        )
        Spacer(Modifier.height(10.dp))
        Body(
            "Nothing leaves. There is no account to sign into and no server to sign into it " +
                "from. Photographs are stored in the app's private folder, the record lives in " +
                "a local database, and the only network call the product makes is the one that " +
                "fetches an exchange rate.",
            paper,
            color = paper.inkMid,
            size = 14.sp,
            lineHeight = 21.sp,
        )

        // The honest diagnostic. If anything threw and was swallowed — which is
        // exactly how a dead button happens — it is written here rather than living
        // only in a console nobody has attached to.
        if (lastFailure != null) {
            Spacer(Modifier.height(20.dp))
            Label("something failed", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(8.dp))
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

        Spacer(Modifier.height(24.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        // ── this trip ───────────────────────────────────────────────────────
        Label("this trip", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Title(state.trip?.name ?: "No trip", paper, size = 18.sp)
                Spacer(Modifier.height(3.dp))
                Label(
                    state.trip?.place ?: "no place set",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 1.1.sp,
                )
            }
            if (state.trip?.isClosed == true) Pill("closed", paper, PillTone.Quiet)
        }
        Spacer(Modifier.height(14.dp))

        QuietAction("the trip's page", paper, { model.go(Route.Trip) }, color = paper.inkMid)
        Spacer(Modifier.height(6.dp))
        if (state.trip?.isClosed == true) {
            // Closing is not a thing a closed trip can do again; the card is where
            // the finished record lives.
            QuietAction("the card", paper, {
                state.trip?.let { model.go(Route.TripCard(it.id)) }
            }, color = paper.inkMid)
        } else {
            QuietAction("close the trip into a card", paper, { model.closeTripIntoCard() }, color = paper.inkMid)
        }

        Spacer(Modifier.height(24.dp))
        Rule(paper, color = paper.edge)
        Spacer(Modifier.height(16.dp))

        if (!state.deleteArmed) {
            QuietAction("Delete this trip", paper, { model.armTripDeletion() }, color = paper.inkMid)
            Spacer(Modifier.height(8.dp))
            Body(
                "Deletes its photographs and its record. Nothing is kept anywhere else, so this " +
                    "is the one action in the app you cannot take back.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
        } else {
            // Armed. The copy says exactly what will go, and the only way out that is
            // not deletion is the quieter of the two controls.
            Label("this deletes everything, now", paper, color = paper.blue, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Action("Delete for good", paper, { model.deleteTripNow() }, enabled = !state.busy)
                OutlineAction("Keep it", paper, { model.armTripDeletion() })
            }
        }

        Spacer(Modifier.height(24.dp))
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
private fun PaperChoice(
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
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                        .rowTap { onChoose(value) }
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
    }
}

/**
 * One exchange rate, as a sentence.
 *
 * "1 THB = [ 2.5 ] INR" is the form a traveller can check against a receipt or a
 * board at the airport. The alternative — two abstract numbers — is what an
 * engineer would write and what nobody can verify.
 *
 * When a rate is on file the field shows it and the row carries the pair; when one
 * is not, the row says so plainly, because a missing rate is the difference between
 * a total that is right and a total that is quietly wrong.
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

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title(code, paper, size = 17.sp)
            Spacer(Modifier.width(10.dp))
            if (used) {
                // Marks the currencies this trip actually holds, so the list is not
                // five speculative rows.
                Label("in use", paper, color = paper.blue, size = 9.sp, tracking = 1.2.sp)
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
                    size = 9.sp,
                    tracking = 1.1.sp,
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
                        textStyle = TextStyle(
                            fontFamily = Bone.Sans,
                            fontSize = 16.sp,
                            color = paper.ink,
                        ),
                        cursorBrush = SolidColor(paper.ink),
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
                size = 9.sp,
                tracking = 1.1.sp,
            )
        }
    }
}

/**
 * A setting that is a fact, not a choice.
 *
 * It says so rather than pretending: the value is printed, the note explains why it
 * cannot move, and there is nothing to tap. A row that looks like a control and does
 * nothing when pressed is worse than a row that never claimed to be one.
 */
@Composable
private fun Readout(
    glyph: Glyph,
    label: String,
    value: String,
    note: String?,
    paper: Paper,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(glyph, paper, size = 15.dp, color = paper.inkFaint)
            Spacer(Modifier.width(12.dp))
            Label(label, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.2.sp)
            Spacer(Modifier.weight(1f))
            Label(value, paper, color = paper.inkMid, size = 10.sp, tracking = 0.4.sp)
        }
        if (note != null) {
            Spacer(Modifier.height(4.dp))
            Label(note, paper, color = paper.inkFaint, size = 9.sp, tracking = 0.4.sp)
        }
    }
}
