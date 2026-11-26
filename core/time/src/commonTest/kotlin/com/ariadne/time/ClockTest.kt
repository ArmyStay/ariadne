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
    fun the_hour_of_an_instant_is_the_hour_it_happened_not_its_remainder() {
        // 2026-11-23T18:40:00Z: the 18th hour. A device in Chennai calls the same
        // instant 00:10 the next day; the rail follows the device, and the fixed
        // clock pins UTC so the walkthrough does not move with the machine.
        val clock = FixedClock(1_795_459_200_000L, LocalDay.of(2026, 11, 23))
        assertEquals(18, clock.hourOf(1_795_459_200_000L))
    }

    @Test
    fun advancing_the_fixed_clock_moves_the_millis_only() {
        val clock = FixedClock(0L, LocalDay.of(2026, 11, 23))
        clock.advance(60_000L)
        assertEquals(60_000L, clock.nowMillis())
        assertEquals(LocalDay.of(2026, 11, 23), clock.today())
    }
}
