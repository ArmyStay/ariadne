package com.ariadne.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

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
