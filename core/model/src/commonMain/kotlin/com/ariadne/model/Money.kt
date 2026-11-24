package com.ariadne.model

import kotlin.jvm.JvmInline

/**
 * An amount of currency held as an integer count of minor units.
 *
 * Money is never a [Double] in this codebase: every arithmetic step happens on
 * [Long]s and the only rounding site is the FX boundary ([com.ariadne.fx]).
 */
@JvmInline
public value class Money private constructor(public val minorUnits: Long) {

    public companion object {
        public fun ofMinor(minorUnits: Long): Money = Money(minorUnits)

        /**
         * Parses user-typed digits ("1250", "1250.5", "1,250.50") into minor
         * units for [currency]. Digits beyond the currency scale are rejected
         * rather than rounded: the input field never lets them through, so
         * seeing them here means something is wrong upstream.
         */
        public fun parseOrNull(text: String, currency: CurrencyCode): Money? {
            val cleaned = text.trim().replace(",", "").replace(" ", "").replace("_", "")
            if (cleaned.isEmpty()) return null
            if (cleaned.any { it !in '0'..'9' && it != '.' }) return null
            val parts = cleaned.split('.')
            if (parts.size > 2) return null
            val whole = parts[0].ifEmpty { "0" }
            if (whole.any { it !in '0'..'9' }) return null
            val fractionRaw = parts.getOrNull(1) ?: ""
            if (fractionRaw.any { it !in '0'..'9' }) return null
            val scale = currency.scale
            if (fractionRaw.length > scale) return null
            val fraction = fractionRaw.padEnd(scale, '0')
            val wholeValue = whole.toLongOrNull() ?: return null
            val fractionValue = if (scale == 0) 0L else fraction.toLongOrNull() ?: return null
            if (wholeValue > Long.MAX_VALUE / pow10(scale)) return null
            return Money(wholeValue * pow10(scale) + fractionValue)
        }

        private fun pow10(scale: Int): Long = when (scale) {
            0 -> 1L
            1 -> 10L
            2 -> 100L
            3 -> 1_000L
            else -> 1L
        }
    }

    public operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)

    public operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)

    public operator fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    public val isZero: Boolean get() = minorUnits == 0L

    /** "1,250.50" — grouping separators, exactly [currency]'s scale. */
    public fun format(currency: CurrencyCode, grouping: Boolean = true): String {
        val scale = currency.scale
        val negative = minorUnits < 0
        val abs = if (negative) -minorUnits else minorUnits
        val divisor = pow10Long(scale)
        val whole = (abs / divisor).toString()
        val fraction = if (scale == 0) "" else (abs % divisor).toString().padStart(scale, '0')
        val grouped = if (!grouping) whole else groupIndianOrWestern(whole, currency)
        val sign = if (negative) "-" else ""
        return if (scale == 0) "$sign$grouped" else "$sign$grouped.$fraction"
    }

    public fun formatWithSymbol(currency: CurrencyCode): String = "${currency.symbol()}${format(currency)}"

    /**
     * Indian lakh grouping for INR/NPR/LKR/PKR, three-digit groups elsewhere.
     * Display only — parsing strips separators.
     */
    private fun groupIndianOrWestern(whole: String, currency: CurrencyCode): String {
        val indian = currency.code in setOf("INR", "NPR", "LKR", "PKR", "BDT")
        if (whole.length <= 3) return whole
        val sb = StringBuilder()
        if (indian) {
            val lastThree = whole.takeLast(3)
            var rest = whole.dropLast(3)
            while (rest.length > 2) {
                sb.insert(0, "," + rest.takeLast(2))
                rest = rest.dropLast(2)
            }
            if (rest.isNotEmpty()) sb.insert(0, rest)
            return sb.toString() + "," + lastThree
        }
        var rest = whole
        while (rest.length > 3) {
            sb.insert(0, "," + rest.takeLast(3))
            rest = rest.dropLast(3)
        }
        if (rest.isNotEmpty()) sb.insert(0, rest)
        return sb.toString()
    }

    private fun pow10Long(scale: Int): Long = when (scale) {
        0 -> 1L
        1 -> 10L
        2 -> 100L
        3 -> 1_000L
        else -> 1L
    }
}
