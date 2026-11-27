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
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIPasteboard
import platform.UIKit.popoverPresentationController
import platform.UIKit.UIViewController
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.CoreGraphics.CGRectMake
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRecognizedText
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

    /**
     * The system pasteboard.
     *
     * Read back after writing rather than trusting the setter: iOS will refuse the
     * write in some contexts, and "copied" must mean copied.
     */
    override fun clipboard(): com.ariadne.app.Clipboard = object : com.ariadne.app.Clipboard {
        override fun copy(text: String): Boolean {
            val pasteboard = UIPasteboard.generalPasteboard
            pasteboard.string = text
            return pasteboard.string == text
        }
    }

    override fun reviewManager(): AppReviewManager = object : AppReviewManager {
        override fun requestReview() = SKStoreReviewController.requestReview()
    }

    override fun close() = Unit
}

/**
 * Write [bytes] where [path] says.
 *
 * POSIX rather than `NSData` + `writeToFile`: the Foundation factory names do not
 * resolve in this module's interop, and a file is a file. Two calls, no bridging.
 */
@OptIn(ExperimentalForeignApi::class)
private fun writeBytes(path: String, bytes: ByteArray): Boolean = memScoped {
    val file = fopen(path, "wb") ?: return@memScoped false
    try {
        if (bytes.isEmpty()) return@memScoped true
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file) == bytes.size.toULong()
        }
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosTripCardImageExporter(
    private val hostController: () -> UIViewController,
) : TripCardImageExporter {
    override suspend fun savePng(fileName: String, bytes: ByteArray): Boolean {
        val path = NSTemporaryDirectory() + fileName
        check(writeBytes(path, bytes)) {
            "The Trip Card PNG could not be written to temporary storage."
        }
        val url = NSURL.fileURLWithPath(path)
        val activity = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        val host = hostController()
        host.view?.let { view ->
            activity.popoverPresentationController?.apply {
                sourceView = view
                sourceRect = CGRectMake(
                    CGRectGetMidX(view.bounds),
                    CGRectGetMidY(view.bounds),
                    0.0,
                    0.0,
                )
            }
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
        check(writeBytes(path, bytes)) {
            "The Ariadne archive could not be written to temporary storage."
        }
        val activity = UIActivityViewController(
            activityItems = listOf(NSURL.fileURLWithPath(path)),
            applicationActivities = null,
        )
        val host = hostController()
        host.view?.let { view ->
            activity.popoverPresentationController?.apply {
                sourceView = view
                sourceRect = CGRectMake(
                    CGRectGetMidX(view.bounds),
                    CGRectGetMidY(view.bounds),
                    0.0,
                    0.0,
                )
            }
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
                    // `topCandidates` is bridged as a list of `Any`, so the candidate
                    // has to be named before it can be read.
                    observation.topCandidates(1u)
                        .filterIsInstance<VNRecognizedText>()
                        .firstOrNull()
                        ?.string?.trim()?.takeIf { it.isNotEmpty() }
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
