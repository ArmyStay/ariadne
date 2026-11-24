package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalDayTest {

    @Test
    fun epoch_day_round_trips_over_forty_years() {
        var day = LocalDay.of(1990, 1, 1)
        repeat(40 * 365) {
            val back = LocalDay.fromEpochDay(LocalDay.toEpochDay(day))
            assertEquals(day, back)
            day = day.plusDays(1)
        }
    }

    @Test
    fun epoch_day_is_anchored_at_1970() {
        assertEquals(0L, LocalDay.toEpochDay(LocalDay.of(1970, 1, 1)))
        assertEquals(-1L, LocalDay.toEpochDay(LocalDay.of(1969, 12, 31)))
        assertEquals(19_723L, LocalDay.toEpochDay(LocalDay.of(2024, 1, 1)))
    }

    @Test
    fun leap_days_are_handled() {
        assertEquals(LocalDay.of(2024, 2, 29), LocalDay.of(2024, 2, 28).plusDays(1))
        assertNull(LocalDay.parseOrNull("2026-02-29"))
        assertEquals(29, LocalDay.daysInMonth(2000, 2))
        assertEquals(28, LocalDay.daysInMonth(1900, 2))
    }

    @Test
    fun parsing_is_lenient_but_not_wrong() {
        assertEquals(LocalDay.of(2026, 11, 23), LocalDay.parseOrNull("2026-11-23"))
        assertNull(LocalDay.parseOrNull("2026-13-01"))
        assertNull(LocalDay.parseOrNull("2026-11-31"))
        assertNull(LocalDay.parseOrNull("23/11/2026"))
        assertNull(LocalDay.parseOrNull(null))
    }

    @Test
    fun day_ranges_are_inclusive_and_ordered() {
        val days = dayRange(LocalDay.of(2026, 11, 23), LocalDay.of(2026, 11, 26))
        assertEquals(4, days.size)
        assertEquals(LocalDay.of(2026, 11, 26), days.last())
        assertTrue(dayRange(LocalDay.of(2026, 11, 26), LocalDay.of(2026, 11, 23)).isEmpty())
    }

    @Test
    fun iso_weekday_is_one_for_monday() {
        // 2026-11-23 is a Monday.
        assertEquals(1, LocalDay.of(2026, 11, 23).isoWeekday())
        assertEquals(7, LocalDay.of(2026, 11, 29).isoWeekday())
    }

    @Test
    fun comparing_days_sorts_chronologically() {
        assertTrue(LocalDay.of(2026, 1, 9) < LocalDay.of(2026, 1, 10))
        assertTrue(LocalDay.of(2026, 2, 1) > LocalDay.of(2026, 1, 31))
    }
}
