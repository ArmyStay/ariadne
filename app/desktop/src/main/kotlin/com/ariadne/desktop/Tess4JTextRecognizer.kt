package com.ariadne.desktop

import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.sourceforge.tess4j.Tesseract
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Desktop OCR via Tess4J/Tesseract.
 *
 * Requires `eng.traineddata` to be discoverable via TESSDATA_PREFIX, a local `tessdata/` directory,
 * or bundled resources/configuration on the host machine. If unavailable, recognition fails gracefully.
 */
internal class Tess4JTextRecognizer(
    private val language: String = "eng",
    private val dataPath: String? = resolveDataPath(),
) : TextRecognizer {

    override suspend fun recognize(imagePath: String): OcrResult = withContext(Dispatchers.IO) {
        withTimeoutOrNull(8_000L) {
            runCatching {
                val file = File(imagePath)
                if (!file.exists()) return@runCatching OcrResult.Failure("Photo file missing")
                val image = ImageIO.read(file) ?: return@runCatching OcrResult.Failure("Unsupported image")
                val scaled = downscale(image, 1600)
                val tesseract = Tesseract().apply {
                    setLanguage(language)
                    dataPath?.let { setDatapath(it) }
                    // Receipts benefit from sparse text/page segmentation in many cases; default is acceptable fallback.
                    setPageSegMode(6)
                }
                val text = tesseract.doOCR(scaled).orEmpty()
                val lines = text.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                OcrResult.Text(lines)
            }.getOrElse { error ->
                OcrResult.Failure(error.message ?: "Desktop OCR failed")
            }
        } ?: OcrResult.Failure("Text recognition timed out")
    }

    private fun downscale(image: BufferedImage, maxEdge: Int): BufferedImage {
        val w = image.width
        val h = image.height
        val longEdge = max(w, h)
        if (longEdge <= maxEdge) return image
        val scale = maxEdge.toDouble() / longEdge.toDouble()
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        val out = BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(image, 0, 0, nw, nh, null)
        } finally {
            g.dispose()
        }
        return out
    }

    companion object {
        private fun resolveDataPath(): String? {
            val env = System.getenv("TESSDATA_PREFIX")?.takeIf { it.isNotBlank() }
            if (env != null) return env
            val candidates = listOf(
                File("tessdata"),
                File(System.getProperty("user.dir") ?: ".", "tessdata"),
                File(System.getProperty("user.home") ?: ".", ".ariadne/tessdata"),
            )
            return candidates.firstOrNull { it.exists() && it.isDirectory }?.absolutePath
        }
    }
}