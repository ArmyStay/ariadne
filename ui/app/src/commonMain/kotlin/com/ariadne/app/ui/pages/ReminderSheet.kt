package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Label
import com.ariadne.design.OutlineAction
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Title

/**
 * The evening reminder, asked for once and adjustable for ever.
 *
 * One popup, one question, two buttons — and the same panel from Preferences when it is
 * a setting rather than an offer. It is a sheet over whatever page is showing, not a
 * route: it is a question about the app, not a place in it.
 *
 * The copy is careful about the three things a reminder can honestly be: an alarm the
 * phone keeps while the app is closed (Android), a line inside the app while it is open
 * (every target), and nothing at all (a build with no notification service). The note
 * under the clock says which one this is, so nobody arms a reminder that cannot arrive.
 */
@Composable
public fun ReminderOverlay(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
) {
    if (!state.reminderSheetOpen) return
    Box(
        Modifier
            .fillMaxSize()
            .background(paper.ink.copy(alpha = 0.34f))
            // Tapping the paper behind closes the question. Tapping the panel does not.
            .pointerInput(Unit) { detectTapGestures { model.closeReminderSheet() } }
            .semantics { contentDescription = "The evening reminder" },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = if (wide) 0.dp else 22.dp)
                .background(paper.raised)
                .border(1.dp, paper.edgeStrong)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 26.dp, vertical = 26.dp),
        ) {
            Label(
                if (state.reminderIsOffer) "once, and asked once" else "the evening",
                paper,
                color = paper.blue,
                size = 9.sp,
                tracking = 1.8.sp,
            )
            Spacer(Modifier.height(12.dp))
            Title(
                if (state.reminderIsOffer) "One line in the evening?" else "The evening reminder",
                paper,
                size = if (wide) 34.sp else 27.sp,
                italic = true,
            )
            Spacer(Modifier.height(14.dp))
            Body(
                "Once a day, at the time below, Ariadne says how many photographs are still " +
                    "waiting for a figure — and nothing else. It is set on this phone: no " +
                    "account, no server, nothing sent to you from anywhere.",
                paper,
                serif = true,
                color = paper.inkMid,
                size = 16.sp,
                lineHeight = 25.sp,
            )

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                QuietAction(
                    "earlier",
                    paper,
                    { model.setReminderHour(state.reminder.hour - 1) },
                    color = paper.inkSoft,
                )
                Title(
                    state.reminder.clock,
                    paper,
                    size = if (wide) 40.sp else 34.sp,
                )
                QuietAction(
                    "later",
                    paper,
                    { model.setReminderHour(state.reminder.hour + 1) },
                    color = paper.inkSoft,
                )
            }
            Spacer(Modifier.height(6.dp))
            Label(
                "every day ${state.reminder.spoken}",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.4.sp,
            )

            val note = state.reminderNote
            if (note != null) {
                Spacer(Modifier.height(16.dp))
                Body(note, paper, color = paper.inkSoft, size = 13.sp, lineHeight = 19.sp)
            }

            Spacer(Modifier.height(22.dp))
            Action("Set the reminder", paper, { model.enableReminder() }, accent = true)
            Spacer(Modifier.height(10.dp))
            if (state.reminder.enabled) {
                OutlineAction("Turn it off", paper, { model.disableReminder() })
            } else {
                QuietAction(
                    if (state.reminderIsOffer) "Not for me" else "Not now",
                    paper,
                    { model.closeReminderSheet() },
                    color = paper.inkSoft,
                )
            }
        }
    }
}
