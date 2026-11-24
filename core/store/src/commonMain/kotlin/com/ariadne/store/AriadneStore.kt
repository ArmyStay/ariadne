package com.ariadne.store

import app.cash.sqldelight.db.SqlDriver
import com.ariadne.fx.FxTable
import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CategoryTotal
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.Rate
import com.ariadne.model.Settings
import com.ariadne.model.Trip
import com.ariadne.model.dayRange
import com.ariadne.queue.CaptureRepository
import com.ariadne.store.db.AriadneDb
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The one place SQL is touched.
 *
 * Drivers are provided per target (`AndroidSqliteDriver`, `NativeSqliteDriver`,
 * `JdbcSqliteDriver`, web-worker driver); everything above this class sees the
 * domain types only, which is why `:ui:*` never imports a driver.
 */
public class AriadneStore(
    driver: SqlDriver,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) : Store {

    private val db = AriadneDb(driver)
    private val q = db.ariadneQueries
    private val revision = MutableStateFlow(0L)

    // -- trips -------------------------------------------------------------

    public suspend override fun trips(): List<Trip> = withContext(io) {
        q.selectTrips().executeAsList().map(::toTrip)
    }

    public suspend override fun trip(id: String): Trip? = withContext(io) {
        q.selectTripById(id).executeAsOneOrNull()?.let(::toTrip)
    }

    public suspend override fun activeTrip(): Trip? = withContext(io) {
        q.selectTrips().executeAsList().map(::toTrip).firstOrNull { !it.isClosed }
    }

    /**
     * Creates a trip and its empty day cards in one transaction. A trip with a
     * missing day would be a hole in the record before the first capture.
     */
    public suspend override fun createTrip(trip: Trip): Unit = withContext(io) {
        q.transaction {
            q.insertTrip(
                id = trip.id,
                name = trip.name,
                startDate = trip.startDate.iso,
                endDate = trip.endDate.iso,
                baseCurrency = trip.baseCurrency.code,
                homeCurrency = trip.homeCurrency.code,
                place = trip.place,
                budgetMinor = trip.budgetMinor,
                closedAt = trip.closedAt,
            )
            dayRange(trip.startDate, trip.endDate).forEach { day ->
                q.upsertDayCard(id = "${trip.id}:$day", tripId = trip.id, localDate = day.iso)
            }
        }
        bump()
    }

    public suspend override fun updateTrip(trip: Trip): Unit = withContext(io) {
        q.updateTrip(
            name = trip.name,
            place = trip.place,
            startDate = trip.startDate.iso,
            endDate = trip.endDate.iso,
            baseCurrency = trip.baseCurrency.code,
            homeCurrency = trip.homeCurrency.code,
            budgetMinor = trip.budgetMinor,
            id = trip.id,
        )
    }

    public suspend override fun closeTrip(id: String, at: Long): Unit = withContext(io) {
        q.closeTrip(at, id)
        bump()
    }

    public suspend override fun deleteTrip(id: String): Unit = withContext(io) {
        q.transaction {
            q.deleteTrip(id)
            q.selectDayCards(id).executeAsList()
        }
        bump()
    }

    // -- capture: CaptureRepository ---------------------------------------

    override suspend fun insert(capture: Capture): Unit = withContext(io) {
        q.insertCapture(
            id = capture.id,
            tripId = capture.tripId,
            localDate = capture.localDate.iso,
            photoPath = capture.photoPath,
            createdAt = capture.createdAt,
            state = capture.state.name,
            amountMinor = capture.amountMinor,
            currency = capture.currency?.code,
            merchant = capture.merchant,
            extractionConfidence = capture.extractionConfidence,
            confirmedAt = capture.confirmedAt,
            markedNoAmount = if (capture.markedNoAmount) 1L else 0L,
            category = capture.category?.name,
            note = capture.note,
        )
        bump()
    }

    override suspend fun updateState(id: String, state: CaptureState): Unit = withContext(io) {
        q.updateCaptureState(state.name, id)
        bump()
    }

    override suspend fun updateDetails(
        id: String,
        amountMinor: Long?,
        currencyCode: String?,
        merchant: String?,
        markedNoAmount: Boolean,
        confirmedAt: Long?,
    ): Unit = withContext(io) {
        q.updateCaptureDetails(
            amountMinor = amountMinor,
            currency = currencyCode,
            merchant = merchant,
            markedNoAmount = if (markedNoAmount) 1L else 0L,
            confirmedAt = confirmedAt,
            id = id,
        )
        bump()
    }

    override suspend fun findById(id: String): Capture? = withContext(io) {
        q.selectCaptureById(id).executeAsOneOrNull()?.let(::toCapture)
    }

    override suspend fun openCaptures(): List<Capture> = withContext(io) {
        q.selectOpenCaptures().executeAsList().map(::toCapture)
    }

    override suspend fun capturesForDay(tripId: String, day: LocalDay): List<Capture> = withContext(io) {
        q.selectCapturesForDay(tripId, day.iso).executeAsList().map(::toCapture)
    }

    override suspend fun delete(id: String): Unit = withContext(io) {
        q.deleteCapture(id)
        bump()
    }

    // -- capture: extras used by the extraction worker ---------------------

    public suspend override fun capturesReadyForExtraction(nowMillis: Long): List<Capture> = withContext(io) {
        q.selectOpenCapturesReady(nowMillis).executeAsList().map(::toCapture)
    }

    public suspend override fun recordRetry(id: String, failedAttempts: Int, notBefore: Long?): Unit = withContext(io) {
        q.updateCaptureRetry(failedAttempts.toLong(), notBefore, id)
        bump()
    }

    public suspend override fun staleExtractions(): List<Capture> = withContext(io) {
        q.selectExtracting().executeAsList().map(::toCapture)
    }

    public suspend override fun confirmedForTrip(tripId: String): List<Capture> = withContext(io) {
        q.selectConfirmedForTrip(tripId).executeAsList().map(::toCapture)
    }

    // -- days --------------------------------------------------------------

    public suspend override fun dayCards(tripId: String): List<DayCard> = withContext(io) {
        q.selectDayCards(tripId).executeAsList().map { row ->
            DayCard(
                id = row.id,
                tripId = row.tripId,
                localDate = LocalDay.parseOrNull(row.localDate) ?: LocalDay.of(1970, 1, 1),
                closedAt = row.closedAt,
            )
        }
    }

    public suspend override fun closeDay(tripId: String, day: LocalDay, at: Long): Unit = withContext(io) {
        q.transaction {
            q.closeDay(at, tripId, day.iso)
        }
        bump()
    }

    public suspend override fun reopenDay(tripId: String, day: LocalDay): Unit = withContext(io) {
        q.reopenDay(tripId, day.iso)
        bump()
    }

    // -- rates -------------------------------------------------------------

    public suspend override fun allRates(): List<Rate> = withContext(io) {
        q.selectAllRates().executeAsList().map { row ->
            Rate(
                base = CurrencyCode.of(row.base),
                quote = CurrencyCode.of(row.quote),
                date = LocalDay.parseOrNull(row.rateDate) ?: LocalDay.of(1970, 1, 1),
                rateScaled = row.rateScaled,
            )
        }
    }

    public suspend override fun upsertRate(rate: Rate): Unit = withContext(io) {
        q.upsertRate(rate.base.code, rate.quote.code, rate.date.iso, rate.rateScaled)
        bump()
    }

    // -- settings ----------------------------------------------------------

    public suspend override fun settings(): Settings = withContext(io) {
        q.selectSettings().executeAsOneOrNull()?.let { row ->
            Settings(
                baseCurrency = CurrencyCode.of(row.baseCurrency),
                homeCurrency = CurrencyCode.of(row.homeCurrency),
                theme = runCatching { Settings.Theme.valueOf(row.theme) }.getOrDefault(Settings.Theme.SYSTEM),
                ocrProvider = runCatching { Settings.OcrProvider.valueOf(row.ocrProvider) }
                    .getOrDefault(Settings.OcrProvider.ON_DEVICE),
            )
        } ?: Settings()
    }

    public suspend override fun saveSettings(settings: Settings): Unit = withContext(io) {
        q.upsertSettings(
            baseCurrency = settings.baseCurrency.code,
            homeCurrency = settings.homeCurrency.code,
            theme = settings.theme.name,
            ocrProvider = settings.ocrProvider.name,
        )
        bump()
    }

    /** Bumped after every write so screens can re-read without polling. */
    public override val changes: Flow<Long> = revision

    public fun changeFlow(): Flow<Long> = revision.map { it }.flowOn(io)

    private fun bump() {
        revision.value = revision.value + 1
    }

    // -- mapping -----------------------------------------------------------

    private fun toTrip(row: com.ariadne.store.db.Trip) = Trip(
        id = row.id,
        name = row.name,
        startDate = LocalDay.parseOrNull(row.startDate) ?: LocalDay.of(1970, 1, 1),
        endDate = LocalDay.parseOrNull(row.endDate) ?: LocalDay.of(1970, 1, 1),
        baseCurrency = CurrencyCode.of(row.baseCurrency),
        homeCurrency = CurrencyCode.of(row.homeCurrency),
        closedAt = row.closedAt,
        place = row.place,
        budgetMinor = row.budgetMinor,
    )

    private fun toCapture(row: com.ariadne.store.db.Capture) = Capture(
        id = row.id,
        tripId = row.tripId,
        localDate = LocalDay.parseOrNull(row.localDate) ?: LocalDay.of(1970, 1, 1),
        photoPath = row.photoPath,
        createdAt = row.createdAt,
        state = CaptureState.entries.firstOrNull { it.name == row.state } ?: CaptureState.QUEUED,
        amountMinor = row.amountMinor,
        currency = CurrencyCode.parseOrNull(row.currency),
        merchant = row.merchant,
        extractionConfidence = row.extractionConfidence,
        confirmedAt = row.confirmedAt,
        markedNoAmount = row.markedNoAmount != 0L,
        category = Category.ofOrNull(row.category),
        note = row.note,
    )

    override suspend fun updateDetail(
        id: String,
        amountMinor: Long?,
        currencyCode: String?,
        merchant: String?,
        category: String?,
        note: String?,
    ): Unit = withContext(io) {
        q.updateCaptureDetail(
            amountMinor = amountMinor,
            currency = currencyCode,
            merchant = merchant,
            category = category,
            note = note,
            id = id,
        )
        revision.value++
    }

    /**
     * Category totals, converted into the trip's base currency through the same
     * FX table the ledger uses — so the breakdown and the total can never tell two
     * different stories. Captures with no category come back as one row with a
     * null category rather than being dropped: an uncategorised rupee is still a
     * rupee you spent.
     */
    override suspend fun categoryTotals(tripId: String): List<CategoryTotal> = withContext(io) {
        val trip = trip(tripId) ?: return@withContext emptyList()
        val rates = allRates()
        val rows = q.selectCategoryTotals(tripId).executeAsList().mapNotNull { row ->
            val category = Category.ofOrNull(row.category) ?: return@mapNotNull null
            val minor = row.minor?.toLong() ?: return@mapNotNull null
            val currency = CurrencyCode.parseOrNull(row.currency) ?: trip.baseCurrency
            CategoryTotal(
                category = category,
                captures = (row.captures ?: 0L).toInt(),
                baseMinor = toBase(minor, currency, trip.baseCurrency, trip.startDate, rates),
            )
        }
        val out = rows.toMutableList()
        q.selectUncategorisedTotal(tripId).executeAsList().forEach { loose ->
            val currency = CurrencyCode.parseOrNull(loose.currency) ?: trip.baseCurrency
            out += CategoryTotal(
                category = null,
                captures = (loose.captures ?: 0L).toInt(),
                baseMinor = toBase(loose.minor?.toLong() ?: 0L, currency, trip.baseCurrency, trip.startDate, rates),
            )
        }
        out.sortedByDescending { it.baseMinor }
    }

    /** Converts minor units at the stored scaled rate, rounding once, HALF_UP. */
    /**
     * Converts one currency's minor units into the trip's base currency.
     *
     * Delegates to [FxTable], which is the app's single rounding site; this class
     * must not grow a second one. `date` is the capture's own day so a rate
     * entered later never rewrites a figure that was already closed — the amount
     * was true on the day it was spent.
     */
    private fun toBase(
        minor: Long,
        from: CurrencyCode,
        to: CurrencyCode,
        date: LocalDay,
        rates: List<Rate>,
    ): Long = FxTable(rates).convert(Money.ofMinor(minor), from, to, date).amount.minorUnits
}
