package com.ariadne.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * A press that a screen reader can make as well.
 *
 * `pointerInput { detectTapGestures { … } }` gives a node that TalkBack can focus and
 * read out but cannot activate: the gesture lives outside the semantics tree, so the
 * one thing the control does is the one thing assistive technology cannot do. This is
 * the same press, wired through `clickable`, so the node carries a click action, a
 * role, and the words for what the action will perform — with no indication, because
 * nothing in this book ripples.
 *
 * Pass the words the tap performs, not its name: "Delete this photograph" is an
 * instruction a person can follow without seeing the icon; "Tap" is not.
 */
@Composable
internal fun Modifier.tap(
    label: String,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = label,
        onClick = onClick,
    )
}
