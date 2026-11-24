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
 * No container and no reflection: nine dependencies, all visible in this
 * constructor. `expect/actual` lives at the edges ([PlatformServices]) so the
 * rest of the app is plain Kotlin, and the store arrives as an interface so the
 * same graph runs against SQLDelight on a device and against [InMemoryStore] in
 * a test or a preview.
 */
public class AppGraph(
    private val platform: PlatformServices,
    private val clock: Clock,
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
        println("ariadne: ${error::class.simpleName}: ${error.message}")
        error.printStackTrace()
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
            clock = clock,
            scope = scope,
            // Time-ordered ids sort the way the photographs were taken, so the record
            // and the queue agree without a sequence table.
            newId = { "c-${clock.nowMillis()}-${idCounter++}" },
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
 * Four implementations, and they differ in exactly one thing: which store they
 * hand over. Everything else — camera, OCR, file import — arrives later through
 * optional interfaces like [PlatformPhotos] so a target can adopt them one at a
 * time without the graph changing shape.
 */
public interface PlatformServices {
    public fun store(): Store
    public fun recognizer(): com.ariadne.extract.TextRecognizer

    /** Where photographs are written. Null for a target that has no file system. */
    public fun files(): PhotoFiles? = null

    /** The system clipboard. Null where there is not one. */
    public fun clipboard(): Clipboard? = null

    public fun close()
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
