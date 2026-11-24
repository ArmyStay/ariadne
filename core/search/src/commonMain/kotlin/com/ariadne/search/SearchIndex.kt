package com.ariadne.search

import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Money

/**
 * In-memory inverted index, rebuilt on launch from confirmed captures.
 *
 * Chosen over SQLite FTS on purpose: FTS5 behaves differently per platform and
 * cannot be exercised from `commonTest`, whereas this is 150 lines of pure
 * Kotlin with a test for every axis a traveller actually remembers by.
 */
public class SearchIndex(private val cap: Int = 5_000) {

    private val byMerchant = HashMap<String, MutableList<Capture>>()
    private val byDay = HashMap<LocalDay, MutableList<Capture>>()
    private val byCurrency = HashMap<CurrencyCode, MutableList<Capture>>()
    private val byAmount = HashMap<Long, MutableList<Capture>>()
    private val all = LinkedHashMap<String, Capture>()

    public val size: Int get() = all.size

    public fun rebuild(captures: List<Capture>) {
        byMerchant.clear()
        byDay.clear()
        byCurrency.clear()
        byAmount.clear()
        all.clear()
        captures.asSequence()
            // Everything that is part of the record, including a capture the
            // human deliberately left amount-less. Queued items are not history.
            .filter {
                it.state == com.ariadne.model.CaptureState.CONFIRMED ||
                    it.state == com.ariadne.model.CaptureState.ARCHIVED
            }
            .sortedByDescending { it.createdAt }
            .take(cap)
            .forEach(::add)
    }

    public fun add(capture: Capture) {
        all[capture.id] = capture
        capture.merchant?.let { merchant ->
            tokens(merchant).forEach { token ->
                byMerchant.getOrPut(token) { mutableListOf() }.add(capture)
            }
        }
        byDay.getOrPut(capture.localDate) { mutableListOf() }.add(capture)
        capture.currency?.let { byCurrency.getOrPut(it) { mutableListOf() }.add(capture) }
        capture.amountMinor?.let { byAmount.getOrPut(it) { mutableListOf() }.add(capture) }
    }

    public fun remove(id: String) {
        val capture = all.remove(id) ?: return
        capture.merchant?.let { merchant ->
            tokens(merchant).forEach { token -> byMerchant[token]?.removeAll { it.id == id } }
        }
        byDay[capture.localDate]?.removeAll { it.id == id }
        capture.currency?.let { byCurrency[it]?.removeAll { c -> c.id == id } }
        capture.amountMinor?.let { byAmount[it]?.removeAll { c -> c.id == id } }
    }

    /**
     * Query by what a person remembers: a shop name fragment, a price, a day.
     * Results are captures, newest first — never text rows.
     */
    public fun search(query: String, limit: Int = 50): List<Capture> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = LinkedHashMap<String, Capture>()

        // Merchant: any indexed token starting with what was typed.
        val queryTokens = tokens(trimmed)
        queryTokens.forEach { qt ->
            byMerchant.forEach { (token, captures) ->
                if (token.startsWith(qt) || token.contains(qt)) {
                    captures.forEach { results[it.id] = it }
                }
            }
        }

        // Amount: "1200", "1,200.50" — matched in minor units.
        Money.parseOrNull(trimmed, CurrencyCode.of("XXX"))?.let { parsed ->
            byAmount[parsed.minorUnits]?.forEach { results[it.id] = it }
        }
        // Short queries like "120" should still widen to a plausible price.
        trimmed.toLongOrNull()?.let { plain ->
            byAmount.forEach { (minor, captures) ->
                if (minor.toString().startsWith(plain.toString())) {
                    captures.forEach { results[it.id] = it }
                }
            }
        }

        // Day: ISO, or just a day-of-month for "the 14th".
        LocalDay.parseOrNull(trimmed)?.let { day ->
            byDay[day]?.forEach { results[it.id] = it }
        }
        trimmed.toIntOrNull()?.takeIf { it in 1..31 }?.let { dom ->
            byDay.forEach { (day, captures) ->
                if (day.day == dom) captures.forEach { results[it.id] = it }
            }
        }

        // Currency code or symbol.
        CurrencyCode.parseOrNull(trimmed)?.let { code ->
            byCurrency[code]?.forEach { results[it.id] = it }
        }

        return results.values.sortedByDescending { it.createdAt }.take(limit)
    }

    private fun tokens(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotEmpty() }
}
