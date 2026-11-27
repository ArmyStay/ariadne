package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.app.SheetPreview
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.Paper
import com.ariadne.design.Pill
import com.ariadne.design.PillTone
import com.ariadne.design.QuietAction
import com.ariadne.design.RuledKeypad
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CurrencyCatalog
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.entryStatus
import com.ariadne.model.isoWeekday
import com.ariadne.model.symbol

/**
 * The capture sheet: the photograph, and the sheet that fills in what it was.
 *
 * The first decision is deliberately small: keep the photograph and add the amount
 * later, or open the complete editor. OCR suggestions stay unconfirmed for Close Day.
 *
 * The sheet is drawn from the same parts as every other page — hairline edges, paper
 * ground, ink type, one blue action. A Material modal sheet would have brought its own
 * surface, elevation and corner radius into a design that has none of those.
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

    if (wide) {
        Row(
            Modifier.fillMaxSize().background(paper.bg).padding(horizontal = 56.dp, vertical = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(44.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Print(capture, paper)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                QuickCaptureDetails(capture, model, paper)
                Spacer(Modifier.height(18.dp))
                SheetActions(capture, model, paper)
                Spacer(Modifier.height(26.dp))
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(paper.bg)) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(0.64f)
                .padding(horizontal = Bone.Margin, vertical = 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(paper.sunk)
                .border(Bone.Hairline, paper.edgeStrong, RoundedCornerShape(2.dp)),
        ) {
            PhotoPreview(
                photoPath = capture.photoPath,
                caption = model.titleOf(capture),
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(0.36f)
                .background(paper.raised)
                .border(width = Bone.Hairline, color = paper.edgeStrong),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Bone.Margin - 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SheetBack(paper) { model.keepFrameForLater() }
                Spacer(Modifier.width(2.dp))
                Column(Modifier.weight(1f)) {
                    Label(
                        "the photograph is kept",
                        paper,
                        color = paper.inkFaint,
                        size = 8.sp,
                        tracking = 1.0.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Label(
                        longDay(capture.localDate) + " · " + model.clockTimeOf(capture.createdAt),
                        paper,
                        color = paper.inkGhost,
                        size = 8.sp,
                        tracking = 0.8.sp,
                    )
                }
                Pill(
                    capture.entryStatus.label,
                    paper,
                    if (capture.entryStatus.isPending) PillTone.Blue else PillTone.Quiet,
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Bone.Margin),
            ) {
                QuickCaptureDetails(capture, model, paper, showLabel = false)
                Spacer(Modifier.height(16.dp))
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = Bone.Margin)) {
                SheetActions(capture, model, paper)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/**
 * The two immediate choices: keep the photograph for Close Day, or open the full
 * editor. Pinned to the foot in both layouts.
 */
@Composable
private fun SheetActions(capture: Capture, model: AppModel, paper: Paper) {
    Action("Keep for later", paper, { model.keepFrameForLater() }, accent = true)
    Spacer(Modifier.height(6.dp))
    QuietAction(
        "Open the full editor",
        paper,
        { model.openFrame(capture.id) },
        color = paper.inkSoft,
    )
}

@Composable
private fun QuickCaptureDetails(
    capture: Capture,
    model: AppModel,
    paper: Paper,
    showLabel: Boolean = true,
) {
    if (showLabel) {
        Label("Photograph kept", paper, color = paper.inkFaint, tracking = 1.2.sp)
        Spacer(Modifier.height(8.dp))
    }
    Title(model.titleOf(capture), paper, size = 23.sp)
    Spacer(Modifier.height(8.dp))
    Body(
        "No amount is needed now. You can finish it at Close Day.",
        paper,
        color = paper.inkSoft,
        size = 14.sp,
        lineHeight = 20.sp,
    )
    capture.amountMinor?.takeIf { capture.hasUnconfirmedAmount }?.let { amount ->
        Spacer(Modifier.height(10.dp))
        Body(
            "Found ${Money.ofMinor(amount).formatWithSymbol(capture.currency ?: CurrencyCode.of("INR"))}. " +
                "Confirm it at Close Day.",
            paper,
            serif = true,
            color = paper.inkMid,
            size = 13.sp,
        )
    }
}

/**
 * The sheet's contents, in the order a receipt is read: what it was, what it came
 * to, what that is worth, and then the four questions the product is allowed to ask.
 */
