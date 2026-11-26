package com.ariadne.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The icon set.
 *
 * One weight of line, one 24-unit grid, drawn rather than imported. A Material
 * icon font would arrive with its own optical weight, its own rounded joins and
 * its own 24px padding assumptions, and those are the tells that make an app look
 * assembled from parts: the set would not match the type, and the type is the
 * loudest thing here. These are strokes at 1.4dp — the same weight as the tab
 * glyphs and the ruled hairlines — so an icon, a rule and a letter all look like
 * they came off the same plate.
 *
 * Every glyph is described on a 0..1 grid and scaled at draw time, so the same
 * definition works at 14dp beside a label and at 40dp as a page ornament.
 */
public enum class Glyph {
    /** The shutter: a ring with a disc inside it. */
    Shutter,
    /** A trip: a folder with a strap, or a suitcase seen from above. */
    Trip,
    /** A calendar page: a date, not a schedule. */
    Date,
    /** A pin: where you are. */
    Place,
    /** A fence: a wall of stacked coin lines — the budget. */
    Budget,
    /** A camera body with a lens. */
    Camera,
    /** A photograph: a frame with a horizon. */
    Photograph,
    /** Night: a crescent, for the ritual. */
    Night,
    /** A magnifier: search. */
    Search,
    /** A list of ruled lines: the record. */
    Record,
    /** Bars: what it came to. */
    Stats,
    /** A gear: preferences. */
    Gear,
    /** A dot with a tail: what is waiting. */
    Waiting,
    /** A cross in a square: remove. */
    Remove,
    /** An arrow leaving a box: share or export. */
    Share,
    /** A return arrow: back. */
    Back,
    /** A key-shaped arrow with a cross: remove the previous character. */
    Backspace,
    /** A check: settled. */
    Done,
    /** A disc, half filled: the light/dark choice. */
    Paper,
    /** An open book: the whole record. */
    Ledger,
    /** A pen nib: edit. */
    Edit,
    /** A thin forward angle: the whole of it is behind this row. */
    Chevron,
}

@Composable
public fun Icon(
    glyph: Glyph,
    paper: Paper,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color? = null,
) {
    val ink = color ?: paper.inkSoft
    Canvas(modifier.size(size)) {
        drawGlyph(glyph, ink, 1.4.dp.toPx() * (size.value / 18f).coerceIn(0.85f, 2.2f))
    }
}

/** The glyph plus a tracked-out label, which is how most of this app labels things. */
@Composable
public fun IconLabel(
    glyph: Glyph,
    text: String,
    paper: Paper,
    color: Color? = null,
    size: Dp = 15.dp,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(glyph, paper, size = size, color = color)
        Spacer(Modifier.size(8.dp))
        Label(text, paper, color = color ?: paper.inkSoft)
    }
}

