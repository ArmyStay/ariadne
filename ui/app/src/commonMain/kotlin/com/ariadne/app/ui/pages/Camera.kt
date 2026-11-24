package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.ariadne.design.Bone
import com.ariadne.design.Paper

/*
 * The viewfinder, and the front camera.
 *
 * No target owns a camera in shared code: Android has CameraX, iOS has
 * AVCaptureSession, the browser has getUserMedia, and the desktop has nothing at
 * all. So the page asks composition for a [CameraBinding] through a local and
 * draws whatever it gets back. When the local is empty — every target today —
 * the ruled plate is drawn instead, the shutter still fires, and the frame is
 * still stored. The page never branches on the target; it branches on whether it
 * was handed a bitmap.
 */

/**
 * What a platform hands over to give the app a camera.
 *
 * One method: draw the preview and call [onFrame] with the bytes of a still. A
 * target implements it with its own camera stack and supplies it at the root; the
 * shared UI never learns which one it got.
 */
public interface CameraBinding {
    public val available: Boolean

    @Composable
    public fun Preview(paper: Paper, modifier: Modifier, onFrame: (ByteArray) -> Unit)
}

/** Supplied at the root. Empty by default, which is the honest state today. */
public val LocalCamera: androidx.compose.runtime.ProvidableCompositionLocal<CameraBinding?> =
    androidx.compose.runtime.staticCompositionLocalOf { null }

private object NoCamera : CameraBinding {
    override val available: Boolean = false

    @Composable
    override fun Preview(paper: Paper, modifier: Modifier, onFrame: (ByteArray) -> Unit) {
        ViewfinderPlate(paper, modifier)
    }
}

/**
 * A live camera preview, drawn behind whatever the caller puts on top.
 *
 * Returns null when the binding has no frame to give, which makes the caller fall
 * back to the plate. The binding owns the camera's lifetime: it starts on entry
 * and releases on leave, because a camera held open behind a page is a battery
 * bug and, on iOS, a permission prompt the user cannot dismiss.
 */
@Composable
public fun rememberCameraFrame(
    paper: Paper,
    modifier: Modifier = Modifier,
    onFrame: (ByteArray) -> Unit,
): ImageBitmap? {
    val camera = LocalCamera.current ?: NoCamera
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(camera) { frame = null }
    camera.Preview(paper, modifier, onFrame)
    return frame
}

/** True when the current composition was handed a working camera. */
@Composable
public fun cameraAvailable(): Boolean = (LocalCamera.current ?: NoCamera).available

/**
 * The plate, drawn the way a photographic plate is: ruled lines, crop marks at
 * the trim, one register cross in the middle. This is both the no-camera
 * fallback and the frame the live preview is inset into, so the viewfinder looks
 * the same on every target.
 */
@Composable
public fun ViewfinderPlate(paper: Paper, modifier: Modifier = Modifier, marks: Boolean = true) {
    Canvas(modifier) {
        val step = 26f
        var y = step
        while (y < size.height) {
            drawLine(
                color = paper.edge.copy(alpha = 0.45f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5f,
            )
            y += step
        }
        if (!marks) return@Canvas
        val inset = size.minDimension * 0.07f
        val arm = size.minDimension * 0.05f
        val ink = paper.edgeStrong
        fun mark(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(ink, Offset(x, y), Offset(x + dx, y), 1f)
            drawLine(ink, Offset(x, y), Offset(x, y + dy), 1f)
        }
        mark(inset, inset, arm, arm)
        mark(size.width - inset, inset, -arm, arm)
        mark(inset, size.height - inset, arm, -arm)
        mark(size.width - inset, size.height - inset, -arm, -arm)

        val cx = size.width / 2
        val cy = size.height / 2
        drawLine(ink, Offset(cx - 9f, cy), Offset(cx + 9f, cy), 1f)
        drawLine(ink, Offset(cx, cy - 9f), Offset(cx, cy + 9f), 1f)
    }
}

/**
 * The frame from the camera, or the plate when there is none.
 *
 * Kept in one place so the shutter and the post-shutter page show the same
 * object: the photograph you are about to take and the photograph you just took
 * are framed identically, which is what makes the second one feel like a result
 * rather than a screen.
 */
@Composable
public fun CameraPlate(
    paper: Paper,
    modifier: Modifier = Modifier,
    live: Boolean,
    onFrame: (ByteArray) -> Unit,
) {
    Box(
        modifier.border(Bone.Hairline, paper.edge),
        contentAlignment = Alignment.Center,
    ) {
        if (live) {
            val frame = rememberCameraFrame(paper, Modifier.fillMaxSize(), onFrame)
            if (frame != null) {
                androidx.compose.foundation.Image(
                    bitmap = frame,
                    contentDescription = "Camera viewfinder",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            ViewfinderPlate(paper, Modifier.fillMaxSize())
        }
    }
}
