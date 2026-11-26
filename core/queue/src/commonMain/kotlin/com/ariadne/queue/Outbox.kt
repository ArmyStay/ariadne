package com.ariadne.queue

import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.LocalDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The outbox: every capture the app has accepted but not yet archived.
 *
 * Durable writes belong to [CaptureRepository]; this class owns ordering,
 * transition legality and retry accounting, and holds no I/O of its own so the
 * whole thing is testable in `commonTest` with an in-memory repository.
 */
public class Outbox(
    private val repository: CaptureRepository,
    private val now: () -> Long,
) {

    private val mutex = Mutex()
    // Retry accounting survives a Retried event: a capture that failed five
    // times is a capture the human should look at, not one that silently loops.
    private val failures = mutableMapOf<String, Int>()
    private val retryNotBefore = mutableMapOf<String, Long>()
    private val _pending = MutableStateFlow<List<Capture>>(emptyList())
    public val pending: StateFlow<List<Capture>> = _pending.asStateFlow()

    /**
     * Resume-on-start: anything left mid-flight (a process death during
     * extraction) is returned to [CaptureState.QUEUED] before the UI binds.
     */
    public suspend fun resume() = mutex.withLock {
        val open = repository.openCaptures()
        open.filter { it.state == CaptureState.EXTRACTING }.forEach { stale ->
            repository.updateState(stale.id, CaptureState.QUEUED)
        }
        publish()
    }

    /** Commits a capture. The caller must persist the photo *before* this returns. */
    public suspend fun enqueue(capture: Capture): Unit = mutex.withLock {
        repository.insert(capture.copy(state = CaptureState.QUEUED, createdAt = capture.createdAt))
        failures.remove(capture.id)
        publish()
    }

    /** Enqueues a batch in capture order, one durable write per capture. */
    public suspend fun enqueueAll(captures: List<Capture>) {
        captures.forEach { enqueue(it) }
    }

    /** The next capture the extractor should look at, oldest first. */
    public suspend fun nextForExtraction(): Capture? {
        val horizon = now()
        val open = repository.openCaptures()
        return open
            .filter { it.state == CaptureState.QUEUED }
            .filter { (retryNotBefore[it.id] ?: Long.MIN_VALUE) <= horizon }
            .minByOrNull { it.createdAt }
    }

    /** When a backed-off capture becomes eligible again. Exposed for the UI's honesty, not for polling. */
    public fun retryNotBefore(id: String): Long? = retryNotBefore[id]

    /**
     * Applies an event to one capture. Returns the resulting state, or null if
     * the machine refused the move (which is a programming error, not a user one).
     */
    public suspend fun apply(id: String, event: QueueEvent): CaptureState? {
        val current = repository.findById(id) ?: return null
        return when (val result = QueueState.next(current.state, event)) {
            is QueueTransition.Rejected -> null
            is QueueTransition.Accepted -> {
                mutex.withLock {
                    repository.updateState(id, result.to)
                    when (event) {
                        is QueueEvent.ExtractionFailed -> {
                            failures[id] = (failures[id] ?: 0) + 1
                            retryNotBefore[id] = now() + Backoff.delayMillis(failures[id] ?: 1)
                        }
                        is QueueEvent.ExtractionFoundAmount,
                        QueueEvent.Confirmed,
                        QueueEvent.Discarded,
                        -> {
                            failures.remove(id)
                            retryNotBefore.remove(id)
                        }
                        else -> Unit
                    }
                    publish()
                }
                result.to
            }
        }
    }

    /** True when the capture has exhausted [Backoff.MAX_ATTEMPTS] and should be handed to the human. */
    public fun exhaustedRetries(id: String): Boolean = failures[id]?.let { !Backoff.shouldRetry(it) } ?: false

    public fun failuresFor(id: String): Int = failures[id] ?: 0

    /** The day's captures, oldest first — the order Nightfall stacks them in. */
    public suspend fun forDay(tripId: String, day: LocalDay): List<Capture> =
        repository.capturesForDay(tripId, day).sortedBy { it.createdAt }

    /** Captures still needing the human's attention on [day]. */
    public suspend fun unconfirmedForDay(tripId: String, day: LocalDay): List<Capture> =
        repository.capturesForDay(tripId, day)
            .filter { it.state.isOpen || it.state == CaptureState.CONFIRMED }
            .sortedBy { it.createdAt }

    private suspend fun publish() {
        _pending.value = repository.openCaptures().sortedBy { it.createdAt }
    }
}

