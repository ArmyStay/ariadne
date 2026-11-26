package com.ariadne.core.time

import com.ariadne.model.LocalDay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock as KotlinClock
import kotlin.time.Instant

/**
 * Time as a dependency, not an ambient fact.
 *
 * Injected everywhere and faked in tests, so "nightfall" and "the trip is over"
 * are decided by a value the test owns. Backed by kotlinx-datetime rather than
 * `expect/actual` because it already covers all four targets, including Wasm.
 */
public interface Clock {
    /** Milliseconds since the epoch. */
    public fun nowMillis(): Long

    /** Today in the device's current zone — the day a capture belongs to. */
    public fun today(): LocalDay

    /**
     * The hour of the day [millis] falls in, in the device's current zone.
     *
     * The timeline draws a capture at the hour it was taken, and an epoch
     * instant is not an hour: in Chennai an 18:00 photograph is 12:30 in UTC.
     * Reading the instant's own remainder would put it on the rail two-thirds of
     * a day away from when it happened, so the zone lives here, once, instead of
     * being assumed at the call site.
     */
    public fun hourOf(millis: Long): Int
}

public class SystemClock(private val zone: TimeZone = TimeZone.currentSystemDefault()) : Clock {

    override fun nowMillis(): Long = KotlinClock.System.now().toEpochMilliseconds()

    override fun hourOf(millis: Long): Int =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).hour

    override fun today(): LocalDay {
        val date = KotlinClock.System.now().toLocalDateTime(zone).date
        // Parsed rather than field-read: the date type's accessors have moved
        // between kotlinx-datetime releases and its toString is ISO-8601 by spec.
        return LocalDay.parseOrNull(date.toString())
            ?: error("system clock produced a non-ISO date: $date")
    }
}

/** Deterministic clock for tests and for the demo's scripted walkthrough. */
public class FixedClock(private var millis: Long, private var day: LocalDay) : Clock {
    override fun nowMillis(): Long = millis
    override fun today(): LocalDay = day

    /** UTC, deliberately: a scripted walkthrough must not change with the machine. */
    override fun hourOf(millis: Long): Int =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).hour

    public fun advance(delta: Long) { millis += delta }
    public fun setDay(day: LocalDay) { this.day = day }
}
