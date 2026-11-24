package com.ariadne.store

import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Rate
import com.ariadne.model.Settings
import com.ariadne.model.Trip
import com.ariadne.model.dayRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for [AriadneStore], implementing exactly the same surface.
 *
 * Two reasons it exists and both are honest: it is the reference implementation
 * the SQLDelight queries are checked against, and it makes the whole UI layer
 * compilable and testable without code generation on any target — including the
 * JVM harness in `tools/verify-ui.sh`.
 */
public class InMemoryStore : Store {

    private val trips = LinkedHashMap<String, Trip>()
    private val captures = LinkedHashMap<String, Capture>()
    private val dayCards = LinkedHashMap<String, DayCard>()
    private val rates = LinkedHashMap<String, Rate>()
    private var settings = Settings()
    private val revision = MutableStateFlow(0L)

    private fun bump() {
        revision.value = revision.value + 1
    }

    // -- trips ---------------------------------------------------------------

    public override suspend fun trips(): List<Trip> = trips.values.sortedByDescending { LocalDay.toEpochDay(it.startDate) }

    public override suspend fun trip(id: String): Trip? = trips[id]

    public override suspend fun activeTrip(): Trip? = trips.values.firstOrNull { !it.isClosed }

    public override suspend fun createTrip(trip: Trip) {
        trips[trip.id] = trip
        dayRange(trip.startDate, trip.endDate).forEach { day ->
            dayCards[dayKey(trip.id, day)] = DayCard("${trip.id}:$day", trip.id, day, null)
        }
        bump()
    }

    public override suspend fun closeTrip(id: String, at: Long) {
        trips[id]?.let { trips[id] = it.copy(closedAt = at) }
        bump()
    }

    public override suspend fun deleteTrip(id: String) {
        trips.remove(id)
        captures.values.filter { it.tripId == id }.map { it.id }.forEach(captures::remove)
        dayCards.values.filter { it.tripId == id }.map { it.id }.forEach(dayCards::remove)
        bump()
    }

    // -- captures (CaptureRepository) ----------------------------------------

    override suspend fun insert(capture: Capture) {
        captures[capture.id] = capture
        bump()
    }

    override suspend fun updateState(id: String, state: CaptureState) {
        captures[id]?.let { captures[id] = it.copy(state = state) }
        bump()
    }

    override suspend fun updateDetails(
        id: String,
        amountMinor: Long?,
        currencyCode: String?,
        merchant: String?,
        markedNoAmount: Boolean,
        confirmedAt: Long?,
    ) {
        captures[id]?.let {
            captures[id] = it.copy(
                amountMinor = amountMinor,
                currency = CurrencyCode.parseOrNull(currencyCode),
                merchant = merchant ?: it.merchant,
                markedNoAmount = markedNoAmount,
                confirmedAt = confirmedAt,
            )
        }
        bump()
    }

    override suspend fun findById(id: String): Capture? = captures[id]

    override suspend fun openCaptures(): List<Capture> =
        captures.values.filter { it.state.isOpen }.sortedBy { it.createdAt }

    override suspend fun capturesForDay(tripId: String, day: LocalDay): List<Capture> =
        captures.values.filter { it.tripId == tripId && it.localDate == day }.sortedBy { it.createdAt }

    override suspend fun delete(id: String) {
        captures.remove(id)
        bump()
    }

    // -- extras the model layer uses -----------------------------------------

    public override suspend fun capturesReadyForExtraction(nowMillis: Long): List<Capture> =
        captures.values.filter { it.state == CaptureState.QUEUED }.sortedBy { it.createdAt }

    public override suspend fun recordRetry(id: String, failedAttempts: Int, notBefore: Long?) = Unit

    public override suspend fun staleExtractions(): List<Capture> =
        captures.values.filter { it.state == CaptureState.EXTRACTING }

    public override suspend fun confirmedForTrip(tripId: String): List<Capture> =
        captures.values
            .filter { it.tripId == tripId && (it.state == CaptureState.CONFIRMED || it.state == CaptureState.ARCHIVED) }
            .sortedWith(compareBy({ LocalDay.toEpochDay(it.localDate) }, { it.createdAt }))

    public fun allCaptures(): List<Capture> = captures.values.sortedBy { it.createdAt }

    // -- days ----------------------------------------------------------------

    public override suspend fun dayCards(tripId: String): List<DayCard> =
        dayCards.values.filter { it.tripId == tripId }.sortedBy { LocalDay.toEpochDay(it.localDate) }

    public override suspend fun closeDay(tripId: String, day: LocalDay, at: Long) {
        dayCards[dayKey(tripId, day)]?.let { dayCards[dayKey(tripId, day)] = it.copy(closedAt = at) }
        bump()
    }

    public override suspend fun reopenDay(tripId: String, day: LocalDay) {
        dayCards[dayKey(tripId, day)]?.let { dayCards[dayKey(tripId, day)] = it.copy(closedAt = null) }
        bump()
    }

    // -- rates & settings ----------------------------------------------------

    public override suspend fun allRates(): List<Rate> = rates.values.toList()

    public override suspend fun upsertRate(rate: Rate) {
        rates["${rate.base.code}:${rate.quote.code}:${rate.date.iso}"] = rate
        bump()
    }

    public override suspend fun settings(): Settings = settings

    public override suspend fun saveSettings(settings: Settings) {
        this.settings = settings
        bump()
    }

    public override val changes: Flow<Long> get() = revision

    public fun changeFlow(): Flow<Long> = revision.map { it }

    private fun dayKey(tripId: String, day: LocalDay) = "$tripId:${day.iso}"
}
