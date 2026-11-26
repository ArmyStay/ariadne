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
        return convertAt(amount, from, to, rate)
    }

    /**
     * Converts [amount] at a rate the caller already holds.
     *
     * This is what a confirmed capture uses: the rate is read from the row, not
     * looked up again, so the figure that was confirmed is the figure that is
     * totalled — today, tomorrow, and after the next rate refresh.
     */
    public fun convertAt(amount: Money, from: CurrencyCode, to: CurrencyCode, rateScaled: Long): Conversion {
        if (from == to) return Conversion(amount, from, to, applied = false)
        if (rateScaled <= 0L) return Conversion(amount, from, to, applied = false)
        val target = to.scale
        val source = from.scale
        // Work in a single wide integer: minor units × rate, then rescale.
        val numerator = amount.minorUnits * rateScaled
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

/**
 * Totals for a set of confirmed captures, per currency and in the base currency.
 *
 * The rule this class exists to enforce: **nothing enters the base total without
 * a rate**. Before this, a capture whose rate was missing had its own minor units
 * added to the base figure anyway, so THB 2,555 + INR 50 printed as "₹2,605" —
 * two different currencies added as if they were the same one. Money that cannot
 * be converted is now held aside in [TallyResult.unconverted] and named on screen
 * rather than silently folded into a number that is not true.
 *
 * Each capture converts at *its own* rate: the snapshot written when the amount
 * was confirmed, or, for rows that predate the snapshot, the rate table on the
 * capture's own day. It used to be three different dates depending on the page.
 */
public class Tally(
    private val fx: FxTable,
    private val baseCurrency: CurrencyCode,
) {
    public fun total(captures: List<com.ariadne.model.Capture>): TallyResult {
        var base = 0L
        var converted = 0
        val perCurrency = mutableMapOf<CurrencyCode, Long>()
        val unconverted = mutableMapOf<CurrencyCode, Long>()
        val rateDates = mutableMapOf<CurrencyCode, LocalDay>()
        var noAmountCount = 0
        var unconvertedCount = 0
        for (capture in captures) {
            val raw = capture.amountMinor
            val code = capture.currency
            if (raw == null || code == null) {
                noAmountCount++
                continue
            }
            perCurrency[code] = (perCurrency[code] ?: 0L) + raw
            val amount = Money.ofMinor(raw)
            if (code == baseCurrency) {
                base += raw
                converted++
                continue
            }
            val snapshot = capture.rateScaled
            val conversion = if (snapshot != null) {
                fx.convertAt(amount, code, baseCurrency, snapshot)
            } else {
                fx.convert(amount, code, baseCurrency, capture.localDate)
            }
            if (!conversion.applied) {
                // Held aside, never added. This is the ₹2,605 bug.
                unconverted[code] = (unconverted[code] ?: 0L) + raw
                unconvertedCount++
                continue
            }
            base += conversion.amount.minorUnits
            converted++
            val rateDay = capture.rateDate ?: capture.localDate
            val existing = rateDates[code]
            // The oldest day wins: the label reads "at <day>", and a rate that was
            // true earlier is true for the figure it converted.
            if (existing == null || LocalDay.toEpochDay(rateDay) < LocalDay.toEpochDay(existing)) {
                rateDates[code] = rateDay
            }
        }
        return TallyResult(
            baseTotal = Money.ofMinor(base),
            baseCurrency = baseCurrency,
            perCurrency = perCurrency,
            noAmountCount = noAmountCount,
            unconvertedCount = unconvertedCount,
            unconverted = unconverted,
            convertedCount = converted,
            rateDates = rateDates,
        )
    }
}

public data class TallyResult(
    public val baseTotal: Money,
    public val baseCurrency: CurrencyCode,
    public val perCurrency: Map<CurrencyCode, Long>,
    public val noAmountCount: Int,
    /** How many captures had an amount but no usable rate — none of them counted. */
    public val unconvertedCount: Int,
    /** The money that could not be converted, in its own currency, so it can be named. */
    public val unconverted: Map<CurrencyCode, Long> = emptyMap(),
    public val convertedCount: Int = 0,
    /** The day each foreign currency's rate was true, for "≈ at 20 Nov". */
    public val rateDates: Map<CurrencyCode, LocalDay> = emptyMap(),
) {
    /** Two currencies or more — the state the first 30 seconds must reach. */
    public val isMultiCurrency: Boolean get() = perCurrency.keys.size > 1

    /**
     * True when the base total accounts for every amount in the set.
     *
     * False means the figure is a floor, not the spend, and the UI must say so.
     */
    public val isComplete: Boolean get() = unconverted.isEmpty()
}
