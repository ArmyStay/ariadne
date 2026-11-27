package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.ReceiptCard
import com.ariadne.design.ReceiptHeadLine
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.EntryStatus
import com.ariadne.model.Money
import com.ariadne.model.SpokenExpense
import com.ariadne.model.entryStatus
import com.ariadne.design.tap

/**
 * The photograph you have just kept.
 *
 * This surface has one job and it is not data entry: it says *the picture is safe,
 * the amount can wait*, and it offers the two things a thumb might want — keep it,
 * or go and fill it in. The old version of this page said that in small type over a
 * sheet that covered its own photograph; the photograph was a 16 % sliver at the top
 * of the screen and the rest of the sheet was empty.
 *
 * So the picture is the page now. It lies on a torn-off receipt card — the same
 * mark a shop makes when it tears a bill off the roll — under a head that prints
 * `RECEIPT` on the left and the time on the right, and the state of the entry is
 * worn in the corner of the masthead as a pill, exactly where the reference puts it.
 * Under the card, three lines: that it was kept, what still has to happen, and
 * where that happens. Then the two answers, pinned where a thumb is.
 *
 * Nothing here is a Material sheet. The card is paper with a hairline and a cut
 * foot; the primary action is an ink block with a 14 dp corner, because the
 * reference draws that button round and it is the only round control in the book.
 */
