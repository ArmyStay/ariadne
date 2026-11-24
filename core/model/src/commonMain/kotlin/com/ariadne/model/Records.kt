package com.ariadne.model

/** Lifecycle of a single capture, from shutter to archive. */
public enum class CaptureState {
    /** Committed to disk, nothing else known. The only state the shutter guarantees. */
    QUEUED,

    /** Extraction is in flight in the background. Still editable. */
    EXTRACTING,

    /** Extraction finished but the amount is missing or below the confidence floor. */
    NEEDS_AMOUNT,

    /** An amount is known; the human has not confirmed it yet. */
    NEEDS_CONFIRMATION,

    /** The human confirmed it at Nightfall. Immutable except by explicit edit. */
    CONFIRMED,

    /** Day closed. Reads only, and the state a trip card is drawn from. */
    ARCHIVED,

    /** Something failed, but the capture and its photo are intact. */
    FAILED,
    ;

    public val isOpen: Boolean
        get() = this == QUEUED || this == EXTRACTING || this == NEEDS_AMOUNT ||
            this == NEEDS_CONFIRMATION || this == FAILED

    /** States a capture can be discarded from without leaving a hole. */
    public val isDiscardable: Boolean get() = this != ARCHIVED

    /**
     * Finished: the human has settled it. The single rule the ledger uses — only
     * a finished photograph has a figure that counts.
     */
    public val isFinished: Boolean
        get() = this == CONFIRMED || this == ARCHIVED
}

/**
 * An optional label on a capture.
 *
 * Deliberately small and closed. The product's wager is that "the photograph is
 * the record", so a category is something you may add in five seconds, not
 * something the app demands before it will keep your receipt. [Other] exists so
 * that a record never has to be forced into a bucket that does not fit.
 */
public enum class Category(public val label: String) {
    Food("Food"),
    Transport("Transport"),
    Lodging("Lodging"),
    Shopping("Shopping"),
    Entertainment("Entertainment"),
    Other("Other"),
    ;

    public companion object {
        public fun ofOrNull(name: String?): Category? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/** One row of the spending breakdown: how much a category took, in base currency. */
public data class CategoryTotal(
    public val category: Category?,
    public val captures: Int,
    public val baseMinor: Long,
)

/** A trip is the container the whole record is scoped to. */
public data class Trip(
    public val id: String,
    public val name: String,
    public val startDate: LocalDay,
    public val endDate: LocalDay,
    public val baseCurrency: CurrencyCode,
    public val homeCurrency: CurrencyCode,
    public val closedAt: Long? = null,
    /** Where the traveller says they are. Free text: a city, a coast, a train. */
    public val place: String? = null,
    /**
     * What they meant to spend, in [baseCurrency], as minor units.
     *
     * Optional, because a trip without a budget is still a trip. When it is set it
     * is a fence, not a target: the record compares the finished total to it and
     * says how much room is left, and never calls an overspend a failure.
     */
    public val budgetMinor: Long? = null,
) {
    public val isClosed: Boolean get() = closedAt != null

    public fun budget(): Money? = budgetMinor?.let(Money::ofMinor)

    /** Negative means over budget. Null when no budget was set. */
    public fun remainingAgainstBudget(spentMinor: Long): Long? =
        budgetMinor?.let { it - spentMinor }
}

/**
 * One photograph and whatever is known about it.
 *
 * Everything except [id], [tripId], [localDate], [photoPath] and [createdAt] is
 * optional by design: a capture with no amount is a valid record, not a hole.
 */
public data class Capture(
    public val id: String,
    public val tripId: String,
    public val localDate: LocalDay,
    public val photoPath: String,
    public val createdAt: Long,
    public val state: CaptureState = CaptureState.QUEUED,
    public val amountMinor: Long? = null,
    public val currency: CurrencyCode? = null,
    public val merchant: String? = null,
    public val extractionConfidence: Double? = null,
    public val confirmedAt: Long? = null,
    /** Set when the human chose "no amount" — a souvenir, a gift, an ATM withdrawal. */
    public val markedNoAmount: Boolean = false,
    /**
     * What it was, when the human said. Never required, never guessed by the OCR:
     * a category is a claim, not an observation.
     */
    public val category: Category? = null,
    /** Anything the person wanted to remember about the photograph. */
    public val note: String? = null,
) {
    public val hasAmount: Boolean get() = amountMinor != null && currency != null

    public fun amount(): Money? = amountMinor?.let(Money::ofMinor)
}

/** One local calendar day inside a trip. Empty days exist from day one. */
public data class DayCard(
    public val id: String,
    public val tripId: String,
    public val localDate: LocalDay,
    public val closedAt: Long? = null,
) {
    public val isClosed: Boolean get() = closedAt != null
}

/** An FX rate snapshot: 1 [base] = [rateScaled] / 1e6 [quote]. */
public data class Rate(
    public val base: CurrencyCode,
    public val quote: CurrencyCode,
    public val date: LocalDay,
    public val rateScaled: Long,
)

/** Single-row settings table. */
public data class Settings(
    public val baseCurrency: CurrencyCode = CurrencyCode.of("INR"),
    public val homeCurrency: CurrencyCode = CurrencyCode.of("INR"),
    public val theme: Theme = Theme.SYSTEM,
    public val ocrProvider: OcrProvider = OcrProvider.ON_DEVICE,
) {
    public enum class Theme { LIGHT, DARK, SYSTEM }
    public enum class OcrProvider { ON_DEVICE, CLOUD_BYOK, NONE }
}
