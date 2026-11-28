package com.ariadne.android

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.speech.RecognizerIntent
import com.ariadne.app.ui.pages.VoiceEntry

/**
 * The amount, said out loud.
 *
 * Android's speech input is another app's dialog: it is opened with an intent and
 * answers through an activity result. So this holds no microphone of its own — it
 * hands the intent to a launcher the Compose root owns, and the root hands the
 * transcript back to [deliver].
 *
 * `available` is false until that launcher exists, which is why the interface can
 * offer the voice control only on a screen where it can actually work.
 */
public class AndroidVoiceEntry(private val activity: Activity) : VoiceEntry {

    /** Bound by the Compose root; null before the first frame and on any other target. */
    public var launch: ((Intent) -> Unit)? = null

    private var pending: ((String?) -> Unit)? = null

    override val available: Boolean get() = launch != null

    override fun listen(onHeard: (String?) -> Unit) {
        val opener = launch
        if (opener == null) {
            onHeard(null)
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Voice input")
            .setMessage("Your device's speech recognizer is another app and may send audio to its provider. You can type instead.")
            .setNegativeButton("Type instead") { _, _ -> onHeard(null) }
            .setOnCancelListener { onHeard(null) }
            .setPositiveButton("Continue") { _, _ ->
                pending = onHeard
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the amount")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    // A preference, not a guarantee: providers may ignore this. Keep the warning.
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                if (runCatching { opener(intent) }.isFailure) {
                    pending = null
                    onHeard(null)
                }
            }
            .show()
    }

    /** The recogniser's answer: the first transcript, or null when nothing was heard. */
    public fun deliver(text: String?) {
        val answer = pending ?: return
        pending = null
        answer(text)
    }
}
