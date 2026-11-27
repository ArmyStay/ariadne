package com.ariadne.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * On-device text recognition via bundled ML Kit modules (no Play Services runtime model download).
 *
 * Latin is always attempted first. Additional bundled script recognizers (Devanagari/Japanese/Korean/Chinese)
 * are attempted when Latin yields no useful text to improve coverage for common trip scripts; scripts not
 * covered by bundled ML Kit v2 modules (e.g. Thai, Arabic, Cyrillic, Greek) may still require manual entry.
 * Failures are returned as [OcrResult.Failure], never thrown to the app/capture path.
 */
internal class MlKitTextRecognizer(
    private val context: Context,
    private val maxEdge: Int = 1600,
    private val timeoutMs: Long = 8_000L,
) : TextRecognizer {

    private val recognizers by lazy {
        listOf(
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
            TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        )
    }

    override suspend fun recognize(imagePath: String): OcrResult = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(imagePath)
            if (!file.exists()) return@withContext OcrResult.Failure("Photo file missing")
            val bitmap = decodeAndDownscale(file, maxEdge)
                ?: return@withContext OcrResult.Failure("Unsupported image")

            try {
                val input = InputImage.fromBitmap(bitmap, 0)
                val primary = recognizeWith(recognizers.first(), input)
                val primaryLines = primary ?: emptyList()
                if (primaryLines.isNotEmpty()) {
                    return@withContext OcrResult.Text(primaryLines)
                }

                // Try additional bundled script recognizers and merge any non-empty lines.
                val merged = linkedSetOf<String>()
                merged.addAll(primaryLines)
                for (recognizer in recognizers.drop(1)) {
                    val lines = recognizeWith(recognizer, input).orEmpty()
                    merged.addAll(lines.filter { it.isNotBlank() })
                }
                if (merged.isNotEmpty()) OcrResult.Text(merged.toList()) else OcrResult.Text(emptyList())
            } finally {
                bitmap.recycle()
            }
        }.getOrElse { error ->
            OcrResult.Failure(error.message ?: "Text recognition failed")
        }
    }

    private suspend fun recognizeWith(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        image: InputImage,
    ): List<String>? {
        val result = withTimeoutOrNull(timeoutMs) {
            runCatching { recognizer.process(image).awaitTask() }.getOrNull()
        } ?: return null
        val lines = result.textBlocks
            .flatMap { block -> block.lines.map { it.text.trim() }.filter { it.isNotEmpty() } }
            .ifEmpty { result.text.lines().map { it.trim() }.filter { it.isNotEmpty() } }
        return lines
    }
}

private fun decodeAndDownscale(file: File, maxEdge: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val srcW = bounds.outWidth
    val srcH = bounds.outHeight
    if (!safeImageBounds(srcW, srcH) || file.length() > MAX_COMPRESSED_IMAGE_BYTES) return null

    val sample = calculateInSampleSize(srcW, srcH, maxEdge, maxEdge)
    val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
    val rotated = applyExifRotation(file, decoded)
    return scaleToMaxEdge(rotated, maxEdge)
}

private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
    var inSampleSize = 1
    var halfH = srcH / 2
    var halfW = srcW / 2
    while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
        inSampleSize *= 2
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val longEdge = max(w, h)
    if (longEdge <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longEdge.toFloat()
    val nw = (w * scale).roundToInt().coerceAtLeast(1)
    val nh = (h * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    if (scaled != bitmap) bitmap.recycle()
    return scaled
}

private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
    val orientation = runCatching {
        ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated != bitmap) bitmap.recycle()
    return rotated
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { error ->
        if (cont.isActive) cont.resumeWithException(error)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
