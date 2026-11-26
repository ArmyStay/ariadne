package com.ariadne.ios

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ariadne.app.AppGraph
import com.ariadne.app.AppReviewManager
import com.ariadne.app.DataArchiveExporter
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.app.ui.pages.LocalTripCardImageExporter
import com.ariadne.app.ui.pages.LocalDataArchiveExporter
import com.ariadne.app.ui.pages.TripCardImageExporter
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.CoreGraphics.CGRectMake
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Swift calls this once: `MainViewControllerKt.MainViewController()`. */
fun MainViewController(): UIViewController {
    val graph = AppGraph(IosServices(), SystemClock())
    lateinit var host: UIViewController
    host = ComposeUIViewController {
        CompositionLocalProvider(
            LocalTripCardImageExporter provides IosTripCardImageExporter { host },
            LocalDataArchiveExporter provides IosDataArchiveExporter { host },
        ) {
            AriadneApp(model = graph.model, wide = false)
        }
    }
    return host
}

private class IosServices : PlatformServices {

    override val storageDisclosure: String =
        "Trip data is stored in this app's local database. Receipt-photo file storage is not yet available in this iOS build."

    override val rateDisclosure: String =
        "This build does not fetch rates online. Rates you enter are kept in the local database, and the date shown is the saved rate's effective date."

    override fun store(): com.ariadne.store.Store = com.ariadne.store.AriadneStore(
        driver = NativeSqliteDriver(
            schema = com.ariadne.store.db.AriadneDb.Schema,
            name = "ariadne.db",
        ),
    )

    override fun recognizer(): TextRecognizer = VisionTextRecognizer()

    override fun reviewManager(): AppReviewManager = object : AppReviewManager {
        override fun requestReview() = SKStoreReviewController.requestReview()
    }

    override fun close() = Unit
}

@OptIn(ExperimentalForeignApi::class)
private class IosTripCardImageExporter(
    private val hostController: () -> UIViewController,
) : TripCardImageExporter {
    override suspend fun savePng(fileName: String, bytes: ByteArray): Boolean {
        val path = NSTemporaryDirectory() + fileName
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        check(data.writeToFile(path, atomically = true)) {
            "The Trip Card PNG could not be written to temporary storage."
        }
        val url = NSURL.fileURLWithPath(path)
        val activity = UIActivityViewController(listOf(url), null)
        val host = hostController()
        activity.popoverPresentationController?.apply {
            sourceView = host.view
            sourceRect = CGRectMake(
                host.view.bounds.size.width / 2.0,
                host.view.bounds.size.height / 2.0,
                0.0,
                0.0,
            )
        }
        return suspendCoroutine { continuation ->
            activity.completionWithItemsHandler = { _, completed, _, error ->
                if (error != null) {
                    continuation.resumeWithException(
                        IllegalStateException(error.localizedDescription ?: "Trip Card sharing failed."),
                    )
                } else {
                    continuation.resume(completed)
                }
            }
            host.presentViewController(activity, animated = true, completion = null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosDataArchiveExporter(
    private val hostController: () -> UIViewController,
) : DataArchiveExporter {
    override suspend fun saveZip(fileName: String, bytes: ByteArray): Boolean {
        val path = NSTemporaryDirectory() + fileName
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        check(data.writeToFile(path, atomically = true)) {
            "The Ariadne archive could not be written to temporary storage."
        }
        val activity = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
        val host = hostController()
        activity.popoverPresentationController?.apply {
            sourceView = host.view
            sourceRect = CGRectMake(
                host.view.bounds.size.width / 2.0,
                host.view.bounds.size.height / 2.0,
                0.0,
                0.0,
            )
        }
        return suspendCoroutine { continuation ->
            activity.completionWithItemsHandler = { _, completed, _, error ->
                if (error != null) {
                    continuation.resumeWithException(
                        IllegalStateException(error.localizedDescription ?: "Ariadne archive sharing failed."),
                    )
                } else {
                    continuation.resume(completed)
                }
            }
            host.presentViewController(activity, animated = true, completion = null)
        }
    }
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
