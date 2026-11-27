package com.ariadne.android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ariadne.app.AppGraph
import com.ariadne.app.AppLogger
import com.ariadne.app.AppReviewManager
import com.ariadne.app.PlatformServices
import com.ariadne.app.ui.AriadneApp
import com.ariadne.app.ui.pages.LocalCamera
import com.ariadne.app.ui.pages.LocalVoiceEntry
import com.ariadne.app.ui.pages.LocalWearableCapture
import com.ariadne.app.ui.pages.LocalPhotoImageRenderer
import com.ariadne.app.ui.pages.LocalPhotoPicker
import com.ariadne.app.ui.pages.LocalTripCardImageExporter
import com.ariadne.app.ui.pages.LocalDataArchiveExporter
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.TextRecognizer
import com.meta.wearable.dat.core.Wearables
import android.speech.RecognizerIntent
import com.google.android.play.core.review.ReviewManagerFactory

class MainActivity : ComponentActivity() {

    private lateinit var graph: AppGraph
    private lateinit var wearable: AndroidWearableCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate(); shows the system splash
        // (the activity theme is @style/Theme.App.Starting) until the app is up.
        installSplashScreen()
        AppLogger.initialize(BuildConfig.DEBUG)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startMock = intent.getBooleanExtra("mock_glasses", false) || intent.hasExtra("mock_glasses")
        // The DAT has to be initialized once, before any other call, and before the
        // glasses object can answer anything.
        runCatching { Wearables.initialize(applicationContext) }
        wearable = AndroidWearableCapture(this, startMockServer = startMock)
        wearable.attach(this)
        val voice = AndroidVoiceEntry(this)
        val initialBytes = intent.getByteArrayExtra("glasses_capture_bytes")
        graph = AppGraph(AndroidServices(this), SystemClock(), initialCaptureBytes = initialBytes)
        val camera = AndroidCamera(this, this)
        setContent {
            // The amount said out loud: the recogniser is another app, so it answers
            // through this launcher and the transcript lands back in `voice`.
            val speech = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                val heard = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                voice.deliver(heard)
            }
            SideEffect { voice.launch = { wanted -> speech.launch(wanted) } }

            // The one place a camera is attached to the shared UI. Every other
            // target leaves the local empty and gets the ruled plate.
            CompositionLocalProvider(
                LocalVoiceEntry provides voice,
                LocalCamera provides camera,
                LocalWearableCapture provides wearable,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val startMock = intent.getBooleanExtra("mock_glasses", false) || intent.hasExtra("mock_glasses")
        if (startMock && ::wearable.isInitialized) {
            wearable.enableMockServer()
        }
        val incomingBytes = intent.getByteArrayExtra("glasses_capture_bytes")
        if (incomingBytes != null && incomingBytes.isNotEmpty() && ::graph.isInitialized) {
            graph.model.capturePhoto(incomingBytes)
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
        "Trip records, receipt photographs, OCR text, merchant names, and notes stay in Ariadne's private app storage on this device. Nothing is synced."

    override val rateDisclosure: String =
        "Only when you choose Fetch exchange rates now, Ariadne requests rates from frankfurter.app. The provider receives currency pairs and your IP address; the pairs can reveal currencies used on your trip. Saved rates remain available offline."

    override fun store(): com.ariadne.store.Store = com.ariadne.store.AriadneStore(
        driver = openAndroidDatabase(activity, "ariadne.db"),
    )

    override fun recognizer(): TextRecognizer = MlKitTextRecognizer(activity.applicationContext)

    // Exchange rates use a restricted network client. Voice uses a separate recognizer app.
    override fun rateSource(): com.ariadne.fx.RateSource = AndroidRateSource()

    override fun files(): com.ariadne.app.PhotoFiles = AndroidFiles(activity)

    override fun clipboard(): com.ariadne.app.Clipboard = AndroidClipboard(activity)

    /**
     * The evening reminder: an alarm the phone keeps after the app is closed.
     *
     * Lazy and single: [AndroidReminders] registers an activity-result launcher, and a
     * launcher may only be registered before the activity is started — and only once.
     */
    private val evening: AndroidReminders by lazy { AndroidReminders(activity) }

    override fun reminders(): com.ariadne.app.Reminders = evening

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
