package com.ariadne.model

/**
 * What a photograph is, as far as a person reading the screen is concerned.
 *
 * The database keeps seven [CaptureState]s because retries, extraction and
 * archiving need them. None of the seven is a thing a traveller wants to read, and
 * — more importantly — before this file existed every screen re-derived its own
 * label from a different subset of the row. That is how Home came to say "all
 * settled" while the record said "still open" about the same afternoon: one screen
 * counted a queue that happened to be empty, the other counted a day that happened
 * to be unclosed.
 *
 * So there is exactly one ladder here, and every label, badge, tray count and total
 * in the app is computed from it:
 *
 * ```
 *   Captured ──► HasAmount ──► Confirmed ──► InRecord
 *       │            │
 *       └────────────┴──► NoAmount        (chosen, not a failure)
 * ```
 *
 * [Failed] hangs off the first rung: the photograph is safe, the extraction is not.
 * It is deliberately *not* a separate kind of thing in the UI — it sits in the same
 * tray as a capture with no amount, because to the person holding the phone the
 * question is identical ("what was this?").
 */
public enum class EntryStatus {
    /** Photographed. Nothing is known about the amount yet. */
    Captured,

    /** An amount exists but has not been confirmed. It is not in any total. */
    HasAmount,

    /** Confirmed by the person. It is in the totals. */
    Confirmed,

    /** Deliberately kept without a figure — a souvenir, a gift, an ATM. */
    NoAmount,

    /** The day is filed: read-only, and still counted as confirmed. */
    InRecord,

    /** Extraction failed and the photograph is intact. */
    Failed,
    ;

    /** Nothing more is needed from the person before the day can be closed. */
    public val isResolved: Boolean
        get() = this == Confirmed || this == NoAmount || this == InRecord

    /** It still asks for something: an amount, a confirmation, or a second look. */
    public val isPending: Boolean get() = !isResolved

    /** In the figure on the receipt — confirmed, and not a deliberate blank. */
    public val countsInTotals: Boolean get() = this == Confirmed || this == InRecord

    /**
     * Can the day be closed with the entry in this state?
     *
     * A pending capture does not block closing a day: the whole point of "add the
     * amount later" is that the day can end first. This exists so the *copy* can
     * say how many figures are still open, not to gate the button.
     */
    public val asksForAmount: Boolean get() = this == Captured || this == Failed

    /** The word the UI prints. One place, so two screens cannot disagree. */
    public val label: String
        get() = when (this) {
            Captured -> "Needs an amount"
            HasAmount -> "Not confirmed"
            Confirmed -> "Confirmed"
            NoAmount -> "No amount"
            InRecord -> "In the record"
            Failed -> "Couldn't read it"
        }

    /**
     * The moves the ladder allows.
     *
     * Encoded rather than implied, because "which states may follow this one" is
     * the question the two contradictory screens were each answering differently.
     */
    public fun canMoveTo(next: EntryStatus): Boolean = when (this) {
        Captured -> next == HasAmount || next == NoAmount || next == Failed
        HasAmount -> next == Confirmed || next == NoAmount || next == Captured
        Confirmed -> next == InRecord || next == HasAmount
        NoAmount -> next == InRecord || next == HasAmount
        InRecord -> next == Confirmed
        // A failed extraction is retried into any of the live states.
        Failed -> next == Captured || next == HasAmount || next == NoAmount
    }
}

/**
 * The one derivation. Given a stored row, what is it?
 *
 * Order matters and is the whole specification: "kept without an amount" wins over
 * a stale amount, a filed day wins over everything, and an amount that has not been
 * confirmed is never counted as a figure.
 */
public fun entryStatusOf(capture: Capture): EntryStatus {
    val state = capture.state
    return when {
        state == CaptureState.ARCHIVED -> EntryStatus.InRecord
        state == CaptureState.CONFIRMED -> EntryStatus.Confirmed
        // "No amount" is a choice, and it outranks whatever a stale row says.
        capture.markedNoAmount -> EntryStatus.NoAmount
        state == CaptureState.FAILED -> EntryStatus.Failed
        state == CaptureState.NEEDS_CONFIRMATION && capture.amountMinor != null -> EntryStatus.HasAmount
        state == CaptureState.NEEDS_AMOUNT && capture.amountMinor != null -> EntryStatus.HasAmount
        // QUEUED, EXTRACTING, NEEDS_AMOUNT without a figure.
        else -> EntryStatus.Captured
    }
}

/** Convenience for the pages: `capture.entryStatus`. */
public val Capture.entryStatus: EntryStatus get() = entryStatusOf(this)

/**
 * A day's counts, derived once from its captures.
 *
 * Home, the Days tab, the record sheet and the close-day pager all print these
 * numbers, so they may not each count for themselves.
 */
public data class DaySummary(
    public val captured: Int,
    public val needsAmount: Int,
    public val hasAmount: Int,
    public val confirmed: Int,
    public val noAmount: Int,
    public val inRecord: Int,
    public val failed: Int,
) {
    /** Entries the tray offers to finish, in the order they were taken. */
    public val pending: Int get() = needsAmount + hasAmount + failed

    /** Everything the day holds. */
    public val total: Int get() = captured + needsAmount + hasAmount + confirmed + noAmount + inRecord + failed

    /** True when nothing on the day still asks for an amount or a confirmation. */
    public val isSettled: Boolean get() = pending == 0

    public companion object {
        public val Empty: DaySummary = DaySummary(0, 0, 0, 0, 0, 0, 0)

        public fun of(captures: List<Capture>): DaySummary {
            var captured = 0
            var needsAmount = 0
            var hasAmount = 0
            var confirmed = 0
            var noAmount = 0
            var inRecord = 0
            var failed = 0
            for (capture in captures) {
                when (capture.entryStatus) {
                    EntryStatus.Captured -> captured++
                    EntryStatus.HasAmount -> hasAmount++
                    EntryStatus.Confirmed -> confirmed++
                    EntryStatus.NoAmount -> noAmount++
                    EntryStatus.InRecord -> inRecord++
                    EntryStatus.Failed -> failed++
                }
            }
            return DaySummary(captured, needsAmount, hasAmount, confirmed, noAmount, inRecord, failed)
        }
    }
}
