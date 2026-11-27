package com.ariadne.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ariadne.core.design.generated.resources.Res
import ariadne.core.design.generated.resources.globe_hero
import org.jetbrains.compose.resources.painterResource

/**
 * The globe a journey page opens on: a watercolour of the earth's limb with a
 * photograph of a plane crossing it, drawn by hand rather than by the app.
 *
 * It is the one picture in the book that is not a plate study, and it is allowed
 * here for a reason: a trip is a place before it is a list of days, and a page
 * that opens on paper alone has to say that in a sentence. A wash at the top of
 * the page says it before the headline is read.
 *
 * Three rules keep it from becoming decoration:
 *
 *  - it never carries information, so it is `contentDescription = null` and a
 *    screen reader steps straight over it;
 *  - it is the *ground* of the masthead, not a tile: it bleeds off the page edge
 *    and the type is set over it;
 *  - it is drawn once, behind everything, and does not scroll. A watermark that
 *    slides under the text is a background image; one that stays put is paper.
 *
 * The artwork is already keyed to transparency around the globe, so it sits on
 * [Paper.bg] with no edge and works on both printings — faintly, at a fifth of
 * its weight, on night stock.
 */
@Composable
public fun GlobeSky(paper: Paper, modifier: Modifier = Modifier, opacity: Float = 1f) {
    Image(
        painter = painterResource(Res.drawable.globe_hero),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
        // Night has nothing for a white wash to sit on: the same drawing at a
        // fifth of the ink reads as a ghost of the same page rather than as a
        // light rectangle in a dark room.
        alpha = (if (paper.isNight) 0.18f else 0.92f) * opacity,
    )
}

/**
 * The sky a page of places opens on: three arcs off the top-right corner, a paper
 * plane crossing them, and two sparks.
 *
 * Drawn rather than imported, for the same reason the glyphs are: an illustration
 * that ships as a bitmap has to be redrawn for night stock, scaled for a tablet and
 * re-exported when the palette moves. This is four paths and a stroke, it sits on
 * either printing, and it costs no request.
 *
 * It is decoration and says so — nothing here is a control and nothing is announced
 * to a screen reader. It is also quiet: the arcs are a 7% wash, so the type above
 * them stays the loudest thing on the page.
 */
@Composable
public fun FlightSky(paper: Paper, modifier: Modifier = Modifier) {
    val ink = paper.blue
    val weight = if (paper.isNight) 0.6f else 1f
    Canvas(modifier) {
        val w = size.width
        // Three arcs, centred just outside the page: the curve of a globe seen from
        // above, not a ring behind the text.
        for (ring in 1..3) {
            drawCircle(
                color = ink.copy(alpha = 0.09f * weight),
                radius = w * (0.46f + 0.22f * ring),
                center = Offset(w * 1.06f, -w * 0.06f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        plane(
            colour = ink.copy(alpha = 0.9f * weight),
            centre = Offset(w - 46.dp.toPx(), 52.dp.toPx()),
            span = 16.dp.toPx(),
        )
        // The margin spark, level with the first line of the headline; and a smaller
        // one out at the arcs, so the two corners answer each other.
        spark(Offset(17.dp.toPx(), 27.dp.toPx()), 8.dp.toPx(), ink.copy(alpha = 0.95f * weight))
        spark(Offset(w - 118.dp.toPx(), 132.dp.toPx()), 4.dp.toPx(), ink.copy(alpha = 0.30f * weight))
    }
}

/** A paper plane, two triangles about a fold: nose up and to the right. */
private fun DrawScope.plane(colour: Color, centre: Offset, span: Float) {
    val x = centre.x
    val y = centre.y
    val nose = Offset(x + span, y - span * 0.86f)
    val tail = Offset(x - span, y + span * 0.10f)
    val fold = Offset(x - span * 0.04f, y + span * 0.10f)
    val foot = Offset(x - span * 0.02f, y + span * 1.02f)
    val upper = Path().apply {
        moveTo(nose.x, nose.y); lineTo(tail.x, tail.y); lineTo(fold.x, fold.y); close()
    }
    val lower = Path().apply {
        moveTo(nose.x, nose.y); lineTo(fold.x, fold.y); lineTo(foot.x, foot.y); close()
    }
    drawPath(upper, colour)
    drawPath(lower, colour.copy(alpha = colour.alpha * 0.72f))
}
