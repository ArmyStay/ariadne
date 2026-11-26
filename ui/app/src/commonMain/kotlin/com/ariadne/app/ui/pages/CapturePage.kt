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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.OutlineAction
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule

/**
 * Page 4 — Capture.
 *
 * One frame, one control, no fields. The photograph is inset with a wide margin
 * so the page reads as a plate in a book rather than a camera app: everything
 * around it is quiet paper, and the only ink is the shutter.
 */
@Composable
public fun CapturePage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    onShutter: () -> Unit,
    onImport: () -> Unit = {},
    onFrame: (ByteArray) -> Unit = {},
) {
    val shoot = shutterAction()
    val closedTrip = state.trip?.takeIf { it.isClosed }

    // The picker is shared with the front page; see rememberPhotoImport. A target
    // with no picker falls through to the host's fallback rather than presenting
    // a control that silently does nothing.
    val import = rememberPhotoImport(
        onBytes = { model.importPhoto(it) },
        onUnavailable = onImport,
    )
    // With a camera, the shutter fires the sensor and the bytes come back through
    // onFrame. Without one, the caller's placeholder path runs instead.
    val fire: () -> Unit = if (cameraAvailable()) ({ shoot() }) else onShutter
    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 22.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietAction("Home", paper, { model.go(Route.Home) })
            Label(state.trip?.name ?: "Ariadne", paper, color = paper.inkFaint)
            QuietAction("Today", paper, { model.openNightfall(state.today) })
        }

        Spacer(Modifier.height(20.dp))
        Rule(paper)

        // A closed trip cannot receive photographs, and neither can nothing.
        // Say so, and give the ways out, rather than showing a shutter that would
        // silently do nothing.
        if (state.trip == null) {
            Spacer(Modifier.weight(1f))
            Body(
                "No trip is open. A photograph needs a journey to belong to.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 18.sp,
            )
            Spacer(Modifier.height(24.dp))
            OutlineAction("The register", paper, { model.go(Route.Trips) })
            Spacer(Modifier.weight(1f))
            return@Column
        }
        if (closedTrip != null) {
            Spacer(Modifier.weight(1f))
            Body(
                "This trip is closed. Its photographs are still here, and its card is still yours.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 18.sp,
            )
            Spacer(Modifier.height(24.dp))
            Action("See the card", paper, { model.go(Route.TripCard(closedTrip.id)) }, accent = true)
            Spacer(Modifier.height(12.dp))
            OutlineAction("The register", paper, { model.go(Route.Trips) })
            Spacer(Modifier.weight(1f))
            return@Column
        }

        Spacer(Modifier.weight(0.6f))

        // The plate. Square, inset, hairline-framed; the frame is the point.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = "Viewfinder. Tap to keep the photograph." }
                .pointerInput(fire) { detectTapGestures(onTap = { fire() }) },
        ) {
            // The live frame if the target has a camera, the ruled plate if not.
            // Same frame either way, so the shutter never lies about what it saw.
            CameraPlate(paper, Modifier.fillMaxSize(), live = true, onFrame = onFrame)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(
                        if (cameraAvailable()) "Tap anywhere to keep" else "No camera here — tap to keep a plate",
                        paper,
                        color = paper.inkFaint,
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

        Spacer(Modifier.weight(0.5f))

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Offered only where a picker exists: the shutter is one way in, the
            // library is the other, and a control that cannot work is not offered.
            // The shutter stays centred either way, because it is the page.
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (pickerAvailable()) {
                    QuietAction("From the library", paper, import)
                }
            }
            Shutter(paper, fire)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                QuietAction(
                    if (state.stack.isEmpty()) "Nothing waiting" else "${state.stack.size} waiting",
                    paper,
                    { model.openNightfall(state.today) },
                    color = if (state.stack.isEmpty()) paper.inkFaint else paper.inkMid,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Rule(paper)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Budget, paper, size = 12.dp, color = paper.inkFaint)
            Spacer(Modifier.width(7.dp))
            Label("no figure here · settle it at nightfall", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.3.sp)
        }
    }
}

/**
 * The shutter: an ink dot with a hairline ring. Nothing about it is a button in
 * the Material sense, which is exactly why it reads as a physical control.
 */
@Composable
private fun Shutter(paper: Paper, onClick: () -> Unit) {
    Box(
        Modifier
            .size(66.dp)
            .semantics { contentDescription = "Shutter" }
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(66.dp).border(Bone.Hairline, paper.edge, CircleShape))
        Box(Modifier.size(46.dp).background(paper.ink, CircleShape))
    }
}

/**
 * Ruled field: what a blank photographic plate looks like in this language.
 *
 * Crop marks in the four corners and one register cross in the middle. It reads
 * as a plate waiting for an image rather than as an empty box — which matters,
 * because on a device with no camera bound the frame is exactly what it is.
 */
@Composable
private fun PlateField(paper: Paper) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val step = 26f
        var y = step
        while (y < size.height) {
            drawLine(
                color = paper.edge.copy(alpha = 0.45f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 0.5f,
            )
            y += step
        }

        // Crop marks: 10% in from each corner, the way a plate is trimmed.
        val inset = size.minDimension * 0.08f
        val arm = size.minDimension * 0.05f
        val ink = paper.edge
        fun mark(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(ink, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x + dx, y), 1f)
            drawLine(ink, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x, y + dy), 1f)
        }
        mark(inset, inset, arm, arm)
        mark(size.width - inset, inset, -arm, arm)
        mark(inset, size.height - inset, arm, -arm)
        mark(size.width - inset, size.height - inset, -arm, -arm)

        val cx = size.width / 2
        val cy = size.height / 2
        drawLine(ink, androidx.compose.ui.geometry.Offset(cx - 8f, cy), androidx.compose.ui.geometry.Offset(cx + 8f, cy), 1f)
        drawLine(ink, androidx.compose.ui.geometry.Offset(cx, cy - 8f), androidx.compose.ui.geometry.Offset(cx, cy + 8f), 1f)
    }
}

/** Used by other pages where a photograph would sit. */
@Composable
internal fun PhotographPlaceholder(paper: Paper, caption: String, modifier: Modifier = Modifier) {
    Box(modifier.border(Bone.Hairline, paper.edge)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val step = 22f
            var y = step
            while (y < size.height) {
                drawLine(
                    color = paper.edge.copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 0.5f,
                )
                y += step
            }
        }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) {
            Label(caption, paper, color = paper.inkFaint)
        }
    }
}
