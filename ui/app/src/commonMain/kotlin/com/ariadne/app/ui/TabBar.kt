package com.ariadne.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Paper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The bottom bar, after the reference: four quiet destinations, and the camera
 * seated in a notch cut out of the bar itself.
 *
 * The notch is drawn rather than decorated — a filled path with a circular bite
 * taken out, plus a hairline traced along its top edge so the bar reads as one
 * continuous line that dips for the shutter. That single detail is what makes
 * the reference feel made rather than assembled, so it is worth the path maths.
 */

/** The four destinations in the bar. Capture is not one of them; it floats. */
public enum class Tab(public val label: String) {
    Home("Home"),
    Timeline("Timeline"),
    Trip("Trip"),
    Stats("Stats"),
}

@Composable
public fun TabBar(
    paper: Paper,
    /**
     * The destination being shown, or null on a page that is stacked on top of a
     * tab. The register and Preferences are stacked from Home: they keep the bar
     * (it is how you get back to the rest of the app) but no destination is
     * selected, because none of them is where the person is.
     */
    selected: Tab?,
    onSelect: (Tab) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    waiting: Int = 0,
    /** Read by a screen reader as the description of the bar itself. */
    barDescription: String? = null,
    /**
     * Whether a photograph can be taken at all.
     *
     * False when no trip is open — a closed trip is a finished record, and the
     * shutter is the one control that would quietly break that. The plate stays
     * where it is (the bar's shape depends on it) but stops being a target, and
     * the screen reader says why.
     */
    captureEnabled: Boolean = true,
) {
    // The bar grows with the system font instead of clipping its labels: at 200%
    // text a fixed 74dp bar would cut the words in half. Capped at 1.6x, because
    // past that the bar is eating the page it belongs to.
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val barHeight = (74f * fontScale).dp
    val shutter = 62.dp
    val described = barDescription
    Box(
        modifier
            .fillMaxWidth()
            .then(
                if (described != null) {
                    Modifier.semantics { contentDescription = described }
                } else {
                    Modifier
                }
            ),
    ) {
        // The bar surface and its notch. The dip is a pair of cubic curves rather
        // than a circular bite: a bite reads as a hole, whereas a curve reads as
        // the bar bending around the shutter, which is what the reference does.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(barHeight + 18.dp),
        ) {
            val barTop = 18.dp.toPx()
            val cx = size.width / 2f
            val r = 44.dp.toPx()          // half the width of the dip
            val depth = 30.dp.toPx()      // how far the curve drops below the bar top
            val shoulder = 26.dp.toPx()   // how far out the curve starts easing

            val notch = Path().apply {
                moveTo(0f, barTop)
                lineTo(cx - r - shoulder, barTop)
                cubicTo(
                    cx - r * 0.55f, barTop,
                    cx - r * 0.62f, barTop + depth,
                    cx, barTop + depth,
                )
                cubicTo(
                    cx + r * 0.62f, barTop + depth,
                    cx + r * 0.55f, barTop,
                    cx + r + shoulder, barTop,
                )
                lineTo(size.width, barTop)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(notch, paper.raised)
            drawPath(notch, paper.edgeStrong, style = Stroke(width = 1f))
        }

        // The four destinations, sitting either side of the notch.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(barHeight)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(Tab.Home, selected, paper, waiting, Modifier.weight(1f)) { onSelect(Tab.Home) }
            TabItem(Tab.Timeline, selected, paper, 0, Modifier.weight(1f)) { onSelect(Tab.Timeline) }
            // Reserved for the shutter.
            Spacer(Modifier.width(84.dp))
            TabItem(Tab.Trip, selected, paper, 0, Modifier.weight(1f)) { onSelect(Tab.Trip) }
            TabItem(Tab.Stats, selected, paper, 0, Modifier.weight(1f)) { onSelect(Tab.Stats) }
        }

        // The shutter: a blue plate sitting in the notch, ringed by a hairline so
        // it reads as seated in the paper rather than pasted on top of it. It was
        // an ink disc; ink is the colour of type here, and the one control that is
        // always one tap away should be the one blue thing on the page.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp)
                .size(shutter)
                .background(paper.isNight.let { if (it) paper.raised else paper.bg }, CircleShape)
                .padding(5.dp)
                // A closed trip keeps the disc's place and loses its colour: the
                // bar should not look like it is offering something it refuses.
                .background(if (captureEnabled) paper.blue else paper.edge, CircleShape)
                .border(Bone.Hairline, paper.bluePressed.copy(alpha = 0.35f), CircleShape)
                // One node, one sentence. The count badge is a child of this
                // plate, so without clearing the subtree a screen reader would
                // announce the number twice.
                .clearAndSetSemantics {
                    contentDescription = when {
                        !captureEnabled -> "A closed trip takes no photographs. See the card instead."
                        waiting > 0 -> "Capture a photograph. $waiting waiting for tonight."
                        else -> "Capture a photograph"
                    }
                    if (captureEnabled) role = Role.Button
                }
                .pointerInput(waiting, captureEnabled) {
                    if (captureEnabled) detectTapGestures(onTap = { onCapture() })
                },
            contentAlignment = Alignment.Center,
        ) {
            ApertureGlyph(if (captureEnabled) paper.onBlue else paper.inkFaint)
            if (waiting > 0) {
                // The count, seated at the edge of the plate: a numeral, not a dot.
                // A pill rather than a fixed 17dp disc: at 200% text, or with a
                // two-digit stack, a disc clips the number it exists to show.
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 1.dp, y = 1.dp)
                        .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                        .background(paper.ink, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Label("$waiting", paper, color = paper.onBlue, size = 8.sp, tracking = 0.4.sp)
                }
            }
        }
    }
}

