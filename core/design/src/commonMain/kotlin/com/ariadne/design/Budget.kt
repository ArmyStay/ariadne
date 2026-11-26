package com.ariadne.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A budget, drawn as a measure.
 *
 * Three pages needed the same thing and each had grown its own version: Today said
 * it in a pill, Trip drew a bar out of two nested `Box`es, Stats said it in a line of
 * figures. This is the one version, and it is a `Canvas` rather than nested boxes
 * because a bar made of `Box`es cannot draw a hairline tick without a third box and
 * a fourth measurement.
 *
 * The tick is the part that matters. A bar alone says how much of the budget is gone;
 * a tick at [pace] says whether that is too much *by now*, which is the only question
 * anyone actually has in the middle of a trip.
 *
 * Not a Material `LinearProgressIndicator`: that arrives with rounded caps, an
 * indeterminate animation hook and a minimum-height rule from the M3 spec, none of
 * which belong in a design made of hairlines.
 */
@Composable
public fun BudgetBar(
    fraction: Float,
    paper: Paper,
    modifier: Modifier = Modifier,
    pace: Float? = null,
    thickness: Dp = 6.dp,
    over: Boolean = false,
) {
    // The bar eases to its value rather than snapping: a figure that has just
    // changed is read against the fence, and watching which way it moved is half
    // the point. Reduced motion gets the settled value on the first frame.
    val reduced = LocalMotionPreference.current.reduced
    val shown = if (reduced) {
        fraction
    } else {
        androidx.compose.animation.core.animateFloatAsState(
            targetValue = fraction,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 420,
                easing = Motion.Ease,
            ),
            label = "budget",
        ).value
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(thickness.coerceAtLeast(6.dp)),
    ) {
        val h = size.height
        val mid = h / 2f
        val stroke = thickness.toPx()

        // The track: a sunk rule, not a grey pill.
        drawLine(
            color = paper.sunk,
            start = Offset(0f, mid),
            end = Offset(size.width, mid),
            strokeWidth = stroke,
            cap = StrokeCap.Butt,
        )

        // The spent share. Over budget is not a different colour — this app has one
        // chromatic value — it is simply drawn to the end of the measure.
        val filled = shown.coerceIn(0f, 1f)
        if (filled > 0f) {
            drawLine(
                color = if (over) paper.ink else paper.ink,
                start = Offset(0f, mid),
                end = Offset(size.width * filled, mid),
                strokeWidth = stroke,
                cap = StrokeCap.Butt,
            )
        }

        // Where the pace would have you by today, as a hairline through the bar.
        if (pace != null) {
            val x = size.width * pace.coerceIn(0f, 1f)
            drawLine(
                color = paper.blue,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}
