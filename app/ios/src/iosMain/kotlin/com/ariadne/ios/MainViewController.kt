package com.ariadne.ios

import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ariadne.app.AppGraph
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import platform.UIKit.UIViewController

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
            schema = com.ariadne.store.Schema,
            name = "ariadne.db",
        ),
    )

    override fun recognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(imagePath: String): OcrResult =
        // Vision's VNRecognizeTextRequest lands here on Dec 3 behind the same
        // interface; nothing above this line changes.
        OcrResult.Failure("Vision recognizer not yet bound")
    }

    override fun close() = Unit
}
