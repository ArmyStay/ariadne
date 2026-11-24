package com.ariadne.app.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Bringing a photograph in from somewhere other than the shutter.
 *
 * The shutter is the fast path and stays the fast path, but a person who forgot
 * to photograph the receipt at the counter should not be told the record is lost.
 * The system photo picker (Android), the file dialog (desktop), the photo library
 * (iOS) and a file input (the browser) all reduce to one question - "which image?"
 * - and one answer - bytes.
 *
 * It is a `CompositionLocal` for the same reason the camera is: the shared UI must
 * not name a platform API, and a target that has no picker simply leaves the local
 * empty, in which case the page does not offer the control at all rather than
 * offering one that cannot work.
 *
 * [pick] is suspending because every real picker is: a dialog has to open, a
 * person has to choose, and the answer comes back later. A callback-shaped API
 * here would push that state into every page.
 */
public interface PhotoPicker {
    /** Opens the platform's picker. Null when the person cancels. */
    public suspend fun pick(): ByteArray?
}

public val LocalPhotoPicker: androidx.compose.runtime.ProvidableCompositionLocal<PhotoPicker?> =
    staticCompositionLocalOf { null }

/** True when the current composition can open a picker. */
@Composable
public fun pickerAvailable(): Boolean = LocalPhotoPicker.current != null
