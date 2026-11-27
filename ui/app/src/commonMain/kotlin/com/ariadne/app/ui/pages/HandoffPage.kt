package com.ariadne.app.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Money

/**
 * 09 — Two phones, one ledger.
 *
 * Both directions of the hand-off live on one page, because they are one idea: a code
 * carries a plan to a phone that has none, and a code carries a day's figures back.
 * Everything here is text — readable, copyable, editable by hand — and nothing here
 * opens a connection.
 *
 * The page states the three honest edges rather than hiding them: a code can be
 * handed over twice (the second paste is refused by its fingerprint), a code can be
 * edited by hand (the checksum catches an accident, not a fraud), and there is no
 * sync, so nothing updates on its own.
 */
@Composable
public fun HandoffPage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
) {
    val trip = state.trip
    if (trip == null) {
        Page(paper = paper, wide = wide) {
            Body("There is no trip open to hand over.", paper, serif = true, color = paper.inkSoft)
            Spacer(Modifier.height(16.dp))
            QuietAction("The register", paper, { model.go(com.ariadne.app.Route.Trips) })
        }
        return
    }
    val code = model.tripCode()

    Page(paper = paper, wide = wide) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Masthead(
                number = "09",
                eyebrow = "two phones",
                title = "One ledger,\nhanded over",
                paper = paper,
                italic = true,
                wide = wide,
                lede = "A code goes out with the plan. A code comes back with the figures. No account, no server.",
            )

            Spacer(Modifier.height(24.dp))

            // ── the plan, going out ──────────────────────────────────────
            Label("the plan", paper, color = paper.blue, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(10.dp))
            Body(
                code ?: "This trip cannot be written as a code — its dates are outside what a code carries.",
                paper,
                serif = false,
                color = paper.ink,
                size = if (wide) 20.sp else 17.sp,
            )
            Spacer(Modifier.height(8.dp))
            Body(
                "Read it out, or send the line. They join with it and have the same dates, " +
                    "currency and budget — the trip's name is theirs to choose.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
            )
            if (model.tripCodeDroppedBudget()) {
                Spacer(Modifier.height(8.dp))
                Body(
                    "The budget is too large for a code to carry, so it is not in it. " +
                        "Everything else is.",
                    paper,
                    serif = true,
                    color = paper.inkSoft,
                    size = 15.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Action("Copy the trip code", paper, { model.copyTripCode() }, accent = !state.codeCopied)
            if (state.codeCopied) {
                Spacer(Modifier.height(10.dp))
                Label("on the clipboard", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
            }

            Rule(paper)

            // ── the day, coming back ─────────────────────────────────────
            Label("a day, handed back", paper, color = paper.blue, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(10.dp))
            Body(
                "Every confirmed figure on a closed day, as one line. No photograph, no note: " +
                    "only the part of the record that has an amount in it.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
            )
            Spacer(Modifier.height(14.dp))
            val closedWithFigures = state.dayCards.filter { card ->
                card.isClosed && state.dayCapturesAll[card.localDate.iso].orEmpty()
                    .any { capture -> capture.amountMinor != null }
            }
            if (closedWithFigures.isEmpty()) {
                Body(
                    "No day is closed with a figure on it yet. Close a day first — the hand-over " +
                        "is what a finished day is for.",
                    paper,
                    serif = true,
                    color = paper.inkFaint,
                    size = 15.sp,
                )
            } else {
                closedWithFigures.forEach { day ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Title(day.localDate.iso, paper, size = 19.sp)
                            val figure = state.dayFigures[day.localDate.iso] ?: 0L
                            Body(
                                Money.ofMinor(figure).formatWithSymbol(trip.baseCurrency),
                                paper,
                                serif = true,
                                color = paper.inkMid,
                                size = 14.sp,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        QuietAction("Copy the day", paper, { model.copyLedgerCode(day.localDate) })
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Rule(paper)

            // ── their figures, coming in ─────────────────────────────────
            Label("their figures", paper, color = paper.blue, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.height(10.dp))
            Body(
                "Paste the line they sent. It is read here before anything is added, and the " +
                    "same line pasted twice is refused rather than counted twice.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
            )
            Spacer(Modifier.height(12.dp))
            PasteField(
                value = state.ledgerPaste,
                paper = paper,
                onChange = model::editLedgerPaste,
            )
            val preview = state.ledgerPreview
            val error = state.ledgerError
            if (error != null) {
                Body(error, paper, serif = true, color = paper.inkSoft, size = 15.sp)
                Spacer(Modifier.height(10.dp))
            } else if (preview != null) {
                val first = preview.days.firstOrNull()
                val last = preview.days.lastOrNull()
                val span = if (first == null || last == null || last == first) {
                    first?.iso.orEmpty()
                } else {
                    "${first.iso} to ${last.iso}"
                }
                Body(
                    "${preview.traveller} · ${preview.entries.size} lines · $span",
                    paper,
                    serif = true,
                    color = paper.ink,
                    size = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
                Action("Add their figures", paper, { model.addPastedLedger() }, accent = true)
                Spacer(Modifier.height(12.dp))
            }

            if (state.travellers.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                state.travellers.forEach { traveller ->
                    val share = state.travellerShares.firstOrNull { it.traveller.id == traveller.id }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Title(traveller.name, paper, size = 19.sp)
                            Body(
                                buildString {
                                    append(Money.ofMinor(share?.baseMinor ?: 0L).formatWithSymbol(trip.baseCurrency))
                                    append(" · ${traveller.entries.size} lines")
                                    share?.unconverted?.forEach { (code, minor) ->
                                        append(" · $code ${Money.ofMinor(minor).format(com.ariadne.model.CurrencyCode.of(code))} with no rate")
                                    }
                                },
                                paper,
                                serif = true,
                                color = paper.inkMid,
                                size = 14.sp,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        QuietAction("Remove", paper, { model.removeTraveller(traveller.id) })
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Rule(paper)

            Body(
                "A code can be handed over twice; the second paste is refused. A code can be " +
                    "edited by hand — the checksum catches an accident, not a fraud. And there is " +
                    "still no sync: nothing here updates on its own.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 14.sp,
            )
            Spacer(Modifier.height(20.dp))
            OutlineAction("Back to the trip", paper, { model.back() })
        }
    }
}

/**
 * A field that accepts a pasted line.
 *
 * Deliberately tall and plain: this is the one input in the book that will hold four
 * hundred characters, and a single-line field would scroll a ledger sideways.
 */
@Composable
private fun PasteField(
    value: String,
    paper: Paper,
    onChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = Optical.bodyTracking,
                color = paper.ink,
            ),
            cursorBrush = SolidColor(paper.blue),
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .semantics { contentDescription = "the ledger line" },
        )
        Spacer(Modifier.height(9.dp))
        Rule(paper)
        Spacer(Modifier.height(16.dp))
    }
}
