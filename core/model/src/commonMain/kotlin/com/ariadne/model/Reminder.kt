package com.ariadne.model

/**
 * The evening reminder.
 *
 * Ariadne's ritual happens once a day, in the evening, and it is the one thing in the
 * product that can be forgotten without anything breaking — which is exactly why it gets
 * forgotten. This is the whole of the settings for it: on or off, and when.
 *
 * It is deliberately small. A reminder with a schedule per weekday, a snooze, a repeat
 * count and a sound is a calendar; a trip is a short thing and one time is enough. The
 * default is 20:00 local, because a day's receipts are in the folder by then and the
 * evening is when someone has a minute.
 *
 * Nothing here touches a server. The reminder is armed on the device that holds the
 * record — an alarm on Android, an in-app line everywhere else — so it works in a dead
 * zone, which is where a trip often is.
 */
public data class Reminder(
    public val enabled: Boolean = false,
    public val hour: Int = 20,
    public val minute: Int = 0,
) {
    /** "20:00" — 24-hour, the same way every other time in the app is printed. */
    public val clock: String
        get() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    /**
     * The half of the day, for a sentence rather than a field: the sheet already prints
     * "20:00" in figures, so repeating the time in words underneath it would be the same
     * fact twice. "every day in the evening" is the other half of that sentence.
     */
    public val spoken: String
        get() = when {
            hour < 12 -> "in the morning"
            hour < 17 -> "in the afternoon"
            hour < 21 -> "in the evening"
            else -> "at night"
        }

    /**
     * The same reminder with a clock the device can actually keep.
     *
     * Clamped rather than rejected: a corrupt row must not stop the app from opening, and
     * 25:00 as 23:59 is a smaller lie than a reminder that never fires.
     */
    public fun normalised(): Reminder = copy(
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
    )

    /**
     * Whether the clock has reached the reminder today.
     *
     * Minutes rather than hours, so 20:00 fires at 20:00 and not at 19:59; and
     * "reached" rather than "equal", because a phone that was asleep at 20:00 and is
     * woken at 20:40 has not missed the evening — it is still the evening.
     */
    public fun isDueAt(hour: Int, minute: Int): Boolean =
        hour * 60 + minute >= this.hour * 60 + this.minute

    /** A different hour, keeping the minute. Wraps at midnight rather than failing. */
    public fun atHour(hour: Int): Reminder = copy(hour = ((hour % 24) + 24) % 24).normalised()

    public companion object {
        /** The one time this app suggests: after dinner, before bed. */
        public const val DEFAULT_HOUR: Int = 20
    }
}
