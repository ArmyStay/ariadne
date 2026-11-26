package com.ariadne.model

import kotlin.jvm.JvmInline

/**
 * ISO-4217 currency, carried as a value class so a raw [String] can never be
 * mistaken for a currency at a call site.
 *
 * Only the alphabetic code is stored; scale comes from the currency itself so
 * that JPY (0 minor digits) and KWD (3 minor digits) round correctly without a
 * per-call argument.
 */
@JvmInline
public value class CurrencyCode private constructor(public val code: String) {

    public val scale: Int get() = scaleOf(code)

    override fun toString(): String = code

    public companion object {
        private val known: Set<String> = setOf(
            "INR", "USD", "EUR", "GBP", "JPY", "CNY", "THB", "VND", "IDR", "MYR",
            "SGD", "HKD", "KRW", "TWD", "PHP", "AUD", "NZD", "CAD", "CHF", "SEK",
            "NOK", "DKK", "PLN", "CZK", "HUF", "TRY", "ZAR", "BRL", "MXN", "ARS",
            "CLP", "COP", "PEN", "AED", "SAR", "QAR", "ILS", "EGP", "MAD", "KES",
            "TZS", "UGX", "RWF", "GHS", "NGN", "LKR", "NPR", "BDT", "PKR", "MMK",
            "KHR", "LAK", "MNT", "KZT", "UZS", "GEL", "AMD", "ISK", "KWD", "BHD",
            "OMR", "JOD", "TND", "DZD", "PYG", "XOF", "XAF",
        )

        /** Three-decimal currencies. Everything else is two, except [zeroScale]. */
        private val threeScale: Set<String> = setOf("KWD", "BHD", "OMR", "JOD", "TND", "DZD")

        /** Currencies with no minor unit at all. */
        private val zeroScale: Set<String> = setOf("JPY", "KRW", "VND", "IDR", "CLP", "ISK", "UGX", "RWF", "PYG", "XOF", "XAF")

        public fun of(raw: String): CurrencyCode {
            val normalized = raw.trim().uppercase()
            require(normalized.length == 3 && normalized.all { it in 'A'..'Z' }) {
                "currency code must be three letters, was '$raw'"
            }
            return CurrencyCode(normalized)
        }

        /** Lenient parse used on OCR text and user input: returns null instead of throwing. */
        public fun parseOrNull(raw: String?): CurrencyCode? {
            if (raw == null) return null
            val normalized = raw.trim().uppercase()
            if (normalized.length != 3 || normalized.any { it !in 'A'..'Z' }) return null
            return CurrencyCode(normalized)
        }

        public fun isKnown(raw: String): Boolean = raw.trim().uppercase() in known

        private fun scaleOf(code: String): Int = when {
            code in threeScale -> 3
            code in zeroScale -> 0
            else -> 2
        }
    }
}

/** Symbol shown inside the amount field; never used for arithmetic. */
public fun CurrencyCode.symbol(): String = when (code) {
    "INR", "NPR", "LKR", "PKR" -> "₹"
    "USD", "AUD", "NZD", "CAD", "SGD", "HKD", "MXN", "ARS", "CLP", "COP" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY", "CNY" -> "¥"
    "KRW" -> "₩"
    "THB" -> "฿"
    "VND" -> "₫"
    "TRY" -> "₺"
    "ILS" -> "₪"
    "AED", "SAR", "QAR" -> "د.إ"
    else -> code
}
