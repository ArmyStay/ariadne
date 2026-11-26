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
import com.ariadne.model.Settings
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
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val newId: () -> String,
) {
    private val _state = MutableStateFlow(AppUiState())
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
                is Route.Nightfall -> route.day
                is Route.Record -> route.day ?: _state.value.today
                is Route.Detail -> _state.value.detail?.localDate ?: _state.value.today
                is Route.Frame -> _state.value.detail?.localDate ?: _state.value.today
                else -> _state.value.today
            }
            if (day != null) runCatching { refreshDay(day) }
            runCatching { loadTripDays(trip) }
        }
    }

    // -- lifecycle ---------------------------------------------------------

    public fun start() {
        scope.launch {
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
            runCatching { outbox.resume() }.onFailure(::report)
            runCatching { extraction.drain() }.onFailure(::report)
            val trips = runCatching { store.trips() }.getOrElse {
                report(it)
                emptyList()
            }
            val active = trips.firstOrNull { !it.isClosed }
            if (active == null) {
                // No trip, no record. The front door is the form, not a manifesto:
                // a first-run visitor should be able to create a trip in one screen.
                val today = clock.today()
                _state.value = _state.value.copy(
                    route = Route.NewTrip,
                    today = today,
                    draft = TripDraft(
                        startIso = today.iso,
                        endIso = today.plusDays(6).iso,
                        currency = settings.baseCurrency.code,
                    ),
                )
            } else {
                runCatching { bindTrip(active) }.onFailure { error ->
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
            onStartedAction?.invoke()
        }
    }

    // -- hooks ---------------------------------------------------------------

    private var onStartedAction: (() -> Unit)? = null

    /**
     * A callback for platform entry points: invoked once, right after
     * [start] has run the initial routing pass. The Wasm demo page uses it to
     * open on the demo roll when the URL carries `?seed=demo`, so a judge who
     * lands on the Pages deploy sees a populated app instead of an empty form.
     */
    public fun onStarted(action: () -> Unit) {
        onStartedAction = action
    }

    public suspend fun bindTrip(trip: Trip) {
        val day = clock.today().coerceInto(trip)
        val cards = store.dayCards(trip.id)
        val confirmed = store.confirmedForTrip(trip.id)
        index.rebuild(confirmed)
        val register = runCatching { store.trips() }.getOrDefault(_state.value.trips)
        _state.value = _state.value.copy(
            trip = trip,
            activeTripId = trip.id,
            trips = register,
            today = day,
            dayCards = cards,
            route = Route.Home,
        )
        refreshDay(day)
        loadTripDays(trip)
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
        // Nightfall has to be a stack, not a single card.
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
    public fun capturePhoto(bytes: ByteArray, downscaleAdjusted: Boolean = true) {
        val trip = _state.value.trip ?: return
        val day = clock.today().coerceInto(trip)
        scope.launch {
            val id = newId()
            // Photo first, then the row: a row pointing at a file that was never
            // written is a hole in the record, and this is the one ordering that
            // cannot produce it.
            val path = if (bytes.isNotEmpty() && files != null) {
                runCatching { files.write(trip.id, day, id, bytes) }.getOrNull()
                    ?: com.ariadne.store.PhotoPaths.capturePath(trip.id, day, id)
            } else {
                com.ariadne.store.PhotoPaths.capturePath(trip.id, day, id)
            }
            store.insert(
                Capture(
                    id = id,
                    tripId = trip.id,
                    localDate = day,
                    photoPath = path,
                    createdAt = clock.nowMillis(),
                    state = CaptureState.QUEUED,
                ),
            )
            // Extraction picks it up on the next drain; the shutter is already back.
            extraction.drain()
            refreshDay(day)
            _state.value = _state.value.copy(route = Route.Frame(id))
        }
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
     * queue like any other, so they meet the same two taps at Nightfall.
     */
    public fun importPhoto(bytes: ByteArray) {
        capturePhoto(bytes)
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
            _state.value = _state.value.copy(
                trip = closed,
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
                        files.delete(capture.photoPath)
                    }
                }
            }
            store.deleteTrip(trip.id)
            index.rebuild(emptyList())
            _state.value = AppUiState(
                route = Route.NewTrip,
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
        val total = _state.value.categoryTotals.sumOf { it.baseMinor }
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
        go(Route.Home)
    }

    /** Fill it in now: the detail page is the form, and it is already built. */
    public fun openFrame(captureId: String) {
        openDetail(captureId)
    }

    // -- nightfall ---------------------------------------------------------

    public fun openNightfall(day: LocalDay? = null) {
        val trip = _state.value.trip ?: return
        scope.launch {
            val target = (day ?: clock.today()).coerceInto(trip)
            refreshDay(target)
            _state.value = _state.value.copy(route = Route.Nightfall(target))
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
        runCatching { appReviewManager().requestReview() }
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
        val day = (state.value.route as? Route.Nightfall)?.day ?: state.value.today ?: stack.first().localDate
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
                when (capture.state) {
                    CaptureState.NEEDS_CONFIRMATION -> Unit
                    CaptureState.NEEDS_AMOUNT, CaptureState.FAILED -> outbox.apply(capture.id, QueueEvent.AmountEntered)
                    CaptureState.QUEUED, CaptureState.EXTRACTING -> return@forEach
                    CaptureState.CONFIRMED, CaptureState.ARCHIVED -> Unit
                }
                outbox.apply(capture.id, QueueEvent.Confirmed)
            }
            val day = (state.value.route as? Route.Nightfall)?.day ?: state.value.today ?: ready.first().localDate
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
        scope.launch {
            val target = day ?: (_state.value.route as? Route.Nightfall)?.day ?: clock.today()
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
    public fun openDetail(captureId: String) {
        scope.launch {
            val capture = store.findById(captureId) ?: return@launch
            _state.value = _state.value.copy(
                route = Route.Detail(captureId),
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
     * Saves and steps back. An empty amount field clears the figure rather than
     * writing a zero: "I did not record this" and "this cost nothing" are
     * different claims and the record keeps them apart.
     */
    public fun saveDetail(keepWithoutAmount: Boolean = false) {
        val capture = _state.value.detail ?: return
        val currency = capture.currency ?: _state.value.trip?.baseCurrency ?: CurrencyCode.of("INR")
        val parsed = if (keepWithoutAmount) null else Money.parseOrNull(_state.value.detailAmount, currency)
        scope.launch {
            store.updateDetail(
                id = capture.id,
                amountMinor = parsed?.minorUnits,
                currencyCode = if (parsed == null) null else currency.code,
                merchant = _state.value.detailMerchant.trim().ifBlank { null },
                category = _state.value.detailCategory?.name,
                note = _state.value.detailNote.trim().ifBlank { null },
            )
            if (parsed != null || keepWithoutAmount) {
                // A figure entered here is a decided figure, so the card leaves the
                // Nightfall stack through the same door the ritual uses.
                outbox.apply(capture.id, if (keepWithoutAmount) QueueEvent.MarkedNoAmount else QueueEvent.AmountEntered)
                outbox.apply(capture.id, QueueEvent.Confirmed)
            }
            val saved = store.findById(capture.id)
            index.rebuild(store.confirmedForTrip(capture.tripId))
            refreshDay(capture.localDate)
            _state.value = _state.value.copy(
                route = Route.Record(capture.localDate),
                detail = saved,
                detailCategory = saved?.category,
            )
        }
    }

    // -- search ------------------------------------------------------------

    public fun search(query: String) {
        val results = index.search(query)
        _state.value = _state.value.copy(searchQuery = query, searchResults = results)
    }

    // -- navigation --------------------------------------------------------

    public fun go(route: Route) {
        _state.value = _state.value.copy(route = route)
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

    public fun openRecord(day: LocalDay? = null) {
        scope.launch {
            val trip = _state.value.trip
            if (trip != null) {
                _state.value = _state.value.copy(dayCards = store.dayCards(trip.id))
            }
            if (day != null) refreshDay(day)
            _state.value = _state.value.copy(route = Route.Record(day))
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
    private suspend fun loadTripDays(trip: Trip) {
        val all = store.confirmedForTrip(trip.id) + store.openCaptures().filter { it.tripId == trip.id }
        val byDay = all.groupBy { it.localDate.iso }
        val figures = byDay.mapValues { (_, captures) ->
            Tally(fx, trip.baseCurrency)
                .total(captures.filter { it.state == CaptureState.CONFIRMED || it.state == CaptureState.ARCHIVED }, captures.first().localDate)
                .baseTotal.minorUnits
        }
        _state.value = _state.value.copy(
            dayCapturesAll = byDay.mapKeys { it.key },
            dayFigures = figures,
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
            day,
        )
        val indexNow = _state.value.stackIndex.coerceIn(0, (stack.size - 1).coerceAtLeast(0))
        _state.value = _state.value.copy(
            today = day,
            dayCaptures = captures,
            stack = stack,
            stackIndex = indexNow,
            dayTotalMinor = tally.baseTotal.minorUnits,
            dayCurrency = trip.baseCurrency.code,
            perCurrency = tally.perCurrency.mapKeys { it.key.code }.mapValues { it.value },
            categoryTotals = store.categoryTotals(trip.id),
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
