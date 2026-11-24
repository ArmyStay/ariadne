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
}

/** A trip is the container the whole record is scoped to. */
public data class Trip(
    public val id: String,
    public val name: String,
    public val startDate: LocalDay,
    public val endDate: LocalDay,
    public val baseCurrency: CurrencyCode,
    public val homeCurrency: CurrencyCode,
    public val closedAt: Long? = null,
) {
    public val isClosed: Boolean get() = closedAt != null
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
