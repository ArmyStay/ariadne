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
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Desktop OCR via Tess4J/Tesseract.
 *
 * Prefers bundled `eng.traineddata` from resources to avoid TESSDATA_PREFIX "works on my machine" issues.
 * All failures (missing native libs, missing traineddata, decode/OCR errors, timeout) return [OcrResult.Failure]
 * so a recognizer problem never crashes the capture path.
 */
internal class Tess4JTextRecognizer(
    private val language: String = "eng",
    private val dataPath: String? = defaultDataPath(),
) : TextRecognizer {

    override suspend fun recognize(imagePath: String): OcrResult = withContext(Dispatchers.IO) {
        withTimeoutOrNull(8_000L) {
            runCatching {
                val file = File(imagePath)
                if (!file.exists()) return@runCatching OcrResult.Failure("Photo file missing")
                if (file.length() > MAX_COMPRESSED_IMAGE_BYTES || !file.hasSafeImageBounds()) {
                    return@runCatching OcrResult.Failure("Image is too large or has unsafe dimensions")
                }
                val image = ImageIO.read(file) ?: return@runCatching OcrResult.Failure("Unsupported image")
                val scaled = downscale(image, 1600)
                val text = Tesseract().apply {
                    setLanguage(language)
                    dataPath?.let { setDatapath(it) }
                    setPageSegMode(6)
                }.doOCR(scaled).orEmpty()
                OcrResult.Text(text.lines().map { it.trim() }.filter { it.isNotEmpty() })
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
        internal fun defaultDataPath(): String? {
            extractBundledTessdata()?.let { return it }
            val env = System.getenv("TESSDATA_PREFIX")?.takeIf { it.isNotBlank() }
            if (env != null) return env
            val candidates = listOf(
                File("tessdata"),
                File(System.getProperty("user.dir") ?: ".", "tessdata"),
                File(System.getProperty("user.home") ?: ".", ".ariadne/tessdata"),
            )
            return candidates.firstOrNull { it.exists() && it.isDirectory }?.absolutePath
        }

        private fun extractBundledTessdata(): String? = runCatching {
            val stream = Tess4JTextRecognizer::class.java.getResourceAsStream("/tessdata/eng.traineddata")
                ?: return@runCatching null
            val dir = File(System.getProperty("java.io.tmpdir"), "ariadne-tessdata").apply { mkdirs() }
            val out = File(dir, "eng.traineddata")
            if (!out.exists() || out.length() == 0L) {
                stream.use { input -> Files.copy(input, out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
            } else {
                stream.close()
            }
            dir.absolutePath
        }.getOrNull()
    }
}
