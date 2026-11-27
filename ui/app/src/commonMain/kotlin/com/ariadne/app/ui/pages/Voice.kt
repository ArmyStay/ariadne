package com.ariadne.app.ui.pages

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What a platform hands over to hear one sentence about money.
 *
 * The app's premise is that the fastest record of an expense is the one you can make
 * without a keyboard: photograph it, or say it. This is the second half. Nothing in
 * the shared UI knows how a microphone works — it asks for a sentence and gets text,
 * which is then read by `SpokenExpense` (a plain function with tests, no platform).
 *
 * [listen] may call back with null: no recogniser, no permission, or nothing heard.
 * A null is not an error to report, it is a sentence that did not happen.
 */
public interface VoiceEntry {
    public val available: Boolean
    public fun listen(onHeard: (String?) -> Unit)
}

/** Supplied at the root. Empty by default, which is the honest state on most targets. */
public val LocalVoiceEntry: ProvidableCompositionLocal<VoiceEntry?> =
    staticCompositionLocalOf { null }
