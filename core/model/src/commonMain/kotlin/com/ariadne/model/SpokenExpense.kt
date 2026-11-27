package com.ariadne.model

/**
 * What a sentence about money came to.
 *
 * `minor` is in the currency's own minor unit, exactly like every other figure in this
 * app; `remainder` is whatever the sentence said that was not the number or the
 * currency — the merchant, in practice — and may be empty.
 */
public data class SpokenAmount(
    public val minor: Long,
    public val currency: CurrencyCode?,
    public val remainder: String,
)

/**
 * A figure, said out loud.
 *
 * The idea the app is built on is that a photograph and a sentence are enough: the
 * person was in a shop, they know what it cost, and typing it into a keypad is the
 * part that makes them close the app. This reads what a speech recogniser hands back —
 * `twelve fifty`, `€5.50`, `five euros fifty`, `coffee twelve fifty` — into a figure.
 *
 * Deliberately not clever: it takes the first number it finds, treats a second one
 * under a hundred as the cents, and keeps the remaining words as the merchant. A
 * misheard amount is corrected in the editor in one tap, which costs less than a
 * parser that pretends to understand everything.
 */
public object SpokenExpense {

    private val units: Map<String, Int> = mapOf(
        "zero" to 0, "oh" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
        "nineteen" to 19,
    )

    private val tens: Map<String, Int> = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
    )

    /** Currency words, symbols and codes, as a recogniser might hand them over. */
    private val spokenCurrencies: Map<String, String> = mapOf(
        "euro" to "EUR", "euros" to "EUR", "eur" to "EUR", "€" to "EUR",
        "dollar" to "USD", "dollars" to "USD", "usd" to "USD", "buck" to "USD", "bucks" to "USD", "$" to "USD",
        "pound" to "GBP", "pounds" to "GBP", "gbp" to "GBP", "quid" to "GBP", "£" to "GBP",
        "rupee" to "INR", "rupees" to "INR", "inr" to "INR", "₹" to "INR",
        "won" to "KRW", "krw" to "KRW", "yen" to "JPY", "jpy" to "JPY",
        "baht" to "THB", "thb" to "THB", "dong" to "VND", "vnd" to "VND",
        "dirham" to "AED", "dirhams" to "AED", "aed" to "AED",
        "franc" to "CHF", "francs" to "CHF", "chf" to "CHF",
    )

    private val filler: Set<String> = setOf(
        "a", "an", "the", "and", "point", "comma", "spent", "spend", "paid", "pay",
        "cost", "costs", "for", "was", "is", "it", "of", "at", "just", "about",
        "roughly", "around", "euro", "euros", "dollar", "dollars", "pound", "pounds",
        "rupee", "rupees", "eur", "usd", "gbp", "inr", "krw", "jpy", "thb", "vnd",
        "aed", "chf", "won", "yen", "baht", "dong", "dirham", "dirhams", "franc",
        "francs", "buck", "bucks", "quid", "oh",
    )

    /**
     * @param spokenCurrency what the sentence's currency should be if it names none.
     */
    public fun parse(text: String, spokenCurrency: CurrencyCode? = null): SpokenAmount? {
        val tokens = tokenise(text)
        if (tokens.isEmpty()) return null

        var currency: CurrencyCode? = null
        val words = mutableListOf<String>()

        var total = 0
        var pendingTens = 0
        var cents: Int? = null
        var seenAny = false

        fun flushTens() {
            if (pendingTens != 0) {
                total += pendingTens
                pendingTens = 0
            }
        }

        for (raw in tokens) {
            val token = raw.trim()
            if (token.isEmpty()) continue
            val code = spokenCurrencies[token]
            if (code != null) {
                if (currency == null) currency = runCatching { CurrencyCode.of(code) }.getOrNull()
                continue
            }
            val value: Int? = units[token] ?: tens[token]
            val isTensWord = tens.containsKey(token)
            when {
                isTensWord -> {
                    flushTens()
                    // "twelve fifty" is twelve and fifty cents; "one hundred twenty"
                    // is not, because the hundred has already been counted.
                    if (total in 1..99 && cents == null) {
                        cents = value
                    } else {
                        pendingTens = value ?: 0
                        seenAny = true
                    }
                }
                value != null -> {
                    // "twenty three" is twenty-three, not twenty and three cents.
                    if (pendingTens != 0) {
                        total += pendingTens + value
                        pendingTens = 0
                        seenAny = true
                    } else if (!seenAny) {
                        total = value
                        seenAny = true
                    } else {
                        // A second number is the cents; a third is a correction.
                        cents = value
                    }
                }
                token == "hundred" -> {
                    flushTens()
                    total = (if (total == 0) 1 else total) * 100
                    seenAny = true
                }
                token == "thousand" -> {
                    flushTens()
                    total = (if (total == 0) 1 else total) * 1000
                    seenAny = true
                }
                token.all { it.isDigit() } -> {
                    flushTens()
                    val n = token.toIntOrNull() ?: continue
                    if (!seenAny) {
                        total = n
                        seenAny = true
                    } else if (cents == null) {
                        cents = n
                    } else {
                        cents = n
                    }
                }
                else -> if (!filler.contains(token)) words.add(token)
            }
        }
        flushTens()
        if (!seenAny) return null

        val code = currency ?: spokenCurrency
        val scale = code?.scale ?: 2
        val whole = total.toLong()
        val fraction = (cents ?: 0).toLong()
        val minor = when (scale) {
            0 -> whole
            3 -> whole * 1000 + fraction.coerceIn(0, 999)
            else -> whole * 100 + fraction.coerceIn(0, 99)
        }
        if (minor <= 0) return null
        if (minor > 100_000_000L) return null

        return SpokenAmount(
            minor = minor,
            currency = code,
            remainder = words.joinToString(" ").trim(),
        )
    }

    private fun tokenise(text: String): List<String> {
        val cleaned = text.lowercase()
            .replace("€", " euro ")
            .replace("£", " pound ")
            .replace("$", " dollar ")
            .replace("₹", " rupee ")
            .replace(Regex("(?<=\\d),(?=\\d)"), ".")
        return cleaned
            .split(Regex("[^a-z0-9.]+"))
            .flatMap { part -> part.split('.') }
            .filter { it.isNotEmpty() }
    }
}
