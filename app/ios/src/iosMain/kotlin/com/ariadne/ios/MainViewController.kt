package com.ariadne.ios

import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ariadne.app.AppGraph
import com.ariadne.app.AppReviewManager
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIViewController
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Swift calls this once: `MainViewControllerKt.MainViewController()`. */
fun MainViewController(): UIViewController {
    val graph = AppGraph(IosServices(), SystemClock())
    return ComposeUIViewController {
        AriadneApp(model = graph.model, wide = false)
    }
}

private class IosServices : PlatformServices {

    override fun store(): com.ariadne.store.Store = com.ariadne.store.AriadneStore(
        driver = NativeSqliteDriver(
            schema = com.ariadne.store.db.AriadneDb.Schema,
            name = "ariadne.db",
        ),
    )

    override fun recognizer(): TextRecognizer = VisionTextRecognizer()

    override fun close() = Unit
}

@OptIn(ExperimentalForeignApi::class)
private class VisionTextRecognizer : TextRecognizer {
    override suspend fun recognize(imagePath: String): OcrResult = suspendCoroutine { cont ->
        try {
            val url = NSURL.fileURLWithPath(imagePath)
            val request = VNRecognizeTextRequest { request, error ->
                if (error != null) {
                    cont.resume(OcrResult.Failure(error.localizedDescription ?: "Vision recognition failed"))
                    return@VNRecognizeTextRequest
                }
                val observations = request?.results?.filterIsInstance<VNRecognizedTextObservation>().orEmpty()
                val lines = observations.mapNotNull { observation ->
                    observation.topCandidates(1u).firstOrNull()?.string?.trim()?.takeIf { it.isNotEmpty() }
                }
                cont.resume(OcrResult.Text(lines))
            }.apply {
                recognitionLevel = VNRequestTextRecognitionLevelAccurate
                usesLanguageCorrection = false
                recognitionLanguages = listOf("en-US")
            }
            val handler = VNImageRequestHandler(url, options = emptyMap<Any?, Any?>())
            // If performRequests fails synchronously, report failure; otherwise the completion handler resumes.
            val started = runCatching { handler.performRequests(listOf(request), null) }.getOrDefault(false)
            if (!started) {
                cont.resume(OcrResult.Failure("Vision request could not be started"))
            }
        } catch (t: Throwable) {
            cont.resume(OcrResult.Failure(t.message ?: "Vision recognition failed"))
        }
    }
}

/** iOS review: the native system-managed prompt. */
actual fun appReviewManager(): AppReviewManager = object : AppReviewManager {
    override fun requestReview() = SKStoreReviewController.requestReview()
}
