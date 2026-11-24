package com.ariadne.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The whole motion vocabulary, in one place.
 *
 * One duration, one curve, one property. If a second duration is ever needed it
 * should be argued for here rather than typed into a page — the restraint is the
 * design, and it is easy to lose one `tween(400)` at a time.
 */
public object Motion {
    /** Milliseconds. Long enough to read as a page turn, short enough to not wait. */
    public const val Page: Int = 180

        /** Slight ease-out: fast away, gentle arrival. No overshoot, ever. */
    public val Ease: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** A number changing. Shorter than a page turn: it is a value, not a place. */
    public const val Value: Int = 140

    /** A panel opening under a row. */
    public const val Reveal: Int = 180

    /**
     * How far a page lifts as it arrives.
     *
     * Horizontal — a few points to the right, settling back — never vertical. The
     * app is a book: pages turn sideways. A vertical lift reads as a phone menu.
     */
    public const val Drift: Float = 10f
}

/** The spec for anything that moves, honouring the platform's reduction. */
@Composable
public fun <T> motionSpec(durationMillis: Int = Motion.Page): androidx.compose.animation.core.TweenSpec<T> {
    val reduced = LocalMotionPreference.current.reduced
    return androidx.compose.animation.core.tween(
        durationMillis = if (reduced) 0 else durationMillis,
        easing = Motion.Ease,
    )
}

/**
 * A number that counts to its new value instead of jumping.
 *
 * Money in this app is read in columns and compared against a fence, and a figure
 * that snaps from one total to another makes it hard to see which way it moved.
 * The motion is only in the presentation: the value underneath is the [Long] it
 * always was, never a float, and it lands exactly on the target.
 */
@Composable
public fun animatedLong(target: Long, durationMillis: Int = Motion.Value): Long {
    val reduced = LocalMotionPreference.current?.reduced ?: false
    if (reduced) return target
    // Compose animates floats, not longs. The float exists only for the frames in
    // between; the value it settles on is the long, rounded once at the end.
    val state = androidx.compose.animation.core.animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = durationMillis,
            easing = Motion.Ease,
        ),
    )
    val settled = state.value
    return if (kotlin.math.abs(settled - target) < 0.5f) target else settled.toLong()
}

/**
 * Whether the platform has asked for reduced motion.
 *
 * Android reads `Settings.Global.TRANSITION_ANIMATION_SCALE`, the browser reads
 * `prefers-reduced-motion`, and desktop can simply stay animated. Whatever the
 * answer, the caller passes it down rather than each page sniffing for itself.
 */
public data class MotionPreference(public val reduced: Boolean) {
    public companion object {
        public val Full: MotionPreference = MotionPreference(reduced = false)
    }
}

public val LocalMotionPreference = compositionLocalOf { MotionPreference.Full }

/** How far apart two rows of the same list arrive. */
private const val StaggerMillis: Long = 34L

/**
 * A block that fades into place, [index] steps down the page.
 *
 * The effect is deliberately one property - opacity - and no offset. A list that
 * slides up as it appears is the single most generic animation on the internet;
 * a list that fades in, slightly behind itself, reads as printed matter settling.
 * Rows past the sixth arrive together, because after six the stagger stops
 * looking intentional and starts looking slow.
 *
 * Opacity is read through a lambda inside `graphicsLayer`, so a page that is
 * still fading does not recompose: only its layer's alpha changes.
 */
@Composable
public fun Reveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = LocalMotionPreference.current.reduced
    if (reduced) {
        content()
        return
    }
    val shown = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(index) {
        shown.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = Motion.Page.toInt() + (index.coerceAtMost(6) * StaggerMillis).toInt(),
                easing = Motion.Ease,
            ),
        )
    }
    androidx.compose.foundation.layout.Box(
        modifier.graphicsLayer { alpha = shown.value },
    ) { content() }
}
