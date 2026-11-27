package com.ariadne.wasm

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.ariadne.app.AppGraph
import com.ariadne.app.DataArchiveExporter
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.app.ui.pages.LocalTripCardImageExporter
import com.ariadne.app.ui.pages.LocalDataArchiveExporter
import com.ariadne.app.ui.pages.TripCardImageExporter
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * The judge's entry point: a browser, nothing installed.
 *
 * No OCR engine is bundled in Wasm. Manual entry remains available, and a host
 * page may provide the optional browser recognition hook below.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val graph = AppGraph(WasmServices(), SystemClock())
    val body = document.body ?: error("Ariadne needs a <body> to mount into")
    val reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ComposeViewport(body) {
        CompositionLocalProvider(
            LocalTripCardImageExporter provides WasmTripCardImageExporter,
            LocalDataArchiveExporter provides WasmDataArchiveExporter,
        ) {
            AriadneApp(
                model = graph.model,
                wide = window.innerWidth > 900,
                reducedMotion = reduced,
            )
        }
    }
}

private class WasmServices : PlatformServices {

    override val storageDisclosure: String =
        "This browser build keeps trips and rates in memory; reloading the page clears them. Receipt photos are not persisted. Trip Card PNGs download to your device."

    override val rateDisclosure: String =
        "This build does not fetch rates online. Rates you enter last only for this page session; the date shown is the saved rate's effective date."

    override fun store(): com.ariadne.store.Store =
        // Browser data is session-local in this build; the disclosure explains
        // that reloads clear the record.
        com.ariadne.store.InMemoryStore()

    override fun recognizer(): TextRecognizer = WasmBrowserRecognizer()

    override fun close() = Unit
}

private object WasmTripCardImageExporter : TripCardImageExporter {
    override suspend fun savePng(fileName: String, bytes: ByteArray): Boolean {
        return downloadPng(fileName, bytes.toBase64())
    }
}

private object WasmDataArchiveExporter : DataArchiveExporter {
    override suspend fun saveZip(fileName: String, bytes: ByteArray): Boolean =
        downloadFile(fileName, "application/zip", bytes.toBase64())
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun downloadPng(fileName: String, base64: String): Boolean = js(
        """(() => {
          const binary = atob(base64);
          const data = new Uint8Array(binary.length);
          for (let i = 0; i < binary.length; i++) data[i] = binary.charCodeAt(i);
          const blob = new Blob([data], { type: 'image/png' });
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = fileName;
          link.click();
          setTimeout(() => URL.revokeObjectURL(url), 1000);
          return true;
        })()""",
    )

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun downloadFile(fileName: String, mimeType: String, base64: String): Boolean = js(
    """(() => {
      const binary = atob(base64);
      const data = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i++) data[i] = binary.charCodeAt(i);
      const blob = new Blob([data], { type: mimeType });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      return true;
    })()""",
)

private fun ByteArray.toBase64(): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    return buildString(((size + 2) / 3) * 4) {
        var index = 0
        while (index < size) {
            val first = this@toBase64[index].toInt() and 0xFF
            val hasSecond = index + 1 < size
            val hasThird = index + 2 < size
            val second = if (hasSecond) this@toBase64[index + 1].toInt() and 0xFF else 0
            val third = if (hasThird) this@toBase64[index + 2].toInt() and 0xFF else 0
            append(alphabet[first ushr 2])
            append(alphabet[((first and 3) shl 4) or (second ushr 4)])
            append(if (hasSecond) alphabet[((second and 15) shl 2) or (third ushr 6)] else '=')
            append(if (hasThird) alphabet[third and 63] else '=')
            index += 3
        }
    }
}

private class WasmBrowserRecognizer : TextRecognizer {
    override suspend fun recognize(imagePath: String): OcrResult =
        OcrResult.Failure("browser OCR is disabled to keep receipt data local")
}
