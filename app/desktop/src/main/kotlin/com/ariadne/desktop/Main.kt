package com.ariadne.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ariadne.app.AppGraph
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import java.io.File

/**
 * Desktop entry point.
 *
 * Desktop is the comfortable triage surface: full keyboard, a real window, drag
 * and drop. It is where the demo's evening half runs.
 */
fun main() {
    val graph = AppGraph(DesktopServices(), SystemClock())
    application {
        Window(
            onCloseRequest = {
                graph.close()
                exitApplication()
            },
            title = "Ariadne",
            state = WindowState(size = DpSize(1180.dp, 820.dp)),
        ) {
            LaunchedEffect(Unit) { graph.model.start() }
            AriadneApp(model = graph.model, wide = true)
        }
    }
}

private class DesktopServices : PlatformServices {
    private val dbFile = File(System.getProperty("user.home"), ".ariadne/ariadne.db")

    override fun store(): com.ariadne.store.Store = com.ariadne.store.AriadneStore(
        driver = run {
            dbFile.parentFile?.mkdirs()
            JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}").also { com.ariadne.store.Schema.create(it) }
        },
    )

    override fun recognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(imagePath: String): OcrResult =
        OcrResult.Failure("desktop OCR (Tesseract) lands in the next commit")
    }

    override fun close() = Unit
}
