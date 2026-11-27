package com.ariadne.desktop

import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

internal const val MAX_COMPRESSED_IMAGE_BYTES = 20L * 1024L * 1024L
internal const val MAX_IMAGE_EDGE = 12_000
internal const val MAX_IMAGE_PIXELS = 40_000_000L

internal fun File.readBoundedImage(): ByteArray? {
    if (length() <= 0L || length() > MAX_COMPRESSED_IMAGE_BYTES || !hasSafeImageBounds()) return null
    return inputStream().use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_COMPRESSED_IMAGE_BYTES) return@use null
            output.write(buffer, 0, count)
        }
        output.toByteArray().takeIf { it.isNotEmpty() }
    }
}

internal fun File.hasSafeImageBounds(): Boolean = runCatching {
    ImageIO.createImageInputStream(this)?.use { input ->
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) return@use false
        val reader = readers.next()
        try {
            reader.input = input
            safeImageBounds(reader.getWidth(0), reader.getHeight(0))
        } finally {
            reader.dispose()
        }
    } ?: false
}.getOrDefault(false)

internal fun safeImageBounds(width: Int, height: Int): Boolean =
    width > 0 && height > 0 &&
        width <= MAX_IMAGE_EDGE && height <= MAX_IMAGE_EDGE &&
        width.toLong() * height.toLong() <= MAX_IMAGE_PIXELS
