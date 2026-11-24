package com.ariadne.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import app.cash.sqldelight.db.SqlDriver
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
            // The system file dialog, supplied to the shared UI exactly as the
            // camera is on Android: the page asks "which image?", the target
            // answers with whatever its platform calls a picker.
            ProvideDesktopPhotoPicker(null) {
                AriadneApp(model = graph.model, wide = true)
            }
        }
    }
}

private class DesktopServices : PlatformServices {
    private val dbFile = File(System.getProperty("user.home"), ".ariadne/ariadne.db")

    override fun store(): com.ariadne.store.Store = com.ariadne.store.AriadneStore(
        driver = run {
            dbFile.parentFile?.mkdirs()
            val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
            // Create at the current version, migrate a database that predates it,
            // or leave a current one alone. See `openOrMigrate`: a file can exist
            // and still report version 0, because no early build wrote the pragma.
            com.ariadne.store.openOrMigrate(driver)
            // The one shape that cannot be migrated honestly: an early build ran
            // `create`, never wrote the version, and the tables have since grown
            // columns. Probe for it, and if it is there move the file aside - kept,
            // not deleted, because it may hold a trip someone cares about - and
            // start clean so the app opens instead of failing under a window.
            if (com.ariadne.store.db.AriadneDb.Schema.version > 0 && !hasCurrentColumns(driver)) {
                val aside = File(dbFile.parentFile, "ariadne-unusable-${System.currentTimeMillis()}.db")
                println("ariadne: this database predates versioning and cannot be migrated; moved to ${aside.name}")
                driver.close()
                dbFile.renameTo(aside)
                val fresh = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
                com.ariadne.store.openOrMigrate(fresh)
                return@run fresh
            }
            driver
        },
    )

    /**
     * Every column the current schema expects, actually present?
     *
     * Cheaper and more honest than a version number here: the whole defect this
     * guards is a version number that did not move when the columns did.
     */
    private fun hasCurrentColumns(driver: app.cash.sqldelight.db.SqlDriver): Boolean = try {
        fun columnsOf(table: String): Set<String> = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($table)",
            mapper = { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) cursor.getString(1)?.let { names += it }
                app.cash.sqldelight.db.QueryResult.Value(names)
            },
            parameters = 0,
        ).value
        val trip = columnsOf("trip")
        val capture = columnsOf("capture")
        // An empty table list means the file is new, which is fine: openOrMigrate
        // has already created it by the time this runs.
        (trip.isEmpty() || "place" in trip) && (capture.isEmpty() || "category" in capture)
    } catch (e: Exception) {
        false
    }

    override fun recognizer(): TextRecognizer = Tess4JTextRecognizer()

    override fun close() = Unit
}
