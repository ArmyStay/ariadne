package com.ariadne.queue

import com.ariadne.model.CaptureState

/**
 * The outbox state machine, expressed as data rather than as scattered `if`s.
 *
 * Every transition in the app goes through [QueueState.next]; the store layer
 * persists the resulting [CaptureState], so a transition that is illegal here
 * can never reach disk.
 */
public sealed interface QueueEvent {
    /** The capture was committed to the outbox. */
    public data object Enqueued : QueueEvent

    /** Background extraction started. */
    public data object ExtractionStarted : QueueEvent

    /** Extraction produced an amount above the confidence floor. */
    public data class ExtractionFoundAmount(val confident: Boolean) : QueueEvent

    /** Extraction produced nothing usable, below the floor, or crashed. */
    public data class ExtractionFailed(val reason: String) : QueueEvent

    /** The human typed an amount in Nightfall. */
    public data object AmountEntered : QueueEvent

    /** The human confirmed the capture (with or without an amount). */
    public data object Confirmed : QueueEvent

    /** The human explicitly said "no amount" — souvenir, gift, ATM withdrawal. */
    public data object MarkedNoAmount : QueueEvent

    /** The day was closed, locking the capture into the record. */
    public data object Archived : QueueEvent

    /** Long-press unlock of a closed day. */
    public data object Unlocked : QueueEvent

    /** Backoff elapsed; the capture re-enters the outbox at the head. */
    public data object Retried : QueueEvent

    /** The human discarded the capture. */
    public data object Discarded : QueueEvent
}

/** Why a transition was refused. Surfaced in tests, never to the user. */
public sealed interface QueueRejection {
    public data class IllegalTransition(val from: CaptureState, val event: QueueEvent) : QueueRejection
}

/** Result of feeding one event through the machine. */
public sealed interface QueueTransition {
    public data class Accepted(val to: CaptureState) : QueueTransition
    public data class Rejected(val rejection: QueueRejection) : QueueTransition
}

/**
 * Pure, synchronous, total: for every (state, event) pair there is an answer.
 *
 * Kept free of coroutines and clocks so the whole machine is exercised in
 * `commonTest` without a scheduler.
 */
public object QueueState {

    public fun next(from: CaptureState, event: QueueEvent): QueueTransition {
        val to: CaptureState? = when (from) {
            CaptureState.QUEUED -> when (event) {
                QueueEvent.Enqueued -> CaptureState.QUEUED
                QueueEvent.ExtractionStarted -> CaptureState.EXTRACTING
                QueueEvent.AmountEntered -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.MarkedNoAmount -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.Confirmed -> CaptureState.CONFIRMED
                QueueEvent.Discarded -> CaptureState.FAILED
                else -> null
            }

            CaptureState.EXTRACTING -> when (event) {
                is QueueEvent.ExtractionFoundAmount ->
                    if (event.confident) CaptureState.NEEDS_CONFIRMATION else CaptureState.NEEDS_AMOUNT
                is QueueEvent.ExtractionFailed -> CaptureState.FAILED
                QueueEvent.Discarded -> CaptureState.FAILED
                else -> null
            }

            CaptureState.NEEDS_AMOUNT -> when (event) {
                QueueEvent.AmountEntered -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.MarkedNoAmount -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.Confirmed -> CaptureState.CONFIRMED
                QueueEvent.Discarded -> CaptureState.FAILED
                // A retry from NEEDS_AMOUNT means the human asked the parser to
                // look again; it goes back through the extractor, not the outbox.
                QueueEvent.Retried -> CaptureState.EXTRACTING
                else -> null
            }

            CaptureState.NEEDS_CONFIRMATION -> when (event) {
                QueueEvent.Confirmed -> CaptureState.CONFIRMED
                QueueEvent.MarkedNoAmount -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.AmountEntered -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.Discarded -> CaptureState.FAILED
                else -> null
            }

            CaptureState.CONFIRMED -> when (event) {
                QueueEvent.Archived -> CaptureState.ARCHIVED
                QueueEvent.Confirmed -> CaptureState.CONFIRMED
                else -> null
            }

            CaptureState.ARCHIVED -> when (event) {
                QueueEvent.Unlocked -> CaptureState.CONFIRMED
                QueueEvent.Archived -> CaptureState.ARCHIVED
                else -> null
            }

            CaptureState.FAILED -> when (event) {
                QueueEvent.Retried -> CaptureState.QUEUED
                // A human can still rescue a failed capture by hand.
                QueueEvent.AmountEntered -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.MarkedNoAmount -> CaptureState.NEEDS_CONFIRMATION
                QueueEvent.Confirmed -> CaptureState.CONFIRMED
                QueueEvent.Discarded -> CaptureState.FAILED
                else -> null
            }
        }
        return if (to == null) {
            QueueTransition.Rejected(QueueRejection.IllegalTransition(from, event))
        } else {
            QueueTransition.Accepted(to)
        }
    }

    /** Convenience for call sites that only care whether the move is allowed. */
    public fun canTransition(from: CaptureState, event: QueueEvent): Boolean =
        next(from, event) is QueueTransition.Accepted
}