@Composable
private fun SheetFields(
    capture: Capture,
    typed: String,
    merchant: String,
    category: Category?,
    note: String,
    preview: SheetPreview?,
    busy: Boolean,
    model: AppModel,
    paper: Paper,
    currency: CurrencyCode,
    base: CurrencyCode,
    options: List<String>,
) {
    Moment(capture, typed, currency, model, paper)

    Suggestion(capture, typed, model, paper)

    Spacer(Modifier.height(14.dp))
    Pair(capture, typed, preview, currency, base, paper)

    Spacer(Modifier.height(12.dp))
    OutlineAction(
        if (busy) "Reading the photograph…" else "Read the photograph again",
        paper,
        { model.reExtractCurrent() },
        enabled = !busy,
    )

    // ── amount ────────────────────────────────────────────────────────────
    Spacer(Modifier.height(22.dp))
    FieldLabel("amount", paper)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // The currency's symbol in its own box, the way a paper field prints the
        // units before the space you write in.
        Box(
            Modifier
                .width(56.dp)
                .height(52.dp)
                .border(Bone.Hairline, paper.edgeStrong, RoundedCornerShape(1.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Numeral(currency.symbol(), paper, color = paper.inkSoft, size = 17.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(52.dp)
                .border(Bone.Hairline, paper.edgeStrong, RoundedCornerShape(1.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (typed.isEmpty()) {
                Label("how much", paper, color = paper.inkGhost, size = 9.sp, tracking = 1.2.sp)
            } else {
                Numeral(typed, paper, size = 26.sp, weight = FontWeight.Medium)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Helper(
        if (currency == base) {
            "already in the trip's currency, ${base.code}."
        } else {
            "will convert to the trip's currency (${base.code}) on save."
        },
        paper,
    )

    Spacer(Modifier.height(12.dp))
    RuledKeypad(
        paper = paper,
        onDigit = { model.editDetailAmount(it) },
        onBackspace = { model.clearDetailAmountDigit() },
        rowHeight = 46.dp,
        digitSize = 19.sp,
    )

    // ── currency ──────────────────────────────────────────────────────────
    Spacer(Modifier.height(22.dp))
    FieldLabel("currency", paper)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { code ->
            CurrencyChip(code, paper, selected = code == currency.code) {
                model.changeDetailCurrency(code)
            }
        }
    }

    // ── category ──────────────────────────────────────────────────────────
    Spacer(Modifier.height(22.dp))
    FieldLabel("category", paper)
    Spacer(Modifier.height(8.dp))
    CategoryChips(category, paper) { model.chooseCategory(it) }

    // ── where, and what for ───────────────────────────────────────────────
    Spacer(Modifier.height(20.dp))
    LineField(
        label = "Merchant",
        value = merchant,
        placeholder = "the shop, if you want it named",
        paper = paper,
        onChange = model::editDetailMerchant,
        serif = false,
    )

    Spacer(Modifier.height(18.dp))
    LineField(
        label = "Remember it by",
        value = note,
        placeholder = "a line for later",
        paper = paper,
        onChange = model::editDetailNote,
        serif = true,
    )

    // ── the day ───────────────────────────────────────────────────────────
    Spacer(Modifier.height(22.dp))
    FieldLabel("date", paper)
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .border(Bone.Hairline, paper.edge, RoundedCornerShape(1.dp))
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Numeral(numericDay(capture.localDate), paper, color = paper.inkMid, size = 14.sp)
        Spacer(Modifier.width(12.dp))
        Label(model.clockTimeOf(capture.createdAt), paper, color = paper.inkFaint, size = 9.sp)
        Spacer(Modifier.weight(1f))
        Label("as photographed", paper, color = paper.inkGhost, size = 8.sp, tracking = 0.9.sp)
    }
}

/**
 * The head of the sheet: the print, the moment, the figure as it stands.
 *
 * The thumbnail is here rather than only behind the sheet because the sheet now
 * covers most of the photograph; without it the person would be typing a figure for
 * a picture they can no longer see.
 */
@Composable
private fun Moment(
    capture: Capture,
    typed: String,
    currency: CurrencyCode,
    model: AppModel,
    paper: Paper,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(62.dp)
                .aspectRatio(1f)
                .border(Bone.Hairline, paper.edge),
        ) {
            PhotoPreview(
                photoPath = capture.photoPath,
                caption = model.titleOf(capture),
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Label(
                longDay(capture.localDate) + " · " + model.clockTimeOf(capture.createdAt),
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Numeral(currency.symbol(), paper, color = paper.inkSoft, size = 16.sp)
                Spacer(Modifier.width(5.dp))
                Numeral(
                    typed.ifEmpty { "0" },
                    paper,
                    size = 30.sp,
                    weight = FontWeight.Medium,
                )
                if (typed.isEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Label("nothing typed yet", paper, color = paper.inkGhost, size = 8.sp)
                }
            }
        }
    }
}

/**
 * "Found S$18.41 on it" — the machine's reading, offered and never typed in.
 *
 * Saying yes is one tap and files it; saying no clears the suggestion and leaves the
 * field for the person. Both answers are one line, because a receipt has two.
 */
@Composable
private fun Suggestion(capture: Capture, typed: String, model: AppModel, paper: Paper) {
    if (!capture.hasUnconfirmedAmount || typed.isNotEmpty()) return
    val suggested = capture.amountMinor?.let {
        Money.ofMinor(it).formatWithSymbol(capture.currency ?: CurrencyCode.of("INR"))
    } ?: return
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Body("Found $suggested on it.", paper, serif = true, color = paper.inkSoft, size = 15.sp)
        Spacer(Modifier.weight(1f))
        QuietAction("yes", paper, { model.acceptDetailSuggestion() }, color = paper.blue)
        Spacer(Modifier.width(12.dp))
        QuietAction(
            "no",
            paper,
            { model.rejectDetailExtractedAmount() },
            color = paper.inkFaint,
        )
    }
}

/**
 * The pair: what was paid, and what it is worth in the trip's currency.
 *
 * Two labelled columns over one bordered block, the figure on the left, an "≈"
 * between them, and the rate printed underneath — the shape a currency conversion
 * takes on a receipt, and the reason this block exists: the number the person typed
 * and the number the totals will move by should be visible at the same time, before
 * either of them is saved.
 *
 * When there is no rate on file the block says so instead of guessing. That is the
 * ₹2,605 bug's lesson: money that cannot be converted is named, never added.
 */
@Composable
private fun Pair(
    capture: Capture,
    typed: String,
    preview: SheetPreview?,
    currency: CurrencyCode,
    base: CurrencyCode,
    paper: Paper,
) {
    val from = preview?.from ?: capture.currency ?: currency
    // What was paid: what the person typed, or what the machine read, or nothing.
    val original: Money? = if (typed.isNotEmpty()) {
        Money.parseOrNull(typed, from)
    } else {
        capture.amountMinor?.let(Money::ofMinor)
    }
    val converted: Money? = if (original == null) null else preview?.base

    Column(
        Modifier
            .fillMaxWidth()
            .background(paper.sunk, RoundedCornerShape(1.dp))
            .border(Bone.Hairline, paper.edge, RoundedCornerShape(1.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Label(
                "original purchase",
                paper,
                color = paper.inkFaint,
                size = 8.sp,
                tracking = 1.4.sp,
                modifier = Modifier.weight(1f),
            )
            Label(
                "base trip equivalent",
                paper,
                color = paper.inkFaint,
                size = 8.sp,
                tracking = 1.4.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Numeral(
                original?.formatWithSymbol(from) ?: "—",
                paper,
                color = if (original == null) paper.inkGhost else paper.ink,
                size = 19.sp,
            )
            Spacer(Modifier.width(10.dp))
            Numeral("≈", paper, color = paper.inkGhost, size = 15.sp)
            Spacer(Modifier.weight(1f))
            Numeral(
                converted?.formatWithSymbol(base) ?: "—",
                paper,
                color = if (converted == null) paper.inkGhost else paper.ink,
                size = 19.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        val note = when {
            original == null -> "type an amount and the pair updates"
            preview?.sameCurrency == true -> "already in the trip's currency"
            preview?.base == null -> "no ${from.code} rate yet — it will be counted in ${from.code}"
            preview?.rateLabel != null -> "rate: ${preview.rateLabel}"
            else -> ""
        }
        if (note.isNotEmpty()) {
            Body(note, paper, color = paper.inkFaint, size = 12.sp)
        }
    }
}

// ── small parts ────────────────────────────────────────────────────────────────

/** The way back out of the sheet, drawn rather than lettered. */
@Composable
private fun SheetBack(paper: Paper, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .semantics { contentDescription = "Back" }
            .rowTap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(15.dp)) {
            val w = this.size.width
            val h = this.size.height
            drawLine(paper.inkSoft, Offset(w * 0.78f, h * 0.06f), Offset(w * 0.22f, h * 0.5f), 1.7f)
            drawLine(paper.inkSoft, Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.94f), 1.7f)
        }
    }
}

/** A tracked label over a field, the way every section on this sheet announces itself. */
@Composable
private fun FieldLabel(text: String, paper: Paper) {
    Label(text, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.7.sp)
}

/** One sentence under a field, in the smallest voice on the page. */
@Composable
private fun Helper(text: String, paper: Paper) {
    Body(text, paper, color = paper.inkFaint, size = 11.sp)
}

/** "TUESDAY 11 NOVEMBER" — the day a receipt was printed, spelled out. */
private fun longDay(day: LocalDay): String {
    val weekday = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY",
    )[(day.isoWeekday() - 1).coerceIn(0, 6)]
    val month = listOf(
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER",
    )[(day.month - 1).coerceIn(0, 11)]
    return "$weekday ${day.day} $month"
}

/** "11/11/2025" — the same day, the way the field prints it. */
private fun numericDay(day: LocalDay): String =
    "${day.iso.substring(8, 10)}/${day.iso.substring(5, 7)}/${day.iso.substring(0, 4)}"

/** The print, on the wide spread: the same plate geometry as everywhere else. */
@Composable
private fun Print(capture: Capture, paper: Paper) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .background(paper.sunk)
                .border(Bone.Hairline, paper.edge),
        ) {
            PhotoPreview(
                photoPath = capture.photoPath,
                caption = "",
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