/** One destination: a drawn glyph over tracked-out type. */
@Composable
private fun TabItem(
    tab: Tab,
    selected: Tab?,
    paper: Paper,
    waiting: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val active = tab == selected
    val ink = if (active) paper.ink else paper.inkSoft
    Column(
        modifier
            .semantics {
                contentDescription = tab.label
                role = Role.Button
            }
            .pointerInput(tab) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            TabGlyph(tab, ink)
            if (tab == Tab.Home && waiting > 0) {
                Box(
                    Modifier
                        .offset(x = 5.dp, y = (-2).dp)
                        .size(6.dp)
                        .background(paper.blue, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Label(
            tab.label,
            paper,
            color = ink,
            size = 9.sp,
            tracking = 1.5.sp,
        )
    }
}

/**
 * The four glyphs, drawn as line art at a single weight so the set looks cut
 * from the same plate: house, ticket, the trip's ruled column, bar chart.
 */
@Composable
private fun TabGlyph(tab: Tab, color: Color) {
    Canvas(
        Modifier
            .size(22.dp)
            .semantics { contentDescription = "${tab.label} marker" },
    ) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = 1.4.dp.toPx())
        when (tab) {
            Tab.Home -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.12f, h * 0.46f)
                    lineTo(w * 0.12f, h * 0.86f)
                    lineTo(w * 0.88f, h * 0.86f)
                    lineTo(w * 0.88f, h * 0.46f)
                    close()
                }
                drawPath(p, color, style = s)
                drawLine(color, Offset(w * 0.4f, h * 0.86f), Offset(w * 0.4f, h * 0.62f), s.width)
            }

            Tab.Timeline -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.3f)
                    lineTo(w * 0.92f, h * 0.3f)
                    lineTo(w * 0.92f, h * 0.86f)
                    lineTo(w * 0.08f, h * 0.86f)
                    close()
                }
                drawPath(p, color, style = s)
                drawLine(color, Offset(w * 0.08f, h * 0.44f), Offset(w * 0.92f, h * 0.44f), s.width)
                drawLine(color, Offset(w * 0.32f, h * 0.2f), Offset(w * 0.32f, h * 0.36f), s.width)
                drawLine(color, Offset(w * 0.68f, h * 0.2f), Offset(w * 0.68f, h * 0.36f), s.width)
            }

            Tab.Stats -> {
                drawLine(color, Offset(w * 0.16f, h * 0.86f), Offset(w * 0.16f, h * 0.58f), s.width)
                drawLine(color, Offset(w * 0.5f, h * 0.86f), Offset(w * 0.5f, h * 0.22f), s.width)
                drawLine(color, Offset(w * 0.84f, h * 0.86f), Offset(w * 0.84f, h * 0.44f), s.width)
            }

            Tab.Trip -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.30f)
                    lineTo(w * 0.92f, h * 0.30f)
                    lineTo(w * 0.92f, h * 0.62f)
                    lineTo(w * 0.08f, h * 0.62f)
                    close()
                }
                drawPath(p, color, style = s)
                drawLine(color, Offset(w * 0.08f, h * 0.46f), Offset(w * 0.92f, h * 0.46f), s.width)
                drawLine(color, Offset(w * 0.50f, h * 0.62f), Offset(w * 0.50f, h * 0.86f), s.width)
            }
        }
    }
}

/** A camera aperture, drawn: circle, inner ring, six blades. */
@Composable
private fun ApertureGlyph(color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val c = Offset(w / 2f, w / 2f)
        val s = Stroke(width = 1.5.dp.toPx())
        drawCircle(color, radius = w * 0.40f, center = c, style = s)
        drawCircle(color, radius = w * 0.17f, center = c, style = s)
        for (i in 0 until 6) {
            val a = PI * i / 3 + 0.4
            drawLine(
                color,
                Offset(c.x + (cos(a) * w * 0.18f).toFloat(), c.y + (sin(a) * w * 0.18f).toFloat()),
                Offset(c.x + (cos(a) * w * 0.40f).toFloat(), c.y + (sin(a) * w * 0.40f).toFloat()),
                strokeWidth = s.width * 0.85f,
            )
        }
    }
}
