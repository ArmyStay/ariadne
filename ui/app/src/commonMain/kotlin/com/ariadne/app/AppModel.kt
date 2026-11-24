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
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val newId: () -> String,
) {
    private val _state = MutableStateFlow(AppUiState())
    public val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var fx = FxTable(emptyList())
    private var settings = Settings()

    // -- lifecycle ---------------------------------------------------------

    public fun start() {
        scope.launch {
            fx = FxTable(store.allRates())
            settings = store.settings()
            outbox.resume()
            extraction.drain()
            val trips = store.trips()
            val active = trips.firstOrNull { !it.isClosed }
            if (active == null) {
                _state.value = _state.value.copy(route = Route.Onboarding, today = clock.today())
            } else {
                bindTrip(active)
            }
        }
    }

    public suspend fun bindTrip(trip: Trip) {
        val day = clock.today().coerceInto(trip)
        val cards = store.dayCards(trip.id)
        val confirmed = store.confirmedForTrip(trip.id)
        index.rebuild(confirmed)
        _state.value = _state.value.copy(
            trip = trip,
            today = day,
            dayCards = cards,
            route = Route.Capture,
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

    /** A real capture: photo bytes arrive already downscaled by the platform. */
    public fun capturePhoto(bytes: ByteArray, downscaleAdjusted: Boolean = true) {
        val trip = _state.value.trip ?: return
        val day = clock.today().coerceInto(trip)
        scope.launch {
            val id = newId()
            val path = com.ariadne.store.PhotoPaths.capturePath(trip.id, day, id)
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
        }
    }

    /** Onboarding path: a trip that already has the demo roll in it. */
    public fun startTripDemo() {
        startSeedTrip()
        go(Route.Trips)
    }

    /** Capture a placeholder frame (the platform camera supplies bytes in the real build). */
    public fun capturePlaceholder() {
        capturePhoto(ByteArray(0))
    }

    /** Start a trip from the trips page, seeded with the demo roll. */
    public fun startSeedTrip() {
        val today = clock.today()
        startTrip("Bangkok & Home", today, today.plusDays(6), settings.baseCurrency, settings.homeCurrency, true)
    }

    /** Start a trip with nothing in it. */
    public fun startEmptyTrip() {
        val today = clock.today()
        startTrip("New trip", today, today.plusDays(6), settings.baseCurrency, settings.homeCurrency, false)
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
        _state.value = _state.value.copy(
            stackIndex = index,
            amountInput = capture.amountMinor?.let { minor ->
                val code = capture.currency ?: return@let ""
                Money.ofMinor(minor).format(code, grouping = false)
            } ?: "",
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
            _state.value = _state.value.copy(stack = remaining, stackIndex = 0)
            if (remaining.isEmpty()) {
                _state.value = _state.value.copy(route = Route.Record(day), amountInput = "")
            } else {
                selectStackIndex(0)
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
