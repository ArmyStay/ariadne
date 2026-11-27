package com.ariadne.app

import com.ariadne.core.time.Clock
import com.ariadne.extract.ExtractionService
import com.ariadne.extract.ReceiptParser
import com.ariadne.model.CurrencyCode
import com.ariadne.queue.Outbox
import com.ariadne.search.SearchIndex
import com.ariadne.store.Store
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The object graph, built by hand, once per target.
 *
 * No container and no reflection: dependencies are visible in this constructor.
 * Platform-specific capabilities live at the edges ([PlatformServices]), so the
 * rest of the app is plain Kotlin, and the store arrives as an interface so the
 * same graph runs against SQLDelight on a device and against [InMemoryStore] in
 * a test or a preview.
 */
public class AppGraph(
    private val platform: PlatformServices,
    private val clock: Clock,
    private val initialCaptureBytes: ByteArray? = null,
) {
    /**
     * Every failure in the model is reported here before it disappears.
     *
     * A launch that throws inside a SupervisorJob is swallowed — which is how a
     * missing column became "the button does nothing" on a device with no crash
     * and no log to explain it. The handler does not make the app fail loudly to
     * the user; it makes the failure visible to whoever is looking.
     */
    private val failures = CoroutineExceptionHandler { _, error ->
        lastFailure = error
        AppLogger.e("ariadne: ${error::class.simpleName}: ${error.message}", error, "AppGraph")
    }

    /**
     * The last error anything in the app saw, for the diagnostic line in
     * Preferences. Written from both the coroutine handler and the model's own
     * `runCatching`, so a failure that was caught and reported to the page is
     * still visible afterwards.
     */
    public var lastFailure: Throwable? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + failures)
    private var idCounter = 0L
    private var baseCurrency: CurrencyCode = CurrencyCode.of("INR")

    public val store: Store = platform.store()

    public val outbox: Outbox = Outbox(store, clock::nowMillis)

    public val index: SearchIndex = SearchIndex()

    public lateinit var extraction: ExtractionService
        private set

    public val model: AppModel

    init {
        extraction = ExtractionService(
            recognizer = platform.recognizer(),
            repository = store,
            outbox = outbox,
            parserFor = { base -> ReceiptParser(base) },
            baseCurrency = { baseCurrency },
            extractionDispatcher = Dispatchers.Default,
            onSettled = { model.refreshAfterExtraction() },
        )
        model = AppModel(
            store = store,
            outbox = outbox,
            extraction = extraction,
            index = index,
            files = files(),
            clipboard = platform.clipboard(),
            reviewManager = platform.reviewManager(),
            reminders = platform.reminders(),
            clock = clock,
            scope = scope,
            // Time-ordered ids sort the way the photographs were taken, so the record
            // and the queue agree without a sequence table.
            newId = { "c-${clock.nowMillis()}-${idCounter++}" },
            rateSource = platform.rateSource(),
            storageDisclosure = platform.storageDisclosure,
            rateDisclosure = platform.rateDisclosure,
            initialCaptureBytes = initialCaptureBytes,
        ).also { m -> m.onFailure = { lastFailure = it } }
    }


    public fun setBaseCurrency(currency: CurrencyCode) {
        baseCurrency = currency
    }

    /** The photo pipeline. Null on previews and tests that never capture. */
    /**
     * The photo pipeline. Null when the platform has not supplied one, in which
     * case a capture is still recorded but its bytes are not written — the record
     * survives, which is the half that matters.
     */
    public fun files(): PhotoFiles? = platform.files()

    public fun photos(): PlatformPhotos? = platform as? PlatformPhotos

    public fun close() {
        scope.cancel()
        platform.close()
    }
}

/**
 * What each platform must supply.
 *
 * Platform implementations supply their native services here. Optional
 * capabilities like camera and file import use separate interfaces so a target
 * can adopt them without coupling the common graph to platform APIs.
 */
public interface PlatformServices {
    public fun store(): Store
    public fun recognizer(): com.ariadne.extract.TextRecognizer

    /** What this build actually persists locally, stated for Preferences. */
    public val storageDisclosure: String
        get() = "This preview keeps data in memory for this session only."

    /** How exchange rates are sourced and retained on this target. */
    public val rateDisclosure: String
        get() = "Rates are entered manually and kept in the current preview session."

    /** Where photographs are written. Null for a target that has no file system. */
    public fun files(): PhotoFiles? = null

    /** The system clipboard. Null where there is not one. */
    public fun clipboard(): Clipboard? = null

    /** A native in-app review prompt where the platform supports one. */
    public fun reviewManager(): AppReviewManager = NoAppReviewManager

    /**
     * The evening reminder, as this platform can deliver it. Null where a target has
     * nothing to deliver it with — desktop and iOS today, which still get the in-app
     * line while they are open.
     */
    public fun reminders(): Reminders? = null

    /**
     * Where exchange rates are fetched from, when the platform can reach a network.
     *
     * Optional, and null is a working state: the app keeps the rates it already has
     * and converts with the last known figure. A trip in a dead zone must not lose
     * its totals because a fetch failed.
     */
    public fun rateSource(): com.ariadne.fx.RateSource? = null

    public fun close()
}

/**
 * A daily reminder, delivered the only way a device with no server can deliver one.
 *
 * Three separate questions, because the answers genuinely differ by platform and a
 * single `Boolean` would flatten them into a lie:
 *
 *  - [available] — can this build put a line on screen while the app is closed?
 *  - [permitted] — has the person allowed it? (Android 13+, and every browser.)
 *  - [schedule] — arm it; true only when the platform will really wake up by itself.
 *
 * A browser cannot promise delivery with the tab closed (that needs a service worker
 * and a push service, which is a server), so it returns false from [schedule] and the
 * app says so instead of pretending. Android's alarm survives the app being killed; it
 * does not survive a reboot, and the reminder is re-armed the next time the app opens.
 */
public interface Reminders {
    public val available: Boolean
    public val permitted: Boolean

    /** Asks the system. [then] runs once, with whether it is allowed now. */
    public fun request(then: (Boolean) -> Unit)

    /** Arms a daily delivery at [hour]:[minute]. False when the platform will not. */
    public fun schedule(hour: Int, minute: Int): Boolean

    public fun cancel()

    /**
     * Posts one immediately, where the platform allows a notification without an alarm.
     * Used by the in-app loop on a target that cannot schedule; false elsewhere.
     */
    public fun notifyNow(title: String, body: String): Boolean = false
}

/**
 * Optional camera/import surface. A platform that can produce image bytes
 * implements this; `AriadneApp` uses it when present and falls back to the
 * placeholder plate when it is not, so the UI is identical either way.
 */
public interface PlatformPhotos {
    public suspend fun capturePhotoBytes(): ByteArray?

    public suspend fun pickPhotoBytes(): ByteArray?
}
