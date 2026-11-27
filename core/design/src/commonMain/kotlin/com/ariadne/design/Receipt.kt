package com.ariadne.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * A sheet with two rounded top corners and a cut bottom edge.
 *
 * The scallops are the same mark a receipt has when a shop tears it off a roll: a
 * row of half-circles, each the same width, cut *downwards* out of the paper. They
 * are drawn as one path rather than as a row of composables so the card can be any
 * height, clip its own contents, and keep its hairline edge along the cut.
 *
 * The depth of the cut is derived from the width, not fixed: a card narrower than
 * one scallop gets one shallow scallop rather than a row of slivers.
 */
public class TornEdgeShape(
    public val corner: Dp = 16.dp,
    public val scallop: Dp = 7.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val wanted = with(density) { scallop.toPx() }.coerceAtLeast(0.5f)
        val top = with(density) { corner.toPx() }.coerceAtMost(min(size.width, size.height) / 2f)
        // Whole scallops only, and always at least one: a partial one at the end
        // reads as a printing error rather than as a torn edge.
        val count = max(1, (size.width / (wanted * 2f)).toInt())
        val depth = min(wanted, size.width / (2f * count))
        val bottom = size.height

        val path = Path().apply {
            moveTo(0f, top)
            arcTo(Rect(0f, 0f, top * 2f, top * 2f), 180f, 90f, false)
            lineTo(size.width - top, 0f)
            arcTo(Rect(size.width - top * 2f, 0f, size.width, top * 2f), 270f, 90f, false)
            lineTo(size.width, bottom - depth)
            var x = size.width
            repeat(count) {
                // 0° is three o'clock and sweeps clockwise, so this runs from the
                // right-hand lip of each scallop, through its lowest point, to the
                // left-hand lip.
                arcTo(Rect(x - depth * 2f, bottom - depth * 2f, x, bottom), 0f, 180f, false)
                x -= depth * 2f
            }
            lineTo(0f, bottom - depth)
            lineTo(0f, top)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * The card a caught photograph is laid on: white paper, a hairline edge and a torn
 * foot, sitting on the page rather than floating above it.
 *
 * It is not a Material `Card`. There is no elevation, no shadow and no ripple in
 * this book — the card is separated from the page by a hairline and by the cut at
 * its foot, which is the same argument the rest of the app makes with rules instead
 * of boxes.
 *
 * The bottom padding is deeper than the other three so content never sits inside
 * the scallops.
 */
@Composable
public fun ReceiptCard(
    paper: Paper,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val edge = TornEdgeShape()
    Column(
        modifier
            .fillMaxWidth()
            .clip(edge)
            .background(paper.raised)
            .border(Bone.Hairline, paper.edge, edge)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
        content = content,
    )
}

/** One line of a receipt's head: a tracked word, a rule, and a figure or a time. */
@Composable
public fun ReceiptHeadLine(
    left: String,
    paper: Paper,
    right: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label(left, paper, color = paper.inkSoft, size = 9.sp, tracking = 2.4.sp)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(Bone.Hairline).background(paper.edgeStrong))
        if (right != null) {
            Spacer(Modifier.width(12.dp))
            Label(right, paper, color = paper.inkMid, size = 9.sp, tracking = 1.2.sp)
        }
    }
}
