package com.ariadne.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The shape of "there is nothing here yet".
 *
 * Every page that can be empty used to say so its own way: a bare sentence here, an
 * 18sp serif line there, an empty stretch of paper somewhere else. The words were
 * fine and the shape was not — an empty screen is the first screen a person sees on
 * a new trip, and it should look deliberate rather than unfinished.
 *
 * So: a small title that names the situation, then one sentence that says what to do
 * about it, then optionally the one control that acts. Never a illustration, never a
 * shrug, and never a centred panel — this is a page in a book, and a page with
 * nothing on it still has a margin.
 */
@Composable
public fun EmptyState(
    title: String,
    body: String,
    paper: Paper,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Title(title, paper, size = 21.sp, italic = true, color = paper.ink)
        Spacer(Modifier.height(8.dp))
        Body(
            body,
            paper,
            serif = true,
            color = paper.inkSoft,
            size = 14.sp,
            lineHeight = 21.sp,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
