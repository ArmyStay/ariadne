package com.ariadne.fx

import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Rate

/**
 * Where a rate comes from, when the device can reach the network at all.
 *
 * An interface with one method rather than an HTTP client in here, for three
 * reasons: the policy below is testable without a server, the platform supplies
 * the transport (Android's `HttpURLConnection` in `:app:android`), and the shared
 * module never grows a dependency it does not need. `get` returns null for every
 * kind of failure — no signal, a 500, a captive portal's HTML — because a rate
 * that cannot be fetched is not an error the traveller should be shown twice.
 */
public interface RateSource {
    public suspend fun get(url: String): String?
}

/**
 * Reads frankfurter.app, the free no-key rate feed.
 *
 * The response is small and flat:
 *
 * ```json
 * {"amount":1.0,"base":"THB","date":"2026-11-20","rates":{"INR":2.5873,"JPY":4.42}}
 * ```
 *
 * Parsed by hand rather than with a serialization plugin: this module is a pure
 * Kotlin library with no compiler plugin and no reflection, the shape is two
 * fields and a number map, and a hand parser that returns null on anything
 * unexpected is easier to prove correct here than a plugin we cannot compile in
 * this sandbox. It is deliberately strict: a body it does not fully understand
 * yields no rates at all, so a changed API shows up as "no rates" rather than as
 * silently wrong money.
 */
public object Frankfurter {

    private const val LATEST = "https://api.frankfurter.app/latest"

    /** Latest published rates for [quotes], or null when there is nothing to ask for. */
    public fun url(base: CurrencyCode, quotes: List<CurrencyCode>): String? {
        val wanted = quotes.filter { it != base }.joinToString(",") { it.code }.ifEmpty { return null }
        return "$LATEST?from=${base.code}&to=$wanted"
    }

    /** The rates as published on [day] — used to fill a gap when the cache is empty. */
    public fun urlForDay(base: CurrencyCode, quotes: List<CurrencyCode>, day: LocalDay): String? {
        val wanted = quotes.filter { it != base }.joinToString(",") { it.code }.ifEmpty { return null }
        return "https://api.frankfurter.app/${day.iso}?from=${base.code}&to=$wanted"
    }

    /**
     * The rows a response implies, or null if it cannot be read.
     *
     * Every row is stored as base → quote on the response's own date, so a cached
     * rate remembers the day it was true instead of pretending to be timeless.
     */
    public fun parse(body: String?, base: CurrencyCode): ParsedRates? {
        if (body.isNullOrBlank()) return null
        val day = LocalDay.parseOrNull(stringField(body, "date")) ?: return null
        val ratesObject = objectField(body, "rates") ?: return null
        val rates = buildList {
            var index = 0
            while (index < ratesObject.length) {
                val quoteStart = ratesObject.indexOf('"', index)
                if (quoteStart < 0) break
                val quoteEnd = ratesObject.indexOf('"', quoteStart + 1)
                if (quoteEnd < 0) break
                val code = CurrencyCode.parseOrNull(ratesObject.substring(quoteStart + 1, quoteEnd))
                val colon = ratesObject.indexOf(':', quoteEnd)
                if (colon < 0) break
                var valueEnd = colon + 1
                while (valueEnd < ratesObject.length && ratesObject[valueEnd] !in ",}") valueEnd++
                val raw = ratesObject.substring(colon + 1, valueEnd).trim()
                if (code != null && code != base) {
                    FxTable.rateFromDecimal(raw)?.takeIf { it > 0L }?.let { scaled ->
                        add(Rate(base = base, quote = code, date = day, rateScaled = scaled))
                    }
                }
                index = valueEnd + 1
            }
        }
        return if (rates.isEmpty()) null else ParsedRates(day, rates)
    }

    private fun stringField(body: String, name: String): String? {
        val key = "\"$name\""
        val at = body.indexOf(key)
        if (at < 0) return null
        val open = body.indexOf('"', at + key.length + 1)
        if (open < 0) return null
        val close = body.indexOf('"', open + 1)
        if (close < 0) return null
        return body.substring(open + 1, close)
    }

    /** The contents of the `"rates": { ... }` object, braces excluded. */
    private fun objectField(body: String, name: String): String? {
        val key = "\"$name\""
        val at = body.indexOf(key)
        if (at < 0) return null
        val open = body.indexOf('{', at + key.length)
        if (open < 0) return null
        var depth = 0
        for (i in open until body.length) {
            when (body[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return body.substring(open + 1, i)
                }
            }
        }
        return null
    }
}

/** A parsed response: the day it was published, and one row per quote. */
public data class ParsedRates(
    public val day: LocalDay,
    public val rates: List<Rate>,
)

/**
 * Decides when a rate is old enough to be worth asking for again.
 *
 * Kept separate from the transport and from the store so the rule is one sentence
 * a test can hold: a rate is fresh while it is from today or yesterday, and stale
 * afterwards. A stale rate is still *used* — the last known figure beats no figure
 * — it is only re-fetched, and the UI prints its date so nobody mistakes it for
 * today's.
 */
public object RatePolicy {

    public const val MAX_AGE_DAYS: Int = 2

    public fun isStale(rateDay: LocalDay?, today: LocalDay): Boolean {
        if (rateDay == null) return true
        val age = LocalDay.toEpochDay(today) - LocalDay.toEpochDay(rateDay)
        return age >= MAX_AGE_DAYS || age < 0
    }

    /**
     * True when it is worth spending a request on [quotes].
     *
     * One request covers every currency the trip has used, so the cost is the same
     * whether one foreign currency is in play or six.
     */
    public fun shouldFetch(cached: Map<CurrencyCode, LocalDay?>, quotes: List<CurrencyCode>, today: LocalDay): Boolean =
        quotes.distinct().any { isStale(cached[it], today) }
}
