package com.ariadne.core.time

import com.ariadne.model.LocalDay
import kotlin.test.Test
import kotlin.test.assertEquals

class ClockTest {

    @Test
    fun fixed_clock_reports_what_it_was_given() {
        val clock: Clock = FixedClock(1_700_000_000_000L, LocalDay.of(2026, 11, 23))
        assertEquals(1_700_000_000_000L, clock.nowMillis())
        assertEquals(LocalDay.of(2026, 11, 23), clock.today())
    }

    @Test
    fun advancing_the_fixed_clock_moves_the_millis_only() {
        val clock = FixedClock(0L, LocalDay.of(2026, 11, 23))
        clock.advance(60_000L)
        assertEquals(60_000L, clock.nowMillis())
        assertEquals(LocalDay.of(2026, 11, 23), clock.today())
    }
}
