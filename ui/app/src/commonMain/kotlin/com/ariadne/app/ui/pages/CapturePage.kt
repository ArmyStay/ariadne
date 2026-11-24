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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title

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
    onImport: () -> Unit,
    onFrame: (ByteArray) -> Unit = {},
) {
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

        Spacer(Modifier.weight(0.6f))

        // The plate. Square, inset, hairline-framed; the frame is the point.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = "Viewfinder. Tap to keep the photograph." }
                .pointerInput(Unit) { detectTapGestures(onTap = { onShutter() }) },
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
            QuietAction("From photos", paper, onImport)
            Shutter(paper, onShutter)
            QuietAction(
                if (state.stack.isEmpty()) "Nothing waiting" else "${state.stack.size} waiting",
                paper,
                { model.openNightfall(state.today) },
                color = if (state.stack.isEmpty()) paper.inkFaint else paper.inkMid,
            )
        }

        Spacer(Modifier.height(6.dp))
        Rule(paper)
        Spacer(Modifier.height(12.dp))
        Body(
            "No amount. No category. The photograph is the record until nightfall, when two taps finish it.",
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            align = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
