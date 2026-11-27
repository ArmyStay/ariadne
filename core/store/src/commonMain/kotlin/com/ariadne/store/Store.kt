package com.ariadne.store

import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Rate
import com.ariadne.model.Settings
import com.ariadne.model.Traveller
import com.ariadne.model.Trip
import com.ariadne.queue.CaptureRepository
import kotlinx.coroutines.flow.Flow

/**
 * Everything the app needs from storage, and nothing about how it is stored.
 *
 * The UI and model layers depend on this interface only; [AriadneStore] is the
 * SQLDelight implementation, [InMemoryStore] the reference one used by tests and
 * by the compile harness. A driver type never crosses this line.
 */
public interface Store : CaptureRepository {
    public suspend fun trips(): List<Trip>
    public suspend fun trip(id: String): Trip?
    public suspend fun activeTrip(): Trip?
    public suspend fun createTrip(trip: Trip)

    /** Edits an existing trip in place: a budget revised mid-trip is the usual case. */
    public suspend fun updateTrip(trip: Trip)
    public suspend fun closeTrip(id: String, at: Long)
    public suspend fun deleteTrip(id: String)

    public suspend fun capturesReadyForExtraction(nowMillis: Long): List<Capture>
    public suspend fun recordRetry(id: String, failedAttempts: Int, notBefore: Long?)
    public suspend fun staleExtractions(): List<Capture>
    public suspend fun confirmedForTrip(tripId: String): List<Capture>

    /** Every capture of a trip in any state: what deletion has to clean up. */
    public suspend fun capturesForTrip(tripId: String): List<Capture>

    public suspend fun dayCards(tripId: String): List<DayCard>
    public suspend fun closeDay(tripId: String, day: LocalDay, at: Long)
    public suspend fun reopenDay(tripId: String, day: LocalDay)

    public suspend fun allRates(): List<Rate>
    public suspend fun upsertRate(rate: Rate)

    /** Forget every rate for this pair: a wrong rate must be removable. */
    public suspend fun deleteRate(base: CurrencyCode, quote: CurrencyCode)

    /**
     * Somebody else's figures for this trip, oldest hand-off first.
     *
     * Kept in their own tables and returned as their own type, so no page can
     * accidentally treat a traveller's line as one of your captures.
     */
    public suspend fun travellers(tripId: String): List<Traveller>

    /** The traveller a code already produced, if it has been pasted here before. */
    public suspend fun travellerByCode(tripId: String, fingerprint: String): Traveller?

    /** Stores a traveller and every line that came with them, in one transaction. */
    public suspend fun addTraveller(traveller: Traveller)

    public suspend fun removeTraveller(id: String)

    public suspend fun settings(): Settings
    public suspend fun saveSettings(settings: Settings)

    public val changes: Flow<Long>
}
