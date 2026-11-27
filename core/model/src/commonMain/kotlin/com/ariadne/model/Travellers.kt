package com.ariadne.model

/**
 * Somebody else's day, as it arrived.
 *
 * A traveller is **not** a second person using this app, and a traveller's entries are
 * **not** captures. They live in their own rows, they never appear in the day's stack,
 * and the app never asks you to confirm a figure you did not photograph. That separation
 * is the whole design: your record stays exactly what you took, and their record is
 * quoted beside it.
 *
 * The code they handed over is kept as a [codeFingerprint] rather than as text: pasting
 * the same line twice should be recognised, and the line itself is long enough that
 * keeping a copy of it would be keeping a copy of their record twice over.
 */
public data class Traveller(
    public val id: String,
    public val tripId: String,
    /** What they called themselves in the code. Never invented, never merged with yours. */
    public val name: String,
    public val addedAt: Long,
    /** Which code this came from, so the same one pasted twice is recognised. */
    public val codeFingerprint: String,
    public val entries: List<TravellerEntry> = emptyList(),
) {
    /** What they spent, in each currency they spent it in. */
    public fun perCurrency(): Map<String, Long> =
        entries.groupBy { it.currency.code }
            .mapValues { (_, rows) -> rows.sumOf { it.amountMinor } }

    /** Rows for one local day, or for the whole trip when [day] is null. */
    public fun entriesOn(day: LocalDay?): List<TravellerEntry> =
        if (day == null) entries else entries.filter { it.day == day }

    /** True when this traveller has anything at all on the given day. */
    public fun hasFiguresOn(day: LocalDay): Boolean = entries.any { it.day == day }
}

/**
 * One confirmed figure, in the form it travelled.
 *
 * [digest] is the eight-bit check the codec carries per line: it is what turns a
 * truncated paste into "line 7 is damaged" instead of a quietly wrong total.
 */
public data class TravellerEntry(
    public val day: LocalDay,
    public val amountMinor: Long,
    public val currency: CurrencyCode,
    public val category: Category? = null,
    public val merchantInitials: String? = null,
    public val digest: Int = 0,
) {
    public fun summary(): String = merchantInitials?.takeIf { it.isNotBlank() } ?: "a receipt"
}

/** A traveller plus what they owe across a set of days, already converted. */
public data class TravellerShare(
    public val traveller: Traveller,
    /** Their figures on the day or trip being shown, in the base currency. */
    public val baseMinor: Long,
    /** Their money this app could not convert, in its own currency. */
    public val unconverted: Map<String, Long> = emptyMap(),
)
