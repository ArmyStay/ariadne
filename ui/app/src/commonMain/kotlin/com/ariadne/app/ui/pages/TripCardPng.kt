package com.ariadne.app.ui.pages

import androidx.compose.ui.graphics.ImageBitmap

/** A small, dependency-free PNG writer for the pixels rendered by Compose. */
internal fun ImageBitmap.toPngBytes(): ByteArray {
    require(width > 0 && height > 0) { "A Trip Card image must have non-zero dimensions." }

    val pixels = IntArray(width * height)
    readPixels(pixels)
    return encodeRgbaPng(width, height, pixels)
}

internal fun encodeRgbaPng(width: Int, height: Int, pixels: IntArray): ByteArray {
    require(width > 0 && height > 0) { "A Trip Card image must have non-zero dimensions." }
    require(pixels.size == width * height) { "Pixel count must match the image dimensions." }

    val rowBytes = width * 4 + 1
    val raw = ByteArray(rowBytes * height)
    for (y in 0 until height) {
        val row = y * rowBytes
        raw[row] = 0
        for (x in 0 until width) {
            val pixel = pixels[y * width + x]
            val offset = row + 1 + x * 4
            raw[offset] = (pixel shr 16).toByte()
            raw[offset + 1] = (pixel shr 8).toByte()
            raw[offset + 2] = pixel.toByte()
            raw[offset + 3] = (pixel ushr 24).toByte()
        }
    }

    val ihdr = ByteArray(13)
    putInt(ihdr, 0, width)
    putInt(ihdr, 4, height)
    ihdr[8] = 8
    ihdr[9] = 6

    val blocks = (raw.size + MAX_STORED_BLOCK - 1) / MAX_STORED_BLOCK
    val compressed = ByteArray(2 + raw.size + blocks * 5 + 4)
    var compressedOffset = 0
    compressed[compressedOffset++] = 0x78
    compressed[compressedOffset++] = 0x01
    var rawOffset = 0
    while (rawOffset < raw.size) {
        val length = minOf(MAX_STORED_BLOCK, raw.size - rawOffset)
        val final = rawOffset + length == raw.size
        compressed[compressedOffset++] = if (final) 1 else 0
        compressed[compressedOffset++] = length.toByte()
        compressed[compressedOffset++] = (length ushr 8).toByte()
        val inverse = length.inv()
        compressed[compressedOffset++] = inverse.toByte()
        compressed[compressedOffset++] = (inverse ushr 8).toByte()
        raw.copyInto(compressed, compressedOffset, rawOffset, rawOffset + length)
        compressedOffset += length
        rawOffset += length
    }
    putInt(compressed, compressedOffset, adler32(raw))

    val output = ByteArray(PNG_SIGNATURE.size + 25 + compressed.size + 12 + 12)
    var outputOffset = 0
    PNG_SIGNATURE.copyInto(output, outputOffset)
    outputOffset += PNG_SIGNATURE.size
    outputOffset += writeChunk(output, outputOffset, "IHDR", ihdr)
    outputOffset += writeChunk(output, outputOffset, "IDAT", compressed)
    writeChunk(output, outputOffset, "IEND", ByteArray(0))
    return output
}

private const val MAX_STORED_BLOCK = 65_535
private val PNG_SIGNATURE = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)

private fun writeChunk(output: ByteArray, offset: Int, type: String, data: ByteArray): Int {
    putInt(output, offset, data.size)
    val typeOffset = offset + 4
    for (index in type.indices) output[typeOffset + index] = type[index].code.toByte()
    data.copyInto(output, typeOffset + 4)
    putInt(output, typeOffset + 4 + data.size, crc32(type, data))
    return data.size + 12
}

private fun putInt(bytes: ByteArray, offset: Int, value: Int) {
    bytes[offset] = (value ushr 24).toByte()
    bytes[offset + 1] = (value ushr 16).toByte()
    bytes[offset + 2] = (value ushr 8).toByte()
    bytes[offset + 3] = value.toByte()
}

private fun crc32(type: String, data: ByteArray): Int {
    var crc = -1
    fun update(value: Int) {
        crc = crc xor value
        repeat(8) {
            crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
        }
    }
    type.forEach { update(it.code) }
    data.forEach { update(it.toInt() and 0xFF) }
    return crc xor -1
}

private fun adler32(data: ByteArray): Int {
    var a = 1
    var b = 0
    data.forEach {
        a = (a + (it.toInt() and 0xFF)) % 65_521
        b = (b + a) % 65_521
    }
    return (b shl 16) or a
}
