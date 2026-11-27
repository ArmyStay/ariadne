package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordDatesTest {

    private val today = LocalDay.of(2026, 11, 27)
    private val start = LocalDay.of(2026, 11, 24)
    private val end = LocalDay.of(2026, 11, 30)

    @Test
    fun today_and_earlier_have_happened() {
        assertTrue(RecordDates.isFiled(today, today))
        assertTrue(RecordDates.isFiled(LocalDay.of(2024, 1, 1), today))
        assertFalse(RecordDates.isFiled(today.plusDays(1), today))
    }

    @Test
    fun a_running_journey_files_into_today() {
        assertEquals(today, RecordDates.filingDay(today, start, end))
    }

    @Test
    fun a_journey_that_has_not_started_has_nowhere_to_file() {
        val later = LocalDay.of(2026, 12, 3)
        assertNull(RecordDates.filingDay(today, later, later.plusDays(5)))
        assertTrue(RecordDates.openDays(today, later, later.plusDays(5)).isEmpty())
    }

    @Test
    fun a_finished_journey_files_into_its_last_day() {
        val past = LocalDay.of(2026, 11, 10)
        assertEquals(past, RecordDates.filingDay(today, LocalDay.of(2026, 11, 1), past))
    }

    @Test
    fun the_open_days_end_at_today() {
        val days = RecordDates.openDays(today, start, end)
        assertEquals(LocalDay.of(2026, 11, 24), days.first())
        assertEquals(today, days.last())
        assertEquals(4, days.size)
        assertTrue(days.none { it > today })
    }

    @Test
    fun a_journey_still_running_offers_nothing_beyond_today() {
        val days = RecordDates.openDays(today, start, end)
        assertFalse(days.contains(today.plusDays(1)))
    }

    @Test
    fun choosing_a_past_day_is_honoured() {
        val tuesday = LocalDay.of(2026, 11, 24)
        assertEquals(tuesday, RecordDates.chosenOrFiling(tuesday, today, start, end))
    }

    @Test
    fun choosing_a_future_day_falls_back_to_today() {
        assertEquals(
            today,
            RecordDates.chosenOrFiling(today.plusDays(3), today, start, end),
        )
    }

    @Test
    fun choosing_a_day_outside_the_trip_falls_back_to_today() {
        assertEquals(
            today,
            RecordDates.chosenOrFiling(today.minusDays(90), today, start, end),
        )
        assertEquals(
            today,
            RecordDates.chosenOrFiling(null, today, start, end),
        )
    }

    @Test
    fun a_finished_journey_never_reaches_forward_to_today() {
        val from = LocalDay.of(2026, 11, 1)
        val to = LocalDay.of(2026, 11, 10)
        assertEquals(to, RecordDates.chosenOrFiling(today, today, from, to))
        assertEquals(to, RecordDates.chosenOrFiling(null, today, from, to))
    }
}
