package com.ariadne.wasm

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ariadne.app.PhotoFiles
import com.ariadne.app.PhotoNaming
import com.ariadne.app.ui.pages.PhotoImageRenderer
import com.ariadne.model.LocalDay

/**
 * Photographs in a browser tab: a map, held for as long as the page is.
 *
 * The browser build has no file system and no camera, so before this every photograph
 * it "kept" was an empty one — the ruled plate, filed as a photograph, which is honest
 * about what a browser can do and useless for showing what the app does with film.
 *
 * A map of path → bytes is enough to make the rest of the app behave exactly as it does
 * on a phone: the shutter's bytes are written, the capture row holds a real path, the
 * record draws the image, and a reload forgets all of it — which the storage
 * disclosure on Preferences already says, in those words.
 *
 * The path is the same `PhotoNaming.relative` string every other target uses, so an
 * exported archive names its files the same way here as anywhere else.
 */
internal object BrowserPhotoFiles : PhotoFiles {

    private val kept = mutableMapOf<String, ByteArray>()

    override suspend fun write(
        tripId: String,
        day: LocalDay,
        id: String,
        bytes: ByteArray,
    ): String {
        val path = PhotoNaming.relative(tripId, day, id)
        kept[path] = bytes
        return path
    }

    override suspend fun read(path: String): ByteArray? = kept[path]

    override suspend fun delete(path: String) {
        kept.remove(path)
    }

    override suspend fun footprintBytes(): Long = kept.values.sumOf { it.size.toLong() }

    /** The synchronous look-up the renderer needs inside a composition. */
    fun bytes(path: String): ByteArray? = kept[path]
}

/**
 * The photograph itself, decoded for Compose.
 *
 * A browser has the decoder we need already — `createImageBitmap` under the decode
 * helper Compose exposes for every target — so there is no native code here, only a
 * decode and a failure that draws nothing rather than a broken frame: a photograph
 * that cannot be decoded leaves the ruled plate, which is the same thing the app shows
 * for a photograph it does not have.
 */
internal object BrowserPhotoImageRenderer : PhotoImageRenderer {

    @Composable
    override fun Render(photoPath: String, modifier: Modifier) {
        val bitmap = remember(photoPath) {
            BrowserPhotoFiles.bytes(photoPath)?.let { bytes ->
                runCatching { bytes.decodeToImageBitmap() }.getOrNull()
            }
        }
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Captured receipt",
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        }
    }
}
