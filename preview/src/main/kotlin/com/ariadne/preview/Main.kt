package com.ariadne.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.ariadne.app.AppGraph
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import com.ariadne.store.InMemoryStore

/**
 * The preview app.
 *
 * A real desktop window over the real shared UI, with the in-memory store, so the
 * whole product can be walked through without Gradle, a database, an emulator or
 * a phone. This is the fastest way to see whether the design is right, and it is
 * also the surface the contest demo runs on.
 *
 * It is a preview target, not a shipping one: `:app:desktop` is the real desktop
 * app and it uses SQLDelight.
 */
fun main(args: Array<String>) {
    val narrow = args.contains("--phone")
    val dark = args.contains("--night")
    val graph = AppGraph(PreviewServices(), SystemClock())

    application {
        Window(
            onCloseRequest = {
                graph.close()
                exitApplication()
            },
            title = if (narrow) "Ariadne — phone" else "Ariadne",
            state = WindowState(
                size = if (narrow) DpSize(420.dp, 860.dp) else DpSize(1280.dp, 880.dp),
            ),
        ) {
            LaunchedEffect(Unit) { graph.model.start() }
            Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                AriadneApp(model = graph.model, wide = !narrow, dark = dark)
            }
        }
    }
}

/**
 * Everything the shared UI needs, with no platform dependencies at all: the
 * in-memory store and an OCR recognizer that reports a clean failure, which is
 * the same thing a phone in a dead zone reports.
 */
private class PreviewServices : PlatformServices {
    private val store = InMemoryStore()

    override fun store(): com.ariadne.store.Store = store

    override fun recognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(imagePath: String): OcrResult =
            OcrResult.Failure("preview build: OCR not bound")
    }

    override fun close() = Unit
}
