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
    ComposeViewport(body) {
        AriadneApp(
            model = graph.model,
            wide = window.innerWidth > 900,
            reducedMotion = reduced,
        )
    }
}

private class WasmServices : PlatformServices {

    override fun store(): com.ariadne.store.Store =
        // The IndexedDB-backed worker driver is wired on Dec 2 with the Pages
        // deploy; until then the browser build keeps the trip in memory so the
        // whole flow is explorable with no server and no storage permission.
        com.ariadne.store.InMemoryStore()

    override fun recognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(imagePath: String): OcrResult =
        OcrResult.Failure("browser build uses typed amounts")
    }

    override fun close() = Unit
}
