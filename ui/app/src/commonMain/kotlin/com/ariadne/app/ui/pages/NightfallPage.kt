package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.ariadne.design.OutlineAction
import com.ariadne.design.Masthead
import com.ariadne.design.Paper
import com.ariadne.design.Plate
import com.ariadne.design.Subject
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money
import com.ariadne.model.symbol

/**
 * Page 5 — Nightfall.
 *
 * The ritual. One card at a time, photographed plate above, one figure below,
 * two actions underneath. On a wide screen the plate gets the left page and the
 * triage gets the right, like a spread; on a phone it is one column and the
 * keypad is a grid of hairlines.
 *
 * Nothing here warns, scolds, or suggests a target. The only judgement on the
 * page is the total, and it is just a number.
 */
@Composable
public fun NightfallPage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
) {
    val capture = state.current
    val day = (state.route as? Route.Nightfall)?.day
    // Read here, once, and passed down as values. Everything below is boxed on
    // its own keys, so a keypress recomposes the amount and nothing else.
    val typed = state.amountInput

    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 22.dp),
    ) {
        Masthead(
            number = "05",
            eyebrow = day?.iso ?: "Nightfall",
            title = "Nightfall",
            paper = paper,
            wide = wide,
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    Label("Today", paper, color = paper.inkFaint)
                    Spacer(Modifier.height(4.dp))
                    Numeral(
                        Money.ofMinor(state.dayTotalMinor).formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
                        paper,
                        size = 19.sp,
                    )
                }
            },
        )

        Spacer(Modifier.height(20.dp))

        if (capture == null) {
            RestingDay(paper, model, day)
            return@Column
        }

        if (wide) {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(44.dp),
            ) {
                // The plate takes the space that is left, not a fixed ratio: a 4:5
                // box in a weighted row overran the viewport on a laptop.
                Column(Modifier.weight(1.05f)) {
                    Box(Modifier.weight(1f)) {
                        PhotographOf(capture, paper, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label(
                            "plate ${state.stackIndex + 1} of ${state.stack.size}",
                            paper,
                            color = paper.inkFaint,
                            size = 9.sp,
                            tracking = 1.4.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        QuietAction("Earlier", paper, { model.previous() }, color = paper.inkFaint)
                        Spacer(Modifier.width(16.dp))
                        QuietAction("Later", paper, { model.next() }, color = paper.inkFaint)
                    }
                }
                Column(Modifier.weight(1f)) {
                    // Boxing on the card, not on the state: the whole of the
                    // right-hand column only has to recompose when the card
                    // changes. The amount it holds is boxed again further down.
                    val card = state.current
                    val typedIn = state.amountInput
                    key(card?.id, card?.amountMinor, card?.markedNoAmount, card?.currency) {
                        Triage(state, model, paper, capture, wide = true, typed = typedIn)
                    }
                }
            }
        } else {
            // Phone: the plate, the merchant and the typed amount stay put; the
            // keypad is pinned to the bottom edge so a number is always one tap
            // away, and the ritual never needs a scroll to finish a card.
            Column(Modifier.fillMaxWidth().weight(1f)) {
                // Only the plate scrolls. The merchant, the amount and both
                // decisions are pinned, because a ritual you have to scroll to
                // finish is not a ritual, it is a form.
                Column(
                    Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PhotographOf(
                        capture,
                        paper,
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                    Spacer(Modifier.height(14.dp))
                    Header(state, model, paper, capture)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(10.dp))
                Amount(typed, state.dayCurrency, paper)
                Spacer(Modifier.height(12.dp))
                // The decision never scrolls away: two taps finish a card, so both
                // taps have to be on screen at the same time as the number.
                Decisions(state, model, paper, wide = false)
                Keypad(paper, model)
            }
        }
    }
}

@Composable
private fun Triage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    capture: Capture,
    wide: Boolean = false,
    typed: String = "",
) {
    Column(Modifier.fillMaxWidth()) {
        Header(state, model, paper, capture)
        Spacer(Modifier.height(18.dp))
        Rule(paper)
        Spacer(Modifier.height(18.dp))
        key(typed) { Amount(typed, state.dayCurrency, paper) }
        Spacer(Modifier.height(18.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))
        // Decisions reads only two facts: which card, and whether anything has
        // been typed. Keying on those keeps the buttons still under a keystroke.
        key(state.current?.id, typed.isEmpty(), state.stackIndex, state.stack.size) {
            Decisions(state, model, paper, wide = true, typed = typed)
        }
    }
}

/** Merchant, currency and the claim about where the figure came from. */
@Composable
private fun Header(state: AppUiState, model: AppModel, paper: Paper, capture: Capture) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Title(capture.merchant ?: "Untitled", paper, size = 24.sp)
                Spacer(Modifier.height(6.dp))
                Label(
                    when {
                        capture.markedNoAmount -> "kept without an amount"
                        capture.amountMinor == null -> "nothing read — type it, or leave it"
                        else -> "read from the photograph"
                    },
                    paper,
                    color = paper.inkFaint,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Label(capture.currency?.code ?: state.dayCurrency, paper, color = paper.inkFaint)
                Spacer(Modifier.height(6.dp))
                // Opens the full sheet for this one photograph: currency, category,
                // a note. The ritual still ends in two taps if you never come here.
                QuietAction("Edit", paper, { model.openDetail(capture.id) }, color = paper.inkSoft)
            }
        }

    }
}

