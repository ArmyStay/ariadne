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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.ariadne.design.OutlineAction
import com.ariadne.design.Masthead
import com.ariadne.design.Paper
import com.ariadne.design.Plate
import com.ariadne.design.Subject
import com.ariadne.design.QuietAction
import com.ariadne.design.RuledKeypad
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
            // Nightfall is the one page that is not a "record of something" — it
            // is a calm commit — so it gets the sunk ground, the same warm
            // paper the table headers read on.
            .background(if (paper.isNight) paper.bg else paper.sunk)
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
            RestingDay(paper, model, day, closedTrip = state.trip?.isClosed == true)
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
                // The photograph is the thing being read, so it does not scroll.
                //
                // It used to live in a scrolling column above a pinned keypad, and
                // the moment the amount was typed the receipt it came from was
                // gone — the one screen where you need to look at the paper showed
                // a keypad and a number with nothing behind them. Now the plate
                // takes whatever space the pinned parts below do not need, and at
                // 200% text it gets smaller instead of leaving the screen.
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    // The same plate as everywhere else: 1:1. A 16:9 crop here
                    // made the triage card look like a different app.
                    PhotographOf(capture, paper, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(12.dp))
                Header(state, model, paper, capture)
                Spacer(Modifier.height(10.dp))
                Amount(typed, state.dayCurrency, paper)
                Spacer(Modifier.height(12.dp))
                // The decision never scrolls away: two taps finish a card, so both
                // taps have to be on screen at the same time as the number.
                Decisions(state, model, paper, wide = false)
                if (!capture.hasUnconfirmedAmount) {
                    RuledKeypad(
                        paper = paper,
                        onDigit = { model.typeAmount(it) },
                        onBackspace = { model.backspace() },
                    )
                }
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
                Title(model.titleOf(capture), paper, size = 24.sp)
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
    val readyCount = state.stack.count { model.isReadyToConfirm(it) }
    val extractedAmount = state.current
        ?.takeIf(Capture::hasUnconfirmedAmount)
        ?.let { capture ->
            capture.amountMinor?.let { minor ->
                Money.ofMinor(minor).formatWithSymbol(capture.currency ?: CurrencyCode.of(state.dayCurrency))
            }
        }
    Column(Modifier.fillMaxWidth()) {
        if (extractedAmount != null) {
            Body(
                "Is it $extractedAmount?",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 16.sp,
            )
            Spacer(Modifier.height(10.dp))
        }
        if (wide) {
            if (extractedAmount != null) {
                Action("Yes, save it", paper, { model.confirmCurrent() }, accent = true)
                Spacer(Modifier.height(8.dp))
                OutlineAction("No, let me enter it", paper, { model.rejectCurrentExtractedAmount() })
            } else {
                Action("Confirm", paper, { model.confirmCurrent() }, accent = true)
            }
            if (readyCount > 1) {
                Spacer(Modifier.height(8.dp))
                OutlineAction(
                    "Confirm $readyCount ready",
                    paper,
                    { model.confirmReadyInStack() },
                )
                Spacer(Modifier.height(4.dp))
                Body(
                    "Keeps every photograph that already has an amount; the rest stay in the stack.",
                    paper,
                    color = paper.inkFaint,
                    size = 11.sp,
                    lineHeight = 16.sp,
                )
            }
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
            if (state.stack.size > 1) {
                Spacer(Modifier.height(6.dp))
                OutlineAction(
                    "Confirm rest as no amount",
                    paper,
                    { model.confirmRestAsNoAmount() },
                )
            }
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
                "Closing files the day. Any card can still be edited from the record.",
                paper,
                color = paper.inkFaint,
                size = 11.sp,
                lineHeight = 16.sp,
            )
        } else {
            if (readyCount > 1) {
                OutlineAction(
                    "Confirm $readyCount ready",
                    paper,
                    { model.confirmReadyInStack() },
                )
                Spacer(Modifier.height(8.dp))
            }
            // Stacked, not side by side. Two bars in half a measure fit at 100%
            // text and clip at 200%, and the phone is where 200% actually happens:
            // the blue decision comes first, the way out of it under it.
            if (extractedAmount != null) {
                Action("Yes, save", paper, { model.confirmCurrent() }, accent = true)
                Spacer(Modifier.height(8.dp))
                OutlineAction(
                    "No, enter it",
                    paper,
                    { model.rejectCurrentExtractedAmount() },
                )
            } else {
                Action("Confirm", paper, { model.confirmCurrent() }, accent = true)
                Spacer(Modifier.height(8.dp))
                OutlineAction(
                    "No amount",
                    paper,
                    { model.confirmCurrent(noAmount = true) },
                )
            }
            if (state.stack.size > 1) {
                Spacer(Modifier.height(8.dp))
                OutlineAction(
                    "Confirm rest as no amount",
                    paper,
                    { model.confirmRestAsNoAmount() },
                )
            }
            Spacer(Modifier.height(4.dp))
            // A column rather than the ends of one line: at 200% "3 of 5" and
            // "Next photograph" do not both fit across a phone, and a footer that
            // reflows is better than a footer that truncates.
            Column(Modifier.fillMaxWidth()) {
                QuietAction(
                    "${state.stackIndex + 1} of ${state.stack.size}",
                    paper,
                    { model.previous() },
                    color = paper.inkFaint,
                )
                QuietAction(
                    if (state.remaining > 1) "Next photograph" else "Close the day",
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

@Composable
private fun PhotographOf(capture: Capture, paper: Paper, modifier: Modifier) {
    Box(modifier.border(Bone.Hairline, paper.edge)) {
        PhotoPreview(
            photoPath = capture.photoPath,
            caption = com.ariadne.model.MerchantName.receiptTitle(capture.merchant),
            paper = paper,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RestingDay(
    paper: Paper,
    model: AppModel,
    day: com.ariadne.model.LocalDay?,
    closedTrip: Boolean,
) {
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
        if (closedTrip) {
            // A finished journey has no day left to close. The page says so and
            // offers the only two places that still do something.
            Body(
                "This trip is closed, so there is no day left to file.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            QuietAction("Back to the record", paper, { model.go(Route.Record(day)) })
        } else {
            Action("Close the day", paper, { model.closeDay(day) }, accent = true)
            Spacer(Modifier.height(10.dp))
            QuietAction("Back to the record", paper, { model.go(Route.Record(day)) })
        }
    }
}
