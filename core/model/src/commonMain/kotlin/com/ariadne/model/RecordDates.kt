package com.ariadne.model

/**
 * When a day may be filed into.
 *
 * The record has exactly one hard edge. A receipt is a photograph of something that
 * has happened, so **a day that has not happened yet cannot be filed into** — not by
 * the shutter, not by the library, not by a typed date. Everything earlier is fair
 * game: a receipt found in a coat pocket on Thursday belongs to Tuesday, and the app
 * should let it there rather than insisting it belongs to today.
 *
 * These are predicates rather than checks in the screens on purpose. The page, the
 * shutter sheet, the typed date and the demo seed all ask the same four questions,
 * and the answers are here so no two of them can disagree about which days exist.
 */
public object RecordDates {

    /** Whether [day] has happened, and so may hold a record. Today counts. */
    public fun isFiled(day: LocalDay, today: LocalDay): Boolean = day <= today

    /**
     * The day a photograph taken on [today] belongs to.
     *
     * Null when the journey has not begun: a trip that starts on 3 December has no
     * day to file into on 27 November, and saying "today" would put a receipt inside
     * a journey that has not started. A trip that ended last week still accepts
     * paper: it lands on its last day, because that is the last day it has.
     */
    public fun filingDay(today: LocalDay, start: LocalDay, end: LocalDay): LocalDay? = when {
        today < start -> null
        today > end -> end
        else -> today
    }

    /** The latest day in the trip that has happened. */
    public fun latestFiled(today: LocalDay, end: LocalDay): LocalDay =
        if (today > end) end else today

    /**
     * Every day the person may choose between: the trip's days that have happened,
     * in order. Empty while the journey is still ahead.
     */
    public fun openDays(today: LocalDay, start: LocalDay, end: LocalDay): List<LocalDay> {
        if (today < start) return emptyList()
        return dayRange(start, latestFiled(today, end))
    }

    /**
     * [chosen], if it is a day that may be filed into; otherwise [filingDay].
     *
     * This is what the shutter asks before it opens. A page can ask for a day that
     * is in the future — a record opened from a stale link, a date typed by hand —
     * and the answer is never an error dialog: it is today, which is what the person
     * meant.
     */
    public fun chosenOrFiling(
        chosen: LocalDay?,
        today: LocalDay,
        start: LocalDay,
        end: LocalDay,
    ): LocalDay? {
        val filing = filingDay(today, start, end) ?: return null
        if (chosen == null) return filing
        if (!isFiled(chosen, today)) return filing
        if (chosen < start || chosen > end) return filing
        return chosen
    }
}
