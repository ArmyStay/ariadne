package com.ariadne.extract

import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.queue.InMemoryCaptureRepository
import com.ariadne.queue.Outbox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExtractionServiceFailureTest {

    @Test
    fun recognizerThrowKeepsCaptureCommittedAndVisible() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repo = InMemoryCaptureRepository()
        val outbox = Outbox(repo) { 1_700_000_000_000L }
        val capture = Capture(
            id = "c1",
            tripId = "t1",
            localDate = LocalDay.of(2026, 11, 24),
            photoPath = "/tmp/missing-or-bad.jpg",
            createdAt = 1_700_000_000_000L,
            state = CaptureState.QUEUED,
        )
        outbox.resume()
        outbox.enqueue(capture)

        val service = ExtractionService(
            recognizer = object : TextRecognizer {
                override suspend fun recognize(imagePath: String): OcrResult {
                    throw IllegalStateException("boom")
                }
            },
            repository = repo,
            outbox = outbox,
            parserFor = { ReceiptParser(it) },
            baseCurrency = { CurrencyCode.of("INR") },
            extractionDispatcher = dispatcher,
            retryDelayMillis = { 0L },
        )

        service.extractOne(capture.id, capture.photoPath)
        testScheduler.advanceUntilIdle()

        val stored = repo.findById(capture.id)
        assertNotNull(stored, "capture must remain in the repository after recognizer failure")
        assertEquals(capture.photoPath, stored.photoPath, "photo path must remain intact")
        assertTrue(
            stored.state == CaptureState.FAILED || stored.state == CaptureState.QUEUED,
            "failed extraction should leave the capture in a visible retryable/failed state, was ${stored.state}",
        )
        assertTrue(outbox.failuresFor(capture.id) >= 1, "failure should be recorded for retry/backoff handling")
        val open = repo.openCaptures().any { it.id == capture.id }
        assertTrue(open, "capture must remain visible/open after extraction failure")
    }
}