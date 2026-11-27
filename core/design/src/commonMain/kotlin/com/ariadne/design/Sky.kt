package com.ariadne.design

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
