package com.ariadne.store

import com.ariadne.model.Capture
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Rate
import com.ariadne.model.Settings
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
    public suspend fun closeTrip(id: String, at: Long)
    public suspend fun deleteTrip(id: String)

    public suspend fun capturesReadyForExtraction(nowMillis: Long): List<Capture>
    public suspend fun recordRetry(id: String, failedAttempts: Int, notBefore: Long?)
    public suspend fun staleExtractions(): List<Capture>
    public suspend fun confirmedForTrip(tripId: String): List<Capture>

    public suspend fun dayCards(tripId: String): List<DayCard>
    public suspend fun closeDay(tripId: String, day: LocalDay, at: Long)
    public suspend fun reopenDay(tripId: String, day: LocalDay)

    public suspend fun allRates(): List<Rate>
    public suspend fun upsertRate(rate: Rate)

    public suspend fun settings(): Settings
    public suspend fun saveSettings(settings: Settings)

    public val changes: Flow<Long>
}
