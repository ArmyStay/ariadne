package com.ariadne.fx

import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.Rate

/**
 * Currency conversion.
 *
 * Rates are integers scaled by 1e6 (1 base = rateScaled / 1e6 quote) so the rate
 * table is exact in the database and never a float. Conversion is the single
 * rounding site in the app: minor units in, minor units out, HALF_UP, once.
 */
public class FxTable(private val rates: List<Rate>) {

    private val byPair: Map<Pair<String, String>, List<Rate>> = rates
        .groupBy { it.base.code to it.quote.code }

    /**
     * Rate for [from] → [to] on [date], falling back to the most recent earlier
     * snapshot, then to the inverse pair, then to identity.
     */
    public fun rateOrNull(from: CurrencyCode, to: CurrencyCode, date: LocalDay): Long? {
        if (from == to) return SCALE
        val direct = byPair[from.code to to.code]
        val found = direct?.filter { it.date <= date }?.maxByOrNull { LocalDay.toEpochDay(it.date) }
        if (found != null) return found.rateScaled
        val inverse = byPair[to.code to from.code]
            ?.filter { it.date <= date }
            ?.maxByOrNull { LocalDay.toEpochDay(it.date) }
        if (inverse != null && inverse.rateScaled != 0L) {
            return (SCALE * SCALE) / inverse.rateScaled
        }
        return null
    }

    /**
     * Converts [amount] from [from] to [to]. Returns the amount unchanged when
     * no rate exists — a missing rate must never zero out a record.
     */
    public fun convert(amount: Money, from: CurrencyCode, to: CurrencyCode, date: LocalDay): Conversion {
        if (from == to) return Conversion(amount, from, to, applied = false)
        val rate = rateOrNull(from, to, date)
            ?: return Conversion(amount, from, to, applied = false)
        val target = to.scale
        val source = from.scale
        // Work in a single wide integer: minor units × rate, then rescale.
        val numerator = amount.minorUnits * rate
        val denominator = SCALE
        var result = numerator / denominator
        val remainder = numerator % denominator
        // HALF_UP on the remainder, the one place rounding is allowed.
        if (kotlin.math.abs(remainder) * 2 >= denominator) {
            result += if (numerator >= 0) 1 else -1
        }
        // Align scales between source and target currency (INR 2 → JPY 0 etc.).
        if (target != source) {
            result = rescale(result, source, target)
        }
        return Conversion(Money.ofMinor(result), from, to, applied = true)
    }

    private fun rescale(value: Long, fromScale: Int, toScale: Int): Long {
        if (fromScale == toScale) return value
        return if (toScale > fromScale) {
            value * pow10(toScale - fromScale)
        } else {
            val divisor = pow10(fromScale - toScale)
            val quotient = value / divisor
            val remainder = value % divisor
            if (kotlin.math.abs(remainder) * 2 >= divisor) {
                quotient + if (value >= 0) 1 else -1
            } else {
                quotient
            }
        }
    }

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }

    public companion object {
        public const val SCALE: Long = 1_000_000L

        /** Build a rate from a human-typed decimal string like "0.0112". */
        public fun rateFromDecimal(text: String): Long? {
            val cleaned = text.trim()
            if (cleaned.isEmpty()) return null
            val parts = cleaned.split('.')
            if (parts.size > 2) return null
            val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
            val fractionRaw = parts.getOrNull(1) ?: ""
            if (fractionRaw.any { it !in '0'..'9' }) return null
            val fraction = fractionRaw.take(6).padEnd(6, '0').ifEmpty { "000000" }.toLongOrNull() ?: return null
            if (whole > Long.MAX_VALUE / SCALE) return null
            return whole * SCALE + fraction
        }
    }
}

public data class Conversion(
    public val amount: Money,
    public val from: CurrencyCode,
    public val to: CurrencyCode,
    /** False when no rate was available and the amount is stale-currency. */
    public val applied: Boolean,
)

/** Totals for a set of confirmed captures, per currency and in the base currency. */
public class Tally(
    private val fx: FxTable,
    private val baseCurrency: CurrencyCode,
) {
    public fun total(captures: List<com.ariadne.model.Capture>, date: LocalDay): TallyResult {
        var base = 0L
        val perCurrency = mutableMapOf<CurrencyCode, Long>()
        var noAmountCount = 0
        var unconverted = 0
        for (capture in captures) {
            val raw = capture.amountMinor
            val code = capture.currency
            if (raw == null || code == null) {
                noAmountCount++
                continue
            }
            perCurrency[code] = (perCurrency[code] ?: 0L) + raw
            val conversion = fx.convert(Money.ofMinor(raw), code, baseCurrency, date)
            if (!conversion.applied && code != baseCurrency) unconverted++
            base += conversion.amount.minorUnits
        }
        return TallyResult(
            baseTotal = Money.ofMinor(base),
            baseCurrency = baseCurrency,
            perCurrency = perCurrency,
            noAmountCount = noAmountCount,
            unconvertedCount = unconverted,
        )
    }
}

public data class TallyResult(
    public val baseTotal: Money,
    public val baseCurrency: CurrencyCode,
    public val perCurrency: Map<CurrencyCode, Long>,
    public val noAmountCount: Int,
    public val unconvertedCount: Int,
) {
    /** Two currencies or more — the state the first 30 seconds must reach. */
    public val isMultiCurrency: Boolean get() = perCurrency.keys.size > 1
}
