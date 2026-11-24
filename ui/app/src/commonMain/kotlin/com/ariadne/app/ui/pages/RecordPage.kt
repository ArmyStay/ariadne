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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.FigureRow
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.PageHeader
import com.ariadne.design.Paper
import com.ariadne.design.Plate
import com.ariadne.design.Subject
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Money

/**
 * Page 6 — Record.
 *
 * A day per plate. The strip across the top is the trip as a run of days, closed
 * ones inked and open ones outlined; below it, the day you are looking at, in the
 * order it happened, with the total ruled off at the bottom the way a till slip
 * is ruled off.
 *
 * There is no table anywhere on this page, and that is deliberate: a spreadsheet
 * is the one shape this product refuses.
 */
@Composable
public fun RecordPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val days = state.dayCards
    val shown = state.route.let { (it as? Route.Record)?.day } ?: state.today

    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 22.dp),
    ) {
        PageHeader(
            eyebrow = state.trip?.name ?: "Record",
            title = "The record",
            paper = paper,
            trailing = { QuietAction("Capture", paper, { model.go(Route.Capture) }) },
        )

        Spacer(Modifier.height(18.dp))

        // The day being read, as type rather than as a label: this is the one
        // place the date needs to be large, because it is what you are looking at.
        Title(shown?.iso ?: "No day yet", paper, size = 24.sp)
        Spacer(Modifier.height(14.dp))

        // The run of days.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { card ->
                DayTab(
                    card = card,
                    selected = card.localDate == shown,
                    paper = paper,
                    onClick = { model.openRecord(card.localDate) },
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        val captures = state.dayCaptures
        if (captures.isEmpty()) {
            Body(
                "Nothing kept on this day. An empty day is still part of the trip — it is the one you spent walking.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 17.sp,
            )
            Spacer(Modifier.height(22.dp))
            Action("Add a photograph", paper, { model.go(Route.Capture) })
        } else {
            captures.forEachIndexed { i, capture ->
                ReceiptLine(capture, i, paper) { model.openDetail(capture.id) }
            }
        }

        Spacer(Modifier.height(26.dp))
        Rule(paper)
        Spacer(Modifier.height(14.dp))

        if (state.perCurrency.isNotEmpty()) {
            state.perCurrency.forEach { (code, minor) ->
                FigureRow(code, Money.ofMinor(minor).format(CurrencyCode.of(code)), paper)
            }
            Spacer(Modifier.height(4.dp))
            Rule(paper)
        }
        FigureRow(
            "Tallied",
            Money.ofMinor(state.dayTotalMinor).formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
            paper,
            emphasised = true,
        )
        // The total only ever holds finished cards. Saying so is the difference
        // between a number you trust and a number that looks broken.
        val waiting = state.stack.size
        if (waiting > 0) {
            Spacer(Modifier.height(6.dp))
            Body(
                if (waiting == 1) {
                    "1 card is still open. Nothing is in the total until nightfall — the figures " +
                        "above are what the photographs said, not what you have confirmed."
                } else {
                    "$waiting cards are still open. Nothing is in the total until nightfall — the " +
                        "figures above are what the photographs said, not what you have confirmed."
                },
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
        }

        Spacer(Modifier.height(26.dp))
        // One primary move per page: the day is done, so the only forward action
        // is the ritual. Search and the trip are deliberately quieter.
        Action("Nightfall", paper, { model.openNightfall(shown) }, accent = true)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            QuietAction("Search", paper, { model.go(Route.Search) }, color = paper.inkMid)
            QuietAction("Trip", paper, { model.go(Route.Trip) }, color = paper.inkMid)
        }

        Spacer(Modifier.height(20.dp))
        days.getOrNull(days.indexOfFirst { it.localDate == shown })?.let { card ->
            if (card.isClosed) {
                Body(
                    "This day is closed. Long-press any card to open it again.",
                    paper,
                    color = paper.inkFaint,
                    size = 12.sp,
                )
            } else {
                QuietAction("Close this day", paper, { model.closeDay(shown) }, color = paper.inkMid)
            }
        }
    }
}

/** One day in the strip: inked when closed, outlined when open. */
@Composable
private fun DayTab(card: DayCard, selected: Boolean, paper: Paper, onClick: () -> Unit) {
    val border = if (selected) paper.ink else paper.edge
    Column(
        Modifier
            .width(64.dp)
            .border(if (selected) 1.dp else Bone.Hairline, border)
            .background(if (card.isClosed) paper.sunk else paper.bg)
            .androidTap(onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Numeral(card.localDate.iso.takeLast(2), paper, size = 17.sp, align = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Label(
            monthAbbrev(card.localDate),
            paper,
            color = if (card.isClosed) paper.inkMid else paper.inkFaint,
        )
        if (card.isClosed) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(14.dp).height(1.dp).background(paper.ink))
        }
    }
}

/** One capture as a line of a slip: plate, name, figure. */
@Composable
private fun ReceiptLine(capture: Capture, index: Int, paper: Paper, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().androidTap(onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(54.dp)
                .aspectRatio(1f)
                .border(Bone.Hairline, paper.edge),
        ) {
            Plate(Subject.forMerchant(capture.merchant), paper, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Title(capture.merchant ?: "Untitled", paper, size = 16.sp)
            Spacer(Modifier.height(3.dp))
            Label(
                when {
                    capture.markedNoAmount -> "no amount"
                    capture.currency == null -> "not filled in"
                    else -> capture.currency.code
                },
                paper,
                color = paper.inkFaint,
            )
        }
        Numeral(
            capture.amountMinor?.let { Money.ofMinor(it).format(capture.currency ?: CurrencyCode.of("INR")) } ?: "—",
            paper,
            color = if (capture.amountMinor == null) paper.inkFaint else paper.ink,
            size = 16.sp,
        )
    }
    Rule(paper, color = paper.edge.copy(alpha = 0.7f))
}

private fun monthAbbrev(day: LocalDay): String = when (day.month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
}

/** Shared tap modifier for pages that need a plain press on a whole row. */
internal fun Modifier.androidTap(onClick: () -> Unit): Modifier =
    this.pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
