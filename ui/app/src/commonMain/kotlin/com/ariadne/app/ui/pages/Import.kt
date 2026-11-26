package com.ariadne.app.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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

    /**
     * Several photographs at once.
     *
     * A person who photographed four receipts and forgot to keep them should not
     * have to open the picker four times. Targets that cannot offer a
     * multi-selection fall back to one pick, so this is safe to call everywhere and
     * the caller never has to ask which kind of picker it has.
     */
    public suspend fun pickMany(limit: Int = 12): List<ByteArray> = listOfNotNull(pick())
}

public val LocalPhotoPicker: ProvidableCompositionLocal<PhotoPicker?> =
    staticCompositionLocalOf { null }

/** True when the current composition can open a picker. */
@Composable
public fun pickerAvailable(): Boolean = LocalPhotoPicker.current != null

/**
 * The library, as one call.
 *
 * Both the shutter page and the front page offer "bring one in from the library",
 * and both must open the platform picker directly: routing to another page and
 * asking for a second tap is the kind of small dishonesty that makes a control
 * feel dead. Targets with no picker fall back to whatever the caller can offer,
 * so the control is never present and inert.
 */
@Composable
public fun rememberPhotoImport(
    onBytes: (ByteArray) -> Unit,
    onUnavailable: () -> Unit,
    onFailure: (Exception) -> Unit = {},
): () -> Unit {
    val picker = LocalPhotoPicker.current
    val scope = rememberCoroutineScope()
    return {
        if (picker == null) {
            onUnavailable()
        } else {
            scope.launch {
                val bytes = try {
                    picker.pick()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    onFailure(failure)
                    null
                }
                if (bytes != null && bytes.isNotEmpty()) onBytes(bytes)
            }
        }
    }
}

/**
 * The library, several at a time.
 *
 * Used where the pile is the point — the tray of photographs waiting for an amount.
 * A picker with no multi-selection hands back a list of one, and the page behaves
 * exactly as it did before.
 */
@Composable
public fun rememberPhotoImportMany(
    onBytes: (List<ByteArray>) -> Unit,
    onUnavailable: () -> Unit,
    onFailure: (Exception) -> Unit = {},
): () -> Unit {
    val picker = LocalPhotoPicker.current
    val scope = rememberCoroutineScope()
    return {
        if (picker == null) {
            onUnavailable()
        } else {
            scope.launch {
                val bytes = try {
                    picker.pickMany()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    onFailure(failure)
                    emptyList()
                }
                val photos = bytes.filter { it.isNotEmpty() }
                if (photos.isNotEmpty()) onBytes(photos)
            }
        }
    }
}
