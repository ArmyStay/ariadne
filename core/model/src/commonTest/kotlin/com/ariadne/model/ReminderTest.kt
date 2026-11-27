package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderTest {

    @Test
    fun prints_a_twenty_four_hour_clock() {
        assertEquals("20:00", Reminder(enabled = true).clock)
        assertEquals("07:05", Reminder(hour = 7, minute = 5).clock)
    }

    @Test
    fun names_the_half_of_the_day_for_a_sentence() {
        assertEquals("in the evening", Reminder().spoken)
        assertEquals("in the morning", Reminder(hour = 7, minute = 30).spoken)
        assertEquals("in the afternoon", Reminder(hour = 12).spoken)
        assertEquals("in the evening", Reminder(hour = 16, minute = 59).spoken)
        assertEquals("at night", Reminder(hour = 23, minute = 15).spoken)
    }

    @Test
    fun the_reminder_is_due_from_its_minute_onwards() {
        val evening = Reminder(hour = 20, minute = 0)
        assertEquals(false, evening.isDueAt(19, 59))
        assertEquals(true, evening.isDueAt(20, 0))
        assertEquals(true, evening.isDueAt(20, 40))
        assertEquals(true, evening.isDueAt(23, 59))
        // A reminder just after midnight is due before the rest of that hour's day.
        assertEquals(true, Reminder(hour = 0, minute = 30).isDueAt(0, 30))
        assertEquals(false, Reminder(hour = 0, minute = 30).isDueAt(0, 29))
    }

    @Test
    fun a_corrupt_clock_is_clamped_rather_than_thrown() {
        assertEquals(23, Reminder(hour = 99, minute = 99).normalised().hour)
        assertEquals(59, Reminder(hour = 99, minute = 99).normalised().minute)
        assertEquals(0, Reminder(hour = -4).normalised().hour)
    }

    @Test
    fun changing_the_hour_keeps_the_minute_and_wraps_at_midnight() {
        val evening = Reminder(enabled = true, hour = 20, minute = 30)
        assertEquals(21, evening.atHour(21).hour)
        assertEquals(30, evening.atHour(21).minute)
        assertEquals(0, evening.atHour(24).hour)
        assertEquals(23, evening.atHour(-1).hour)
    }
}