/** Persistence contract the outbox is written against. Implemented in `:core:store`. */
public interface CaptureRepository {
    public suspend fun insert(capture: Capture)
    public suspend fun updateState(id: String, state: CaptureState)
    public suspend fun updateDetails(
        id: String,
        amountMinor: Long?,
        currencyCode: String?,
        merchant: String?,
        markedNoAmount: Boolean,
        confirmedAt: Long?,
    )
    public suspend fun findById(id: String): Capture?
    public suspend fun openCaptures(): List<Capture>
    public suspend fun capturesForDay(tripId: String, day: LocalDay): List<Capture>
    public suspend fun delete(id: String)

    /**
     * Writes the whole editable surface of a capture in one statement. Used by the
     * detail page, where someone corrects a figure the OCR got wrong minutes after
     * the fact — the amount and its currency must move together or not at all.
     */
    public suspend fun updateDetail(
        id: String,
        amountMinor: Long?,
        currencyCode: String?,
        merchant: String?,
        category: String?,
        note: String?,
    )

    /**
     * Pins the rate an amount was confirmed at to the capture.
     *
     * The default body is deliberate: a repository that does not persist rates —
     * the in-memory double in a test that only exercises the queue — keeps
     * compiling and keeps working, and the real store writes the two columns. A
     * rate is not the record the way the amount is; losing it must never lose the
     * photograph.
     */
    public suspend fun recordRate(id: String, rateScaled: Long?, rateDate: String?) {}

    /** Category totals for the trip, largest first, with an uncategorised row. */
    public suspend fun categoryTotals(tripId: String): List<com.ariadne.model.CategoryTotal>
}

/** In-memory implementation: the test double, and the shape every driver must match. */
public class InMemoryCaptureRepository(
    initial: List<Capture> = emptyList(),
) : CaptureRepository {
    private val items = initial.associateBy { it.id }.toMutableMap()

    override suspend fun insert(capture: Capture) {
        items[capture.id] = capture
    }

    override suspend fun updateState(id: String, state: CaptureState) {
        items[id]?.let { items[id] = it.copy(state = state) }
    }

    override suspend fun updateDetails(
        id: String,
        amountMinor: Long?,
        currencyCode: String?,
        merchant: String?,
        markedNoAmount: Boolean,
        confirmedAt: Long?,
    ) {
        items[id]?.let {
            items[id] = it.copy(
                amountMinor = amountMinor,
                currency = currencyCode?.let(com.ariadne.model.CurrencyCode::of),
                merchant = merchant,
                markedNoAmount = markedNoAmount,
                confirmedAt = confirmedAt,
            )
        }
    }

    override suspend fun recordRate(id: String, rateScaled: Long?, rateDate: String?) {
        items[id]?.let {
            items[id] = it.copy(
                rateScaled = rateScaled,
                rateDate = rateDate?.let { com.ariadne.model.LocalDay.parseOrNull(it) },
            )
        }
    }

    override suspend fun findById(id: String): Capture? = items[id]

    override suspend fun openCaptures(): List<Capture> = items.values.filter { it.state.isOpen }

    override suspend fun capturesForDay(tripId: String, day: LocalDay): List<Capture> =
        items.values.filter { it.tripId == tripId && it.localDate == day }

    override suspend fun delete(id: String) {
        items.remove(id)
    }

    override suspend fun updateDetail(
        id: String,
        amountMinor: Long?,
        currencyCode: String?,
        merchant: String?,
        category: String?,
        note: String?,
    ) {
        items[id]?.let {
            items[id] = it.copy(
                amountMinor = amountMinor,
                currency = currencyCode?.let(com.ariadne.model.CurrencyCode::of),
                merchant = merchant,
                category = com.ariadne.model.Category.ofOrNull(category),
                note = note,
            )
        }
    }

    /**
     * No FX table here, so totals are reported per category in raw base-currency
     * units. The interface allows that: a caller without rates must not invent a
     * conversion, and this repository never has rates.
     */
    override suspend fun categoryTotals(tripId: String): List<com.ariadne.model.CategoryTotal> =
        items.values
            .filter { it.tripId == tripId && it.amountMinor != null && it.state.isFinished }
            .groupBy { it.category }
            .map { (category, list) ->
                com.ariadne.model.CategoryTotal(
                    category = category,
                    captures = list.size,
                    baseMinor = list.sumOf { it.amountMinor ?: 0L },
                )
            }
            .sortedByDescending { it.baseMinor }
}
