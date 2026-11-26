package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The ladder every screen reads.
 *
 * Written as a table because the bug it fixes was a disagreement between two
 * screens, and a disagreement is easiest to catch when the whole matrix is
 * spelled out once.
 */
class EntryStatusTest {

    private fun capture(
        state: CaptureState,
        amount: Long? = null,
        markedNoAmount: Boolean = false,
        currency: CurrencyCode? = CurrencyCode.of("THB"),
        category: Category? = null,
    ) = Capture(
        id = "c1",
        tripId = "t1",
        localDate = LocalDay.of(2026, 11, 21),
        photoPath = "/photos/c1.jpg",
        createdAt = 1_795_000_000_000L,
        state = state,
        amountMinor = amount,
        currency = currency,
        markedNoAmount = markedNoAmount,
        category = category,
    )

    @Test
    fun a_fresh_capture_needs_an_amount() {
        assertEquals(EntryStatus.Captured, capture(CaptureState.QUEUED).entryStatus)
        assertEquals(EntryStatus.Captured, capture(CaptureState.EXTRACTING).entryStatus)
        assertEquals(EntryStatus.Captured, capture(CaptureState.NEEDS_AMOUNT).entryStatus)
    }

    @Test
    fun an_extracted_amount_is_not_a_confirmed_one() {
        // The exact shape that made Home and the record disagree: the figure is in
        // the row, the person has not said yes, and the row is not in any total.
        val extracted = capture(CaptureState.NEEDS_CONFIRMATION, amount = 2_555_00)
        assertEquals(EntryStatus.HasAmount, extracted.entryStatus)
        assertFalse(extracted.entryStatus.countsInTotals)
        assertTrue(extracted.entryStatus.isPending)
    }

    @Test
    fun a_confirmed_capture_counts_and_an_archived_one_still_does() {
        assertEquals(EntryStatus.Confirmed, capture(CaptureState.CONFIRMED, 2_555_00).entryStatus)
        assertEquals(EntryStatus.InRecord, capture(CaptureState.ARCHIVED, 2_555_00).entryStatus)
        assertTrue(capture(CaptureState.CONFIRMED, 2_555_00).entryStatus.countsInTotals)
        assertTrue(capture(CaptureState.ARCHIVED, 2_555_00).entryStatus.countsInTotals)
    }

    @Test
    fun keeping_it_without_an_amount_is_a_choice_not_a_pending_item() {
        val souvenir = capture(CaptureState.NEEDS_AMOUNT, markedNoAmount = true)
        assertEquals(EntryStatus.NoAmount, souvenir.entryStatus)
        assertTrue(souvenir.entryStatus.isResolved)
        assertFalse(souvenir.entryStatus.countsInTotals)
    }

    @Test
    fun marked_no_amount_outranks_a_stale_figure() {
        val stale = capture(CaptureState.CONFIRMED, amount = 500_00, markedNoAmount = true)
        // Confirmed is the stronger fact — the row is already in the record.
        assertEquals(EntryStatus.Confirmed, stale.entryStatus)
        val unfiled = capture(CaptureState.NEEDS_CONFIRMATION, amount = 500_00, markedNoAmount = true)
        assertEquals(EntryStatus.NoAmount, unfiled.entryStatus)
    }

    @Test
    fun a_failed_extraction_is_still_a_capture() {
        val failed = capture(CaptureState.FAILED)
        assertEquals(EntryStatus.Failed, failed.entryStatus)
        assertTrue(failed.entryStatus.isPending)
        assertTrue(failed.entryStatus.asksForAmount)
    }

    @Test
    fun the_ladder_only_moves_forwards_except_where_editing_moves_it_back() {
        val legal = setOf(
            EntryStatus.Captured to EntryStatus.HasAmount,
            EntryStatus.Captured to EntryStatus.NoAmount,
            EntryStatus.Captured to EntryStatus.Failed,
            EntryStatus.HasAmount to EntryStatus.Confirmed,
            EntryStatus.HasAmount to EntryStatus.NoAmount,
            EntryStatus.HasAmount to EntryStatus.Captured,
            EntryStatus.Confirmed to EntryStatus.InRecord,
            EntryStatus.Confirmed to EntryStatus.HasAmount,
            EntryStatus.NoAmount to EntryStatus.InRecord,
            EntryStatus.NoAmount to EntryStatus.HasAmount,
            EntryStatus.InRecord to EntryStatus.Confirmed,
            EntryStatus.Failed to EntryStatus.Captured,
            EntryStatus.Failed to EntryStatus.HasAmount,
            EntryStatus.Failed to EntryStatus.NoAmount,
        )
        for (from in EntryStatus.entries) {
            for (to in EntryStatus.entries) {
                val expected = (from to to) in legal
                assertEquals(
                    expected,
                    from.canMoveTo(to),
                    "$from -> $to should ${if (expected) "be allowed" else "be refused"}",
                )
            }
        }
    }

    @Test
    fun a_day_with_one_unconfirmed_figure_is_not_settled() {
        val captures = listOf(
            capture(CaptureState.CONFIRMED, 50_00),
            capture(CaptureState.NEEDS_CONFIRMATION, 2_555_00),
        )
        val summary = DaySummary.of(captures)
        assertFalse(summary.isSettled, "a pending figure means the day is not settled")
        assertEquals(1, summary.pending)
        assertEquals(2, summary.total)
    }

    @Test
    fun a_day_of_confirmed_and_deliberate_blanks_is_settled() {
        val summary = DaySummary.of(
            listOf(
                capture(CaptureState.CONFIRMED, 50_00),
                capture(CaptureState.NEEDS_AMOUNT, markedNoAmount = true),
                capture(CaptureState.ARCHIVED, 2_555_00),
            ),
        )
        assertTrue(summary.isSettled)
        assertEquals(3, summary.total)
        assertEquals(0, summary.pending)
    }

    @Test
    fun an_empty_day_is_settled_and_empty() {
        val summary = DaySummary.of(emptyList())
        assertTrue(summary.isSettled)
        assertEquals(0, summary.total)
        assertEquals(DaySummary.Empty, summary)
    }

    @Test
    fun every_state_has_a_label_a_person_can_read() {
        for (status in EntryStatus.entries) {
            assertTrue(status.label.isNotBlank(), "$status has no label")
            // No database vocabulary reaches the screen.
            assertFalse(status.label.contains("_"), "$status leaks an enum name")
        }
    }
}
