package com.ariadne.model

import kotlin.jvm.JvmInline

/**
 * A date in the trip's local calendar, independent of any offset.
 *
 * Deliberately not [kotlinx.datetime.LocalDate]: most of the model layer never
 * needs a clock, and a day-shaped type keeps the arithmetic honest.
 */
@JvmInline
public value class LocalDay private constructor(public val iso: String) {

    public val year: Int get() = iso.substring(0, 4).toInt()
    public val month: Int get() = iso.substring(5, 7).toInt()
    public val day: Int get() = iso.substring(8, 10).toInt()

    override fun toString(): String = iso

    public companion object {
        private const val UNIX_EPOCH_JULIAN_OFFSET = 719_468L

        public fun of(year: Int, month: Int, day: Int): LocalDay {
            require(month in 1..12) { "month out of range: $month" }
            require(day in 1..daysInMonth(year, month)) { "day out of range: $day for $year-$month" }
            val m = month.toString().padStart(2, '0')
            val d = day.toString().padStart(2, '0')
            return LocalDay("$year-$m-$d")
        }

        /** Lenient parse for persisted values and OCR candidates. */
        public fun parseOrNull(raw: String?): LocalDay? {
            if (raw == null || raw.length != 10) return null
            if (raw[4] != '-' || raw[7] != '-') return null
            val y = raw.substring(0, 4).toIntOrNull() ?: return null
            val m = raw.substring(5, 7).toIntOrNull() ?: return null
            val d = raw.substring(8, 10).toIntOrNull() ?: return null
            if (m !in 1..12) return null
            if (d !in 1..daysInMonth(y, m)) return null
            return LocalDay(raw)
        }

        public fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

        public fun daysInMonth(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeap(year)) 29 else 28
            else -> 0
        }

        /** Days since 1970-01-01. Howard Hinnant's `days_from_civil`. */
        public fun toEpochDay(day: LocalDay): Long {
            var y = day.year.toLong()
            val m = day.month.toLong()
            val d = day.day.toLong()
            if (m <= 2) y -= 1
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = if (m > 2) m - 3 else m + 9
            val doy = (153 * mp + 2) / 5 + d - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146_097 + doe - UNIX_EPOCH_JULIAN_OFFSET
        }

        /** Inverse of [toEpochDay]. Howard Hinnant's `civil_from_days`. */
        public fun fromEpochDay(epochDay: Long): LocalDay {
            var z = epochDay + UNIX_EPOCH_JULIAN_OFFSET
            val era = (if (z >= 0) z else z - 146_096) / 146_097
            val doe = z - era * 146_097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146_096) / 365
            var y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = if (mp < 10) mp + 3 else mp - 9
            if (m <= 2) y += 1
            return of(y.toInt(), m.toInt(), d.toInt())
        }
    }

    public fun plusDays(count: Int): LocalDay = fromEpochDay(toEpochDay(this) + count)

    public fun minusDays(count: Int): LocalDay = plusDays(-count)

    public operator fun compareTo(other: LocalDay): Int = iso.compareTo(other.iso)

    /** Days in the inclusive span, always at least 1 for a valid range. */
    public fun daysUntil(other: LocalDay): Int = (toEpochDay(other) - toEpochDay(this)).toInt()
}

/** Inclusive sequence of days from [start] to [end]; empty when [end] precedes [start]. */
public fun dayRange(start: LocalDay, end: LocalDay): List<LocalDay> {
    val span = start.daysUntil(end)
    if (span < 0) return emptyList()
    return (0..span).map { start.plusDays(it) }
}

/** ISO weekday index, Monday = 1 … Sunday = 7. */
public fun LocalDay.isoWeekday(): Int {
    val epoch = LocalDay.toEpochDay(this)
    val shifted = (epoch + 3).mod(7L)
    return (shifted + 1).toInt()
}
