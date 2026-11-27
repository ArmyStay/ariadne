package com.ariadne.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Leaves: the card, and the four things that sit on one.
 *
 * The book was ruled for a year — figures straight on the paper, hairlines instead
 * of boxes, nothing that casts a shadow. That reads well on a page of type and
 * badly on a phone held at arm's length, where a figure needs an edge to sit on
 * before the eye will trust it. A leaf is that edge, and only that: raised paper,
 * one hairline, a soft corner and a shadow measured in single dp. It is still a
 * sheet, not a material — no tint, no elevation that implies a sheet of glass.
 *
 * The rules a leaf obeys, so the app does not quietly become a dashboard:
 *
 *  - **One figure a card.** A card holds a number, a list, or a form; never two
 *    numbers of equal weight.
 *  - **A disc, not an icon set.** The mark is a wash circle with a glyph in it,
 *    the same size on every card, so the cards line up down the left edge.
 *  - **The note goes inside.** Whatever qualifies the figure — "finished entries
 *    only", "not converted" — is set in the same card, under a hairline.
 */

/** The corner a leaf wears: soft enough to be a card, tight enough to be paper. */
public val LeafRadius: Dp = 20.dp

@Composable
public fun Leaf(
    paper: Paper,
    modifier: Modifier = Modifier,
    inset: Dp = 18.dp,
    radius: Dp = LeafRadius,
    tint: Color? = null,
    edge: Color = paper.edge,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .fillMaxWidth()
            .then(
                // A shadow on night stock is a smudge; the hairline does the work there.
                if (paper.isNight || tint != null) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = paper.ink.copy(alpha = 0.035f),
                        spotColor = paper.ink.copy(alpha = 0.06f),
                    )
                }
            )
            .clip(shape)
            .background(tint ?: paper.raised)
            .border(Bone.Hairline, edge, shape)
            .padding(inset),
        content = content,
    )
}

/** A disc of wash with a glyph or a code inside it: the card's one mark. */
@Composable
public fun DiscBadge(
    paper: Paper,
    glyph: Glyph? = null,
    text: String? = null,
    size: Dp = 38.dp,
) {
    Box(
        Modifier
            .size(size)
            .background(paper.blueWash, CircleShape)
            .border(Bone.Hairline, paper.blueEdge.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            Icon(glyph, paper, size = size * 0.46f, color = paper.blue)
        } else {
            val label = text.orEmpty()
            Numeral(
                label,
                paper,
                color = paper.blue,
                size = if (label.length > 2) 9.sp else 13.sp,
                weight = FontWeight.Medium,
            )
        }
    }
}

/** The card's footnote: an i in a ring, and the sentence that qualifies the figure. */
@Composable
public fun Note(text: String, paper: Paper, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(17.dp).border(Bone.Hairline, paper.edgeStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Numeral("i", paper, color = paper.inkFaint, size = 9.sp)
        }
        Spacer(Modifier.width(10.dp))
        Body(
            text,
            paper,
            color = paper.inkFaint,
            size = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A section, on a page that is a list of cards: a short navy rule, then the words.
 *
 * The roman numeral the rest of the book uses would be wrong here — these sections
 * are places, not chapters — and a bare tracked label floats. A rule is what a
 * designer reaches for, and it costs one box.
 */
@Composable
public fun SectionMark(label: String, paper: Paper, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(13.dp).background(paper.navy, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(10.dp))
        Label(label, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.6.sp)
    }
}

/**
 * One row of an index, as a card: a disc, a serif name, the quiet fact about it,
 * and the angle that says it opens.
 */
@Composable
public fun IndexCard(
    glyph: Glyph,
    label: String,
    detail: String,
    paper: Paper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Leaf(
        paper = paper,
        modifier = modifier.tap(label) { onClick() },
        inset = 14.dp,
        radius = 16.dp,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DiscBadge(paper, glyph = glyph)
            Spacer(Modifier.width(14.dp))
            // The name and its fact are a stack, not a line: "an expense without a
            // photograph" is a sentence, and a sentence set beside a name ends in an
            // ellipsis on the phone this book is mostly read on.
            Column(Modifier.weight(1f)) {
                Title(label, paper, size = 19.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detail,
                    style = TextStyle(
                        fontFamily = Bone.Sans,
                        fontSize = 11.sp,
                        letterSpacing = Optical.bodyTracking,
                    ),
                    color = paper.inkFaint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(Glyph.Chevron, paper, size = 14.dp, color = paper.inkFaint)
        }
    }
}

/** A small pill: the one control a card may carry, and never a second one. */
@Composable
public fun PillButton(
    label: String,
    paper: Paper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .tap(label) { onClick() }
            .clip(shape)
            .then(if (filled) Modifier.background(paper.blue) else Modifier)
            .border(Bone.Hairline, if (filled) paper.blue else paper.blueEdge, shape)
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            ),
            color = if (filled) paper.onBlue else paper.blue,
            maxLines = 1,
        )
    }
}

/** The four-point spark, drawn the way the shutter's badge draws it. */
@Composable
public fun Spark(color: Color, extent: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(extent)) {
        spark(Offset(size.width / 2f, size.height / 2f), size.width * 0.46f, color)
    }
}

/**
 * A spark at a point. `a` is what makes the arms needle-thin: both control points
 * sit at a tenth of the tip radius, so the sides bow almost to the centre and the
 * four arms meet at a small waist.
 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.spark(centre: Offset, r: Float, colour: Color) {
    val a = r * 0.10f
    val path = Path().apply {
        moveTo(centre.x, centre.y - r)
        cubicTo(centre.x + a, centre.y - a, centre.x + a, centre.y - a, centre.x + r, centre.y)
        cubicTo(centre.x + a, centre.y + a, centre.x + a, centre.y + a, centre.x, centre.y + r)
        cubicTo(centre.x - a, centre.y + a, centre.x - a, centre.y + a, centre.x - r, centre.y)
        cubicTo(centre.x - a, centre.y - a, centre.x - a, centre.y - a, centre.x, centre.y - r)
        close()
    }
    drawPath(path, colour)
}
