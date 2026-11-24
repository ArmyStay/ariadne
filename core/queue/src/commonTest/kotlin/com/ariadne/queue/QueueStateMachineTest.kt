package com.ariadne.queue

import com.ariadne.model.CaptureState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QueueStateMachineTest {

    private fun accept(from: CaptureState, event: QueueEvent): CaptureState {
        val result = QueueState.next(from, event)
        assertIs<QueueTransition.Accepted>(result, "expected $from --$event--> accepted")
        return result.to
    }

    private fun reject(from: CaptureState, event: QueueEvent) {
        assertIs<QueueTransition.Rejected>(QueueState.next(from, event))
    }

    @Test
    fun happy_path_is_shutter_to_archive() {
        var state = accept(CaptureState.QUEUED, QueueEvent.ExtractionStarted)
        assertEquals(CaptureState.EXTRACTING, state)
        state = accept(state, QueueEvent.ExtractionFoundAmount(confident = true))
        assertEquals(CaptureState.NEEDS_CONFIRMATION, state)
        state = accept(state, QueueEvent.Confirmed)
        assertEquals(CaptureState.CONFIRMED, state)
        state = accept(state, QueueEvent.Archived)
        assertEquals(CaptureState.ARCHIVED, state)
    }

    @Test
    fun low_confidence_extraction_asks_for_an_amount() {
        val state = accept(CaptureState.EXTRACTING, QueueEvent.ExtractionFoundAmount(confident = false))
        assertEquals(CaptureState.NEEDS_AMOUNT, state)
    }

    @Test
    fun failed_extraction_retries_back_into_the_outbox() {
        val failed = accept(CaptureState.EXTRACTING, QueueEvent.ExtractionFailed("no text"))
        assertEquals(CaptureState.FAILED, failed)
        assertEquals(CaptureState.QUEUED, accept(failed, QueueEvent.Retried))
    }

    @Test
    fun a_failed_capture_can_still_be_rescued_by_hand() {
        assertEquals(
            CaptureState.NEEDS_CONFIRMATION,
            accept(CaptureState.FAILED, QueueEvent.AmountEntered),
        )
    }

    @Test
    fun no_amount_is_a_first_class_choice() {
        val state = accept(CaptureState.NEEDS_AMOUNT, QueueEvent.MarkedNoAmount)
        assertEquals(CaptureState.NEEDS_CONFIRMATION, state)
        assertEquals(CaptureState.CONFIRMED, accept(state, QueueEvent.Confirmed))
    }

    @Test
    fun archived_days_can_be_unlocked_but_not_edited_silently() {
        assertEquals(CaptureState.CONFIRMED, accept(CaptureState.ARCHIVED, QueueEvent.Unlocked))
        reject(CaptureState.ARCHIVED, QueueEvent.AmountEntered)
        reject(CaptureState.ARCHIVED, QueueEvent.ExtractionStarted)
    }

    @Test
    fun confirmation_cannot_be_skipped_from_the_outbox_while_extracting() {
        reject(CaptureState.EXTRACTING, QueueEvent.Confirmed)
    }

    @Test
    fun every_state_answers_every_event_without_throwing() {
        val events = listOf(
            QueueEvent.Enqueued,
            QueueEvent.ExtractionStarted,
            QueueEvent.ExtractionFoundAmount(true),
            QueueEvent.ExtractionFoundAmount(false),
            QueueEvent.ExtractionFailed("x"),
            QueueEvent.AmountEntered,
            QueueEvent.Confirmed,
            QueueEvent.MarkedNoAmount,
            QueueEvent.Archived,
            QueueEvent.Unlocked,
            QueueEvent.Retried,
            QueueEvent.Discarded,
        )
        for (state in CaptureState.entries) {
            for (event in events) {
                // Total: never throws, always a value.
                QueueState.next(state, event)
            }
        }
    }

    @Test
    fun no_event_ever_invents_a_state_outside_the_sealed_set() {
        for (state in CaptureState.entries) {
            for (event in listOf(QueueEvent.Confirmed, QueueEvent.Retried, QueueEvent.Archived)) {
                val result = QueueState.next(state, event)
                if (result is QueueTransition.Accepted) {
                    assertTrue(result.to in CaptureState.entries)
                }
            }
        }
    }
}
