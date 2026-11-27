package com.ariadne.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.ariadne.app.ui.pages.PhotoImageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object AndroidPhotoImageRenderer : PhotoImageRenderer {
    @Composable
    override fun Render(photoPath: String, modifier: Modifier) {
        var bitmap by remember(photoPath) { mutableStateOf<Bitmap?>(null) }
        val context = LocalContext.current

        LaunchedEffect(photoPath) {
            bitmap = withContext(Dispatchers.IO) { loadPhoto(context, photoPath) }
        }

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Captured receipt",
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        }
    }
}

private fun loadPhoto(context: android.content.Context, path: String): Bitmap? {
    val file = safeAndroidPhotoFile(context, path) ?: return null
    if (!file.isFile) {
        Log.w("AriadnePhoto", "Captured photo is missing")
        return null
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // Decode the same canonical file that passed the private-directory check.
    // Capture rows persist canonical paths, but older rows or imported records
    // can contain an equivalent path with different separators or dot segments.
    BitmapFactory.decodeFile(file.path, bounds)
    if (!safeImageBounds(bounds.outWidth, bounds.outHeight) || file.length() > MAX_COMPRESSED_IMAGE_BYTES) {
        Log.w("AriadnePhoto", "Captured photo could not be decoded")
        return null
    }

    val maxEdge = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (maxEdge / sampleSize > 1600) sampleSize *= 2
    val decoded = BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        },
    ) ?: run {
        Log.w("AriadnePhoto", "Captured photo could not be decoded")
        return null
    }
    val decodedEdge = maxOf(decoded.width, decoded.height)
    if (decodedEdge <= 1600) return decoded

    val scale = 1600f / decodedEdge
    return Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).toInt().coerceAtLeast(1),
        (decoded.height * scale).toInt().coerceAtLeast(1),
        true,
    ).also { scaled ->
        if (scaled !== decoded) decoded.recycle()
    }
}
