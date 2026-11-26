package com.ariadne.extract

import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.MerchantName

/**
 * Pure-Kotlin receipt reader: OCR lines in, [ExtractionOutcome] out.
 *
 * No platform types, no clocks, no I/O. The 30 fixtures in `commonTest` are the
 * specification; when a new market teaches the parser something, a fixture is
 * added first.
 *
 * Difficulty being solved: a receipt shows a dozen numbers (quantities, tax
 * lines, timestamps, phone numbers, loyalty balances) and exactly one of them is
 * the total. The parser scores candidates instead of picking the first match.
 */
public class ReceiptParser(
    private val fallbackCurrency: CurrencyCode,
    private val confidenceFloor: Double = DEFAULT_FLOOR,
) {

    public fun parse(lines: List<String>): ExtractionOutcome {
        if (lines.isEmpty()) return ExtractionOutcome.Nothing(text = "")
        val text = lines.joinToString("\n")
        val normalized = lines.map { it.trim() }.filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return ExtractionOutcome.Nothing(text = text)

        val currency = detectCurrency(normalized) ?: fallbackCurrency
        val amount = detectAmount(normalized)
        val merchant = detectMerchant(normalized)
        val date = detectDate(normalized)

        if (amount == null) {
            return ExtractionOutcome.Nothing(confidence = 0.0, text = text)
        }

        val confidence = amount.confidence *
            (if (detectCurrency(normalized) != null) 1.0 else CURRENCY_PENALTY) *
            (if (merchant != null) 1.0 else MERCHANT_PENALTY)

        if (amount.confidence < confidenceFloor) {
            // Below the floor we return what we saw, but the caller leaves the
            // field blank rather than pre-filling a guess.
            return ExtractionOutcome.Nothing(confidence = confidence, text = text)
        }

        val currencyExplicit = detectCurrency(normalized) != null
        val minor = toMinorUnits(amount.value, currency)

        // Money and a merchant: the merchant is a bonus, not a precondition. The
        // parser only reports one when it cleared detection, and the UI shows it
        // in the same field either way.
        return if (merchant != null) {
            ExtractionOutcome.AmountAndMerchant(
                amountMinor = minor,
                currency = currency,
                merchant = merchant,
                confidence = confidence,
                date = date,
            )
        } else {
            ExtractionOutcome.AmountOnly(
                amountMinor = minor,
                currency = currency,
                confidence = if (currencyExplicit) confidence else confidence * FALLBACK_MARK,
                date = date,
            )
        }
    }

    // -- amount ------------------------------------------------------------

    private data class AmountCandidate(val value: BigDecimalish, val confidence: Double)

    /**
     * A tiny decimal holder so the parser stays free of platform number types.
     * Values are stored as minor-unit longs once the currency is known.
     */
    private data class BigDecimalish(val whole: Long, val fraction: Long, val scale: Int) {
        fun toMinor(currency: CurrencyCode): Long {
            val target = currency.scale
            var value = whole * POWERS[target]
            value += when {
                this.scale == target -> fraction
                this.scale < target -> fraction * POWERS[target - this.scale]
                else -> fraction / POWERS[this.scale - target]
            }
            return value
        }

        companion object {
            val POWERS = longArrayOf(1, 10, 100, 1000)
        }
    }

    private fun detectAmount(lines: List<String>): AmountCandidate? {
        var best: AmountCandidate? = null
        var bestScore = 0.0

        lines.forEachIndexed { index, raw ->
            val line = raw.lowercase()
            val isTotalLine = TOTAL_WORDS.any { line.contains(it) }
            val isNegativeContext = NEGATIVE_CONTEXT.any { line.contains(it) }
            val isNoisy = NOISY_CONTEXT.any { line.contains(it) }
            // "Grand total incl. tax" is still a total; a bare tax line is not.
            if (isNegativeContext && !isTotalLine) return@forEachIndexed

            for (match in NUMBER.findAll(raw)) {
                val value = parseNumber(match.value) ?: continue
                if (value.whole == 0L && value.fraction == 0L) continue
                if (value.whole > MAX_PLAUSIBLE_WHOLE) continue

                var score = BASE_SCORE
                if (isTotalLine) score += TOTAL_BONUS
                if (isNegativeContext) score -= 0.10
                if (isNoisy) score -= NOISY_PENALTY
                // Later lines win ties: totals sit at the bottom of a receipt.
                score += (index.toDouble() / lines.size) * POSITION_BONUS
                // A number with a decimal part is far more likely to be money.
                if (value.scale > 0) score += DECIMAL_BONUS
                // Currency symbol or code adjacent to the digits is decisive.
                if (hasCurrencyAdjacency(raw, match.range)) score += ADJACENCY_BONUS
                // Long digit runs with no separator are usually phone numbers,
                // barcodes or loyalty ids, not prices.
                if (match.value.replace(",", "").length >= 7 && value.scale == 0) score -= LONG_RUN_PENALTY

                if (score > bestScore) {
                    bestScore = score
                    best = AmountCandidate(value, score.coerceIn(0.0, 1.0))
                }
            }
        }
        return best
    }

    private fun parseNumber(raw: String): BigDecimalish? {
        // Grouping separators come in two flavours: 1,250.50 (western) and
        // 1.250,50 (much of Europe). Decide by the rightmost separator.
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        val decimalSep: Char? = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 -> if (cleaned.length - lastComma - 1 in 1..2) ',' else null
            lastDot >= 0 -> if (cleaned.length - lastDot - 1 in 1..2) '.' else null
            else -> null
        }
        val digitsOnly = if (decimalSep == null) {
            cleaned.filter { it.isDigit() }
        } else {
            val whole = cleaned.substringBeforeLast(decimalSep).filter { it.isDigit() }
            val fraction = cleaned.substringAfterLast(decimalSep).filter { it.isDigit() }
            return BigDecimalish(
                whole = whole.ifEmpty { "0" }.toLongOrNull() ?: return null,
                fraction = fraction.toLongOrNull() ?: return null,
                scale = fraction.length.coerceAtMost(3),
            ).let {
                if (fraction.length > 3) BigDecimalish(it.whole, fraction.take(3).toLong(), 3) else it
            }
        }
        if (digitsOnly.isEmpty()) return null
        val whole = digitsOnly.toLongOrNull() ?: return null
        return BigDecimalish(whole, 0, 0)
    }

    private fun hasCurrencyAdjacency(line: String, range: IntRange): Boolean {
        val before = line.substring(0, range.first.coerceAtLeast(0)).takeLast(3)
        val after = line.substring((range.last + 1).coerceAtMost(line.length)).take(3)
        val probe = before + after
        return CURRENCY_TOKENS.any { probe.contains(it) }
    }

    private fun toMinorUnits(value: BigDecimalish, currency: CurrencyCode): Long = value.toMinor(currency)

    // -- currency ----------------------------------------------------------

    private fun detectCurrency(lines: List<String>): CurrencyCode? {
        val joined = lines.joinToString(" ")
        // ¥ is shared by JPY and CNY; only the surrounding words disambiguate.
        val lowerJoined = joined.lowercase()
        if (lowerJoined.contains("元") || lowerJoined.contains("rmb") || lowerJoined.contains("cny")) {
            return CurrencyCode.of("CNY")
        }
        if (joined.contains("¥")) return CurrencyCode.of("JPY")
        for ((token, code) in SYMBOL_TO_CODE) {
            if (joined.contains(token)) return CurrencyCode.of(code)
        }
        for (match in CODE_TOKEN.findAll(joined)) {
            val candidate = match.value.uppercase()
            if (candidate in ISO_CODES) return CurrencyCode.of(candidate)
        }
        // Spelled-out currencies, the way kiosk receipts print them.
        val lower = joined.lowercase()
        for ((word, code) in WORD_TO_CODE) {
            if (lower.contains(word)) return CurrencyCode.of(code)
        }
        return null
    }

    // -- merchant ----------------------------------------------------------

    /**
     * The merchant is almost never labelled: it is the first line that reads
     * like a name rather than like a number, a date or a legal footer.
     */
    private fun detectMerchant(lines: List<String>): String? {
        val candidates = lines.take(MERCHANT_SCAN_DEPTH)
        for (raw in candidates) {
            val line = raw.trim()
            if (line.length < 3 || line.length > 48) continue
            if (line.none { it.isLetter() }) continue
            if (MERCHANT_BLOCKLIST.any { line.lowercase().contains(it) }) continue
            if (NUMBER.findAll(line).count() > 2) continue
            // A line that is mostly digits is a receipt number, not a shopfront.
            val digits = line.count { it.isDigit() }
            if (digits > line.length / 3) continue
            // A line that reads like a menu bar is a photographed screen, not a
            // shop. "ction View Go Run Terminal Help" was accepted here before this
            // check existed, and the record carried it as a title. The vocabulary
            // lives in `:core:model` so the UI applies the same rule to a name a
            // person typed, and so both can be tested without OCR.
            // `continue`, not `return null`: the first line of a photographed
            // screen is chrome, and the shop's name may be the line below it.
            MerchantName.clean(line)?.let { return it }
        }
        return null
    }

    // -- date --------------------------------------------------------------

    private val isoDate = Regex("""(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})""")
    private val numericDate = Regex("""(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})""")
    private val namedDate = Regex(
        """(\d{1,2})\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?,?\s+(\d{4})""",
        RegexOption.IGNORE_CASE,
    )

    private fun detectDate(lines: List<String>): LocalDay? {
        val joined = lines.joinToString(" ")
        // ISO first, it is unambiguous.
        for (match in isoDate.findAll(joined)) {
            val m = match.groupValues[2].padStart(2, '0')
            val d = match.groupValues[3].padStart(2, '0')
            LocalDay.parseOrNull("${match.groupValues[1]}-$m-$d")?.let { return it }
        }
        // Day-first is the norm everywhere this app is used on holiday, but the
        // result is only ever a suggestion layered over the capture timestamp.
        for (match in numericDate.findAll(joined)) {
            val a = match.groupValues[1].toIntOrNull() ?: continue
            val b = match.groupValues[2].toIntOrNull() ?: continue
            val y = match.groupValues[3].toIntOrNull() ?: continue
            val (day, month) = if (a > 12) a to b else if (b > 12) b to a else a to b
            if (month !in 1..12 || day !in 1..31) continue
            return runCatching { LocalDay.of(y, month, day) }.getOrNull()
        }
        for (match in namedDate.findAll(joined)) {
            val d = match.groupValues[1].toIntOrNull() ?: continue
            val monthName = match.groupValues[2].lowercase()
            val y = match.groupValues[3].toIntOrNull() ?: continue
            val month = MONTHS.indexOf(monthName) + 1
            if (month !in 1..12) continue
            return runCatching { LocalDay.of(y, month, d) }.getOrNull()
        }
        return null
    }

    public companion object {
        public const val DEFAULT_FLOOR: Double = 0.35
        public const val MERCHANT_MIN_CONFIDENCE: Double = 0.45

        private const val BASE_SCORE = 0.20
        private const val TOTAL_BONUS = 0.45
        private const val NOISY_PENALTY = 0.30
        private const val POSITION_BONUS = 0.12
        private const val DECIMAL_BONUS = 0.10
        private const val ADJACENCY_BONUS = 0.18
        private const val LONG_RUN_PENALTY = 0.40
        private const val CURRENCY_PENALTY = 0.90
        private const val MERCHANT_PENALTY = 0.95
        private const val FALLBACK_MARK = 0.95
        private const val MAX_PLAUSIBLE_WHOLE = 9_999_999L
        private const val MERCHANT_SCAN_DEPTH = 4

        private val NUMBER = Regex("""\d[\d.,]*\d|\d""")

        private val TOTAL_WORDS = listOf(
            "total", "amount due", "grand total", "balance due", "net payable",
            "to pay", "payable", "сумма", "toplam", "total due", "jumlah",
        )

        private val NEGATIVE_CONTEXT = listOf(
            "discount", "change", "cash back", "cashback", "refund", "saving",
            "tax", "vat", "gst", "cgst", "sgst", "service charge", "tip", "round off",
            "subtotal", "sub total", "qty", "quantity", "rate", "unit price",
        )

        private val NOISY_CONTEXT = listOf(
            "invoice", "bill no", "phone", "tel", "mobile", "gstin", "vat no", "table",
            "order", "txn", "terminal", "card", "auth", "approval", "loyalty", "points",
        )

        private val CURRENCY_TOKENS = listOf(
            "₹", "$", "€", "£", "¥", "₩", "฿", "₫", "₺", "₪", "rm", "rs", "inr", "usd",
            "eur", "gbp", "thb", "vnd", "idr", "myr", "sgd", "jpy",
        )

        // ¥ is deliberately absent: detectCurrency resolves it before this table
        // is consulted, because the same glyph is JPY and CNY.
        private val SYMBOL_TO_CODE = listOf(
            "₹" to "INR",
            "฿" to "THB",
            "₫" to "VND",
            "₩" to "KRW",
            "₺" to "TRY",
            "₪" to "ILS",
            "€" to "EUR",
            "£" to "GBP",
            "$" to "USD",
        )

        private val WORD_TO_CODE = listOf(
            "rupiah" to "IDR",
            "ringgit" to "MYR",
            "baht" to "THB",
            "rupee" to "INR",
            "rupees" to "INR",
            "dollar" to "USD",
            "euro" to "EUR",
            "dirham" to "AED",
        )

        private val CODE_TOKEN = Regex("""\b[A-Z]{3}\b""")

        private val ISO_CODES = setOf(
            "INR", "USD", "EUR", "GBP", "JPY", "CNY", "THB", "VND", "IDR", "MYR",
            "SGD", "HKD", "KRW", "TWD", "PHP", "AUD", "NZD", "CAD", "CHF", "SEK",
            "NOK", "DKK", "PLN", "CZK", "HUF", "TRY", "ZAR", "BRL", "MXN", "AED",
            "SAR", "QAR", "ILS", "EGP", "MAD", "KES", "TZS", "LKR", "NPR", "BDT",
            "PKR", "KHR", "LAK", "KZT", "UZS", "GEL", "ISK", "KWD", "BHD", "OMR",
        )

        private val MERCHANT_BLOCKLIST = listOf(
            "tax invoice", "invoice", "receipt", "gstin", "thank you", "welcome",
            "www.", "http", "tel:", "phone", "date:", "time:", "bill no", "order no",
            "cashier", "server", "table", "customer copy", "duplicate", "original",
        )

        private val MONTHS = listOf(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
        )
    }
}