@Composable
public fun FramePage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    captureId: String,
) {
    val capture = state.dayCapturesAll.values.flatten().firstOrNull { it.id == captureId }
        ?: state.detail?.takeIf { it.id == captureId }
        ?: state.stack.firstOrNull { it.id == captureId }

    if (capture == null) {
        // A route can outlive its row (the photograph was deleted from another
        // page). Say so plainly and give the way out; never a blank screen.
        Column(Modifier.fillMaxSize().background(paper.bg).padding(Bone.Margin)) {
            Title("That photograph is not here any more.", paper, size = 24.sp, italic = true)
            Spacer(Modifier.height(16.dp))
            Action("Back to today", paper, { model.go(Route.Home) }, accent = true)
        }
        return
    }

    val status = capture.entryStatus
    // The amount the machine read, offered and never typed in. It is named in the
    // currency on the receipt, not in the trip's.
    val found = capture.amountMinor
        ?.takeIf { status == EntryStatus.HasAmount }
        ?.let { Money.ofMinor(it).formatWithSymbol(capture.currency ?: CurrencyCode.of("INR")) }

    val headline = when {
        found != null -> "Found $found."
        status == EntryStatus.Confirmed -> "Confirmed."
        status == EntryStatus.NoAmount -> "Kept without an amount."
        status == EntryStatus.InRecord -> "In the record."
        else -> "No amount is needed now."
    }
    val subline = when {
        found != null -> "Confirm it at Close Day."
        status == EntryStatus.Confirmed -> "It is already counted."
        status == EntryStatus.NoAmount -> "It stays in the record as it is."
        status == EntryStatus.InRecord -> "The day it belongs to is closed."
        else -> "You can finish it at Close Day."
    }

    // The amount, said out loud. Offered only where the platform has a recogniser and
    // the entry is still open, and never guessed: a sentence that is not a figure is
    // simply not a figure.
    val voice = LocalVoiceEntry.current
    var listening by remember { mutableStateOf(false) }
    val say: () -> Unit = {
        val entry = voice
        if (entry != null) {
            listening = true
            entry.listen { heard ->
                listening = false
                if (heard != null) {
                    val spoken = SpokenExpense.parse(
                        heard,
                        capture.currency ?: state.trip?.baseCurrency,
                    )
                    if (spoken != null) model.hearAmount(spoken.minor, spoken.currency)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(paper.bg)) {
        SheetHead(
            title = model.titleOf(capture).substringBefore(" · "),
            status = status.label,
            paper = paper,
        ) { model.keepFrameForLater() }

        Rule(paper, Modifier.padding(horizontal = Bone.Margin), color = paper.edgeStrong)

        if (wide) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 56.dp, vertical = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(44.dp),
            ) {
                Box(Modifier.weight(1.05f)) {
                    PhotoCard(capture, model, paper, photoHeight = 520.dp)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SheetReadout(headline, subline, paper)
                    Spacer(Modifier.height(30.dp))
                    SheetActions(
                    status = status,
                    paper = paper,
                    onKeep = { model.keepFrameForLater() },
                    onOpen = { model.openFrame(capture.id) },
                    onSay = say,
                    listening = listening,
                )
                }
            }
        } else {
            // A compact window or enlarged system text can scroll instead of
            // squeezing the photograph or hiding the actions below the screen.
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val photoHeight = (maxHeight * 0.36f).coerceIn(160.dp, 300.dp)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = Bone.Margin),
                ) {
                    Spacer(Modifier.height(16.dp))
                    PhotoCard(capture, model, paper, photoHeight = photoHeight)
                    Spacer(Modifier.height(20.dp))
                    SheetReadout(headline, subline, paper)
                    Spacer(Modifier.height(20.dp))
                    SheetActions(
                        status = status,
                        paper = paper,
                        onKeep = { model.keepFrameForLater() },
                        onOpen = { model.openFrame(capture.id) },
                        onSay = say,
                        listening = listening,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * The head of the page: the way back, what this is called, and what it still owes.
 *
 * The title is the merchant's name when the recogniser found one and `Receipt`
 * when it did not — the same rule as everywhere else in the app, minus the time,
 * which the card prints for itself.
 */
@Composable
private fun SheetHead(
    title: String,
    status: String,
    paper: Paper,
    onBack: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Bone.Margin - 10.dp, end = Bone.Margin, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back keeps the photograph. Putting it away is not the same as throwing
        // it away, and this app has no button for the second thing.
        SheetBack(paper, onBack)
        Spacer(Modifier.width(2.dp))
        Title(title, paper, size = 24.sp, lineHeight = 28.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        StatusPill(status, paper)
    }
}

/** The entry's state, worn as a hairline pill rather than announced in a sentence. */
@Composable
private fun StatusPill(text: String, paper: Paper) {
    Box(
        Modifier
            .border(Bone.Hairline, paper.ink, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, paper, color = paper.ink, size = 9.sp, tracking = 1.2.sp)
    }
}

/**
 * The photograph, on the card.
 *
 * The card's head prints the word and the time the way a bill does; the picture
 * fills everything under it. This is the block that was missing: the image used to
 * be a strip behind a sheet that covered it.
 */
@Composable
private fun PhotoCard(
    capture: Capture,
    model: AppModel,
    paper: Paper,
    modifier: Modifier = Modifier,
    photoHeight: Dp? = null,
) {
    ReceiptCard(paper, modifier) {
        ReceiptHeadLine(
            left = "Receipt",
            paper = paper,
            right = model.clockTimeOf(capture.createdAt),
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (photoHeight != null) Modifier.height(photoHeight) else Modifier.weight(1f))
                .background(paper.sunk),
        ) {
            PhotoPreview(
                photoPath = capture.photoPath,
                caption = model.titleOf(capture),
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** What was kept, what is still owed, and where it is owed. */
@Composable
private fun SheetReadout(headline: String, subline: String, paper: Paper) {
    Label("photograph kept", paper, color = paper.inkFaint, size = 8.sp, tracking = 1.2.sp)
    Spacer(Modifier.height(10.dp))
    Title(headline, paper, size = 27.sp, lineHeight = 33.sp)
    Spacer(Modifier.height(8.dp))
    Body(
        subline,
        paper,
        serif = true,
        color = paper.inkSoft,
        size = 15.sp,
        lineHeight = 22.sp,
    )
}

/**
 * The two answers: keep the photograph for Close Day, or open the full editor.
 *
 * Pinned to the foot in both layouts, and in that order — the safe answer first,
 * the one that asks more of you second and quieter.
 */
@Composable
private fun SheetActions(
    status: EntryStatus,
    paper: Paper,
    onKeep: () -> Unit,
    onOpen: () -> Unit,
    onSay: () -> Unit,
    listening: Boolean,
) {
    Action(
        if (status.isResolved) "Done" else "Keep for later",
        paper,
        onKeep,
        fill = false,
        corner = 14.dp,
    )
    Spacer(Modifier.height(10.dp))
    QuietAction("Open the full editor", paper, onOpen, color = paper.inkSoft)
    // The microphone, where one exists and the entry is still open: the figure can be
    // said now, at the shop, or left for Close Day. It is a drawn button rather than
    // a line of underlined type, because it is the one action here you take without
    // looking at the screen.
    if (!status.isResolved) {
        Spacer(Modifier.height(10.dp))
        MicAction(paper, listening = listening, onClick = onSay)
    }
}

// ── small parts ────────────────────────────────────────────────────────────────

/** The way back out of the sheet, drawn rather than lettered. */
@Composable
private fun SheetBack(paper: Paper, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = "Back" }
            .tap("Go back one page") { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(15.dp)) {
            val w = this.size.width
            val h = this.size.height
            drawLine(paper.ink, Offset(w * 0.78f, h * 0.06f), Offset(w * 0.22f, h * 0.5f), 1.7f)
            drawLine(paper.ink, Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.94f), 1.7f)
        }
    }
}
