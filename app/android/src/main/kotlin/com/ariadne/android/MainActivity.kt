package com.ariadne.android

import android.content.Context
import android.net.Uri
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
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

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

    override fun recognizer(): TextRecognizer = MlKitTextRecognizer(activity.applicationContext)

    override fun files(): com.ariadne.app.PhotoFiles = AndroidFiles(activity)

    override fun clipboard(): com.ariadne.app.Clipboard = AndroidClipboard(activity)

    override fun close() = Unit
}

/** On-device text recognition via ML Kit. Failure is returned, never thrown to the app. */
private class MlKitTextRecognizer(private val context: Context) : TextRecognizer {
    private val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(imagePath: String): OcrResult = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(imagePath)
            if (!file.exists()) return@withContext OcrResult.Failure("Photo file missing")
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val result = client.process(image).awaitTask()
            val lines = result.textBlocks
                .flatMap { block -> block.lines.map { it.text.trim() }.filter { it.isNotEmpty() } }
                .ifEmpty { result.text.lines().map { it.trim() }.filter { it.isNotEmpty() } }
            if (lines.isEmpty()) OcrResult.Text(emptyList()) else OcrResult.Text(lines)
        }.getOrElse { error ->
            OcrResult.Failure(error.message ?: "Text recognition failed")
        }
    }
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
