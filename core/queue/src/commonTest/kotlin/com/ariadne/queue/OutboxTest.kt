package com.ariadne.queue

import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutboxTest {

    private val day = LocalDay.of(2026, 11, 23)

    private fun capture(id: String, createdAt: Long, state: CaptureState = CaptureState.QUEUED) = Capture(
        id = id,
        tripId = "trip-1",
        localDate = day,
        photoPath = "photos/$id.jpg",
        createdAt = createdAt,
        state = state,
        amountMinor = null,
        currency = null,
    )

    @Test
    fun enqueue_commits_and_publishes_oldest_first() = runTest {
        val repo = InMemoryCaptureRepository()
        val outbox = Outbox(repo, now = { 0L })
        outbox.enqueue(capture("b", createdAt = 200))
        outbox.enqueue(capture("a", createdAt = 100))
        assertEquals(listOf("a", "b"), outbox.pending.value.map { it.id })
    }

    @Test
    fun background_work_spans_trips_but_day_review_does_not() = runTest {
        val repo = InMemoryCaptureRepository(listOf(
            capture("old-trip", 1).copy(tripId = "previous-trip"),
            capture("today", 2),
            capture("other-day", 3).copy(localDate = day.plusDays(-1)),
        ))
        val outbox = Outbox(repo, now = { 0L })
        outbox.resume()
        assertEquals(listOf("old-trip", "today", "other-day"), outbox.pending.value.map { it.id })
        assertEquals("old-trip", outbox.nextForExtraction()?.id)
        assertEquals(listOf("today"), outbox.forDay("trip-1", day).map { it.id })
        assertEquals(listOf("today"), outbox.unconfirmedForDay("trip-1", day).map { it.id })
        assertEquals(listOf("old-trip"), outbox.forDay("previous-trip", day).map { it.id })
    }

    @Test
    fun batch_capture_keeps_capture_order() = runTest {
        val repo = InMemoryCaptureRepository()
        val outbox = Outbox(repo, now = { 0L })
        val batch = (1..5).map { capture("c$it", createdAt = it.toLong() * 10) }
        outbox.enqueueAll(batch)
        assertEquals(5, outbox.pending.value.size)
        assertEquals("c1", outbox.pending.value.first().id)
    }

    @Test
    fun resume_returns_stale_extractions_to_the_queue() = runTest {
        val repo = InMemoryCaptureRepository(
            listOf(capture("x", 1, CaptureState.EXTRACTING)),
        )
        val outbox = Outbox(repo, now = { 0L })
        outbox.resume()
        assertEquals(CaptureState.QUEUED, repo.findById("x")?.state)
        assertEquals(1, outbox.pending.value.size)
    }

    @Test
    fun illegal_events_are_refused_and_change_nothing() = runTest {
        val repo = InMemoryCaptureRepository(listOf(capture("x", 1, CaptureState.ARCHIVED)))
        val outbox = Outbox(repo, now = { 0L })
        assertNull(outbox.apply("x", QueueEvent.AmountEntered))
        assertEquals(CaptureState.ARCHIVED, repo.findById("x")?.state)
    }

    @Test
    fun retry_accounting_climbs_then_exhausts() = runTest {
        val repo = InMemoryCaptureRepository(listOf(capture("x", 1)))
        val outbox = Outbox(repo, now = { 0L })
        repeat(Backoff.MAX_ATTEMPTS) {
            outbox.apply("x", QueueEvent.ExtractionStarted)
            outbox.apply("x", QueueEvent.ExtractionFailed("dead zone"))
            outbox.apply("x", QueueEvent.Retried)
        }
        // After the last retry the capture is back in the outbox, never lost.
        assertEquals(CaptureState.QUEUED, repo.findById("x")?.state)
        assertTrue(outbox.exhaustedRetries("x"))
        assertEquals(Backoff.MAX_ATTEMPTS, outbox.failuresFor("x"))
    }

    @Test
    fun a_failure_survives_the_retry_and_a_success_clears_it() = runTest {
        val repo = InMemoryCaptureRepository(listOf(capture("x", 1)))
        val outbox = Outbox(repo, now = { 0L })
        outbox.apply("x", QueueEvent.ExtractionStarted)
        outbox.apply("x", QueueEvent.ExtractionFailed("no signal"))
        // The count is the record of how hard this capture tried.
        outbox.apply("x", QueueEvent.Retried)
        assertEquals(1, outbox.failuresFor("x"))
        outbox.apply("x", QueueEvent.ExtractionStarted)
        outbox.apply("x", QueueEvent.ExtractionFoundAmount(confident = true))
        assertEquals(0, outbox.failuresFor("x"))
    }

    @Test
    fun a_backed_off_capture_is_not_picked_up_before_its_time() = runTest {
        var clock = 1_000L
        val repo = InMemoryCaptureRepository(listOf(capture("x", 1)))
        val outbox = Outbox(repo, now = { clock })
        outbox.apply("x", QueueEvent.ExtractionStarted)
        outbox.apply("x", QueueEvent.ExtractionFailed("dead zone"))
        outbox.apply("x", QueueEvent.Retried)
        // Inside the 2s window the capture stays queued but is left alone.
        assertNull(outbox.nextForExtraction())
        clock += 2_000L
        assertEquals("x", outbox.nextForExtraction()?.id)
    }

    @Test
    fun next_for_extraction_is_fifo() = runTest {
        val repo = InMemoryCaptureRepository(
            listOf(capture("late", 300), capture("early", 100), capture("mid", 200)),
        )
        val outbox = Outbox(repo, now = { 0L })
        assertEquals("early", outbox.nextForExtraction()?.id)
    }

    @Test
    fun unconfirmed_for_day_keeps_confirmed_captures_visible() = runTest {
        val repo = InMemoryCaptureRepository(
            listOf(
                capture("q", 1, CaptureState.QUEUED),
                capture("c", 2, CaptureState.CONFIRMED),
                capture("a", 3, CaptureState.ARCHIVED),
            ),
        )
        val outbox = Outbox(repo, now = { 0L })
        val visible = outbox.unconfirmedForDay("trip-1", day)
        assertEquals(listOf("q", "c"), visible.map { it.id })
    }

    @Test
    fun discard_removes_the_capture_without_leaving_a_hole() = runTest {
        val repo = InMemoryCaptureRepository(listOf(capture("x", 1), capture("y", 2)))
        val outbox = Outbox(repo, now = { 0L })
        repo.delete("x")
        assertEquals(1, repo.openCaptures().size)
        assertNotNull(repo.findById("y"))
    }

    @Test
    fun backoff_schedule_is_the_published_one() {
        assertEquals(2_000L, Backoff.delayMillis(1))
        assertEquals(8_000L, Backoff.delayMillis(2))
        assertEquals(30_000L, Backoff.delayMillis(3))
        assertEquals(120_000L, Backoff.delayMillis(4))
        assertEquals(600_000L, Backoff.delayMillis(5))
        // Capped, not unbounded.
        assertEquals(600_000L, Backoff.delayMillis(99))
    }

    @Test
    fun a_capture_with_no_amount_is_still_a_valid_record() = runTest {
        val repo = InMemoryCaptureRepository()
        val outbox = Outbox(repo, now = { 0L })
        outbox.enqueue(capture("souvenir", 1))
        outbox.apply("souvenir", QueueEvent.MarkedNoAmount)
        outbox.apply("souvenir", QueueEvent.Confirmed)
        val stored = repo.findById("souvenir")
        assertEquals(CaptureState.CONFIRMED, stored?.state)
        assertNull(stored?.amountMinor)
    }

    @Test
    fun currency_is_optional_until_confirmed() = runTest {
        val repo = InMemoryCaptureRepository()
        val outbox = Outbox(repo, now = { 0L })
        outbox.enqueue(capture("x", 1))
        repo.updateDetails("x", 45_000, "THB", "Chatuchak", false, null)
        // findById is suspending: the test is already in runTest, so it reads
        // from the same coroutine rather than forcing a blocking bridge.
        val stored = repo.findById("x")
        assertEquals(CurrencyCode.of("THB"), stored?.currency)
        assertEquals("Chatuchak", stored?.merchant)
    }
}
