package com.ariadne.app.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.design.Bone
import com.ariadne.design.Body
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Motion
import com.ariadne.design.Numeral
import com.ariadne.design.Optical
import com.ariadne.design.Paper
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.design.motionSpec
import com.ariadne.model.CurrencyCatalog
import com.ariadne.model.CurrencyOption
import com.ariadne.design.tap

/**
 * The currency, chosen rather than spelled.
 *
 * A three-letter box accepted anything, including `ABC`, and a row of six fixed
 * chips could not reach a trip in JPY. This is the middle: the sixty-seven codes
 * the arithmetic already understands, searchable by code or by name, with the
 * current one stated on the row so the person can see what they are changing.
 *
 * The list unfolds inside the page rather than in a dialog. A sheet that covers
 * the page is a second place the app can be, and this app is a book: an index
 * that opens below its heading is what a book does.
 */
@Composable
public fun CurrencyField(
    label: String,
    code: String,
    paper: Paper,
    note: String? = null,
    glyph: Glyph = Glyph.Budget,
    onChoose: (String) -> Unit,
) {
    var open by remember(label) { mutableStateOf(false) }
    var query by remember(label) { mutableStateOf("") }
    val chosen = CurrencyCatalog.of(code)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .tap("Choose a currency") { open = !open }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(glyph, paper, size = 16.dp, color = paper.inkSoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Title(label, paper, size = 17.sp)
                Spacer(Modifier.height(3.dp))
                Label(
                    note ?: (chosen?.name ?: "a code outside the catalogue"),
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 1.1.sp,
                )
            }
            Numeral(code, paper, size = 18.sp, weight = FontWeight.Medium, color = paper.ink)
            Spacer(Modifier.width(8.dp))
            Icon(Glyph.Edit, paper, size = 14.dp, color = paper.inkFaint)
        }

        AnimatedVisibility(
            visible = open,
            enter = expandVertically(motionSpec(Motion.Reveal)) + fadeIn(motionSpec(Motion.Reveal)),
            exit = shrinkVertically(motionSpec(Motion.Reveal)) + fadeOut(motionSpec(Motion.Reveal)),
        ) {
            Column(Modifier.fillMaxWidth()) {
                CurrencySearch(query, paper) { query = it }
                val results = CurrencyCatalog.search(query, limit = 6)
                if (results.isEmpty()) {
                    Body(
                        "Nothing matches “$query”. A photograph's own currency is still read " +
                            "from the receipt, whatever this list holds.",
                        paper,
                        color = paper.inkFaint,
                        size = 12.sp,
                        lineHeight = 18.sp,
                    )
                } else {
                    results.forEach { option ->
                        CurrencyRow(
                            option = option,
                            selected = option.code == code,
                            paper = paper,
                            onTap = {
                                onChoose(option.code)
                                open = false
                                query = ""
                            },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // The scope stated once, in the same words in all three places
                // this field appears: the list is the catalogue, not a keyboard,
                // and a code it does not hold can still arrive from a receipt.
                Label(
                    "the ${CurrencyCatalog.all.size} codes the arithmetic reads",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 1.2.sp,
                )
                Spacer(Modifier.height(6.dp))
                Body(
                    "A receipt in any other code is still read and shown as it is; " +
                        "it simply cannot be a trip's own currency.",
                    paper,
                    color = paper.inkFaint,
                    size = 12.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        Rule(paper)
    }
}

/** One line of search, ruled like every other field in the book. */
@Composable
private fun CurrencySearch(query: String, paper: Paper, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            if (query.isEmpty()) {
                Body("Search by code or name", paper, color = paper.inkFaint, size = 15.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
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
                    .semantics { contentDescription = "Search currencies" },
            )
        }
        Spacer(Modifier.height(8.dp))
        Rule(paper, color = paper.edgeStrong)
        Spacer(Modifier.height(4.dp))
    }
}

/** One currency, as a printable row: code, name, sign. */
@Composable
private fun CurrencyRow(
    option: CurrencyOption,
    selected: Boolean,
    paper: Paper,
    onTap: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .tap("Choose ${option.code}", onClick = onTap)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(44.dp)) {
            Numeral(
                option.code,
                paper,
                size = 13.sp,
                weight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) paper.blue else paper.ink,
            )
        }
        Spacer(Modifier.width(12.dp))
        Body(
            option.name,
            paper,
            color = paper.inkSoft,
            size = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Label(option.symbol, paper, color = paper.inkFaint, size = 10.sp, tracking = 1.2.sp)
        if (selected) {
            Spacer(Modifier.width(10.dp))
            // A short blue tab under the code: the one mark that says "this one",
            // in the palette's only chromatic value.
            Box(Modifier.width(14.dp).height(2.dp).background(paper.blue))
        }
    }
}
