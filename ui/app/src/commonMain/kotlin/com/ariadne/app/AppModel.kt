package com.ariadne.app

import com.ariadne.core.time.Clock
import com.ariadne.extract.ExtractionService
import com.ariadne.fx.FxTable
import com.ariadne.fx.Tally
import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.MerchantName
import com.ariadne.model.RecordDates
import com.ariadne.model.Settings
import com.ariadne.model.ShareCodes
import com.ariadne.model.Traveller
import com.ariadne.model.TravellerEntry
import com.ariadne.model.TravellerShare
import com.ariadne.model.Trip
import com.ariadne.model.dayRange
import com.ariadne.queue.Outbox
import com.ariadne.queue.QueueEvent
import com.ariadne.search.SearchIndex
import com.ariadne.store.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * The app's single state holder.
 *
 * Deliberately not a ViewModel: there is one instance per process, it is created
 * by [AppGraph] with explicit constructor arguments, and it owns exactly one
 * supervisor scope. Screens are pure functions of [state].
 */
public class AppModel(
    private val store: Store,
    private val outbox: Outbox,
    private val extraction: ExtractionService,
    private val index: SearchIndex,
    private val files: PhotoFiles? = null,
    private val clipboard: Clipboard? = null,
    private val reviewManager: AppReviewManager = NoAppReviewManager,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val newId: () -> String,
    /** Where rates come from. Null on a platform with no network, or in a test. */
    private val rateSource: com.ariadne.fx.RateSource? = null,
    private val storageDisclosure: String = "This preview keeps data in memory for this session only.",
    private val rateDisclosure: String = "Rates are entered manually and kept in the current preview session.",
    private val initialCaptureBytes: ByteArray? = null,
) {
    private val _state = MutableStateFlow(
        AppUiState(
            storageDisclosure = storageDisclosure,
            rateDisclosure = rateDisclosure,
        ),
    )

    /**
     * How deep Back may go.
     *
     * A phone trip is a few dozen moves; a cap exists only so a session that opens
     * the same page five hundred times cannot grow a list without bound.
     */
    private companion object {
        const val MAX_HISTORY = 24
    }
    public val state: StateFlow<AppUiState> = _state.asStateFlow()

    /**
     * Where a failed write is reported.
     *
     * Set by the graph so the same error the user is shown is the one the
     * diagnostic line in Preferences can quote. Left alone in tests, which is
     * why it is a plain assignable rather than a constructor parameter.
     */
    public var onFailure: ((Throwable) -> Unit)? = null

    /**
     * Records a failure twice: once in state, where the page can show it, and
     * once to the graph, where the console handler can print it. Both, because a
     * failure caught here never reaches the coroutine handler.
     */
    private fun report(failure: Throwable) {
        onFailure?.invoke(failure)
        _state.value = _state.value.copy(
            failureNote = "${failure::class.simpleName}: ${failure.message ?: "no message"}",
        )
    }

    private var fx = FxTable(emptyList())

    /**
     * Rates, in the currency the traveller actually types.
     *
     * The engine in `:core:fx` was always exact - scaled integers, HALF_UP once,
     * no floats - but nothing ever wrote a row into the rate table, so every
     * foreign amount silently failed to convert and the totals quietly fell back
     * to showing the raw figure. A mechanism with no input is a mechanism that
     * does not work, however well it is written.
     */
    public var fxRates: List<com.ariadne.model.Rate> = emptyList()
        private set

    private var settings = Settings()

    /** Re-reads the day/stack/timeline figures after background extraction changes state. */
    public fun refreshAfterExtraction() {
        val trip = _state.value.trip ?: return
        scope.launch {
            val route = _state.value.route
            val day = when (route) {
                is Route.CloseDay -> route.day
                is Route.Record -> route.day ?: _state.value.today
                is Route.Detail -> _state.value.detail?.localDate ?: _state.value.today
                is Route.Frame -> _state.value.detail?.localDate ?: _state.value.today
                else -> _state.value.today
            }
            if (day != null) runCatching { refreshDay(day) }
            runCatching { loadTripDays(trip) }
        }
    }

    // -- rates -------------------------------------------------------------

    /**
     * Fetches the rates this trip needs, when they are worth fetching.
     *
     * Three rules, each of which used to be missing:
     *
     *  - one request covers every foreign currency the trip has used;
     *  - a rate younger than [com.ariadne.fx.RatePolicy.MAX_AGE_DAYS] is not
     *    re-fetched, so opening the app five times a day is not five requests;
     *  - a failure changes nothing. The table keeps the last known rate, the
     *    amounts keep converting at it, and the UI prints its date.
     *
     * [force] exists for the one deliberate case: the person taps "Update rates".
     */
    public fun refreshRates(force: Boolean = false) {
        val source = rateSource ?: return
        val trip = _state.value.trip ?: return
        val base = trip.baseCurrency
        val quotes = currenciesInUse().mapNotNull { CurrencyCode.parseOrNull(it) }
        scope.launch {
            val today = clock.today()
            val known = runCatching { store.allRates() }.getOrDefault(emptyList())
            val newest = known.filter { it.base == base }
                .groupBy { it.quote }
                .mapValues { (_, rows) -> rows.maxByOrNull { LocalDay.toEpochDay(it.date) }?.date }
            if (!force && !com.ariadne.fx.RatePolicy.shouldFetch(newest, quotes, today)) return@launch
            val url = com.ariadne.fx.Frankfurter.url(base, quotes) ?: return@launch
            val body = runCatching { source.get(url) }.getOrNull() ?: return@launch
            val parsed = com.ariadne.fx.Frankfurter.parse(body, base) ?: return@launch
            runCatching {
                parsed.rates.forEach { store.upsertRate(it) }
                fxRates = store.allRates()
                fx = FxTable(fxRates)
            }.onFailure(::report)
            // New rates change the figures on every page that converts, so the day
            // and the whole trip are re-read rather than left stale behind them.
            _state.value.today?.let { refreshDay(it) }
            loadTripDays(trip)
        }
    }

    // -- lifecycle ---------------------------------------------------------

    public fun start() {
        scope.launch {
            try {
                // Every step below is guarded on its own, and the route is decided
                // even if all of them fail.
                //
                // This ordering is the whole fix for "the landing page is empty and
                // the button does nothing". `outbox.resume()` runs a SELECT over a
                // table that a stale database may not have, and it used to run before
                // the route was chosen - so one SQLiteException left the app on its
                // default screen with no trip state at all, looking like a form whose
                // button was wired to nothing. A failure in the background work must
                // never be able to prevent the app from showing a screen.
                runCatching {
                    fxRates = store.allRates()
                    fx = FxTable(fxRates)
                }.onFailure(::report)
                runCatching { settings = store.settings() }.onFailure(::report)
                // The stock the book is printed on comes from the store, not from a
                // default: a person who chose night paper gets it back before the
                // first page is drawn, so the app never flashes white at launch.
                _state.value = _state.value.copy(theme = settings.theme)
                _state.value = _state.value.copy(displayName = settings.displayName)
                runCatching { outbox.resume() }.onFailure(::report)
                runCatching { extraction.drain() }.onFailure(::report)
                val trips = runCatching { store.trips() }.getOrElse {
                    report(it)
                    emptyList()
                }
                val active = trips.firstOrNull { !it.isClosed } ?: trips.firstOrNull()

                if (initialCaptureBytes != null && initialCaptureBytes.isNotEmpty()) {
                    val trip = active ?: run {
                        val today = clock.today()
                        val newTripId = newId()
                        val defaultTrip = Trip(
                            id = newTripId,
                            name = "Journey",
                            startDate = today,
                            endDate = today.plusDays(6),
                            baseCurrency = settings.baseCurrency,
                            homeCurrency = settings.baseCurrency,
                            place = "",
                            budgetMinor = null,
                        )
                        store.createTrip(defaultTrip)
                        defaultTrip
                    }
                    // A photograph handed in from outside (a share, an intent) is
                    // filed on a day that has happened, or not at all.
                    val today = RecordDates.filingDay(clock.today(), trip.startDate, trip.endDate)
                        ?: return@launch
                    val capture = runCatching { persistPhotograph(trip, today, initialCaptureBytes) }.getOrNull()
                    if (capture != null) {
                        bindTrip(trip)
                        _state.value = _state.value.copy(
                            route = Route.Frame(capture.id),
                            sheetCaptureId = capture.id,
                            detail = capture,
                            detailOrigin = DetailOrigin.Record,
                            isStartingUp = false,
                            trips = store.trips(),
                            today = today,
                        )
                        extraction.drain()
                        refreshDay(today)
                        return@launch
                    }
                }

                val activeOpen = trips.firstOrNull { !it.isClosed }
                if (activeOpen == null) {
                    val today = clock.today()
                    val draft = TripDraft(
                        startIso = today.iso,
                        endIso = today.plusDays(6).iso,
                        currency = settings.baseCurrency.code,
                    )
                    // No open trip. If the device has never held one, the first run
                    // opens on the welcome page; if every trip on disk is closed,
                    // the register is the right page - it is where those trips remain readable.
                    _state.value = _state.value.copy(
                        route = startRoute(hasAnyTrip = trips.isNotEmpty(), hasOpenTrip = false),
                        trips = trips,
                        today = today,
                        draft = draft,
                    )
                } else {
                    runCatching { bindTrip(activeOpen) }.onFailure { error ->
                        report(error)
                        // The trip is on disk but could not be read. Say so on the
                        // front door rather than showing an app with no trip in it.
                        _state.value = _state.value.copy(
                            route = Route.NewTrip,
                            today = clock.today(),
                            draft = TripDraft(
                                startIso = clock.today().iso,
                                endIso = clock.today().plusDays(6).iso,
                                currency = settings.baseCurrency.code,
                                error = "Your trip could not be opened. " +
                                    (error.message ?: "Unknown error"),
                            ),
                        )
                    }
                }
                // Routes and the saved theme are now ready to appear together.
                _state.value = _state.value.copy(isStartingUp = false)
                // Rates last, and off the launch path: a slow or missing network
                // cannot keep the startup page visible.
                runCatching { refreshRates() }.onFailure(::report)
                onStartedAction?.invoke()
            } finally {
                _state.value = _state.value.copy(isStartingUp = false)
            }
        }
    }

    // -- hooks ---------------------------------------------------------------

    private var onStartedAction: (() -> Unit)? = null

    /**
     * A callback for platform entry points: invoked once, right after
     * [start] has run the initial routing pass.
     *
     * The browser build registers one: when the page carries `?seed=demo` it opens on
     * the sample trip instead of the first instalment, so a reviewer can be sent one
     * link that lands on a kept trip. The intro also offers the same thing as a quiet
     * action, so the URL is a shortcut rather than the only door.
     */
    public fun onStarted(action: () -> Unit) {
        onStartedAction = action
    }

    /**
     * Choose the stock: light paper, night paper, or whatever the device asks for.
     *
     * The palette has existed in `:core:design` since the beginning and no target
     * ever selected it, so Preferences had to say "light paper only". This is the
     * switch it was missing. It is written to the store immediately: a paper
     * choice that resets when the app closes is a bug, not a preference.
     */
    public fun setTheme(theme: Settings.Theme) {
        if (settings.theme == theme && _state.value.theme == theme) return
        settings = settings.copy(theme = theme)
        _state.value = _state.value.copy(theme = theme)
        scope.launch {
            runCatching { store.saveSettings(settings) }.onFailure(::report)
        }
    }

    public fun editProfileName(name: String) {
        val updated = settings.copy(displayName = name.take(40))
        settings = updated
        _state.value = _state.value.copy(displayName = updated.displayName)
        scope.launch {
            runCatching { store.saveSettings(updated) }.onFailure(::report)
        }
    }

    /**
     * Saves a photo-free expense as already confirmed. It never enters the OCR
     * outbox, and its empty photo path is rendered as a manual record marker.
     */
    public fun saveManualEntry(
        amountText: String,
        currencyCode: String,
        dateText: String,
        merchant: String,
        category: Category?,
        note: String,
    ): String? {
        val trip = _state.value.trip ?: return "Create or open a trip before adding an expense."
        if (trip.isClosed) return "This trip is closed. Open another trip to add an expense."
        val currency = CurrencyCode.parseOrNull(currencyCode)
            ?: return "Choose a supported currency."
        if (amountText.none { it.isDigit() }) return "Enter an amount."
        val amount = Money.parseOrNull(amountText, currency)
            ?: return "Enter an amount using ${currency.code}'s decimal places."
        val day = LocalDay.parseOrNull(dateText.trim())
            ?: return "Enter the date as YYYY-MM-DD."
        if (day < trip.startDate || day > trip.endDate) {
            return "Choose a date within this trip (${trip.startDate.iso} to ${trip.endDate.iso})."
        }
        // The record is a record. A day that has not happened is not one, however
        // the date arrived here — typed, remembered wrongly, or pasted from a
        // booking confirmation for tomorrow.
        if (!RecordDates.isFiled(day, clock.today())) {
            return "That day hasn't happened yet. Add it on the day, or choose today or an earlier day."
        }
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            val createdAt = clock.nowMillis()
            try {
                val capture = Capture(
                    id = newId(),
                    tripId = trip.id,
                    localDate = day,
                    photoPath = "",
                    createdAt = createdAt,
                    state = CaptureState.CONFIRMED,
                    amountMinor = amount.minorUnits,
                    currency = currency,
                    merchant = merchant.trim().take(60).ifBlank { null },
                    confirmedAt = createdAt,
                    category = category,
                    note = note.trim().take(140).ifBlank { null },
                )
                store.insert(capture)
                pinRate(capture, capture.amountMinor, currency)
                index.rebuild(store.confirmedForTrip(trip.id))
                refreshDay(day)
                loadTripDays(trip)
                _state.value = _state.value.copy(busy = false)
                go(Route.Home)
                say("Manual expense saved.")
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                report(failure)
                _state.value = _state.value.copy(busy = false)
                say("The manual expense could not be saved.")
            }
        }
        return null
    }

    /** Build the full local archive and pass it to the target's save surface. */
    public fun exportData(exporter: DataArchiveExporter?) {
        if (exporter == null) {
            say("Data export is not available in this build.")
            return
        }
        if (_state.value.busy) return
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            try {
                val trips = store.trips()
                val days = trips.flatMap { store.dayCards(it.id) }
                val captures = trips.flatMap { store.capturesForTrip(it.id) }
                val rates = store.allRates()
                val photoEntries = mutableMapOf<String, String>()
                val archiveFiles = mutableListOf<ArchiveFile>()
                captures.forEach { capture ->
                    val path = capture.photoPath
                    if (path.isNotBlank() && !path.startsWith("demo:") && files != null) {
                        val photo = files.read(path)
                            ?: error("Receipt photo ${capture.id} could not be read; no archive was saved.")
                        val entryName =
                            "photos/${archiveSafeName(capture.tripId)}/${archiveSafeName(capture.id)}.jpg"
                        photoEntries[capture.id] = entryName
                        archiveFiles += ArchiveFile(entryName, photo)
                    }
                }
                val exportSettings = store.settings()
                archiveFiles += ArchiveFile(
                    "data/ariadne.json",
                    DataArchiveZip.manifest(exportSettings, trips, days, captures, rates, photoEntries)
                        .encodeToByteArray(),
                )
                archiveFiles += ArchiveFile(
                    "data/trips.csv",
                    DataArchiveZip.tripsCsv(trips).encodeToByteArray(),
                )
                archiveFiles += ArchiveFile(
                    "data/captures.csv",
                    DataArchiveZip.capturesCsv(captures, photoEntries).encodeToByteArray(),
                )
                archiveFiles += ArchiveFile(
                    "data/days.csv",
                    DataArchiveZip.daysCsv(days).encodeToByteArray(),
                )
                archiveFiles += ArchiveFile(
                    "README.txt",
                    (
                        "Ariadne local archive. Structured data is in data/ariadne.json and CSV files. " +
                            "Receipt images, when available, are in photos/."
                        ).encodeToByteArray(),
                )
                val bytes = DataArchiveZip.encode(archiveFiles)
                val saved = exporter.saveZip("ariadne-export-${clock.today().iso}.zip", bytes)
                if (saved) say("Your Ariadne archive is ready.")
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                report(failure)
                say(failure.message ?: "The data archive could not be created.")
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    private fun archiveSafeName(value: String): String =
        value.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifBlank { "record" }

    public suspend fun bindTrip(trip: Trip) {
        val day = clock.today().coerceInto(trip)
        val cards = store.dayCards(trip.id)
        val confirmed = store.confirmedForTrip(trip.id)
        index.rebuild(confirmed)
        val register = runCatching { store.trips() }.getOrDefault(_state.value.trips)
        _state.value = _state.value.copy(
            trip = trip,
            // A closed trip can be opened and read, but it is not the journey a
            // new photograph belongs to, so it is not marked active.
            activeTripId = trip.id.takeIf { !trip.isClosed },
            trips = register,
            today = day,
            dayCards = cards,
            route = Route.Home,
        )
        refreshDay(day)
        loadTripDays(trip)
        loadTravellers(trip)
    }

    // -- onboarding --------------------------------------------------------

    /**
     * Creates a trip and seeds five captures from the demo roll, so a judge sees
     * two currencies and a non-zero total inside the first thirty seconds
     * without installing anything or being asked to invent a trip.
     */
    public fun startTrip(
        name: String,
        start: LocalDay,
        end: LocalDay,
        baseCurrency: CurrencyCode,
        homeCurrency: CurrencyCode,
        withDemoRoll: Boolean,
    ) {
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            val trip = Trip(
                id = newId(),
                name = name.ifBlank { "Untitled trip" },
                startDate = start,
                endDate = if (end < start) start else end,
                baseCurrency = baseCurrency,
                homeCurrency = homeCurrency,
            )
            store.createTrip(trip)
            store.saveSettings(settings.copy(baseCurrency = baseCurrency, homeCurrency = homeCurrency))
            if (withDemoRoll) seedDemoRoll(trip)
            bindTrip(trip)
            _state.value = _state.value.copy(busy = false)
        }
    }

    private suspend fun seedDemoRoll(trip: Trip) {
        // Every demo capture lands on today: the stack a visitor sees at
        // Closing the day has to be a stack, not a single card.
        val day = clock.today()
        DemoRoll.entries.forEachIndexed { i, entry ->
            val id = newId()
            store.insert(
                Capture(
                    id = id,
                    tripId = trip.id,
                    localDate = day,
                    photoPath = "demo:${entry.sketch}",
                    createdAt = clock.nowMillis() + i,
                    state = CaptureState.NEEDS_AMOUNT,
                    amountMinor = entry.amountMinor,
                    currency = CurrencyCode.of(entry.currency),
                    merchant = entry.merchant,
                    extractionConfidence = entry.confidence,
                    category = entry.category,
                    note = entry.note,
                ),
            )
        }
    }

    // -- capture -----------------------------------------------------------

    /**
     * A real capture: photo bytes arrive already downscaled by the platform.
     *
     * The caller is told the id because the shutter now hands the frame straight
     * back to the person holding it: keep it for nightfall, or fill it in now.
     */
    public fun capturePhoto(
        bytes: ByteArray,
        downscaleAdjusted: Boolean = true,
        openSheet: Boolean = true,
    ) {
        val trip = _state.value.trip ?: return
        // A closed trip is a finished record, not a destination for new paper.
        if (trip.isClosed) return
        // The shutter names its day when it opens. A capture that arrives with no
        // open shutter (a platform callback after the sheet was dismissed) still
        // files, but on a day that has happened.
        val day = _state.value.shutterDay ?: RecordDates.filingDay(
            today = clock.today(),
            start = trip.startDate,
            end = trip.endDate,
        ) ?: return
        scope.launch {
            val capture = try {
                persistPhotograph(trip, day, bytes)
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                captureFailed(failure)
                return@launch
            }

            _state.value = if (openSheet) {
                _state.value.copy(
                    shutterOpen = false,
                    sheetCaptureId = capture.id,
                    detail = capture,
                    detailOrigin = DetailOrigin.Record,
                    detailAmount = "",
                    detailMerchant = capture.merchant ?: "",
                    detailNote = "",
                    detailCategory = capture.category,
                    captureError = null,
                )
            } else {
                _state.value.copy(shutterOpen = false, captureError = null)
            }

            // The kept photograph is visible before OCR begins. A slow recognizer
            // must not turn a zero-decision shutter into a wait screen.
            extraction.drain()
            refreshDay(day)
            runCatching { refreshRates() }.onFailure(::report)
            if (openSheet) {
                val saved = store.findById(capture.id)
                _state.value = _state.value.copy(
                    detail = saved,
                    detailMerchant = saved?.merchant ?: "",
                    detailCategory = saved?.category,
                )
            }
        }
    }

    private suspend fun persistPhotograph(trip: Trip, day: LocalDay, bytes: ByteArray): Capture {
        val id = newId()
        val path = if (bytes.isEmpty()) {
            com.ariadne.store.PhotoPaths.capturePath(trip.id, day, id)
        } else {
            val photoFiles = files
                ?: throw IllegalStateException("Photo storage is unavailable on this platform.")
            photoFiles.write(trip.id, day, id, bytes)
        }
        val capture = Capture(
            id = id,
            tripId = trip.id,
            localDate = day,
            photoPath = path,
            createdAt = clock.nowMillis(),
            state = CaptureState.QUEUED,
        )
        try {
            store.insert(capture)
        } catch (failure: Exception) {
            if (bytes.isNotEmpty()) {
                try {
                    files?.delete(path)
                } catch (cleanupFailure: Exception) {
                    report(cleanupFailure)
                }
            }
            throw failure
        }
        return capture
    }

    private fun captureFailed(failure: Exception) {
        report(failure)
        val message = "The photograph could not be kept. Try again."
        _state.value = _state.value.copy(captureError = message)
        say(message)
    }

    public fun photoImportFailed(failure: Exception) {
        report(failure)
        val message = "That photograph could not be read. Choose it again."
        _state.value = _state.value.copy(captureError = message)
        say(message)
    }

    public fun tripCardExportFailed(failure: Exception) {
        report(failure)
        say("The Trip Card image could not be saved.")
    }

    /** The demo path, still one call so the judged route stays short. */
    public fun startTripDemo() {
        fillDraftFromDemo()
        commitDraft(withDemoRoll = true)
    }

    /** Capture a placeholder frame (the platform camera supplies bytes in the real build). */
    public fun capturePlaceholder() {
        capturePhoto(ByteArray(0))
    }

    /**
     * A photograph that arrived from somewhere other than the shutter.
     *
     * Deliberately the same path as [capturePhoto] once the bytes exist: the
     * record does not care, and should not care, whether an image came from the
     * sensor or from the photo library. Imported captures go straight into the
     * queue like any other, so they meet the same two taps when the day is closed.
     */
    public fun importPhoto(bytes: ByteArray) {
        capturePhoto(bytes)
    }

    /**
     * Several photographs from the library, filed in one act.
     *
     * No sheet opens: the point of a batch is to get the photographs into the record
     * and out of the way, and each one meets the same "add the amount later" pager
     * whenever the day is closed. A single photograph still opens its sheet, because
     * a person who picked one thing probably wants to type the figure now.
     */
    public fun importPhotos(photos: List<ByteArray>) {
        val selected = photos.filter { it.isNotEmpty() }
        if (selected.isEmpty()) return
        if (selected.size == 1) {
            capturePhoto(selected.first())
            return
        }
        val trip = _state.value.trip ?: return
        if (trip.isClosed) return
        val day = _state.value.shutterDay ?: RecordDates.filingDay(
            today = clock.today(),
            start = trip.startDate,
            end = trip.endDate,
        ) ?: return
        scope.launch {
            var saved = 0
            var failed = 0
            selected.forEach { bytes ->
                try {
                    persistPhotograph(trip, day, bytes)
                    saved++
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    captureFailed(failure)
                    failed++
                }
            }
            if (saved > 0) {
                extraction.drain()
                refreshDay(day)
                runCatching { refreshRates() }.onFailure(::report)
            }
            _state.value = _state.value.copy(shutterOpen = false, captureError = null)
            // The day is named when it is not today: a photograph filed on Tuesday
            // is not in the stack the person is looking at, and saying so is the
            // difference between "it vanished" and "it is on Tuesday".
            val where = if (day == clock.today()) "" else " on ${day.iso}"
            say(
                when {
                    saved == 0 -> "None of the selected photographs could be kept."
                    failed == 0 && where.isEmpty() -> "$saved photographs added to the waiting stack."
                    failed == 0 -> "$saved photographs filed$where."
                    else -> "$saved photographs kept$where; $failed could not be saved."
                },
            )
        }
    }

    /** Kept for the older call sites: they now open the form rather than a trip. */
    public fun startEmptyTrip() {
        startNewTrip()
    }

    public fun startSeedTrip() {
        startTripDemo()
    }

    // -- the trip as an object --------------------------------------------

    /**
     * Closes the trip and shows it as a card.
     *
     * Closing is not deletion: the record stays, the totals stay, and a closed
     * trip simply stops being the one the shutter points at. The card is the
     * thing you keep, so this is the one-way door that is not frightening.
     */
    public fun closeTripIntoCard() {
        val trip = _state.value.trip ?: return
        scope.launch {
            store.closeTrip(trip.id, clock.nowMillis())
            val closed = store.trip(trip.id) ?: trip
            // Closing ends the trip as a *capture target*: nothing is active any
            // more, so the shutter cannot file a photograph into a journey that is
            // finished, and the register stops marking it as the open one. The
            // closed trip stays bound so its card can be read.
            _state.value = _state.value.copy(
                trip = closed,
                activeTripId = null,
                trips = runCatching { store.trips() }.getOrDefault(_state.value.trips),
                route = Route.TripCard(trip.id),
            )
        }
    }

    /**
     * Deletes the trip and everything hanging off it: captures, day cards, and
     * the photographs in the app's private folder.
     *
     * Two taps, never one. The first arms it and says what will go; the second
     * does it. There is no undo because there is no copy anywhere else — which is
     * the whole design, and the reason this cannot be a single tap.
     */
    public fun armTripDeletion() {
        _state.value = _state.value.copy(deleteArmed = !_state.value.deleteArmed)
    }

    public fun deleteTripNow() {
        val trip = _state.value.trip ?: return
        if (!_state.value.deleteArmed) return
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            if (files != null) {
                runCatching {
                    // Every capture, not only the finished ones: a photo whose
                    // row goes with the trip would otherwise sit in the folder forever.
                    store.capturesForTrip(trip.id).forEach { capture ->
                        if (capture.photoPath.isNotBlank() && !capture.photoPath.startsWith("demo:")) {
                            files.delete(capture.photoPath)
                        }
                    }
                }
            }
            store.deleteTrip(trip.id)
            index.rebuild(emptyList())
            _state.value = AppUiState(
                route = Route.NewTrip,
                isStartingUp = false,
                // Deleting a trip must not reprint the book: the stock is a
                // preference of the person, not of the record.
                theme = settings.theme,
                today = clock.today(),
                draft = TripDraft(
                    startIso = clock.today().iso,
                    endIso = clock.today().plusDays(6).iso,
                    currency = settings.baseCurrency.code,
                ),
            )
        }
    }

    /**
     * The trip card as text, for the share sheet.
     *
     * Built here rather than in the page because it is content: the numbers in
     * the shared card have to be the numbers in the record, and the way to
     * guarantee that is for both to be read from the same state.
     */
    /**
     * Puts the card text on the system clipboard.
     *
     * The clipboard is a platform thing, so it arrives through the same optional
     * interface as the camera and the file system. With no implementation the
     * label simply never flips, which is honest: nothing was copied.
     */
    public fun copyCardToClipboard() {
        val text = tripCardText()
        if (text.isEmpty()) return
        val copied = clipboard?.copy(text) == true
        _state.value = _state.value.copy(cardCopied = copied)
    }

    public fun tripCardText(): String {
        val trip = _state.value.trip ?: return ""
        val total = _state.value.tripTotalMinor
        val closed = _state.value.dayCards.count { it.isClosed }
        val totalText = Money.ofMinor(total).formatWithSymbol(trip.baseCurrency)
        return buildString {
            appendLine(trip.name)
            trip.place?.let { appendLine(it) }
            appendLine("${trip.startDate.iso} to ${trip.endDate.iso}")
            appendLine()
            appendLine("$closed of ${_state.value.dayCards.size} days closed")
            appendLine("$totalText across ${_state.value.categoryTotals.sumOf { it.captures }} photographs")
            trip.budgetMinor?.let { budget ->
                val left = budget - total
                appendLine(
                    if (left >= 0) {
                        "against a budget of ${Money.ofMinor(budget).formatWithSymbol(trip.baseCurrency)}, " +
                            "${Money.ofMinor(left).formatWithSymbol(trip.baseCurrency)} left"
                    } else {
                        "${Money.ofMinor(-left).formatWithSymbol(trip.baseCurrency)} over a budget of " +
                            Money.ofMinor(budget).formatWithSymbol(trip.baseCurrency)
                    },
                )
            }
            appendLine()
            append("Ariadne — kept on the device it was written on.")
        }
    }

    // -- the new-trip page ------------------------------------------------

    /**
     * The hour a capture happened, in the device's zone, for the timeline rail.
     *
     * Asked of the clock rather than computed here: the view has a millis and no
     * zone, and a rail that reads UTC is wrong in every zone but one.
     */
    public fun hourOf(millis: Long): Int = clock.hourOf(millis)

    /**
     * "21:09" — the time a photograph was taken, in the device's own zone.
     *
     * Used for the fallback title, which is the only place the clock reaches copy:
     * a receipt the recogniser could not name is "Receipt · 21:09" rather than a
     * guessed merchant or the word "Untitled".
     */
    public fun clockTimeOf(millis: Long): String {
        val hour = clock.hourOf(millis).coerceIn(0, 23).toString().padStart(2, '0')
        val minute = clock.minuteOf(millis).coerceIn(0, 59).toString().padStart(2, '0')
        return "$hour:$minute"
    }

    /** What a capture is called on every page that shows it. */
    public fun titleOf(capture: Capture): String =
        MerchantName.receiptTitle(capture.merchant, clockTimeOf(capture.createdAt))

    public fun startNewTrip() {
        val today = clock.today()
        _state.value = _state.value.copy(
            route = Route.NewTrip,
            draft = TripDraft(
                startIso = today.iso,
                endIso = today.plusDays(6).iso,
                currency = settings.baseCurrency.code,
            ),
        )
    }

    public fun editDraft(
        name: String? = null,
        place: String? = null,
        startIso: String? = null,
        endIso: String? = null,
        currency: String? = null,
        budget: String? = null,
    ) {
        val d = _state.value.draft
        _state.value = _state.value.copy(
            draft = d.copy(
                name = name ?: d.name,
                place = place ?: d.place,
                startIso = startIso ?: d.startIso,
                endIso = endIso ?: d.endIso,
                currency = currency ?: d.currency,
                budget = budget ?: d.budget,
                error = null,
            ),
        )
    }

    /** Prefill from the demo roll, so the judged path stays one tap. */
    public fun fillDraftFromDemo() {
        val today = clock.today()
        _state.value = _state.value.copy(
            draft = TripDraft(
                name = "Bangkok & Home",
                place = "Bangkok",
                startIso = today.iso,
                endIso = today.plusDays(6).iso,
                currency = settings.baseCurrency.code,
                budget = "25000",
            ),
        )
    }

    /**
     * Create the trip the draft describes.
     *
     * Every rejection carries a sentence a person can act on. A silent no-op on a
     * page with five fields is the worst possible answer, because the human cannot
     * tell which field was wrong.
     */
    public fun commitDraft(withDemoRoll: Boolean = false) {
        val d = _state.value.draft
        val start = LocalDay.parseOrNull(d.startIso.trim())
        val end = LocalDay.parseOrNull(d.endIso.trim())
        val error = when {
            d.name.isBlank() -> "A trip needs a name."
            start == null -> "The start date should look like 2026-11-24."
            end == null -> "The end date should look like 2026-11-30."
            end < start -> "The trip cannot end before it starts."
            else -> null
        }
        if (error != null) {
            _state.value = _state.value.copy(draft = d.copy(error = error))
            return
        }
        val currency = CurrencyCode.parseOrNull(d.currency.trim().uppercase()) ?: settings.baseCurrency
        val budgetMinor = parseBudgetMinor(d.budget, currency)
        val trip = Trip(
            id = newId(),
            name = d.name.trim(),
            place = d.place.trim().ifBlank { null },
            startDate = start!!,
            endDate = end!!,
            baseCurrency = currency,
            homeCurrency = settings.homeCurrency,
            budgetMinor = budgetMinor,
        )
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            // Whatever goes wrong here has to reach the page. This is the call
            // that used to fail silently: it threw inside a launched coroutine,
            // the exception was swallowed, and "Open the trip" simply did
            // nothing, which reads as a dead button rather than as an error.
            val failure = runCatching {
                store.createTrip(trip)
                store.saveSettings(settings.copy(baseCurrency = currency))
                if (withDemoRoll) seedDemoRoll(trip)
                bindTrip(trip)
            }.exceptionOrNull()
            if (failure != null) {
                report(failure)
                _state.value = _state.value.copy(
                    busy = false,
                    draft = d.copy(
                        error = "The trip could not be saved. " +
                            (failure.message ?: failure::class.simpleName ?: "Unknown error") +
                            " — nothing was lost; try again.",
                    ),
                )
                return@launch
            }
            _state.value = _state.value.copy(busy = false, draft = TripDraft(), route = Route.Home)
        }
    }

    /**
     * "2500", "2,500", "2500.50" -> minor units. Blank means no budget at all,
     * which is a different thing from a budget of zero and is stored as null.
     */
    private fun parseBudgetMinor(text: String, currency: CurrencyCode): Long? {
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 2) return null
        val whole = parts[0].toLongOrNull() ?: return null
        val fraction = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0L
        return whole * 100 + fraction
    }

    // -- the frame just taken ---------------------------------------------

    /** Nothing to change: the photograph waits for nightfall as it is. */
    /**
     * Sets or clears the trip's fence, in base-currency minor units.
     *
     * A separate call from creation because a budget is a thing people revise on
     * the third day, not only when they open the trip.
     */
    public fun editTripBudget(text: String) {
        val trip = _state.value.trip ?: return
        val minor = parseBudgetMinor(text, trip.baseCurrency)
        val updated = trip.copy(budgetMinor = minor)
        scope.launch {
            store.updateTrip(updated)
            _state.value = _state.value.copy(trip = updated)
        }
    }

    /** Base currency is what every total is stated in, so changing it re-states them. */
    /**
     * Record how one unit of [code] trades against the trip's base currency.
     *
     * Stated as "1 THB = 2.5 INR" rather than as a pair of abstract numbers,
     * because that is the sentence a person can check against a receipt or a
     * board at the airport. Stored scaled by 1e6, so it survives the database
     * exactly and converts without ever becoming a float.
     *
     * Returns false when the text is not a number, so the page can say so rather
     * than leaving a field that silently did nothing.
     */
    public fun setFxRate(code: String, decimal: String): Boolean {
        val trip = _state.value.trip ?: return false
        val from = com.ariadne.model.CurrencyCode.parseOrNull(code.trim().uppercase()) ?: return false
        val scaled = FxTable.rateFromDecimal(decimal) ?: return false
        if (scaled <= 0L) return false
        val rate = com.ariadne.model.Rate(
            base = from,
            quote = trip.baseCurrency,
            date = _state.value.today ?: clock.today(),
            rateScaled = scaled,
        )
        scope.launch {
            store.upsertRate(rate)
            fxRates = store.allRates()
            fx = FxTable(fxRates)
            _state.value = _state.value.copy(rateError = null)
            // Every figure on every page is downstream of this table, so the day
            // and the trip both have to be re-read.
            _state.value.today?.let { refreshDay(it) }
            loadTripDays(trip)
        }
        return true
    }

    /** Forget a rate, so a wrong one cannot keep converting silently. */
    public fun clearFxRate(code: String) {
        val trip = _state.value.trip ?: return
        val from = com.ariadne.model.CurrencyCode.parseOrNull(code.trim().uppercase()) ?: return
        scope.launch {
            store.deleteRate(from, trip.baseCurrency)
            fxRates = store.allRates()
            fx = FxTable(fxRates)
            _state.value.today?.let { refreshDay(it) }
            loadTripDays(trip)
        }
    }

    /** "1 THB = 2.5000 INR", or null when no rate is on file. */
    public fun rateLabel(code: String): String? {
        val trip = _state.value.trip ?: return null
        val from = com.ariadne.model.CurrencyCode.parseOrNull(code.trim().uppercase()) ?: return null
        if (from == trip.baseCurrency) return null
        val scaled = fx.rateOrNull(from, trip.baseCurrency, _state.value.today ?: clock.today())
            ?: return null
        return "1 ${from.code} = ${formatScaled(scaled)} ${trip.baseCurrency.code}"
    }

    /** The effective date of the saved rate currently used for this currency pair. */
    public fun rateDateLabel(code: String): String? {
        val trip = _state.value.trip ?: return null
        val from = com.ariadne.model.CurrencyCode.parseOrNull(code.trim().uppercase()) ?: return null
        if (from == trip.baseCurrency) return null
        val date = _state.value.today ?: clock.today()
        val direct = fxRates
            .filter { it.base == from && it.quote == trip.baseCurrency && it.date <= date }
            .maxByOrNull { LocalDay.toEpochDay(it.date) }
        val selected = direct ?: fxRates
            .filter { it.base == trip.baseCurrency && it.quote == from && it.date <= date }
            .maxByOrNull { LocalDay.toEpochDay(it.date) }
        return selected?.date?.iso
    }

    /** Every foreign currency this trip's captures actually used. */
    public fun currenciesInUse(): List<String> {
        val base = _state.value.trip?.baseCurrency?.code ?: return emptyList()
        return (_state.value.dayCapturesAll.values.flatten() + _state.value.dayCaptures)
            .mapNotNull { it.currency?.code }
            .distinct()
            .filter { it != base }
            .sorted()
    }

    private fun formatScaled(scaled: Long): String {
        val whole = scaled / FxTable.SCALE
        val fraction = (scaled % FxTable.SCALE).toString().padStart(6, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
    }

    public fun editTripCurrency(code: String) {
        val trip = _state.value.trip ?: return
        val currency = CurrencyCode.parseOrNull(code) ?: return
        val updated = trip.copy(baseCurrency = currency)
        scope.launch {
            store.updateTrip(updated)
            settings = settings.copy(baseCurrency = currency)
            store.saveSettings(settings)
            _state.value = _state.value.copy(trip = updated)
            _state.value.today?.let { refreshDay(it) }
            loadTripDays(updated)
            // A new base currency means the cached rates are for the wrong base.
            refreshRates(force = true)
        }
    }

    public fun editTripHomeCurrency(code: String) {
        val trip = _state.value.trip ?: return
        val currency = CurrencyCode.parseOrNull(code) ?: return
        val updated = trip.copy(homeCurrency = currency)
        scope.launch {
            store.updateTrip(updated)
            settings = settings.copy(homeCurrency = currency)
            store.saveSettings(settings)
            _state.value = _state.value.copy(trip = updated)
        }
    }

    public fun keepFrameForLater() {
        // "Add the amount later" is not a journey: it closes the sheet and leaves
        // the person on the page they were on, with their photograph filed.
        if (_state.value.sheetCaptureId != null) {
            closeSheet()
            return
        }
        // Reached from the standalone sheet route (an older link, or the whole-page
        // escape): land on Today, which is where a filed photograph belongs.
        go(Route.Home)
    }

    /** The whole page: the sheet's details, without the sheet. */
    public fun openFrame(captureId: String) {
        _state.value = _state.value.copy(sheetCaptureId = null)
        openDetail(captureId)
    }

    // -- nightfall ---------------------------------------------------------

    public fun openCloseDay(day: LocalDay? = null) {
        val trip = _state.value.trip ?: return
        scope.launch {
            val today = clock.today()
            val asked = day ?: today
            // Closing a day is filing everything on it, so it obeys the same rule as
            // the shutter: only a day that has happened can be closed.
            if (!RecordDates.isFiled(asked, today)) {
                say("That day hasn't happened yet. A day is closed after it, not before.")
                return@launch
            }
            val target = asked.coerceInto(trip)
            refreshDay(target)
            go(Route.CloseDay(target))
        }
    }

    public fun typeAmount(digit: Char) {
        val current = _state.value.amountInput
        if (digit == '.') {
            if (current.contains('.')) return
            _state.value = _state.value.copy(amountInput = if (current.isEmpty()) "0." else current + ".")
            return
        }
        if (digit !in '0'..'9') return
        if (current.length >= 9) return
        _state.value = _state.value.copy(amountInput = current + digit)
    }

    public fun backspace() {
        val current = _state.value.amountInput
        _state.value = _state.value.copy(amountInput = current.dropLast(1))
    }

    public fun selectStackIndex(index: Int) {
        val stack = _state.value.stack
        if (index !in stack.indices) return
        val capture = stack[index]
        val currency = capture.currency ?: _state.value.trip?.baseCurrency
        val amountMinor = capture.amountMinor
        _state.value = _state.value.copy(
            stackIndex = index,
            amountInput = if (amountMinor != null && currency != null) {
                Money.ofMinor(amountMinor).format(currency, grouping = false)
            } else {
                ""
            },
        )
    }

    public fun next() = selectStackIndex((_state.value.stackIndex + 1).coerceAtMost(_state.value.stack.lastIndex.coerceAtLeast(0)))

    public fun previous() = selectStackIndex((_state.value.stackIndex - 1).coerceAtLeast(0))

    /**
     * Pins the rate a confirmed amount was converted at to the capture row.
     *
     * Called from every door that confirms a figure — the stack, "confirm all
     * ready", the detail page. One helper, because the ₹2,605 bug was three pages
     * converting the same captures at three different dates: the day being read,
     * the trip's first capture, and the trip's start date.
     *
     * The rate is looked up on the capture's own day, so a receipt photographed in
     * Bangkok is converted at Bangkok's rate for that day, and is never revisited
     * when the rate table moves. No rate available is recorded as such: the amount
     * stays, and the total names it as unconverted instead of quietly pretending.
     */
    private suspend fun pinRate(capture: Capture, amountMinor: Long?, currency: CurrencyCode) {
        val trip = _state.value.trip ?: return
        if (amountMinor == null || currency == trip.baseCurrency) {
            // Nothing to convert: clear any stale snapshot so a currency edit
            // cannot leave an old rate attached to a rupee amount.
            store.recordRate(capture.id, null, null)
            return
        }
        val day = capture.localDate
        val rate = fx.rateOrNull(currency, trip.baseCurrency, day)
        store.recordRate(capture.id, rate, if (rate == null) null else day.iso)
    }

    /**
     * Confirm the card on screen. Two taps from card open at most: the amount is
     * pre-filled when extraction was confident, so Confirm alone is often enough.
     */
    public fun confirmCurrent(noAmount: Boolean = false) {
        val capture = _state.value.current ?: return
        val currentIndex = _state.value.stackIndex
        val currency = capture.currency ?: _state.value.trip?.baseCurrency ?: CurrencyCode.of("INR")
        val parsed = if (noAmount) null else Money.parseOrNull(_state.value.amountInput, currency)
        scope.launch {
            store.updateDetails(
                id = capture.id,
                amountMinor = parsed?.minorUnits,
                currencyCode = if (noAmount) null else currency.code,
                merchant = capture.merchant,
                markedNoAmount = noAmount,
                confirmedAt = clock.nowMillis(),
            )
            pinRate(capture, parsed?.minorUnits, currency)
            outbox.apply(capture.id, if (noAmount) QueueEvent.MarkedNoAmount else QueueEvent.AmountEntered)
            outbox.apply(capture.id, QueueEvent.Confirmed)
            val day = capture.localDate
            refreshDay(day)
            val remaining = _state.value.stack.filter { it.id != capture.id }
            _state.value = _state.value.copy(stack = remaining)
            if (remaining.isEmpty()) {
                _state.value = _state.value.copy(route = Route.Record(day), amountInput = "", stackIndex = 0)
            } else {
                selectStackIndex(currentIndex.coerceAtMost(remaining.lastIndex))
            }
        }
        // Ported from the KMP contest starter kit: offer the trusted in-app review
        // after the first confirmed card — best effort, never blocking.
        runCatching { reviewManager.requestReview() }
    }

    public fun rejectCurrentExtractedAmount() {
        val capture = _state.value.current ?: return
        if (!capture.hasUnconfirmedAmount) return

        _state.value = _state.value.copy(
            stack = _state.value.stack.map {
                if (it.id == capture.id) it.copy(amountMinor = null) else it
            },
            amountInput = "",
        )
        scope.launch {
            store.updateDetails(
                id = capture.id,
                amountMinor = null,
                currencyCode = capture.currency?.code,
                merchant = capture.merchant,
                markedNoAmount = false,
                confirmedAt = null,
            )
            refreshDay(capture.localDate)
        }
    }

    /** Marks every remaining capture in the current stack as "no amount" and confirms it in one pass. */
    public fun confirmRestAsNoAmount() {
        val stack = _state.value.stack
        if (stack.isEmpty()) return
        val now = clock.nowMillis()
        val day = (state.value.route as? Route.CloseDay)?.day ?: state.value.today ?: stack.first().localDate
        scope.launch {
            stack.forEach { capture ->
                store.updateDetails(
                    id = capture.id,
                    amountMinor = null,
                    currencyCode = null,
                    merchant = capture.merchant,
                    markedNoAmount = true,
                    confirmedAt = now,
                )
                when (capture.state) {
                    CaptureState.CONFIRMED, CaptureState.ARCHIVED -> Unit
                    CaptureState.NEEDS_CONFIRMATION -> Unit
                    else -> outbox.apply(capture.id, QueueEvent.MarkedNoAmount)
                }
                runCatching { outbox.apply(capture.id, QueueEvent.Confirmed) }
            }
            refreshDay(day)
            _state.value = _state.value.copy(stack = emptyList(), stackIndex = 0, amountInput = "", route = Route.Record(day))
        }
    }

    /** True when a card can be confirmed without typing or reviewing an extracted amount. */
    public fun isReadyToConfirm(capture: Capture): Boolean =
        !capture.markedNoAmount &&
            capture.amountMinor != null &&
            !capture.hasUnconfirmedAmount &&
            capture.state != CaptureState.QUEUED &&
            capture.state != CaptureState.EXTRACTING

    /** Confirms every ready card in the current stack in one action. */
    public fun confirmReadyInStack() {
        val trip = _state.value.trip ?: return
        val stack = _state.value.stack
        val ready = stack.filter(::isReadyToConfirm)
        if (ready.isEmpty()) return
        scope.launch {
            ready.forEach { capture ->
                val currency = capture.currency ?: trip.baseCurrency
                store.updateDetails(
                    id = capture.id,
                    amountMinor = capture.amountMinor,
                    currencyCode = currency.code,
                    merchant = capture.merchant,
                    markedNoAmount = false,
                    confirmedAt = clock.nowMillis(),
                )
                pinRate(capture, capture.amountMinor, currency)
                when (capture.state) {
                    CaptureState.NEEDS_CONFIRMATION -> Unit
                    CaptureState.NEEDS_AMOUNT, CaptureState.FAILED -> outbox.apply(capture.id, QueueEvent.AmountEntered)
                    CaptureState.QUEUED, CaptureState.EXTRACTING -> return@forEach
                    CaptureState.CONFIRMED, CaptureState.ARCHIVED -> Unit
                }
                outbox.apply(capture.id, QueueEvent.Confirmed)
            }
            val day = (state.value.route as? Route.CloseDay)?.day ?: state.value.today ?: ready.first().localDate
            refreshDay(day)
            loadTripDays(trip)
            val remaining = _state.value.stack
            if (remaining.isEmpty()) {
                _state.value = _state.value.copy(route = Route.Record(day), amountInput = "")
            } else {
                selectStackIndex(_state.value.stackIndex.coerceIn(0, remaining.lastIndex))
            }
        }
    }

    /** "Close the day": one locked card, idempotent, available from anywhere in the stack. */
    public fun closeDay(day: LocalDay? = null) {
        val trip = _state.value.trip ?: return
        // Same rule as the shutter: a closed trip cannot be closed again, and its
        // days may not be re-filed into a finished record.
        if (trip.isClosed) return
        val asked = day ?: (_state.value.route as? Route.CloseDay)?.day ?: clock.today()
        if (!RecordDates.isFiled(asked, clock.today())) {
            say("That day hasn't happened yet. A day is closed after it, not before.")
            return
        }
        scope.launch {
            val target = asked
            store.capturesForDay(trip.id, target).forEach { capture ->
                if (capture.state.isOpen) {
                    // An untouched capture is not silently dropped: it becomes a
                    // no-amount record the human can still edit.
                    outbox.apply(capture.id, QueueEvent.MarkedNoAmount)
                    outbox.apply(capture.id, QueueEvent.Confirmed)
                }
                outbox.apply(capture.id, QueueEvent.Archived)
            }
            store.closeDay(trip.id, target, clock.nowMillis())
            refreshDay(target)
            _state.value = _state.value.copy(route = Route.Record(target), dayCards = store.dayCards(trip.id))
        }
    }

    // -- detail -------------------------------------------------------------

    /**
     * Opens one capture for editing. Everything starts unset except the figures
     * already stored, and the text fields start as empty buffers rather than as
     * the stored value: a field you did not touch must not overwrite what the
     * extraction found.
     */
    /**
     * Open one photograph.
     *
     * [from] is where Back goes: the day's record, or the search that found it.
     * Search state is not cleared on the way in, so returning lands on the same
     * list with the same query.
     */
    public fun openDetail(captureId: String, from: DetailOrigin = DetailOrigin.Record) {
        scope.launch {
            val capture = store.findById(captureId) ?: return@launch
            go(Route.Detail(captureId))
            _state.value = _state.value.copy(
                detailOrigin = from,
                detail = capture,
                detailAmount = amountBuffer(capture),
                detailMerchant = capture.merchant ?: "",
                detailNote = capture.note ?: "",
                detailCategory = capture.category,
            )
        }
    }

    /** The typed figure as the field holds it: ungrouped, so it can be edited. */
    private fun amountBuffer(capture: Capture): String {
        val minor = capture.amountMinor ?: return ""
        val currency = capture.currency ?: return ""
        return Money.ofMinor(minor).format(currency, grouping = false)
    }

    public fun editDetailAmount(digit: Char) {
        val current = _state.value.detailAmount
        if (digit == '.') {
            if (current.contains('.')) return
            _state.value = _state.value.copy(detailAmount = if (current.isEmpty()) "0." else current + ".")
            return
        }
        if (digit !in '0'..'9') return
        if (current.length >= 9) return
        _state.value = _state.value.copy(detailAmount = current + digit)
    }

    public fun clearDetailAmountDigit() {
        _state.value = _state.value.copy(detailAmount = _state.value.detailAmount.dropLast(1))
    }

    /**
     * "Yes, that is the figure": the extracted amount becomes the typed amount and
     * is saved. One call, because it is one decision by the person, not two.
     */
    public fun acceptDetailSuggestion() {
        val capture = _state.value.detail ?: return
        val minor = capture.amountMinor ?: return
        val currency = capture.currency ?: _state.value.trip?.baseCurrency ?: CurrencyCode.of("INR")
        _state.value = _state.value.copy(
            detailAmount = Money.ofMinor(minor).format(currency, grouping = false),
        )
        saveDetail()
    }

    public fun rejectDetailExtractedAmount() {
        val capture = _state.value.detail ?: return
        if (!capture.hasUnconfirmedAmount) return
        _state.value = _state.value.copy(
            detail = capture.copy(amountMinor = null),
            detailAmount = "",
        )
        scope.launch {
            store.updateDetails(
                id = capture.id,
                amountMinor = null,
                currencyCode = capture.currency?.code,
                merchant = capture.merchant,
                markedNoAmount = false,
                confirmedAt = null,
            )
            refreshDay(capture.localDate)
        }
    }

    /**
     * The amount, said out loud.
     *
     * A spoken figure is a decision, not a reading, so it lands confirmed: the entry
     * stops asking, and the day's figure moves. The capture is resolved the way the
     * sheet resolves it — the one the shutter just kept, else the one being edited,
     * else the top of the stack — so the microphone works wherever the person is.
     */
    public fun hearAmount(minor: Long, currency: CurrencyCode?) {
        if (minor <= 0L) return
        val state = _state.value
        val id = state.sheetCaptureId ?: state.detail?.id ?: state.stack.firstOrNull()?.id ?: return
        val capture = state.dayCapturesAll.values.asSequence().flatten().firstOrNull { it.id == id }
            ?: state.detail?.takeIf { it.id == id }
            ?: state.stack.firstOrNull { it.id == id }
            ?: return
        val code = currency ?: capture.currency ?: state.trip?.baseCurrency ?: CurrencyCode.of("INR")
        scope.launch {
            store.updateDetails(
                id = capture.id,
                amountMinor = minor,
                currencyCode = code.code,
                merchant = capture.merchant,
                markedNoAmount = false,
                confirmedAt = clock.nowMillis(),
            )
            pinRate(capture, minor, code)
            outbox.apply(capture.id, QueueEvent.AmountEntered)
            outbox.apply(capture.id, QueueEvent.Confirmed)
            refreshDay(capture.localDate)
            val fresh = _state.value.dayCapturesAll.values.asSequence()
                .flatten().firstOrNull { it.id == capture.id }
            if (fresh != null) {
                _state.value = _state.value.copy(
                    detail = _state.value.detail?.let { if (it.id == fresh.id) fresh else it },
                )
            }
        }
    }

    public fun editDetailMerchant(text: String) {
        _state.value = _state.value.copy(detailMerchant = text.take(60))
    }

    public fun editDetailNote(text: String) {
        _state.value = _state.value.copy(detailNote = text.take(140))
    }

    /** Tapping the chosen category again clears it: a category is never required. */
    public fun chooseCategory(category: Category?) {
        val current = _state.value.detailCategory
        _state.value = _state.value.copy(detailCategory = if (current == category) null else category)
    }

    public fun changeDetailCurrency(code: String) {
        val currency = CurrencyCode.parseOrNull(code) ?: return
        val capture = _state.value.detail ?: return
        // Re-parse the typed figure in the new currency's scale, so 12.5 THB does
        // not become 12.5 JPY.
        val minor = Money.parseOrNull(_state.value.detailAmount, currency)?.minorUnits
        _state.value = _state.value.copy(
            detail = capture.copy(currency = currency),
            detailAmount = minor?.let { Money.ofMinor(it).format(currency, grouping = false) }
                ?: _state.value.detailAmount,
        )
    }

    /**
     * What the typed figure will become, for the two figures the sheet prints.
     *
     * Deliberately a pure read of the current state: the sheet asks on every
     * keystroke, so this must not touch the store, the network or the clock's
     * expensive paths. The rate is the same one [saveDetail] will pin, looked up for
     * today rather than for the capture's day — because the figure has not been
     * saved yet, and today is the day it will be.
     */
    public fun sheetPreview(): SheetPreview? {
        val state = _state.value
        val trip = state.trip ?: return null
        val capture = state.detail ?: return null
        val typed = state.detailAmount
        val from = capture.currency ?: trip.baseCurrency
        // The pair is shown for the machine's reading too, not only for a typed
        // figure: the whole point is knowing what the suggestion is worth before
        // saying yes to it.
        val money = if (typed.isNotEmpty()) {
            Money.parseOrNull(typed, from) ?: return null
        } else {
            capture.amountMinor?.let(Money::ofMinor) ?: return null
        }
        if (from == trip.baseCurrency) {
            return SheetPreview(typed = typed, from = from, base = money, rateLabel = null, sameCurrency = true)
        }
        val scaled = fx.rateOrNull(from, trip.baseCurrency, state.today ?: clock.today())
            ?: return SheetPreview(typed = typed, from = from, base = null, rateLabel = null, sameCurrency = false)
        val converted = fx.convertAt(money, from, trip.baseCurrency, scaled)
        return SheetPreview(
            typed = typed,
            from = from,
            base = if (converted.applied) converted.amount else null,
            rateLabel = "1 ${from.code} \u2248 ${formatScaled(scaled)} ${trip.baseCurrency.code}",
            sameCurrency = false,
        )
    }

    /**
     * Read the photograph again.
     *
     * The amount the right-hand machine found is a suggestion, and when it is wrong
     * the person currently has to retype it. This re-runs the same extraction the
     * shutter ran, against the same file, and leaves the sheet holding whatever came
     * back — a new suggestion, nothing, or a failure, all of which the sheet already
     * knows how to show. A figure the person typed is not preserved: the whole point
     * of the tap is that the machine's reading replaces the field.
     */
    public fun reExtractCurrent() {
        val capture = _state.value.detail ?: return
        if (_state.value.trip?.isClosed == true) return
        if (_state.value.busy) return
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching { extraction.extractOne(capture.id, capture.photoPath) }.onFailure(::report)
            val fresh = runCatching { store.findById(capture.id) }.getOrNull()
            runCatching { refreshDay(capture.localDate) }
            _state.value.trip?.let { loadTripDays(it) }
            _state.value = _state.value.copy(
                busy = false,
                detail = fresh ?: _state.value.detail,
                detailAmount = "",
                detailMerchant = fresh?.merchant ?: "",
                detailCategory = fresh?.category,
            )
        }
    }

    /**
     * Deletes one photograph's row, and its bytes.
     *
     * Two taps, never one, exactly as a trip is deleted: this is the only copy of
     * the record, and an undo after the bytes are gone would be a lie. What does
     * *not* happen matters too — the day's total, the trip's total and the search
     * index are all re-read from the store afterwards, so nothing keeps quoting a
     * figure whose receipt has just been thrown away.
     */
    public fun armEntryDeletion() {
        _state.value = _state.value.copy(entryDeleteArmed = !_state.value.entryDeleteArmed)
    }

    public fun deleteEntryNow() {
        val capture = _state.value.detail ?: return
        if (!_state.value.entryDeleteArmed) return
        val day = capture.localDate
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching {
                // The bytes first, then the row: a row that outlives its file is a
                // broken photograph on a page, and a file that outlives its row is
                // only wasted space.
                if (files != null && capture.photoPath.isNotBlank() &&
                    !capture.photoPath.startsWith("demo:")
                ) {
                    runCatching { files.delete(capture.photoPath) }
                }
                store.delete(capture.id)
                _state.value = _state.value.copy(
                    busy = false,
                    detail = null,
                    sheetCaptureId = null,
                    entryDeleteArmed = false,
                )
                _state.value.trip?.let { trip ->
                    runCatching { index.rebuild(store.confirmedForTrip(trip.id)) }
                    runCatching { refreshDay(day) }
                    runCatching { loadTripDays(trip) }
                }
                if (back()) {
                    say("The photograph is gone.")
                } else {
                    go(Route.Home)
                    say("The photograph is gone.")
                }
            }.onFailure { failure ->
                _state.value = _state.value.copy(busy = false)
                report(failure)
            }
        }
    }

    /** Leave the detail page for wherever it was opened from. */
    public fun closeDetail() {
        // Back, not a guessed destination: the detail page no longer has to
        // remember where it came from, because the stack does.
        if (back()) return
        when (_state.value.detailOrigin) {
            DetailOrigin.Search -> go(Route.Search)
            // openRecord refreshes the day and its cards, so the record shows the
            // edit that was just made.
            DetailOrigin.Record -> openRecord(_state.value.detail?.localDate)
        }
    }

    /**
     * What a photograph looked like before the last edit, so the edit can be taken
     * back exactly — including the rate its figure was pinned at, which is the part
     * a partial undo would silently move.
     */
    private data class EditSnapshot(
        val id: String,
        val day: LocalDay,
        val amountMinor: Long?,
        val currency: String?,
        val merchant: String?,
        val category: String?,
        val note: String?,
        val rateScaled: Long?,
        val rateDate: String?,
        val state: CaptureState,
    )

    private var lastEdit: EditSnapshot? = null
    private var noticeSeq = 0L

    /** Say the last thing that happened, and forget it after long enough to act. */
    private fun say(text: String, actionLabel: String? = null) {
        noticeSeq += 1
        val id = noticeSeq
        _state.value = _state.value.copy(notice = Notice(text, actionLabel, id))
        // Long enough to read and reach, short enough that a stale offer never sits
        // there claiming to undo something three edits ago.
        scope.launch {
            kotlinx.coroutines.delay(9_000)
            if (_state.value.notice?.id == id) _state.value = _state.value.copy(notice = null)
        }
    }

    /** Put the notice away. The offer goes with it: no undo for a notice you closed. */
    public fun dismissNotice() {
        _state.value = _state.value.copy(notice = null)
        lastEdit = null
    }

    /**
     * Take the last edit back.
     *
     * Restores the row as it was — amount, currency, merchant, category, note, the
     * pinned rate and the queue state — and re-derives the day and the index from
     * the store rather than patching the UI's copy. One write per column group, in
     * the order the record has always used.
     */
    public fun undoLastEdit() {
        val snapshot = lastEdit ?: return
        lastEdit = null
        _state.value = _state.value.copy(notice = null)
        scope.launch {
            store.updateDetail(
                id = snapshot.id,
                amountMinor = snapshot.amountMinor,
                currencyCode = snapshot.currency,
                merchant = snapshot.merchant,
                category = snapshot.category,
                note = snapshot.note,
            )
            store.recordRate(snapshot.id, snapshot.rateScaled, snapshot.rateDate)
            store.updateState(snapshot.id, snapshot.state)
            val restored = store.findById(snapshot.id)
            index.rebuild(store.confirmedForTrip(_state.value.trip?.id ?: return@launch))
            refreshDay(snapshot.day)
            _state.value = _state.value.copy(
                detail = restored,
                detailAmount = restored?.let { amountBuffer(it) } ?: "",
                detailMerchant = restored?.merchant ?: "",
                detailNote = restored?.note ?: "",
                detailCategory = restored?.category,
            )
        }
    }

    /**
     * Saves and steps back. An empty amount field clears the figure rather than
     * writing a zero: "I did not record this" and "this cost nothing" are
     * different claims and the record keeps them apart.
     */
    public fun saveDetail(keepWithoutAmount: Boolean = false) {
        val capture = _state.value.detail ?: return
        // A closed trip is a finished record. Its photographs are readable and its
        // card is still yours, but nothing can be filed against it - including an
        // edit that would move its totals.
        if (_state.value.trip?.isClosed == true) return
        val currency = capture.currency ?: _state.value.trip?.baseCurrency ?: CurrencyCode.of("INR")
        val parsed = if (keepWithoutAmount) null else Money.parseOrNull(_state.value.detailAmount, currency)
        // The pre-image, taken before anything is written. Without it an edit is a
        // one-way door, and a one-way door on a figure someone already read is the
        // reason people stop trusting a ledger.
        val before = EditSnapshot(
            id = capture.id,
            day = capture.localDate,
            amountMinor = capture.amountMinor,
            currency = capture.currency?.code,
            merchant = capture.merchant,
            category = capture.category?.name,
            note = capture.note,
            rateScaled = capture.rateScaled,
            rateDate = capture.rateDate?.iso,
            state = capture.state,
        )
        scope.launch {
            store.updateDetail(
                id = capture.id,
                amountMinor = parsed?.minorUnits,
                currencyCode = if (parsed == null) null else currency.code,
                merchant = _state.value.detailMerchant.trim().ifBlank { null },
                category = _state.value.detailCategory?.name,
                note = _state.value.detailNote.trim().ifBlank { null },
            )
            // Same rule as the stack: the figure someone just saved is converted at
            // the rate of that capture's day, and pinned there.
            pinRate(capture, parsed?.minorUnits, currency)
            if (parsed != null || keepWithoutAmount) {
                // A figure entered here is a decided figure, so the card leaves the
                // close-day stack through the same door the pager uses.
                outbox.apply(capture.id, if (keepWithoutAmount) QueueEvent.MarkedNoAmount else QueueEvent.AmountEntered)
                outbox.apply(capture.id, QueueEvent.Confirmed)
            }
            val saved = store.findById(capture.id)
            index.rebuild(store.confirmedForTrip(capture.tripId))
            refreshDay(capture.localDate)
            val fromSheet = _state.value.sheetCaptureId != null
            _state.value = if (fromSheet) {
                // Filed from the sheet: the sheet closes, the page underneath is the
                // page it was opened on, and the day's figure has already moved.
                _state.value.copy(
                    sheetCaptureId = null,
                    detail = saved,
                    detailCategory = saved?.category,
                )
            } else {
                // Filed from the whole page: that page was a destination of its own,
                // so it leaves for the day it belongs to.
                _state.value.copy(
                    route = Route.Record(capture.localDate),
                    detail = saved,
                    detailCategory = saved?.category,
                )
            }
            lastEdit = before
            say("Saved", "Undo")
        }
    }

    // -- search ------------------------------------------------------------

    public fun search(query: String) {
        val results = index.search(query)
        _state.value = _state.value.copy(searchQuery = query, searchResults = results)
    }

    // -- navigation --------------------------------------------------------

    public fun go(route: Route) {
        val current = _state.value.route
        if (current == route) return
        // Moving between pages closes whatever is open over them: an overlay belongs
        // to the page it was raised from, not to the app.
        if (_state.value.shutterOpen || _state.value.sheetCaptureId != null) {
            _state.value = _state.value.copy(shutterOpen = false, sheetCaptureId = null)
        }
        // "Today" is a promise about a date, not just a page: arriving on it reads
        // the calendar's today, even if the last thing looked at was the 14th.
        if (route == Route.Home) {
            _state.value.trip?.let { trip ->
                val day = clock.today().coerceInto(trip)
                if (day != _state.value.today) scope.launch { runCatching { refreshDay(day) } }
            }
        }
        // A move between the four places starts a fresh stack; a move into a page
        // pushes the place it came from. One rule, stated once, so Back means the
        // same thing on every surface.
        _state.value = _state.value.copy(
            route = route,
            history = if (route.isDestination()) {
                // Landing on a place still gives Back one move: to the place you
                // came from, if it was a place.
                if (current.isDestination() && current != route) listOf(current) else emptyList()
            } else {
                (current.takeIf { current != Route.Onboarding && current != Route.NewTrip }
                    ?.let { _state.value.history + it } ?: _state.value.history)
                    .takeLast(MAX_HISTORY)
            },
        )
    }

    /**
     * One step back, or false when there is nowhere left to go.
     *
     * Returning false is the signal the shell needs: with an empty stack the
     * platform's own Back (leave the app) is the right answer, and swallowing it
     * to do nothing is how an app gets a reputation for ignoring the gesture.
     */
    public fun back(): Boolean {
        val state = _state.value
        // Whatever is open over the page is what Back closes first. That is the
        // platform's own expectation, and it is also the reason the overlays are
        // state rather than routes: one gesture, one meaning.
        if (state.sheetCaptureId != null) {
            closeSheet()
            return true
        }
        if (state.shutterOpen) {
            closeShutter()
            return true
        }
        val target = state.history.lastOrNull() ?: return false
        _state.value = state.copy(route = target, history = state.history.dropLast(1))
        // The place being returned to owns its own figures; re-read them rather
        // than showing what it looked like before the page that was open on top.
        val trip = state.trip
        if (trip != null) scope.launch { runCatching { loadTripDays(trip) } }
        return true
    }

    /**
     * Fold or unfold the hanging rail.
     *
     * A view preference, so it is not persisted: reopening the app returns to the
     * full spread, which is the composition the app was designed to be read in.
     */
    public fun toggleRail() {
        _state.value = _state.value.copy(railDocked = !_state.value.railDocked)
    }

    /**
     * Make another trip the active one.
     *
     * Switching is a read, not a write: it re-binds the pages to a trip already on
     * disk. Nothing is moved or merged, because nothing here has a home other than
     * the device it was written on.
     */
    public fun switchTrip(id: String) {
        val target = _state.value.trips.firstOrNull { it.id == id } ?: return
        scope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching { bindTrip(target) }.onFailure { error ->
                report(error)
                _state.value = _state.value.copy(
                    busy = false,
                    draft = _state.value.draft.copy(
                        error = "That trip could not be opened. " +
                            (error.message ?: "Unknown error"),
                    ),
                )
            }
            _state.value = _state.value.copy(busy = false)
        }
    }

    /** Re-read the register: after a trip is created, closed or deleted. */
    public fun refreshTrips() {
        scope.launch {
            val register = runCatching { store.trips() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(trips = register)
        }
    }

    /**
     * The shutter, with the one guard the whole closed-trip matrix rests on.
     *
     * Every way into the camera goes through this: the bar's shutter, the Record
     * page's two actions, the shell's keyboard shortcut, the "take another" on a
     * frame. A closed trip has nothing to add, so the guard lives here rather than
     * in five pages that each have to remember it.
     */
    /**
     * The shutter, opened over whatever is showing.
     *
     * No navigation happens here, and that is the whole point: the old shutter was a
     * route, so keeping a receipt cost three screens — the page you were on, the
     * viewfinder, and the frame. Two of those were the same nothing, and the one you
     * wanted to come back to had to be rebuilt on the way back.
     *
     * With no trip, or a closed one, there is nothing a photograph could belong to,
     * so the explanation page is still the right answer rather than a camera that
     * files into nowhere.
     */
    public fun openCapture(day: LocalDay? = null) {
        val trip = _state.value.trip
        if (trip == null || trip.isClosed) {
            go(Route.Capture)
            return
        }
        // Which day this photograph will belong to. The page the shutter was opened
        // from can name it — the record for Tuesday asks for Tuesday — and the answer
        // is never a day in the future, because there is nothing there to photograph.
        val filing = RecordDates.chosenOrFiling(
            chosen = day,
            today = clock.today(),
            start = trip.startDate,
            end = trip.endDate,
        )
        if (filing == null) {
            // The journey has not begun. A shutter here would file into a day that
            // does not exist yet, so it does not open.
            say("This journey starts on ${trip.startDate.iso}. There is nothing to photograph yet.")
            return
        }
        _state.value = _state.value.copy(
            shutterOpen = true,
            shutterDay = filing,
            captureError = null,
        )
    }

    /**
     * Whether a day has happened, for a page that has to know before it offers a
     * control. The record's one rule, in one line, so no page re-implements it.
     */
    public fun hasHappened(day: LocalDay?): Boolean =
        day != null && RecordDates.isFiled(day, clock.today())

    /**
     * The days the shutter may file into, newest first.
     *
     * The day sheet is this list and nothing else, which is why a future day is not
     * merely refused there — it is absent, so there is nothing to press.
     */
    public fun daysOpenForFiling(): List<LocalDay> {
        val trip = _state.value.trip ?: return emptyList()
        return RecordDates.openDays(clock.today(), trip.startDate, trip.endDate).asReversed()
    }

    /**
     * Files the open shutter into another day of the trip.
     *
     * Only days that have happened are on offer, so this cannot move the shutter
     * forward: a day in the future, or a day outside the trip, is ignored and the
     * shutter keeps the day it had.
     */
    public fun chooseShutterDay(day: LocalDay) {
        val trip = _state.value.trip ?: return
        val allowed = RecordDates.chosenOrFiling(
            chosen = day,
            today = clock.today(),
            start = trip.startDate,
            end = trip.endDate,
        ) ?: return
        if (allowed != day) return
        _state.value = _state.value.copy(shutterDay = allowed)
    }

    /** Put the camera away without taking anything. */
    public fun closeShutter() {
        _state.value = _state.value.copy(shutterOpen = false, captureError = null)
    }

    /** Put the sheet away. The photograph stays in the record, where it was filed. */
    public fun closeSheet() {
        _state.value = _state.value.copy(sheetCaptureId = null)
    }

    /**
     * Show a day without leaving the page.
     *
     * The Days tab picks a date out of a strip, which is a *look*, not a move: the
     * page it is looking at is the same page. Routing to the record and back for
     * every date would make a strip into a maze.
     */
    public fun viewDay(day: LocalDay) {
        scope.launch { runCatching { refreshDay(day) }.onFailure(::report) }
    }

    public fun openRecord(day: LocalDay? = null) {
        scope.launch {
            val trip = _state.value.trip
            if (trip != null) {
                _state.value = _state.value.copy(dayCards = store.dayCards(trip.id))
            }
            if (day != null) refreshDay(day)
            go(Route.Record(day))
        }
    }

    // -- internals ---------------------------------------------------------

    /**
     * The whole trip in one pass: two queries, grouped by day.
     *
     * The timeline shows every day with its own band, so loading only the open day
     * would leave the others claiming "no photographs here" — a statement the app
     * has not earned the right to make. Figures are converted through the same FX
     * table the ritual uses.
     */
    // -- two phones, one ledger --------------------------------------------
    //
    // A code is a copy, not a link. Everything below produces or consumes one, and
    // nothing below opens a socket: the plan arrives as sixteen characters and the
    // figures as one line of text, and after that each phone is on its own.

    /** The code that carries this trip to another phone, or null when there is no trip. */
    public fun tripCode(): String? {
        val trip = _state.value.trip ?: return null
        return runCatching {
            ShareCodes.encodeTrip(
                ShareCodes.Plan(
                    start = trip.startDate,
                    end = trip.endDate,
                    baseCurrency = trip.baseCurrency,
                    budgetMinor = trip.budgetMinor,
                ),
            )
        }.getOrNull()
    }

    /**
     * True when the trip has a budget that a code cannot carry.
     *
     * Thirty-two bits of minor units is about four crore; a budget above that is left
     * out of the code rather than rounded, and the sharing page says so instead of
     * letting the other phone quietly acquire a cheaper trip.
     */
    public fun tripCodeDroppedBudget(): Boolean {
        val trip = _state.value.trip ?: return false
        if (trip.budgetMinor == null) return false
        val code = tripCode() ?: return false
        return ShareCodes.decodeTripOrNull(code)?.budgetMinor == null
    }

    public fun copyTripCode() {
        val code = tripCode() ?: return
        val copied = clipboard?.copy(code) == true
        _state.value = _state.value.copy(codeCopied = copied)
        if (copied) say("The trip code is on the clipboard.")
    }

    /**
     * The line that hands one day's figures to somebody else.
     *
     * Only a finished day, and only amounts the person confirmed: a photograph still
     * waiting for a figure is not a figure to hand over. Null when the day has nothing
     * in it, which is a real answer and one the page states.
     */
    public fun ledgerCodeFor(day: LocalDay): String? {
        val trip = _state.value.trip ?: return null
        val captures = _state.value.dayCapturesAll[day.iso].orEmpty()
            .filter { it.state == CaptureState.CONFIRMED || it.state == CaptureState.ARCHIVED }
            .filter { it.amountMinor != null && it.currency != null }
        if (captures.isEmpty()) return null
        val name = settings.displayName.trim().ifEmpty { "me" }
        return runCatching {
            ShareCodes.encodeLedger(
                ShareCodes.Ledger(
                    traveller = name,
                    entries = captures.map { capture ->
                        ShareCodes.Entry(
                            day = capture.localDate,
                            amountMinor = capture.amountMinor ?: 0L,
                            currency = capture.currency ?: trip.baseCurrency,
                            category = capture.category,
                            merchantInitials = initialsOf(capture.merchant),
                        )
                    },
                ),
            )
        }.getOrNull()
    }

    public fun copyLedgerCode(day: LocalDay) {
        val code = ledgerCodeFor(day)
        if (code == null) {
            // A button that copies nothing and says nothing is the one thing this app
            // does not do. Say why there is no line to hand over.
            say("That day has no confirmed figures to hand over yet.")
            return
        }
        val copied = clipboard?.copy(code) == true
        _state.value = _state.value.copy(codeCopied = copied)
        if (copied) say("The day is on the clipboard — send the whole line.")
    }

    /** Opening the join page: a blank code, and nothing remembered from last time. */
    public fun openJoinTrip() {
        _state.value = _state.value.copy(joinCode = "", joinName = "", joinPlan = null, joinError = null)
        go(Route.JoinTrip)
    }

    /** Types a code, and says what it is the moment it reads. */
    public fun editJoinCode(text: String) {
        _state.value = _state.value.copy(
            joinCode = text,
            joinPlan = ShareCodes.decodeTripOrNull(text),
            joinError = ShareCodes.tripProblem(text),
        )
    }

    public fun editJoinName(text: String) {
        _state.value = _state.value.copy(joinName = text)
    }

    /**
     * Creates the trip the code describes.
     *
     * A joined trip is an ordinary trip in every respect - it gets its own day cards,
     * its own shutter and its own card - because the plan is data, not a subscription.
     * Nothing here will ever update when the other phone does something.
     */
    public fun joinTrip() {
        val plan = _state.value.joinPlan ?: return
        val name = _state.value.joinName.trim().ifEmpty { "the shared trip" }
        scope.launch {
            val trip = Trip(
                id = newId(),
                name = name,
                startDate = plan.start,
                endDate = plan.end,
                baseCurrency = plan.baseCurrency,
                homeCurrency = plan.baseCurrency,
                budgetMinor = plan.budgetMinor,
            )
            runCatching {
                store.createTrip(trip)
                _state.value = _state.value.copy(joinCode = "", joinName = "", joinPlan = null, joinError = null)
                bindTrip(trip)
                // The join page is behind this trip now, not under it: Back from Home
                // should leave the app, not offer to join the same trip again.
                _state.value = _state.value.copy(history = emptyList())
                say("Joined “$name”. The plan is yours now — nothing syncs.")
            }.onFailure(::report)
        }
    }

    /** The hand-off page: both directions, in one place. */
    public fun openHandoff() {
        _state.value = _state.value.copy(ledgerPaste = "", ledgerPreview = null, ledgerError = null, codeCopied = false)
        go(Route.Handoff)
    }

    /** Types or pastes a traveller's line, and says what it says. */
    public fun editLedgerPaste(text: String) {
        _state.value = _state.value.copy(
            ledgerPaste = text,
            ledgerPreview = ShareCodes.decodeLedgerOrNull(text),
            ledgerError = ShareCodes.ledgerProblem(text),
        )
    }

    /**
     * Stores a traveller's figures beside this trip.
     *
     * The same line pasted twice is refused by its fingerprint rather than added
     * twice: a code can be handed over more than once, and a doubled total is the
     * first thing that would make this feature untrustworthy.
     */
    public fun addPastedLedger() {
        val trip = _state.value.trip ?: return
        val ledger = _state.value.ledgerPreview ?: return
        val fingerprint = ShareCodes.fingerprint(_state.value.ledgerPaste)
        scope.launch {
            runCatching {
                if (store.travellerByCode(trip.id, fingerprint) != null) {
                    _state.value = _state.value.copy(ledgerError = "That ledger is already on this trip.")
                    return@runCatching
                }
                val traveller = Traveller(
                    id = newId(),
                    tripId = trip.id,
                    name = ledger.traveller,
                    addedAt = clock.nowMillis(),
                    codeFingerprint = fingerprint,
                    entries = ledger.entries.map { entry ->
                        TravellerEntry(
                            day = entry.day,
                            amountMinor = entry.amountMinor,
                            currency = entry.currency,
                            category = entry.category,
                            merchantInitials = entry.merchantInitials,
                        )
                    },
                )
                store.addTraveller(traveller)
                _state.value = _state.value.copy(ledgerPaste = "", ledgerPreview = null, ledgerError = null)
                loadTravellers(trip)
                say("${ledger.traveller}’s figures are here — ${ledger.entries.size} lines.")
            }.onFailure(::report)
        }
    }

    /** Takes a traveller off this trip. Their rows go; yours were never touched. */
    public fun removeTraveller(id: String) {
        val trip = _state.value.trip ?: return
        scope.launch {
            runCatching {
                val name = _state.value.travellers.firstOrNull { it.id == id }?.name
                store.removeTraveller(id)
                loadTravellers(trip)
                if (name != null) say("$name’s figures are off the trip.")
            }.onFailure(::report)
        }
    }

    /** Two initials for a merchant, so a handed-over line stays a line. */
    private fun initialsOf(merchant: String?): String? {
        val words = merchant.orEmpty().split(' ', '-', '.', ',', '/').filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        return words.take(2).joinToString("") { word -> word.first().uppercase() }
    }

    /**
     * Converts somebody else's figures into the trip's money for one window.
     *
     * The same rule as the record's own total, in the same place: an amount with no
     * rate is named in its own currency and is never added to one in another.
     */
    private fun sharesFor(
        travellers: List<Traveller>,
        trip: Trip,
        day: LocalDay?,
    ): List<TravellerShare> = travellers.mapNotNull { traveller ->
        val entries = traveller.entriesOn(day)
        if (entries.isEmpty()) return@mapNotNull null
        var base = 0L
        val unconverted = mutableMapOf<String, Long>()
        entries.forEach { entry ->
            val conversion = fx.convert(
                Money.ofMinor(entry.amountMinor),
                entry.currency,
                trip.baseCurrency,
                entry.day,
            )
            if (conversion.applied) {
                base += conversion.amount.minorUnits
            } else {
                unconverted[entry.currency.code] = (unconverted[entry.currency.code] ?: 0L) + entry.amountMinor
            }
        }
        TravellerShare(traveller = traveller, baseMinor = base, unconverted = unconverted)
    }

    private suspend fun loadTravellers(trip: Trip) {
        val travellers = runCatching { store.travellers(trip.id) }.getOrDefault(emptyList())
        _state.value = _state.value.copy(
            travellers = travellers,
            travellerShares = sharesFor(travellers, trip, null),
            travellerSharesForDay = sharesFor(travellers, trip, _state.value.today),
        )
    }

    private suspend fun loadTripDays(trip: Trip) {
        val all = store.confirmedForTrip(trip.id) + store.openCaptures().filter { it.tripId == trip.id }
        val byDay = all.groupBy { it.localDate.iso }
        val figures = byDay.mapValues { (_, captures) ->
            Tally(fx, trip.baseCurrency)
                // No date argument: each capture converts at its own pinned rate,
                // or at the rate table for its own day. Passing the first capture's
                // day here is what made the timeline disagree with the day page.
                .total(captures.filter { it.state == CaptureState.CONFIRMED || it.state == CaptureState.ARCHIVED })
                .baseTotal.minorUnits
        }
        val tripTally = Tally(fx, trip.baseCurrency).total(all.filter {
            it.state == CaptureState.CONFIRMED || it.state == CaptureState.ARCHIVED
        })
        _state.value = _state.value.copy(
            dayCapturesAll = byDay.mapKeys { it.key },
            dayFigures = figures,
            tripUnconverted = tripTally.unconverted.mapKeys { it.key.code }.mapValues { it.value },
            tripPerCurrency = tripTally.perCurrency.mapKeys { it.key.code }.mapValues { it.value },
            tripTotalMinor = tripTally.baseTotal.minorUnits,
        )
    }

    private suspend fun refreshDay(day: LocalDay) {
        val trip = _state.value.trip ?: return
        val captures = store.capturesForDay(trip.id, day).sortedBy { it.createdAt }
        val stack = captures.filter {
            it.state == CaptureState.QUEUED ||
                it.state == CaptureState.EXTRACTING ||
                it.state == CaptureState.NEEDS_AMOUNT ||
                it.state == CaptureState.NEEDS_CONFIRMATION ||
                it.state == CaptureState.FAILED
        }
        val tally = Tally(fx, trip.baseCurrency).total(
            captures.filter { it.state == CaptureState.CONFIRMED || it.state == CaptureState.ARCHIVED },
        )
        val indexNow = _state.value.stackIndex.coerceIn(0, (stack.size - 1).coerceAtLeast(0))
        _state.value = _state.value.copy(
            today = day,
            dayCaptures = captures,
            stack = stack,
            stackIndex = indexNow,
            dayTotalMinor = tally.baseTotal.minorUnits,
            dayCurrency = trip.baseCurrency.code,
            dayPerCurrency = tally.perCurrency.mapKeys { it.key.code }.mapValues { it.value },
            dayUnconverted = tally.unconverted.mapKeys { it.key.code }.mapValues { it.value },
            dayRateDates = tally.rateDates.mapKeys { it.key.code }.mapValues { it.value.iso },
            // Aggregated here, from the same finished captures the trip total is
            // computed from, and not by a SQL GROUP BY that returned one row per
            // currency. Stats and Home now add up the same money or neither does.
            categoryTotals = com.ariadne.fx.CategoryTotals.of(
                captures = store.confirmedForTrip(trip.id),
                fx = fx,
                baseCurrency = trip.baseCurrency,
            ),
            // Somebody else's figures, quoted beside the day rather than merged into
            // it: the same conversion rule, and the same refusal to add an amount that
            // has no rate.
            travellerSharesForDay = sharesFor(_state.value.travellers, trip, day),
        )
        loadTripDays(trip)
        _state.value = _state.value.copy(
            amountInput = stack.getOrNull(indexNow)
                ?.takeIf { it.amountMinor != null }
                ?.let { c -> c.amountMinor?.let { Money.ofMinor(it).format(c.currency ?: trip.baseCurrency, false) } }
                ?: _state.value.amountInput,
        )
    }

    private fun LocalDay.coerceInto(trip: Trip): LocalDay = when {
        this < trip.startDate -> trip.startDate
        this > trip.endDate -> trip.endDate
        else -> this
    }
}
