package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.PageHeader
import com.ariadne.design.Paper
import com.ariadne.design.Plate
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Subject
import com.ariadne.design.Title
import com.ariadne.model.Category
import com.ariadne.model.CurrencyCode
import com.ariadne.model.symbol

/**
 * Page — one capture, open in the hand.
 *
 * The photograph is the largest thing on the page and stays that way: the fields
 * are set beneath it in a single column, each on its own hairline, so the screen
 * reads as the back of a print rather than as a form. Everything here is
 * optional except the decision to leave.
 *
 * This page exists because a night of triage cannot be the whole editing surface:
 * some receipts need a shop name corrected, a currency changed, or a line saying
 * what it was for. It is the *only* thing the reference screens justified adding
 * as a plain need — the rest of their feature list (SMS import, voice capture,
 * splitting a bill with friends) is either on nobody's critical path or a
 * different product.
 */
@Composable
public fun DetailPage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
) {
    val capture = state.detail
    val currency = capture?.currency ?: state.trip?.baseCurrency ?: CurrencyCode.of("INR")

    Box(Modifier.fillMaxSize().background(paper.bg), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = if (wide) Bone.Measure + 220.dp else Dp.Unspecified)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) Bone.MarginWide else Bone.Margin, vertical = 22.dp),
        ) {
            PageHeader(
                eyebrow = capture?.localDate?.iso ?: "Photograph",
                title = "The photograph",
                paper = paper,
                italicTitle = true,
                trailing = {
                    QuietAction("Back", paper, { model.openRecord(capture?.localDate) }, color = paper.inkSoft)
                },
            )

            Spacer(Modifier.height(24.dp))

            if (capture == null) {
                Body(
                    "That photograph is not in the record any more.",
                    paper,
                    serif = true,
                    color = paper.inkSoft,
                    size = 16.sp,
                )
                Spacer(Modifier.height(22.dp))
                Action("Back to the record", paper, { model.openRecord(null) }, accent = true)
                return@Column
            }

            if (wide) {
                // A spread: the plate on the left page, the fields on the right.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                    Column(Modifier.weight(1f)) { Photograph(capture.state.name, capture.photoPath, capture.merchant, paper) }
                    Column(Modifier.weight(1f)) { Fields(state, model, paper, currency) }
                }
            } else {
                Photograph(capture.state.name, capture.photoPath, capture.merchant, paper)
                Spacer(Modifier.height(26.dp))
                Fields(state, model, paper, currency)
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Photograph(stateName: String, photoPath: String, merchant: String?, paper: Paper) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).border(Bone.Hairline, paper.edge)) {
            Plate(
                subject = Subject.forMerchant(merchant),
                paper = paper,
                modifier = Modifier.fillMaxSize(),
                caption = photoPath.removePrefix("demo:"),
            )
        }
        Spacer(Modifier.height(10.dp))
        Label(stateName.replace('_', ' '), paper, color = paper.inkFaint)
    }
}

