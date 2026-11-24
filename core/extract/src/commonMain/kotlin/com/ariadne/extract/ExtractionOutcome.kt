package com.ariadne.extract

import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay

/**
 * What the extractor could make of a photograph.
 *
 * Sealed and exhaustive: there is no "partially succeeded" boolean to misread,
 * and [Failed] is a value the UI renders as a blank field, not as an error.
 */
public sealed interface ExtractionOutcome {
    public val confidence: Double

    public data class AmountAndMerchant(
        val amountMinor: Long,
        val currency: CurrencyCode,
        val merchant: String,
        override val confidence: Double,
        val date: LocalDay? = null,
    ) : ExtractionOutcome

    public data class AmountOnly(
        val amountMinor: Long,
        val currency: CurrencyCode,
        override val confidence: Double,
        val date: LocalDay? = null,
    ) : ExtractionOutcome

    /** Text was read but nothing usable came out of it. Not an error. */
    public data class Nothing(
        override val confidence: Double = 0.0,
        val text: String = "",
    ) : ExtractionOutcome

    /** OCR itself did not run or threw. The capture stays queued. */
    public data class Failed(val reason: String) : ExtractionOutcome {
        override val confidence: Double get() = 0.0
    }

    public val amountMinorOrNull: Long?
        get() = when (this) {
            is AmountAndMerchant -> amountMinor
            is AmountOnly -> amountMinor
            is Nothing, is Failed -> null
        }

    public val currencyOrNull: CurrencyCode?
        get() = when (this) {
            is AmountAndMerchant -> currency
            is AmountOnly -> currency
            is Nothing, is Failed -> null
        }

    public val merchantOrNull: String?
        get() = when (this) {
            is AmountAndMerchant -> merchant
            is AmountOnly, is Nothing, is Failed -> null
        }
}

/** Platform OCR. The only thing `expect`ed about extraction. */
public interface TextRecognizer {
    public suspend fun recognize(imagePath: String): OcrResult
}

public sealed interface OcrResult {
    public data class Text(val lines: List<String>) : OcrResult
    public data class Failure(val reason: String) : OcrResult
}
