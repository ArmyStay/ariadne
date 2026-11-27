package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ariadne.design.FigureRow
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.Masthead
import com.ariadne.design.PageAction
import com.ariadne.design.ScrollingMargin
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.DaySummary
import com.ariadne.model.entryStatus
import com.ariadne.app.ui.tap
import com.ariadne.design.tap

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
    val shownCard = days.firstOrNull { it.localDate == shown }
    val dayIsClosed = shownCard?.isClosed == true
    // Every day of the trip has a card from the moment the trip exists, so a record
    // for next Tuesday is reachable. It is a page about a day with nothing on it yet,
    // and it must not offer to put something there.
    val recordable = model.hasHappened(shown)
    // A closed trip is a finished record: even a day that was never filed is not
    // something the ritual can be opened for any more.
    val tripClosed = state.trip?.isClosed == true

    ScrollingMargin(paper, wide, margin = if (wide) 56.dp else Bone.Margin) { _ ->
        Masthead(
            number = "",
            eyebrow = state.trip?.name ?: "Record",
            title = "The record",
            paper = paper,
            // The shutter opened from this day files into this day.
            trailing = if (recordable) {
                { PageAction("Capture", paper, { model.openCapture(shown) }) }
            } else {
                null
            },
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
        if (captures.isEmpty() && !recordable) {
            // The one place the app says the rule to your face rather than by
            // leaving a control out: the shutter is not offered above, and this
            // page explains why before you go looking for it.
            Body(
                "This day hasn't happened yet. There is nothing on it — it is the part of " +
                    "the journey still ahead.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 17.sp,
            )
            Spacer(Modifier.height(22.dp))
            Action("Go to today", paper, { model.openRecord(state.today) })
        } else if (captures.isEmpty()) {
            Body(
                "Nothing kept on this day. An empty day is still part of the trip — it is the one you spent walking.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 17.sp,
            )
            Spacer(Modifier.height(22.dp))
            Action("Add a photograph", paper, { model.openCapture(shown) })
        } else {
            captures.forEachIndexed { i, capture ->
                ReceiptLine(capture, model.titleOf(capture), i, paper) { model.openDetail(capture.id) }
            }
        }

        Spacer(Modifier.height(26.dp))
        Rule(paper)
        Spacer(Modifier.height(14.dp))

        if (state.dayPerCurrency.isNotEmpty()) {
            // With the symbol, not just the digits: a record is read at a glance and
            // a bare number in a row of two currencies is the one thing a glance can
            // get wrong.
            state.dayPerCurrency.forEach { (code, minor) ->
                FigureRow(
                    code,
                    Money.ofMinor(minor).formatWithSymbol(CurrencyCode.of(code)),
                    paper,
                )
            }
            Spacer(Modifier.height(4.dp))
            Rule(paper)
        }
        FigureRow(
            "Tallied",
            // The "≈" appears exactly when the figure contains money that was
            // converted into it; the note beneath says at which rate's date.
            (if (state.dayRateDates.isEmpty()) "" else "≈ ") +
                Money.ofMinor(state.dayTotalMinor).formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
            paper,
            emphasised = true,
        )
        Spacer(Modifier.height(6.dp))
        ConversionNote(state.dayRateDates, state.dayUnconverted, paper)

        // Somebody else's figures for this day, quoted beside the total rather than
        // added into it. Their money is on their phone; this one holds a copy of the
        // numbers and none of the receipts, and the day's own total stays yours.
        if (state.travellerSharesForDay.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            state.travellerSharesForDay.forEach { share ->
                FigureRow(
                    share.traveller.name,
                    Money.ofMinor(share.baseMinor).formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
                    paper,
                )
            }
            val together = state.dayTotalMinor + state.travellerSharesForDay.sumOf { it.baseMinor }
            FigureRow(
                "With them",
                Money.ofMinor(together).formatWithSymbol(CurrencyCode.of(state.dayCurrency)),
                paper,
                emphasised = true,
            )
            Spacer(Modifier.height(6.dp))
            Body(
                "Their lines arrived as a code: confirmed by the person who spent the money, " +
                    "and quoted here. Nothing about them is in your record or in your card.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
        }
        // The total only ever holds finished cards. Saying so is the difference
        // between a number you trust and a number that looks broken.
        // Counted from the entry state, so this sentence and the timeline row for
        // the same day always agree on how many cards are open.
        val waiting = DaySummary.of(state.dayCaptures).pending
        if (waiting > 0) {
            Spacer(Modifier.height(6.dp))
            Body(
                if (waiting == 1) {
                    "1 card is still open. Nothing is in the total until you close the day — the " +
                        "figures above are what the photographs said, not what you have confirmed."
                } else {
                    "$waiting cards are still open. Nothing is in the total until you close the day — " +
                        "the figures above are what the photographs said, not what you have confirmed."
                },
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
        }

        Spacer(Modifier.height(26.dp))
        // One primary move per page: the day is done, so the only forward action
        // is the ritual. A closed day has nothing to settle, so the bar goes
        // genuinely inert rather than inviting a tap that leads nowhere.
        Action(
            "Close day",
            paper,
            { model.openCloseDay(shown) },
            accent = true,
            enabled = recordable && !dayIsClosed && !tripClosed,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            QuietAction("Search", paper, { model.go(Route.Search) }, color = paper.inkMid)
            QuietAction("Trip", paper, { model.go(Route.Trip) }, color = paper.inkMid)
        }

        Spacer(Modifier.height(20.dp))
        if (tripClosed) {
            Body(
                "This trip is closed. The record is readable and its card is yours; " +
                    "nothing can be added to it.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
            )
        }
        shownCard?.let { card ->
            if (card.isClosed) {
                Body(
                    "This day is closed. Its cards can still be edited from here.",
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
            .tap("Open this day", onClick = onClick)
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
private fun ReceiptLine(
    capture: Capture,
    /** The card's name: a believable merchant, or "Receipt · 21:09". Never raw OCR. */
    title: String,
    index: Int,
    paper: Paper,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().tap("Open this photograph", onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(54.dp)
                .aspectRatio(1f)
                .border(Bone.Hairline, paper.edge),
        ) {
            PhotoPreview(
                photoPath = capture.photoPath,
                // A screen reader hears the same words the title uses, so a
                // photograph of a menu bar is described as "Receipt · 21:09"
                // rather than read out as a menu bar.
                caption = title,
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Title(title, paper, size = 16.sp)
            Spacer(Modifier.height(3.dp))
            val currency = capture.currency
            Label(
                // The entry state's own words, or the currency when there is an
                // amount to name. One vocabulary for every row on every page.
                if (currency != null && capture.amountMinor != null) {
                    currency.code
                } else {
                    capture.entryStatus.label
                },
                paper,
                color = paper.inkFaint,
            )
        }
        Numeral(
            // The symbol travels with the figure, so a line read on its own says which
            // money it is: ฿450 and ₹450 are not the same lunch.
            capture.amountMinor?.let {
                Money.ofMinor(it).formatWithSymbol(capture.currency ?: CurrencyCode.of("INR"))
            } ?: "—",
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
