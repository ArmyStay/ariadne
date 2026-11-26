package com.ariadne.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ariadne.app.ui.pages.PhotoPicker
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The Android library: the system photo picker, not a file browser.
 *
 * `PickVisualMedia` is the right door on every version this app supports. On
 * Android 13 and later it is the platform picker; on older versions the same call
 * goes through Google Play services' backported picker. Either way the app gets
 * one photograph and **no storage permission** — the person grants exactly the
 * one image they chose, and the manifest does not have to ask for a library.
 *
 * The bytes are bounded the same way the camera bounds them: decoded, scaled to
 * 1600px on the long edge, re-encoded at 88. A 12MP gallery photograph is four
 * megabytes and a receipt does not need ten of them in a trip.
 *
 * The await is a `CompletableDeferred` rather than a callback, because the page
 * above is a suspend function; the picker contract itself is an activity result,
 * which is a callback by construction. Cancelling the dialog completes with null,
 * so the page simply keeps whatever it had.
 */
@Composable
internal fun rememberAndroidPhotoPicker(context: Context): PhotoPicker {
    // A result arrives through a lambda captured by the activity result registry,
    // so the newest launcher is kept in a state holder rather than captured once.
    val launch = remember { mutableStateOf<(() -> Unit)?>(null) }
    val pending = remember { mutableStateOf<CompletableDeferred<ByteArray?>?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val waiting = pending.value
        pending.value = null
        if (waiting == null) return@rememberLauncherForActivityResult
        if (uri == null) {
            waiting.complete(null)
        } else {
            // Off the composition thread: decoding a large photograph takes long
            // enough to drop frames, and this is a page someone is looking at.
            Thread { waiting.complete(readPhoto(context, uri)) }.start()
        }
    }
    launch.value = {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    return remember(context) {
        object : PhotoPicker {
            override suspend fun pick(): ByteArray? {
                // One dialog at a time: a second tap while the picker is open is
                // the same request, not a queue of them.
                if (pending.value != null) return null
                val answer = CompletableDeferred<ByteArray?>()
                pending.value = answer
                launch.value?.invoke()
                // Ten minutes is not a timeout anyone will hit; it exists so a
                // picker killed by the system cannot strand a coroutine forever.
                return withTimeoutOrNull(10L * 60L * 1000L) { answer.await() }
            }
        }
    }
}

/** Read, bound and re-encode the chosen photograph. Null if it cannot be read. */
private fun readPhoto(context: Context, uri: Uri): ByteArray? = runCatching {
    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return@runCatching null
    if (raw.isEmpty()) return@runCatching null
    // A format this device cannot decode is still a photograph: keep the bytes
    // rather than telling the person their picture is not a picture.
    val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return@runCatching raw
    val scaled = scaleDown(decoded, 1600)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()
    out.toByteArray().takeIf { it.isNotEmpty() } ?: raw
}.getOrNull()
