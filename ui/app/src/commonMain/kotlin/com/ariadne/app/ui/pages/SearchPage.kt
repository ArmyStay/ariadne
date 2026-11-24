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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.Masthead
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.Capture
import com.ariadne.model.Money

/**
 * Page 8 — Search.
 *
 * A single field, ruled underneath, sized like a title. Results are photographs
 * with a price beneath — never text rows, because you recognise the shoe and not
 * the string "Bata Footwear".
 */
@Composable
public fun SearchPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 22.dp),
    ) {
        Masthead(
            number = "08",
            eyebrow = "The record",
            title = "Remember a thing",
            paper = paper,
            trailing = { QuietAction("Record", paper, { model.go(Route.Record(state.today)) }) },
        )

        Spacer(Modifier.height(26.dp))

        // The query, set as type rather than as a form field.
        Box(Modifier.fillMaxWidth()) {
            if (state.searchQuery.isEmpty()) {
                Title("shoes · 1200 · THB · 21 Nov", paper, size = 26.sp, color = paper.inkFaint)
            }
            BasicTextField(
                value = state.searchQuery,
                onValueChange = model::search,
                textStyle = TextStyle(
                    fontFamily = Bone.Serif,
                    fontSize = 26.sp,
                    color = paper.ink,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(paper.ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = "Search the record" },
            )
        }
        Spacer(Modifier.height(12.dp))
        Rule(paper, color = paper.ink)

        Spacer(Modifier.height(24.dp))

        when {
            state.searchQuery.isEmpty() -> Body(
                "Type a shop, a price, a currency, or a day. Everything you kept is in here — " +
                    "including the things you left without an amount.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 16.sp,
            )

            state.searchResults.isEmpty() -> Body(
                "Nothing by that name. Try a price, or the day it happened.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 16.sp,
            )

            else -> {
                Label("${state.searchResults.size} kept", paper, color = paper.inkFaint)
                Spacer(Modifier.height(16.dp))
                state.searchResults.forEach { capture ->
                    SearchRow(capture, paper) { model.openRecord(capture.localDate) }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(capture: Capture, paper: Paper, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().androidTap(onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(58.dp)
                .aspectRatio(1f)
                .border(Bone.Hairline, paper.edge)
                .background(paper.sunk),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Title(capture.merchant ?: "Untitled", paper, size = 16.sp)
            Spacer(Modifier.height(3.dp))
            Label(capture.localDate.iso, paper, color = paper.inkFaint)
        }
        Numeral(
            capture.amountMinor?.let { minor ->
                capture.currency?.let { Money.ofMinor(minor).format(it) } ?: "—"
            } ?: "—",
            paper,
            color = if (capture.amountMinor == null) paper.inkFaint else paper.ink,
            size = 16.sp,
        )
    }
    Rule(paper, color = paper.edge.copy(alpha = 0.7f))
}
