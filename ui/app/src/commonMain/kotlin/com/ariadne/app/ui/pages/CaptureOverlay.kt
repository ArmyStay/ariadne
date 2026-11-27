package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 *
 * The page itself is a plate in a book: paper with a wash of blue and a route drawn
 * across it ([ScannerBackdrop]), a dark frame with crop marks at its corners
 * ([PlateMarks]), the hint beneath it between two hairlines, the glasses on one line,
 * and the shutter in the middle of the library and the day's open figure. On a wide
 * window the frame takes the left and the controls stack on the right, so nothing is
 * cropped and nothing is squeezed — the same page, composed twice.
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

    // The glasses, when this device has them. Offered in the reference's place —
    // under the hint, above the shutter — and only where the surface exists.
    val wearable = LocalWearableCapture.current
    val glasses: (() -> Unit)? = if (wearable != null && wearable.available) {
        if (wearable.connected) ({ wearable.disconnect() }) else ({ wearable.connect() })
    } else {
        null
    }

    val hint = if (cameraAvailable()) {
        "Tap the frame to keep it"
    } else {
        "No camera here — tap the frame for a plate"
    }
    val waiting = if (state.tripPending.isEmpty()) {
        "nothing waiting across this trip"
    } else {
        "${state.tripPending.size} waiting across this trip"
    }
    val onWaiting: () -> Unit = { model.closeShutter(); model.openCloseDay(state.today) }

    // The arrangement follows the window, not a flag: `MainActivity` reports `wide =
    // false` for every phone, so a phone turned on its side would otherwise get the
    // tall layout in 390dp of height and a frame two fingers wide. Anything wider than
    // it is tall takes the two-column page — the frame on the left, the controls on the
    // right — which is also what the wash and the arcs were drawn for.
    BoxWithConstraints(Modifier.fillMaxSize().background(paper.bg)) {
        val across = wide || maxWidth > maxHeight
        ScannerBackdrop(paper, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide) 56.dp else 22.dp, vertical = 16.dp),
        ) {
            OverlayHead(state.trip?.name ?: "Ariadne", paper, { model.closeShutter() })
            Spacer(Modifier.height(12.dp))

            if (across) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FrameBox(paper, Modifier.weight(1.25f).fillMaxHeight(), fire, onFrame)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        HintLine(hint, paper)
                        Spacer(Modifier.height(22.dp))
                        if (glasses != null) {
                            GlassesPill(paper, wearable?.connected == true, glasses)
                            Spacer(Modifier.height(24.dp))
                        }
                        ShutterRow(
                            paper = paper,
                            fire = fire,
                            onImport = import,
                            waiting = waiting,
                            waitingQuiet = state.tripSettled,
                            onWaiting = onWaiting,
                        )
                        Spacer(Modifier.height(18.dp))
                        FigureNote(paper, state.captureError)
                    }
                }
            } else {
                FrameBox(paper, Modifier.fillMaxWidth().weight(1f), fire, onFrame)
                Spacer(Modifier.height(16.dp))
                HintLine(hint, paper)
                Spacer(Modifier.height(16.dp))
                if (glasses != null) {
                    GlassesPill(paper, wearable?.connected == true, glasses)
                    Spacer(Modifier.height(20.dp))
                }
                ShutterRow(
                    paper = paper,
                    fire = fire,
                    onImport = import,
                    waiting = waiting,
                    waitingQuiet = state.tripSettled,
                    onWaiting = onWaiting,
                )
                Spacer(Modifier.height(14.dp))
                FigureNote(paper, state.captureError)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/**
 * The head of the overlay: the way out on the left, the journey this photograph
 * belongs to on the right, both in tracked capitals.
 *
 * Closing is the same gesture as `Back`: `closeShutter()` puts the camera away and
 * leaves you on the page underneath, with nothing to undo.
 */
@Composable
private fun OverlayHead(tripName: String, paper: Paper, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QuietAction("Close", paper, onClose, color = paper.ink)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Label(tripName, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.8.sp)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(44.dp).height(1.dp).background(paper.blueEdge))
        }
    }
}

/**
 * The frame: the camera's own picture, dark by design, with crop marks over it.
 *
 * The ground is ink rather than blank paper so the frame is a photograph the moment
 * the page opens — before the sensor has delivered its first frame, and on a device
 * with no sensor at all, where the shutter keeps a ruled plate instead.
 */
@Composable
private fun FrameBox(
    paper: Paper,
    modifier: Modifier,
    onFire: () -> Unit,
    onFrame: (ByteArray) -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(paper.ink)
            .semantics { contentDescription = "Viewfinder. Tap to keep the photograph." }
            .rowTap(onFire),
    ) {
        CameraPlate(paper, Modifier.fillMaxSize(), live = true, onFrame = onFrame)
        PlateMarks(paper.raised, Modifier.fillMaxSize())
    }
}

/** The hint, between two hairlines that fade out at the margins. */
@Composable
private fun HintLine(text: String, paper: Paper) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Hairline(paper, Modifier.weight(0.32f))
        Label(
            text,
            paper,
            color = paper.inkSoft,
            size = 9.sp,
            tracking = 1.4.sp,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Hairline(paper, Modifier.weight(0.32f))
    }
}

/** The glasses, as the reference draws them: a hairline capsule with the mark in it. */
@Composable
private fun GlassesPill(paper: Paper, connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(paper.raised)
            .border(Bone.Hairline, paper.blueEdge, CircleShape)
            .rowTap(onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassesMark(paper.ink, 16.dp)
        Spacer(Modifier.width(9.dp))
        Label(
            if (connected) "Disconnect AI glasses" else "Connect AI glasses",
            paper,
            color = paper.ink,
            size = 9.sp,
            tracking = 1.8.sp,
        )
    }
}

/**
 * The library, the shutter, and the day's open figure — the shutter in the middle,
 * where a thumb already is, and the two others as quiet capitals beside it.
 */
@Composable
private fun ShutterRow(
    paper: Paper,
    fire: () -> Unit,
    onImport: () -> Unit,
    waiting: String,
    waitingQuiet: Boolean,
    onWaiting: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Offered only where a picker exists: the shutter is one way in, the library
        // is the other, and a control that cannot work is not offered.
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (pickerAvailable()) {
                QuietAction("From the library", paper, onImport, color = paper.ink)
            }
        }
        Shutter(paper, fire)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            QuietAction(
                waiting,
                paper,
                onWaiting,
                color = if (waitingQuiet) paper.inkFaint else paper.inkMid,
            )
        }
    }
}

/** *"no figure here · the amount comes after"*, in the reference's pale capsule. */
@Composable
private fun FigureNote(paper: Paper, error: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        error?.let {
            Body(it, paper, color = paper.inkMid, size = 12.sp)
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier
                .clip(CircleShape)
                .background(paper.blueWash)
                .border(Bone.Hairline, paper.blueEdge, CircleShape)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Budget, paper, size = 12.dp, color = paper.inkSoft)
            Spacer(Modifier.width(8.dp))
            Label(
                "no figure here · the amount comes after",
                paper,
                color = paper.inkSoft,
                size = 9.sp,
                tracking = 1.3.sp,
            )
        }
    }
}

@Composable
private fun Hairline(paper: Paper, modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).background(paper.blueEdge.copy(alpha = 0.75f)))
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