@Composable
private fun Fields(state: AppUiState, model: AppModel, paper: Paper, currency: CurrencyCode) {
    Column(Modifier.fillMaxWidth()) {
        // The figure, set as type rather than as a box: it is the page's subject.
        Label("Amount", paper, color = paper.inkSoft)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral(currency.symbol(), paper, color = paper.inkSoft, size = 20.sp)
            Spacer(Modifier.width(8.dp))
            Numeral(
                state.detailAmount.ifEmpty { "0" },
                paper,
                size = 44.sp,
                weight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(14.dp))
        Rule(paper)
        Spacer(Modifier.height(14.dp))
        Keypad(paper, model)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyChip(currency.code, paper, selected = true) { }
            listOf("THB", "USD", "EUR").filter { it != currency.code }.forEach { code ->
                CurrencyChip(code, paper, selected = false) { model.changeDetailCurrency(code) }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Merchant: one line, written over the hairline rather than boxed.
        LineField(
            label = "Where",
            value = state.detailMerchant,
            placeholder = "the shop, if you want it named",
            paper = paper,
            onChange = model::editDetailMerchant,
            serif = false,
        )

        Spacer(Modifier.height(20.dp))

        // Category: a claim, never a guess. Tap the same chip again to clear it.
        Label("What it was", paper, color = paper.inkSoft)
        Spacer(Modifier.height(10.dp))
        CategoryChips(state.detailCategory, paper) { model.chooseCategory(it) }
        Spacer(Modifier.height(8.dp))
        Label(
            "optional · nothing is sorted for you",
            paper,
            color = paper.inkFaint,
            tracking = 1.2.sp,
            size = 9.sp,
        )

        Spacer(Modifier.height(24.dp))

        LineField(
            label = "Remember it by",
            value = state.detailNote,
            placeholder = "a line for later",
            paper = paper,
            onChange = model::editDetailNote,
            serif = true,
        )

        Spacer(Modifier.height(28.dp))

        Action("Save", paper, { model.saveDetail() }, accent = true)
        Spacer(Modifier.height(10.dp))
        OutlineAction("Keep it without an amount", paper, { model.saveDetail(keepWithoutAmount = true) })
        Spacer(Modifier.height(10.dp))
        QuietAction("Leave it open", paper, { model.openRecord(state.detail?.localDate) }, color = paper.inkSoft)
    }
}

@Composable
private fun LineField(
    label: String,
    value: String,
    placeholder: String,
    paper: Paper,
    onChange: (String) -> Unit,
    serif: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Label(label, paper, color = paper.inkSoft)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Title(placeholder, paper, size = if (serif) 22.sp else 18.sp, color = paper.inkFaint)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = if (serif) Bone.Serif else Bone.Sans,
                    fontSize = if (serif) 22.sp else 18.sp,
                    color = paper.ink,
                ),
                cursorBrush = SolidColor(paper.blue),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label },
            )
        }
        Spacer(Modifier.height(10.dp))
        Rule(paper, color = paper.edgeStrong)
    }
}

@Composable
private fun CategoryChips(selected: Category?, paper: Paper, onSelect: (Category) -> Unit) {
    // Two rows of three on a phone: the whole vocabulary visible at once, no menu.
    val rows = Category.entries.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    val chosen = category == selected
                    Box(
                        Modifier
                            .background(
                                if (chosen) paper.blueWash else paper.raised,
                                androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                            )
                            .border(
                                Bone.Hairline,
                                if (chosen) paper.blueEdge else paper.edge,
                                androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                            )
                            .pointerInput(category) { detectTapGestures(onTap = { onSelect(category) }) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Label(
                            category.label,
                            paper,
                            color = if (chosen) paper.blue else paper.inkMid,
                            size = 10.sp,
                            tracking = 1.4.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyChip(code: String, paper: Paper, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) paper.ink else paper.raised)
            .border(Bone.Hairline, if (selected) paper.ink else paper.edge)
            .pointerInput(code) { detectTapGestures(onTap = { if (!selected) onClick() }) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "Currency $code" },
    ) {
        Label(
            code,
            paper,
            color = if (selected) paper.onBlue else paper.inkMid,
            size = 9.sp,
            tracking = 1.4.sp,
        )
    }
}

/**
 * A keypad drawn the way this book draws everything else: as a grid of hairlines
 * with no fill, the way a till slip rules its columns. Tapping is the only ink.
 */
@Composable
private fun Keypad(paper: Paper, model: AppModel) {
    val rows = listOf("123", "456", "789", "0.")
    Column(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { r, row ->
            if (r > 0) Rule(paper)
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { c, digit ->
                    if (c > 0) Box(Modifier.width(Bone.Hairline).height(48.dp).background(paper.edge))
                    Box(
                        Modifier.weight(1f).height(48.dp).androidTap { model.editDetailAmount(digit) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Numeral(digit.toString(), paper, size = 19.sp, weight = FontWeight.Medium)
                    }
                }
            }
        }
        Rule(paper)
        Box(
            Modifier.fillMaxWidth().height(42.dp).androidTap { model.clearDetailAmountDigit() },
            contentAlignment = Alignment.Center,
        ) {
            Label("Delete", paper, color = paper.inkSoft)
        }
    }
}