private fun DrawScope.drawGlyph(glyph: Glyph, color: Color, stroke: Float) {
    val w = size.width
    val h = size.height
    val s = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Square)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, Offset(w * x1, h * y1), Offset(w * x2, h * y2), stroke)

    fun rect(x1: Float, y1: Float, x2: Float, y2: Float) {
        val p = Path().apply {
            moveTo(w * x1, h * y1); lineTo(w * x2, h * y1)
            lineTo(w * x2, h * y2); lineTo(w * x1, h * y2); close()
        }
        drawPath(p, color, style = s)
    }

    fun circle(cx: Float, cy: Float, r: Float) =
        drawCircle(color, radius = (w.coerceAtMost(h)) * r, center = Offset(w * cx, h * cy), style = s)

    fun path(close: Boolean = false, vararg pts: Float) {
        val p = Path()
        pts.toList().chunked(2).forEachIndexed { i, pair ->
            val x = w * pair[0]
            val y = h * pair[1]
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        if (close) p.close()
        drawPath(p, color, style = s)
    }

    when (glyph) {
        Glyph.Chevron -> {
            // Two strokes, no head and no tail: an arrow would claim a direction
            // of travel, where this claims only that a row opens.
            path(false, 0.42f, 0.24f, 0.66f, 0.50f, 0.42f, 0.76f)
        }

        Glyph.Shutter -> {
            // A ring with a disc in it: the shutter, drawn the way the control on
            // the capture page is drawn, so the button and the glyph agree.
            circle(0.5f, 0.5f, 0.44f)
            drawCircle(color, radius = (w.coerceAtMost(h)) * 0.22f, center = Offset(w * 0.5f, h * 0.5f))
        }
        Glyph.Trip -> {
            rect(0.14f, 0.34f, 0.86f, 0.82f)
            // The handle, drawn as a strap over the top edge.
            path(false, 0.36f, 0.34f, 0.36f, 0.20f, 0.64f, 0.20f, 0.64f, 0.34f)
            line(0.14f, 0.52f, 0.86f, 0.52f)
        }

        Glyph.Date -> {
            rect(0.14f, 0.24f, 0.86f, 0.86f)
            line(0.14f, 0.40f, 0.86f, 0.40f)
            line(0.32f, 0.14f, 0.32f, 0.30f)
            line(0.68f, 0.14f, 0.68f, 0.30f)
            // Three ruled days, so it reads as a month rather than a box.
            line(0.30f, 0.56f, 0.46f, 0.56f)
            line(0.54f, 0.56f, 0.70f, 0.56f)
            line(0.30f, 0.70f, 0.46f, 0.70f)
        }

        Glyph.Place -> {
            path(false, 0.50f, 0.88f, 0.24f, 0.52f, 0.24f, 0.38f)
            // A teardrop: circle over a point, drawn as two arcs' worth of path.
            path(true, 0.24f, 0.38f, 0.24f, 0.20f, 0.76f, 0.20f, 0.76f, 0.38f, 0.50f, 0.88f)
            circle(0.50f, 0.38f, 0.09f)
        }

        Glyph.Budget -> {
            // A stack of coins, drawn as three ellipses so it cannot be mistaken for
            // a menu at 16dp.
            drawOval(
                color,
                topLeft = Offset(w * 0.22f, h * 0.22f),
                size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.18f),
                style = s,
            )
            drawOval(
                color,
                topLeft = Offset(w * 0.22f, h * 0.44f),
                size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.18f),
                style = s,
            )
            drawOval(
                color,
                topLeft = Offset(w * 0.22f, h * 0.66f),
                size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.18f),
                style = s,
            )
        }

        Glyph.Camera -> {
            rect(0.10f, 0.32f, 0.90f, 0.82f)
            // The viewfinder hump.
            path(false, 0.36f, 0.32f, 0.40f, 0.20f, 0.60f, 0.20f, 0.64f, 0.32f)
            circle(0.50f, 0.57f, 0.15f)
        }

        Glyph.Photograph -> {
            rect(0.12f, 0.22f, 0.88f, 0.78f)
            // A horizon and a hill: enough landscape to read as an image.
            path(false, 0.12f, 0.66f, 0.36f, 0.46f, 0.52f, 0.62f, 0.66f, 0.52f, 0.88f, 0.70f)
            line(0.30f, 0.34f, 0.30f, 0.34f)
        }

        Glyph.Night -> {
            // Crescent, as one path so the join is continuous.
            path(false, 0.62f, 0.14f, 0.40f, 0.24f, 0.30f, 0.46f, 0.36f, 0.66f, 0.54f, 0.80f, 0.72f, 0.78f)
            path(false, 0.72f, 0.78f, 0.60f, 0.62f, 0.58f, 0.40f, 0.68f, 0.24f, 0.62f, 0.14f)
        }

        Glyph.Search -> {
            circle(0.44f, 0.44f, 0.26f)
            line(0.63f, 0.63f, 0.86f, 0.86f)
        }

        Glyph.Record -> {
            line(0.20f, 0.24f, 0.80f, 0.24f)
            line(0.20f, 0.44f, 0.80f, 0.44f)
            line(0.20f, 0.64f, 0.62f, 0.64f)
            line(0.20f, 0.84f, 0.48f, 0.84f)
        }

        Glyph.Stats -> {
            line(0.16f, 0.86f, 0.86f, 0.86f)
            line(0.28f, 0.86f, 0.28f, 0.56f)
            line(0.50f, 0.86f, 0.50f, 0.26f)
            line(0.72f, 0.86f, 0.72f, 0.44f)
        }

        Glyph.Gear -> {
            circle(0.50f, 0.50f, 0.18f)
            circle(0.50f, 0.50f, 0.34f)
            line(0.50f, 0.10f, 0.50f, 0.18f)
            line(0.50f, 0.82f, 0.50f, 0.90f)
            line(0.10f, 0.50f, 0.18f, 0.50f)
            line(0.82f, 0.50f, 0.90f, 0.50f)
        }

        Glyph.Waiting -> {
            circle(0.44f, 0.46f, 0.28f)
            line(0.44f, 0.32f, 0.44f, 0.46f)
            line(0.44f, 0.46f, 0.54f, 0.52f)
            line(0.74f, 0.76f, 0.86f, 0.88f)
        }

        Glyph.Remove -> {
            rect(0.14f, 0.14f, 0.86f, 0.86f)
            line(0.34f, 0.34f, 0.66f, 0.66f)
            line(0.66f, 0.34f, 0.34f, 0.66f)
        }

        Glyph.Share -> {
            rect(0.18f, 0.42f, 0.82f, 0.86f)
            line(0.50f, 0.62f, 0.50f, 0.16f)
            path(false, 0.36f, 0.30f, 0.50f, 0.16f, 0.64f, 0.30f)
        }

        Glyph.Back -> {
            line(0.80f, 0.50f, 0.22f, 0.50f)
            path(false, 0.42f, 0.30f, 0.22f, 0.50f, 0.42f, 0.70f)
        }

        Glyph.Backspace -> {
            path(true, 0.88f, 0.20f, 0.44f, 0.20f, 0.14f, 0.50f, 0.44f, 0.80f, 0.88f, 0.80f)
            line(0.54f, 0.38f, 0.74f, 0.62f)
            line(0.74f, 0.38f, 0.54f, 0.62f)
        }

        Glyph.Done -> {
            path(false, 0.18f, 0.54f, 0.40f, 0.76f, 0.84f, 0.26f)
        }

        Glyph.Ledger -> {
            path(true, 0.12f, 0.28f, 0.50f, 0.38f, 0.50f, 0.88f, 0.12f, 0.78f)
            path(true, 0.88f, 0.28f, 0.50f, 0.38f, 0.50f, 0.88f, 0.88f, 0.78f)
        }

        Glyph.Paper -> {
            // A disc with one half shaded: the page in daylight and the page at night.
            drawCircle(color, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.5f), style = s)
            drawArc(
                color = color,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.16f, h * 0.16f),
                size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.68f),
                alpha = 1f,
                style = androidx.compose.ui.graphics.drawscope.Fill,
            )
        }

        Glyph.Edit -> {
            path(true, 0.24f, 0.76f, 0.30f, 0.58f, 0.68f, 0.20f, 0.80f, 0.32f, 0.42f, 0.70f)
            line(0.24f, 0.76f, 0.42f, 0.70f)
        }
    }
}
