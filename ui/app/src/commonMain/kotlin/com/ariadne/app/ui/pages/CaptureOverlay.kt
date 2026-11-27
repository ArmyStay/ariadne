package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import com.ariadne.design.Bone
import com.ariadne.design.Body
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title

/**
 * The camera, open over the page you were on.
 *
 * This is the change that makes the shutter feel like a shutter. Before, tapping it
 * navigated to a viewfinder *page*, and the photograph arrived on a second page after
 * that — three screens to keep one receipt, two of which were the same nothing, and
 * the screen you actually wanted back had to be rebuilt when you returned.
 *
 * Now nothing navigates. The camera rises over Today (or Days, or the day's record),
 * the frame becomes the sheet over that same page, and putting the amount in drops
 * you back exactly where you stood: same tab, same day, same scroll, with the day's
 * figure already moved.
 *
 * It is a full cover rather than a translucent panel on purpose: a camera is a mode.
 * What it is *not* is a route, and the difference is felt in three places — the bar
 * stays where it is under the cover, Back closes the camera instead of leaving the
 * app, and closing the camera costs nothing because nothing was left.
 */
@Composable
public fun CaptureOverlay(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    onFrame: (ByteArray) -> Unit,
) {
    val shoot = shutterAction()
    // Without a sensor the shutter still has to produce a record: a ruled plate is a
    // legitimate photograph in this app, and a shutter that does nothing is not.
    val fire: () -> Unit = if (cameraAvailable()) ({ shoot() }) else ({ model.capturePlaceholder() })

    // The library, on the same overlay: a receipt you already have is the same
    // intent as one in front of you.
    val import = rememberPhotoImportMany(
        onBytes = { model.importPhotos(it) },
        onUnavailable = {},
        onFailure = model::photoImportFailed,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            QuietAction("Close", paper, { model.closeShutter() }, color = paper.inkSoft)
            Spacer(Modifier.weight(1f))
            Label(
                state.trip?.name ?: "Ariadne",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.2.sp,
            )
        }

        Spacer(Modifier.height(14.dp))
        Rule(paper)

        // Let the camera preview use the available space instead of nesting a
        // square frame around the camera's own preview.
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .then(if (wide) Modifier.width(760.dp) else Modifier.fillMaxWidth())
                .weight(1f)
                .semantics { contentDescription = "Viewfinder. Tap to keep the photograph." }
                .pointerInput(fire) { detectTapGestures(onTap = { fire() }) },
        ) {
            CameraPlate(paper, Modifier.fillMaxSize(), live = true, onFrame = onFrame)
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Label(
                if (cameraAvailable()) {
                    "Tap the frame to keep it"
                } else {
                    "No camera here — tap the frame for a plate"
                },
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.2.sp,
            )
        }

        val wearable = LocalWearableCapture.current
        if (wearable != null && wearable.available) {
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Glyph.Camera, paper, size = 11.dp, color = paper.inkFaint)
                Spacer(Modifier.width(6.dp))
                QuietAction(
                    if (wearable.connected) "AI Glasses connected" else "Connect AI Glasses",
                    paper,
                    { if (wearable.connected) wearable.disconnect() else wearable.connect() },
                    color = paper.inkFaint,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (pickerAvailable()) {
                    QuietAction("From the library", paper, import)
                }
            }
            Shutter(paper, fire)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                QuietAction(
                    if (state.tripPending.isEmpty()) {
                        "nothing waiting across this trip"
                    } else {
                        "${state.tripPending.size} waiting across this trip"
                    },
                    paper,
                    { model.closeShutter(); model.openCloseDay(state.today) },
                    color = if (state.tripSettled) paper.inkFaint else paper.inkMid,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Rule(paper)
        Spacer(Modifier.height(10.dp))
        state.captureError?.let {
            Body(it, paper, color = paper.inkMid, size = 12.sp)
            Spacer(Modifier.height(8.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Budget, paper, size = 12.dp, color = paper.inkFaint)
            Spacer(Modifier.width(7.dp))
            Label(
                "no figure here · the amount comes after",
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.3.sp,
            )
        }
    }
}

/**
 * The empty case: a camera with no trip to file into.
 *
 * Kept as a real screen rather than an overlay, because there is nothing to overlay:
 * the person has not started a journey, and the honest answer is a page that says so
 * with the two ways forward.
 */
@Composable
public fun NoTripForCamera(model: AppModel, paper: Paper, closed: Boolean) {
    Column(Modifier.fillMaxSize().background(paper.bg).padding(Bone.Margin)) {
        Spacer(Modifier.height(30.dp))
        Title(
            if (closed) "This trip is closed." else "No trip is open.",
            paper,
            size = 30.sp,
            italic = true,
        )
        Spacer(Modifier.height(12.dp))
        Label(
            if (closed) {
                "Its photographs are still here, and its card is still yours."
            } else {
                "A photograph needs a journey to belong to."
            },
            paper,
            color = paper.inkSoft,
            size = 11.sp,
            tracking = 0.4.sp,
        )
        Spacer(Modifier.height(20.dp))
        QuietAction("the register", paper, { model.go(com.ariadne.app.Route.Trips) }, color = paper.inkMid)
    }
}
