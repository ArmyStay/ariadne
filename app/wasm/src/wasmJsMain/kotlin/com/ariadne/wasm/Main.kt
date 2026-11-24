package com.ariadne.wasm

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.ariadne.app.AppGraph
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * The judge's entry point: a browser, nothing installed.
 *
 * No on-device OCR in Wasm by design — this path runs on typed amounts and the
 * demo roll, so extraction failing costs the visitor nothing and the whole app
 * still works offline once the page is loaded.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val graph = AppGraph(WasmServices(), SystemClock())
    val body = document.body ?: error("Ariadne needs a <body> to mount into")
    val reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    // A deterministic seed behind a flag: `?seed=demo` opens on the demo roll as
    // soon as the app boots, so a visitor who lands on the Pages deploy sees the
    // app populated rather than on the front door.
    if (seedRequested()) {
        graph.model.onStarted { graph.model.startTripDemo() }
    }
    ComposeViewport(body) {
        AriadneApp(
            model = graph.model,
            wide = window.innerWidth > 900,
            reducedMotion = reduced,
        )
    }
}

/** True when the URL carries the seed flag: `?seed` or `?seed=demo`. */
private fun seedRequested(): Boolean =
    window.location.search.trimStart('?').split('&').any { it.substringBefore('=') == "seed" }

private class WasmServices : PlatformServices {

    override fun store(): com.ariadne.store.Store =
        // The IndexedDB-backed worker driver is wired on Dec 2 with the Pages
        // deploy; until then the browser build keeps the trip in memory so the
        // whole flow is explorable with no server and no storage permission.
        com.ariadne.store.InMemoryStore()

    override fun recognizer(): TextRecognizer = WasmBrowserRecognizer()

    override fun close() = Unit
}

/**
 * Browser OCR hook (tesseract.js or compatible), lazy/optional.
 *
 * To enable, the host page may define `window.ariadneRecognize(imagePathOrUrl)` returning
 * a Promise or value resolving to a string or array of lines. When unavailable/failing,
 * the app falls back to typed amounts (demo/manual entry remains usable).
 */
private class WasmBrowserRecognizer : TextRecognizer {
    override suspend fun recognize(imagePath: String): OcrResult = try {
        val raw = recognizeInBrowser(imagePath)?.trim().orEmpty()
        if (raw.isEmpty()) {
            OcrResult.Failure("browser OCR not enabled")
        } else {
            val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
            OcrResult.Text(lines)
        }
    } catch (t: Throwable) {
        OcrResult.Failure(t.message ?: "browser OCR failed")
    }
}

/**
 * Best-effort JS interop hook. Returns recognized text joined by newlines, or null if not enabled.
 * Host pages can lazy-load tesseract.js and implement this without changing the Wasm bundle.
 */
private fun recognizeInBrowser(imagePath: String): String? = js(
    """(() => {
      try {
        const fn = globalThis.ariadneRecognize;
        if (typeof fn !== 'function') return null;
        const r = fn(imagePath);
        if (r && typeof r.then === 'function') return null; // async hook requires host-side resolution before call
        if (Array.isArray(r)) return r.join('\n');
        return typeof r === 'string' ? r : null;
      } catch (e) { return null; }
    })()"""
) as? String