/** The amount: the largest thing on the page, monospace, tabular. */
@Composable
private fun Amount(amountInput: String, dayCurrency: String, paper: Paper) {
    Column(Modifier.fillMaxWidth()) {
        Label("Amount", paper, color = paper.inkSoft)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral(
                CurrencyCode.of(dayCurrency).symbol(),
                paper,
                color = paper.inkSoft,
                size = 20.sp,
            )
            Spacer(Modifier.width(8.dp))
            Numeral(
                amountInput.ifEmpty { "0" },
                paper,
                size = 44.sp,
                weight = androidx.compose.ui.text.font.FontWeight.Medium,
            )
        }
    }
}

/**
 * The two taps. Confirm is the page's single blue element; keeping a photograph
 * without a price is deliberately just as reachable, because a record that only
 * holds priced things is a spreadsheet.
 */
@Composable
private fun Decisions(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    typed: String = "",
) {
    Column(Modifier.fillMaxWidth()) {
        if (wide) {
            Action("Confirm", paper, { model.confirmCurrent() }, accent = true)
            Spacer(Modifier.height(6.dp))
            QuietAction(
                if (state.current?.amountMinor == null && typed.isEmpty()) {
                    "Keep it without an amount"
                } else {
                    "No amount"
                },
                paper,
                { model.confirmCurrent(noAmount = true) },
            )
            Spacer(Modifier.height(18.dp))
            Rule(paper)
            Spacer(Modifier.height(12.dp))
            // The only blue thing on the page is Confirm. Closing the day is how you
            // leave, not a second decision competing with the first — so it is a
            // hairline bar, and the caption under it says what it costs you.
            OutlineAction(
                "Close the day",
                paper,
                { model.closeDay((state.route as? Route.Nightfall)?.day) },
            )
            Spacer(Modifier.height(9.dp))
            Body(
                "Closing locks the day into the record. Long-press any card to open it again.",
                paper,
                color = paper.inkFaint,
                size = 11.sp,
                lineHeight = 16.sp,
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    OutlineAction(
                        "No amount",
                        paper,
                        { model.confirmCurrent(noAmount = true) },
                    )
                }
                Box(Modifier.weight(1.4f)) {
                    Action("Confirm", paper, { model.confirmCurrent() }, accent = true)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuietAction(
                    "${state.stackIndex + 1} of ${state.stack.size}",
                    paper,
                    { model.previous() },
                    color = paper.inkFaint,
                )
                QuietAction(
                    if (state.remaining > 1) "Next" else "Close the day",
                    paper,
                    {
                        if (state.remaining > 1) {
                            model.next()
                        } else {
                            model.closeDay((state.route as? Route.Nightfall)?.day)
                        }
                    },
                    color = paper.inkSoft,
                )
            }
        }
    }
}

/**
 * The keypad, ruled.
 *
 * Drawn as a grid of hairlines with no fills — a till slip rules its columns, it
 * does not draw a calculator. Rounded keys with white fills would be the single
 * most generic object on the most important page in the app.
 */
@Composable
private fun Keypad(paper: Paper, model: AppModel) {
    val rows = listOf("123", "456", "789", "0.")
    Column(Modifier.fillMaxWidth()) {
        Rule(paper)
        rows.forEachIndexed { r, row ->
            if (r > 0) Rule(paper)
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { c, digit ->
                    if (c > 0) Box(Modifier.width(Bone.Hairline).height(50.dp).background(paper.edge))
                    Box(
                        Modifier.weight(1f).height(50.dp).androidTap { model.typeAmount(digit) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Numeral(digit.toString(), paper, size = 21.sp, weight = FontWeight.Medium)
                    }
                }
            }
        }
        Rule(paper)
        Box(
            Modifier.fillMaxWidth().height(44.dp).androidTap { model.backspace() },
            contentAlignment = Alignment.Center,
        ) {
            Label("Delete", paper, color = paper.inkSoft)
        }
    }
}

@Composable
private fun PhotographOf(capture: Capture, paper: Paper, modifier: Modifier) {
    Box(modifier.border(Bone.Hairline, paper.edge)) {
        Plate(
            subject = Subject.forMerchant(capture.merchant),
            paper = paper,
            modifier = Modifier.fillMaxSize(),
            caption = capture.photoPath.removePrefix("demo:"),
        )
    }
}

@Composable
private fun RestingDay(paper: Paper, model: AppModel, day: com.ariadne.model.LocalDay?) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp)) {
        Title("The day is finished.", paper, size = 28.sp)
        Spacer(Modifier.height(12.dp))
        Body(
            "Everything you kept today is tallied. Nothing is waiting, and nothing is scolding you for it.",
            paper,
            serif = true,
            color = paper.inkSoft,
        )
        Spacer(Modifier.height(28.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))
        Action("Close the day", paper, { model.closeDay(day) }, accent = true)
        Spacer(Modifier.height(10.dp))
        QuietAction("Back to the record", paper, { model.go(Route.Record(day)) })
    }
}
