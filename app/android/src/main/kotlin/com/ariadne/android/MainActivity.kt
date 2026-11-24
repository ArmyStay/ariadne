package com.ariadne.android

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ariadne.app.AppGraph
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.app.ui.pages.LocalCamera
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer

class MainActivity : ComponentActivity() {

    private lateinit var graph: AppGraph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        graph = AppGraph(AndroidServices(this), SystemClock())
        val camera = AndroidCamera(this, this)
        setContent {
            // The one place a camera is attached to the shared UI. Every other
            // target leaves the local empty and gets the ruled plate.
            CompositionLocalProvider(LocalCamera provides camera) {
                AriadneApp(model = graph.model, wide = false, reducedMotion = reducedMotion())
            }
        }
    }

    /** The platform's own answer: animator duration scale 0 means "don't". */
    private fun reducedMotion(): Boolean = try {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    } catch (e: Settings.SettingNotFoundException) {
        false
    }

    override fun onDestroy() {
        graph.close()
        super.onDestroy()
    }
}

private class AndroidServices(
    private val activity: ComponentActivity,
) : PlatformServices {

    override fun store(): com.ariadne.store.Store = com.ariadne.store.AriadneStore(
        driver = AndroidSqliteDriver(
            schema = com.ariadne.store.db.AriadneDb.Schema,
            context = activity,
            name = "ariadne.db",
        ),
    )

    override fun recognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(imagePath: String): OcrResult =
        // ML Kit binds here: TextRecognition on the downscaled bitmap, lines out.
        // A failure keeps the capture queued and the field blank, which is the
        // designed behaviour rather than a stub wearing a hat.
        OcrResult.Failure("ML Kit recognizer not yet bound")
    }

    override fun files(): com.ariadne.app.PhotoFiles = AndroidFiles(activity)

    override fun clipboard(): com.ariadne.app.Clipboard = AndroidClipboard(activity)

    override fun close() = Unit
}
