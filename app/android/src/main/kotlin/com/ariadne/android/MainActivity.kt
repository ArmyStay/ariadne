package com.ariadne.android

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ariadne.app.AppGraph
import com.ariadne.app.AppLogger
import com.ariadne.app.AppReviewManager
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.app.ui.pages.LocalCamera
import com.ariadne.app.ui.pages.LocalPhotoImageRenderer
import com.ariadne.app.ui.pages.LocalPhotoPicker
import com.ariadne.app.ui.pages.LocalTripCardImageExporter
import com.ariadne.app.ui.pages.LocalDataArchiveExporter
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.TextRecognizer
import com.google.android.play.core.review.ReviewManagerFactory

class MainActivity : ComponentActivity() {

    private lateinit var graph: AppGraph

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate(); shows the system splash
        // (the activity theme is @style/Theme.App.Starting) until the app is up.
        installSplashScreen()
        AppLogger.initialize()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        graph = AppGraph(AndroidServices(this), SystemClock())
        val camera = AndroidCamera(this, this)
        setContent {
            // The one place a camera is attached to the shared UI. Every other
            // target leaves the local empty and gets the ruled plate.
            CompositionLocalProvider(
                LocalCamera provides camera,
                LocalPhotoImageRenderer provides AndroidPhotoImageRenderer,
                // The library, on the one target that has a system picker bound.
                // No storage permission is needed for it: the person hands over a
                // single image rather than opening their library to the app.
                LocalPhotoPicker provides rememberAndroidPhotoPicker(this@MainActivity),
                LocalTripCardImageExporter provides rememberAndroidTripCardImageExporter(this@MainActivity),
                LocalDataArchiveExporter provides rememberAndroidDataArchiveExporter(this@MainActivity),
            ) {
                AriadneApp(
                    model = graph.model,
                    wide = false,
                    reducedMotion = reducedMotion(),
                    // The system back gesture, wired to the app's own stack. When
                    // the stack is empty the handler is disabled and the platform
                    // closes the app, which is what Back at a root destination
                    // should do.
                    onBack = { enabled, onBack ->
                        androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
                    },
                )
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

    override val storageDisclosure: String =
        "Trips, rates, and receipt photographs are stored in Ariadne's private app storage on this device. Nothing is synced."

    override val rateDisclosure: String =
        "When online, Ariadne requests currency rates from frankfurter.app using currency pairs only, not trip details. Saved rates remain available offline; the date shown is the rate's effective date."

    override fun store(): com.ariadne.store.Store = com.ariadne.store.AriadneStore(
        driver = AndroidSqliteDriver(
            schema = com.ariadne.store.db.AriadneDb.Schema,
            context = activity,
            name = "ariadne.db",
        ),
    )

    override fun recognizer(): TextRecognizer = MlKitTextRecognizer(activity.applicationContext)

    // The only outbound request in the app: exchange rates. See AndroidRateSource.
    override fun rateSource(): com.ariadne.fx.RateSource = AndroidRateSource()

    override fun files(): com.ariadne.app.PhotoFiles = AndroidFiles(activity)

    override fun clipboard(): com.ariadne.app.Clipboard = AndroidClipboard(activity)

    override fun reviewManager(): AppReviewManager {
        val manager = ReviewManagerFactory.create(activity.applicationContext)
        return object : AppReviewManager {
            override fun requestReview() {
                manager.requestReviewFlow().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        manager.launchReviewFlow(activity, task.result)
                            .addOnFailureListener { e -> AppLogger.e("InApp review launch failed", e, "AppReviewManager") }
                    } else {
                        AppLogger.e("InApp review request failed", task.exception, "AppReviewManager")
                    }
                }
            }
        }
    }

    override fun close() = Unit
}

// MlKitTextRecognizer extracted to MlKitTextRecognizer.kt for testability and to keep MainActivity focused on wiring.
