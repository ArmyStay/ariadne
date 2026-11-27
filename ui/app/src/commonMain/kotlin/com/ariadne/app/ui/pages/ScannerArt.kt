package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ariadne.design.Paper

/**
 * The page's weather.
 *
 * The reference for the shutter is a travel scanner: white paper, a wash of blue
 * where the light falls, two thin arcs of a route, a plane, and a frame drawn like a
 * plate in a book. The wash in that reference is a photograph of watercolour, and a
 * photograph cannot be responsive — it is one crop, and the other orientation gets
 * whatever falls outside it.
 *
 * So the weather is drawn, not pasted. Every mark below is placed in fractions of the
 * canvas, so the same page composes itself into a tall phone, a wide window, or a
 * tablet: the fields move to the corners that exist, the arcs keep their sweep, and
 * the tides stay at the foot. Nothing is cropped, and nothing is a bitmap the APK has
 * to carry.
 */
@Composable
internal fun ScannerBackdrop(paper: Paper, modifier: Modifier = Modifier) {
    val wash = paper.blueWash
    val edge = paper.blueEdge
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val w = size.width
        val h = size.height

        // Where the light falls: three soft fields of wash that fade to nothing. At a
        // fifth of the strength of the timeline's globe, because this page must never
        // compete with the frame.
        softField(Offset(w * 0.88f, h * 0.04f), w * 0.80f, wash, 1f)
        softField(Offset(w * 0.00f, h * 0.34f), w * 0.60f, wash, 0.9f)
        softField(Offset(w * 0.98f, h * 0.58f), w * 0.55f, wash, 0.8f)
        softField(Offset(w * 0.50f, h * 1.02f), w * 0.95f, edge, 0.18f)

        // Two tides at the foot: a coast, not a gradient.
        tide(h * 0.88f, h * 0.045f, edge, 0.15f)
        tide(h * 0.96f, h * 0.030f, wash, 1f)

        // The route out: one dashed arc and one faint one, off the frame's shoulder.
        arc(w * 0.62f, h * 0.16f, w * 0.95f, edge.copy(alpha = 0.50f), dashed = true)
        arc(w * 0.42f, h * 0.10f, w * 1.30f, edge.copy(alpha = 0.26f), dashed = false)

        globe(Offset(w * 0.94f, h * 0.72f), w * 0.20f, edge.copy(alpha = 0.20f))
        plane(Offset(w * 0.70f, h * 0.085f), w * 0.060f, edge.copy(alpha = 0.70f))
    }
}

/**
 * The four marks that make a rectangle a plate rather than a video.
 *
 * Drawn over the live preview, at the corners, at a fixed 20dp arm: crop marks in a
 * book are the same size whatever the picture is.
 */
@Composable
internal fun PlateMarks(
    color: Color,
    modifier: Modifier = Modifier,
    inset: Dp = 12.dp,
    arm: Dp = 20.dp,
    thickness: Dp = 2.dp,
) {
    Canvas(modifier) {
        val i = inset.toPx()
        val a = arm.toPx()
        val sw = thickness.toPx()
        val right = size.width - i
        val bottom = size.height - i
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + dx, y), sw)
            drawLine(color, Offset(x, y), Offset(x, y + dy), sw)
        }
        corner(i, i, a, a)
        corner(right, i, -a, a)
        corner(i, bottom, a, -a)
        corner(right, bottom, -a, -a)
    }
}

/** A pair of glasses: two rings, a bridge, two temples. */
@Composable
internal fun GlassesMark(color: Color, extent: Dp = 15.dp) {
    Canvas(Modifier.size(extent)) {
        val w = size.width
        val h = size.height
        val r = h * 0.30f
        val cy = h * 0.58f
        val left = Offset(r * 1.20f, cy)
        val right = Offset(w - r * 1.20f, cy)
        val sw = 1.3.dp.toPx()
        drawCircle(color, r, left, style = Stroke(sw))
        drawCircle(color, r, right, style = Stroke(sw))
        drawLine(color, Offset(left.x + r, cy), Offset(right.x - r, cy), sw)
        drawLine(color, Offset(left.x - r * 0.90f, cy - r * 0.10f), Offset(0f, cy - r * 0.95f), sw)
        drawLine(color, Offset(right.x + r * 0.90f, cy - r * 0.10f), Offset(w, cy - r * 0.95f), sw)
    }
}

// ── the marks ──────────────────────────────────────────────────────────────────

private fun DrawScope.softField(centre: Offset, radius: Float, tint: Color, strength: Float) {
    if (radius <= 0f || strength <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(tint.copy(alpha = 0.85f * strength), tint.copy(alpha = 0f)),
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
}

private fun DrawScope.tide(baseY: Float, amplitude: Float, tint: Color, strength: Float) {
    val path = Path().apply {
        moveTo(0f, baseY)
        cubicTo(
            size.width * 0.28f, baseY - amplitude,
            size.width * 0.52f, baseY + amplitude,
            size.width, baseY - amplitude * 0.60f,
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(tint.copy(alpha = 0f), tint.copy(alpha = 0.90f * strength)),
            startY = baseY - amplitude,
            endY = size.height,
        ),
    )
}

private fun DrawScope.arc(cx: Float, cy: Float, radius: Float, color: Color, dashed: Boolean) {
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = if (dashed) {
                PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 7.dp.toPx()))
            } else {
                null
            },
        ),
    )
}

private fun DrawScope.globe(centre: Offset, radius: Float, color: Color) {
    val sw = 1.dp.toPx()
    drawCircle(color, radius, centre, style = Stroke(sw))
    drawOval(
        color = color,
        topLeft = Offset(centre.x - radius, centre.y - radius * 0.52f),
        size = Size(radius * 2f, radius * 1.04f),
        style = Stroke(sw),
    )
    drawOval(
        color = color,
        topLeft = Offset(centre.x - radius * 0.44f, centre.y - radius),
        size = Size(radius * 0.88f, radius * 2f),
        style = Stroke(sw),
    )
}

/** A paper plane: one swept body, one fold behind it. */
private fun DrawScope.plane(centre: Offset, span: Float, color: Color) {
    val body = Path().apply {
        moveTo(centre.x - span * 0.50f, centre.y + span * 0.22f)
        lineTo(centre.x + span * 0.50f, centre.y - span * 0.26f)
        lineTo(centre.x + span * 0.02f, centre.y + span * 0.04f)
        close()
    }
    val fold = Path().apply {
        moveTo(centre.x + span * 0.02f, centre.y + span * 0.04f)
        lineTo(centre.x + span * 0.50f, centre.y - span * 0.26f)
        lineTo(centre.x - span * 0.04f, centre.y + span * 0.40f)
        close()
    }
    drawPath(body, color)
    drawPath(fold, color.copy(alpha = color.alpha * 0.55f))
}
