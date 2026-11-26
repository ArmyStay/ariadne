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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.Masthead
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.RuledKeypad
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Category
import com.ariadne.model.CurrencyCatalog
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
    // A closed trip is read, not edited: no keypad, no Save, and one sentence
    // saying why. The model refuses the write too, so a stale route cannot get
    // around the page.
    val readOnly = state.trip?.isClosed == true
    // The chips are the catalogue's shortlist, not a fixed list: the currency the
    // capture is in comes first, then the trip's own two, then the travel
    // defaults. A trip created in JPY can always be switched back to JPY here,
    // which the old fixed row could not do.
    val currencyOptions = CurrencyCatalog.shortlist(
        currency.code,
        state.trip?.baseCurrency?.code,
        state.trip?.homeCurrency?.code,
    )
    // The three values typed on this page, read once here and passed down as
    // primitives. The plate on the left must not recompose because a digit went
    // into the amount on the right, and taking the whole AppUiState is how it did.
    val typedAmount = state.detailAmount
    val merchant = state.detailMerchant
    val category = state.detailCategory
    val typedNote = state.detailNote

    Box(Modifier.fillMaxSize().background(paper.bg), contentAlignment = Alignment.TopCenter) {
        // Wide: one centred, scrolling spread. Phone: a fixed frame whose middle
        // scrolls and whose keypad is pinned - so the page ends where a hand does.
        // The scroll is applied to different elements in the two layouts on
        // purpose: `weight(1f)` inside a scrolling parent has unbounded height and
        // collapses to nothing.
        Column(
            Modifier
                .widthIn(max = if (wide) Bone.Measure + 220.dp else Dp.Unspecified)
                .fillMaxWidth()
                .then(
                    if (wide) Modifier.verticalScroll(rememberScrollState()) else Modifier.fillMaxSize(),
                )
                .padding(horizontal = if (wide) Bone.MarginWide else Bone.Margin, vertical = 22.dp),
        ) {
            Masthead(
                number = "",
                eyebrow = capture?.localDate?.iso ?: "Photograph",
                title = "The photograph",
                paper = paper,
                italic = true,
                trailing = {
                    // Back returns to the search that found the photograph, or to
                    // the day's record - whichever page opened this one.
                    QuietAction("Back", paper, { model.closeDetail() }, color = paper.inkSoft)
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
                Action("Back to the record", paper, { model.closeDetail() }, accent = true)
                return@Column
            }

            if (wide) {
                // A spread: the plate on the left page, the fields on the right.
                // Both are boxed on the facts they read, so the plate and the
                // header stay still while a figure is typed beside them.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                    Column(Modifier.weight(1f)) {
                        key(capture.id, capture.state) {
                            Photograph(capture.state.name, capture.photoPath, capture.merchant, paper)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        key(typedAmount, merchant, category, typedNote) {
                            Fields(
                                capture = capture,
                                typedAmount = typedAmount,
                                merchant = merchant,
                                category = category,
                                typedNote = typedNote,
                                model = model,
                                paper = paper,
                                currency = currency,
                                options = currencyOptions,
                                keypad = true,
                                readOnly = readOnly,
                            )
                        }
                    }
                }
            } else {
                // Phone: the plate and the fields scroll; the keypad is pinned to
                // the bottom edge. The page used to be one column that ended
                // wherever its content ended, which on a capture with two fields
                // filled left a screen of white under the Save button.
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        key(capture.id, capture.state) {
                            Photograph(
                                capture.state.name,
                                capture.photoPath,
                                capture.merchant,
                                paper,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        key(typedAmount, merchant, category, typedNote) {
                            Fields(
                                capture = capture,
                                typedAmount = typedAmount,
                                merchant = merchant,
                                category = category,
                                typedNote = typedNote,
                                model = model,
                                paper = paper,
                                currency = currency,
                                options = currencyOptions,
                                keypad = false,
                                readOnly = readOnly,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    if (!capture.hasUnconfirmedAmount && !readOnly) {
                        RuledKeypad(
                            paper = paper,
                            onDigit = { model.editDetailAmount(it) },
                            onBackspace = { model.clearDetailAmountDigit() },
                            rowHeight = 48.dp,
                            digitSize = 19.sp,
                        )
                    }
                }
            }

            if (wide) Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Photograph(
    stateName: String,
    photoPath: String,
    merchant: String?,
    paper: Paper,
) {
    Column(Modifier.fillMaxWidth()) {
        // One plate geometry in the whole book: 1:1, hairline, no rounding. It was
        // 4:5 here, 16:10 on a phone, 16:9 in the triage card and 1.4:1 on a wide
        // frame, which meant the same photograph was a different object on every
        // page. Square matches the viewfinder, so the shutter and the print are
        // visibly the same frame.
        Box(Modifier.fillMaxWidth().aspectRatio(1f).border(Bone.Hairline, paper.edge)) {
            PhotoPreview(
                photoPath = photoPath,
                // The same name the row and the search result carry, so a screen
                // reader describes one photograph one way throughout.
                caption = com.ariadne.model.MerchantName.receiptTitle(merchant),
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Label(stateName.replace('_', ' '), paper, color = paper.inkFaint)
    }
}

@Composable
private fun Fields(
    capture: com.ariadne.model.Capture,
    typedAmount: String,
    merchant: String,
    category: com.ariadne.model.Category?,
    typedNote: String,
    model: AppModel,
    paper: Paper,
    currency: CurrencyCode,
    options: List<String>,
    keypad: Boolean,
    readOnly: Boolean = false,
) {
    val state = capture
    Column(Modifier.fillMaxWidth()) {
        // The figure, set as type rather than as a box: it is the page's subject.
        Label("Amount", paper, color = paper.inkSoft)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Numeral(currency.symbol(), paper, color = paper.inkSoft, size = 20.sp)
            Spacer(Modifier.width(8.dp))
            Numeral(
                typedAmount.ifEmpty { "0" },
                paper,
                size = 44.sp,
                weight = FontWeight.Medium,
            )
        }
        if (capture.hasUnconfirmedAmount && !readOnly) {
            val extractedAmount = capture.amountMinor?.let {
                com.ariadne.model.Money.ofMinor(it).formatWithSymbol(capture.currency ?: currency)
            }
            if (extractedAmount != null) {
                Spacer(Modifier.height(8.dp))
                Body(
                    "Is it $extractedAmount?",
                    paper,
                    serif = true,
                    color = paper.inkSoft,
                    size = 15.sp,
                )
                Spacer(Modifier.height(8.dp))
                Action("Yes, save it", paper, { model.saveDetail() }, accent = true)
                Spacer(Modifier.height(8.dp))
                OutlineAction("No, let me enter it", paper, { model.rejectDetailExtractedAmount() })
            }
        }
        Spacer(Modifier.height(14.dp))
        Rule(paper)
        Spacer(Modifier.height(14.dp))
        if (keypad && !readOnly) {
            if (!capture.hasUnconfirmedAmount) {
                RuledKeypad(
                    paper = paper,
                    onDigit = { model.editDetailAmount(it) },
                    onBackspace = { model.clearDetailAmountDigit() },
                    rowHeight = 48.dp,
                    digitSize = 19.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { code ->
                CurrencyChip(
                    code,
                    paper,
                    selected = code == currency.code,
                    enabled = !readOnly,
                ) {
                    model.changeDetailCurrency(code)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Merchant: one line, written over the hairline rather than boxed.
        LineField(
            label = "Where",
            value = merchant,
            placeholder = "the shop, if you want it named",
            paper = paper,
            enabled = !readOnly,
            onChange = model::editDetailMerchant,
            serif = false,
        )

        Spacer(Modifier.height(20.dp))

        // Category: a claim, never a guess. Tap the same chip again to clear it.
        Label("What it was", paper, color = paper.inkSoft)
        Spacer(Modifier.height(10.dp))
        CategoryChips(category, paper, enabled = !readOnly) { model.chooseCategory(it) }
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
            value = typedNote,
            placeholder = "a line for later",
            paper = paper,
            enabled = !readOnly,
            onChange = model::editDetailNote,
            serif = true,
        )

        Spacer(Modifier.height(28.dp))

        if (readOnly) {
            Body(
                "This trip is closed. The photograph and its figures stay exactly as they " +
                    "were; nothing new can be filed against a finished journey.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 18.sp,
            )
        } else {
            if (!capture.hasUnconfirmedAmount) {
                Action("Save", paper, { model.saveDetail() }, accent = true)
                Spacer(Modifier.height(10.dp))
                OutlineAction("Keep it without an amount", paper, { model.saveDetail(keepWithoutAmount = true) })
            }
            Spacer(Modifier.height(10.dp))
            QuietAction("Leave it open", paper, { model.openRecord(capture.localDate) }, color = paper.inkSoft)
        }
    }
}

@Composable
internal fun LineField(
    label: String,
    value: String,
    placeholder: String,
    paper: Paper,
    onChange: (String) -> Unit,
    serif: Boolean,
    enabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth()) {
        Label(label, paper, color = paper.inkSoft)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Title(placeholder, paper, size = if (serif) 22.sp else 18.sp, color = paper.inkFaint)
            }
            // A closed trip's fields are printed, not typed: the same type in the
            // same place, without a cursor, because a cursor promises a change.
            if (!enabled) {
                if (value.isNotEmpty()) {
                    Title(
                        value,
                        paper,
                        size = if (serif) 22.sp else 18.sp,
                        color = paper.inkMid,
                    )
                }
            } else BasicTextField(
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
internal fun CategoryChips(
    selected: Category?,
    paper: Paper,
    enabled: Boolean = true,
    onSelect: (Category) -> Unit,
) {
    // Two rows of three on a phone: the whole vocabulary visible at once, no menu.
    val rows = Category.entries.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    val chosen = category == selected
                    // The chip stays a small printed tag; the tap target around it
                    // is a finger's width. Density and reach are separate decisions.
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .pointerInput(category, enabled) {
                                if (enabled) detectTapGestures(onTap = { onSelect(category) })
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .background(
                                    // A chosen chip is filled, not tinted: the same
                                    // mark the currency chips already use, so a row of
                                    // chips reads the same way wherever it appears.
                                    if (chosen) paper.ink else paper.raised,
                                    androidx.compose.foundation.shape.RoundedCornerShape(1.dp),
                                )
                                .border(
                                    Bone.Hairline,
                                    if (chosen) paper.ink else paper.edge,
                                    androidx.compose.foundation.shape.RoundedCornerShape(1.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Label(
                                category.label,
                                paper,
                                color = if (chosen) paper.onBlue else paper.inkSoft,
                                size = 9.sp,
                                tracking = 1.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CurrencyChip(
    code: String,
    paper: Paper,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .pointerInput(code, enabled) {
                if (enabled) detectTapGestures(onTap = { if (!selected) onClick() })
            }
            .semantics {
                contentDescription = when {
                    selected -> "Currency $code, the one on this photograph"
                    enabled -> "Currency $code"
                    else -> "Currency $code, not changeable on a closed trip"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .background(if (selected) paper.ink else paper.raised)
                .border(Bone.Hairline, if (selected) paper.ink else paper.edge)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Label(
                code,
                paper,
                // An unactionable chip is still readable, and stops looking pressable.
                color = when {
                    selected -> paper.onBlue
                    enabled -> paper.inkMid
                    else -> paper.inkFaint
                },
                size = 9.sp,
                tracking = 1.4.sp,
            )
        }
    }
}
