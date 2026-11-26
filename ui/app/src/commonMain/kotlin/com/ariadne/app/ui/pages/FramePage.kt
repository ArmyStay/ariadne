package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.RuledKeypad
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CurrencyCatalog
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money
import com.ariadne.model.symbol

/**
 * The capture sheet: the photograph, and the sheet that rises over it.
 *
 * This used to be two pages. The shutter opened a "held up" page with two buttons,
 * and "Fill it in now" opened the full editing page — so the amount, the one thing
 * a person actually came to enter, was two taps and a page transition away from the
 * shutter, with the photograph shrinking at every step.
 *
 * Now the shutter opens the sheet directly: the photograph stays behind, full
 * height, and the amount field, keypad, currency, category and note are on the sheet
 * in front of it. One decision, one surface. "Add the amount later" is still a first
 * class answer and still costs nothing — it is the sheet's secondary button, not a
 * page you have to find.
 *
 * On a wide window the sheet becomes a spread instead (print on the left, the field
 * on the right), because a 1200dp window with a 66% sheet over a photograph wastes
 * both halves.
 *
 * The sheet is drawn from the same parts as every other page — hairline top edge,
 * paper ground, ink type, one blue action. A Material modal sheet would have brought
 * its own surface, elevation and corner radius into a design that has none of those.
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

    val currency = capture?.currency ?: state.trip?.baseCurrency ?: CurrencyCode.of("INR")
    // The chips are the catalogue's shortlist, the same one the full page offers:
    // the capture's own currency first, then the trip's two, then the defaults.
    val options = CurrencyCatalog.shortlist(
        currency.code,
        state.trip?.baseCurrency?.code,
        state.trip?.homeCurrency?.code,
    )
    // Read once here and passed down as primitives, so a digit typed into the
    // amount does not recompose the photograph behind it.
    val typed = state.detailAmount
    val merchant = state.detailMerchant
    val category = state.detailCategory
    val note = state.detailNote

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
                key(typed, merchant, category, note) {
                    SheetFields(
                        capture = capture,
                        typed = typed,
                        merchant = merchant,
                        category = category,
                        note = note,
                        model = model,
                        paper = paper,
                        currency = currency,
                        options = options,
                    )
                }
                Spacer(Modifier.height(26.dp))
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(paper.bg)) {
        // The photograph, whole, behind everything. Square and top-aligned, the same
        // frame the viewfinder showed, so the shutter and the sheet are visibly the
        // same object.
        Box(Modifier.fillMaxSize()) {
            PhotoPreview(
                photoPath = capture.photoPath,
                caption = model.titleOf(capture),
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // A scrim only over the part the sheet does not cover: enough to say "the
        // picture is behind this", not enough to hide what was photographed.
        Box(Modifier.fillMaxSize().background(paper.scrim)) { }

        // The sheet.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.66f)
                .background(paper.raised)
                .border(width = Bone.Hairline, color = paper.edgeStrong),
        ) {
            Spacer(Modifier.height(8.dp))
            // The handle is drawn, not a gesture: the sheet is a surface, not a
            // draggable, and a bar that suggests dragging without dragging is worse
            // than a rule that just separates.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(3.dp)
                    .background(paper.edgeStrong),
            )
            Spacer(Modifier.height(10.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Bone.Margin),
            ) {
                key(typed, merchant, category, note) {
                    SheetFields(
                        capture = capture,
                        typed = typed,
                        merchant = merchant,
                        category = category,
                        note = note,
                        model = model,
                        paper = paper,
                        currency = currency,
                        options = options,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // The two answers, pinned where a thumb is, never below the fold.
            Column(Modifier.fillMaxWidth().padding(horizontal = Bone.Margin)) {
                Rule(paper, color = paper.edgeStrong)
                Spacer(Modifier.height(14.dp))
                if (typed.isNotEmpty()) {
                    Action("Save ${typed}", paper, { model.saveDetail() }, accent = true)
                    Spacer(Modifier.height(6.dp))
                    QuietAction("Add the amount later", paper, { model.keepFrameForLater() })
                } else {
                    Action("Add the amount later", paper, { model.keepFrameForLater() }, accent = true)
                }
                Spacer(Modifier.height(4.dp))
                QuietAction("the whole page", paper, { model.openFrame(capture.id) }, color = paper.inkFaint)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/**
 * The sheet's contents: the figure, the keypad, then the three questions the product
 * allows — what it cost, what it was, and what it was for.
 */
@Composable
private fun SheetFields(
    capture: Capture,
    typed: String,
    merchant: String,
    category: Category?,
    note: String,
    model: AppModel,
    paper: Paper,
    currency: CurrencyCode,
    options: List<String>,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Numeral(currency.symbol(), paper, color = paper.inkSoft, size = 18.sp)
        Spacer(Modifier.width(6.dp))
        Numeral(
            typed.ifEmpty { "0" },
            paper,
            size = 40.sp,
            weight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        if (typed.isEmpty()) {
            Label("how much", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        }
    }

    // A suggestion is offered, never typed in. The machine read a figure from the
    // photograph; saying yes is one tap and saying no costs nothing.
    if (capture.hasUnconfirmedAmount && typed.isEmpty()) {
        val suggested = capture.amountMinor?.let {
            Money.ofMinor(it).formatWithSymbol(capture.currency ?: currency)
        }
        if (suggested != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Body("Found $suggested on it.", paper, serif = true, color = paper.inkSoft, size = 15.sp)
                Spacer(Modifier.weight(1f))
                QuietAction("yes", paper, { model.acceptDetailSuggestion() }, color = paper.blue)
                Spacer(Modifier.width(14.dp))
                QuietAction("no", paper, { model.rejectDetailExtractedAmount() }, color = paper.inkFaint)
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    RuledKeypad(
        paper = paper,
        onDigit = { model.editDetailAmount(it) },
        onBackspace = { model.clearDetailAmountDigit() },
        rowHeight = 42.dp,
        digitSize = 18.sp,
    )
    Spacer(Modifier.height(10.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { code ->
            CurrencyChip(code, paper, selected = code == currency.code) {
                model.changeDetailCurrency(code)
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    CategoryChips(category, paper) { model.chooseCategory(it) }

    Spacer(Modifier.height(16.dp))
    LineField(
        label = "Where",
        value = merchant,
        placeholder = "the shop, if you want it named",
        paper = paper,
        onChange = model::editDetailMerchant,
        serif = false,
    )

    Spacer(Modifier.height(14.dp))
    LineField(
        label = "Remember it by",
        value = note,
        placeholder = "a line for later",
        paper = paper,
        onChange = model::editDetailNote,
        serif = true,
    )
}

/** The print, on the wide spread: the same plate geometry as everywhere else. */
@Composable
private fun Print(capture: Capture, paper: Paper) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxSize()
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
