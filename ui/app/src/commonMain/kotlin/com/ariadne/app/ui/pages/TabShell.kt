package com.ariadne.app.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ariadne.design.Page
import com.ariadne.design.Paper
import com.ariadne.design.ScrollRail

/**
 * The frame all four destinations share.
 *
 * One scrolling column of content, a hairline measure hanging in the margin on a
 * wide page, and the hanging margin itself — which is the whole two-axis idea of
 * the design, so it belongs in one place rather than being re-decided per page.
 */
@Composable
internal fun TabPage(
    paper: Paper,
    wide: Boolean,
    railDocked: Boolean,
    onToggleRail: () -> Unit,
    rail: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Page(
        paper,
        wide,
        railDocked = railDocked,
        onToggleRail = onToggleRail,
        rail = rail,
    ) {
        Row(Modifier.fillMaxWidth()) {
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(bottom = 40.dp),
            ) { content() }
            // The measure hangs in the margin, level with the text it measures and
            // never on top of it.
            Box(Modifier.fillMaxHeight().padding(start = 10.dp)) {
                ScrollRail(scroll, paper, Modifier.fillMaxHeight().offset(x = 4.dp))
            }
        }
    }
}

/**
 * Room for the bar at the foot of a scrolling page.
 *
 * The bar is part of the shell, not of the page, so a page cannot measure it; this
 * is the one number both sides agree on. Taller than the bar itself, because the
 * shutter is docked over its top edge and a last row of photographs should not sit
 * under it.
 */
@Composable
internal fun AriadneBarSpacer() {
    Spacer(Modifier.height(104.dp))
}

/**
 * A whole row as one tap target.
 *
 * The rows in this app are hand-built (a title, a figure, a hairline) rather than
 * `ListItem`s, so tap handling is hand-built too — and a `pointerInput` alone gives
 * TalkBack nothing to press and no role to announce. This is the same idea as a
 * clickable list item: one action, one role, no ripple, because nothing in this
 * book ripples.
 */
@Composable
internal fun Modifier.rowTap(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = source,
        indication = null,
        role = Role.Button,
        onClick = onClick,
    )
}
