package com.ariadne.android

import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val MAX_COMPRESSED_IMAGE_BYTES = 20L * 1024L * 1024L
internal const val MAX_IMAGE_EDGE = 12_000
internal const val MAX_IMAGE_PIXELS = 40_000_000L

/** Reads an image stream with a hard compressed-size ceiling. */
internal fun InputStream.readBoundedImage(): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > MAX_COMPRESSED_IMAGE_BYTES) return null
        output.write(buffer, 0, count)
    }
    return output.toByteArray().takeIf { it.isNotEmpty() }
}

/** Checks encoded dimensions before a full bitmap allocation. */
internal fun ByteArray.hasSafeImageBounds(): Boolean {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)
    return bounds.outWidth > 0 &&
        bounds.outHeight > 0 &&
        bounds.outWidth <= MAX_IMAGE_EDGE &&
        bounds.outHeight <= MAX_IMAGE_EDGE &&
        bounds.outWidth.toLong() * bounds.outHeight.toLong() <= MAX_IMAGE_PIXELS
}

internal fun safeImageBounds(width: Int, height: Int): Boolean =
    width > 0 && height > 0 &&
        width <= MAX_IMAGE_EDGE && height <= MAX_IMAGE_EDGE &&
        width.toLong() * height.toLong() <= MAX_IMAGE_PIXELS
