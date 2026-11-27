package com.ariadne.model

/**
 * Two codes, no server.
 *
 * Ariadne's thesis is that a record can live on the device that made it. Group travel is
 * the one place that thesis is tested, because two people are on the same trip and only
 * one of them is holding each receipt.
 *
 * The answer here is a **hand-off**, not a sync. One person reads a code out, the other
 * types it, and afterwards each phone knows the same plan — the dates, the currency, the
 * budget — with no account, no backend and nothing that updates on its own. Later, when a
 * day is closed, a second code carries that day's figures back the other way.
 *
 * Two properties make this honest rather than cute:
 *
 * - **A code is a copy, not a link.** Once decoded it is just data on the receiving
 *   phone. Nothing arrives later, and nothing edited here appears there.
 * - **The codec is pure Kotlin in this module** — the same on Android, iOS, desktop and in
 *   the browser, one encoder, one set of symbols, and property tests over the round trip.
 *
 * ### What a trip code carries, and what it leaves out
 *
 * Seventy bits, fixed: the start day (fourteen bits, days since 2020-01-01, so a code
 * reaches 2064 without a century byte), the number of nights (eight), the base currency
 * (three letters, five bits each), and a budget in minor units (one flag bit, thirty-two
 * bits). Sixteen characters after the `AR1` prefix, checksum included, written in groups
 * of four so it can be read down a phone line: `AR1-K7QM-2X9B-4HDR-8TQP`.
 *
 * The trip's *name* is deliberately not in the payload. A name of the twenty-four bytes
 * the plan allowed would more than double the code and make it unreadable aloud, and a
 * name is the one part of a trip with no arithmetic in it — so the join page asks for it,
 * and the code keeps the parts that have to agree: when the trip is, what money it is in,
 * and what it was meant to cost.
 *
 * ### What a ledger code carries
 *
 * The traveller's name, then one line per confirmed entry: the day, the amount in minor
 * units, its currency, a category if one was claimed, two initials of the merchant, and
 * an eight-bit digest of that line so a damaged paste shows up as a damaged line rather
 * than as a wrong total. No photograph, no note, no path — the parts of a record that
 * only make sense on the phone that took it stay there. This code is longer than a trip
 * code and is meant to be **pasted**, not typed.
 */
public object ShareCodes {

    /** Marks a trip plan, and the codec version: this is version one. */
    public const val TRIP_PREFIX: String = "AR1"

    /** Marks a day's figures handed back. */
    public const val LEDGER_PREFIX: String = "AR2"

    /**
     * The thirty-two symbols a code is made of.
     *
     * No `0`, `1`, `I` or `O`: a code is read aloud and typed by someone who did not write
     * it, and those four are the characters that turn a plan into a wrong trip.
     */
    private const val ALPHABET: String = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    /** Days are counted from here, so fourteen bits reach 2064 without a century byte. */
    private val DAY_ZERO: LocalDay = LocalDay.of(2020, 1, 1)

    private const val MAX_START_DAY: Long = 16383L
    private const val MAX_NIGHTS: Long = 255L
    private const val MAX_BUDGET: Long = 0xFFFFFFFFL
    private const val MAX_NAME_CHARS: Int = 19
    private const val MAX_ENTRIES: Int = 200

    /** Sixteen symbols after the prefix, whatever the group separators look like. */
    public const val TRIP_LENGTH: Int = 16

    /** A short, quotable failure — the reason a person is told. */
    private const val MISMATCH: String = "One character is wrong — check the code and try again."

    /** What one phone tells another about a trip. */
    public data class Plan(
        public val start: LocalDay,
        public val end: LocalDay,
        public val baseCurrency: CurrencyCode,
        /** Null when the trip had no budget, or it was too large for a code to carry. */
        public val budgetMinor: Long?,
    ) {
        /** "2026-12-03 – 2026-12-08 · INR · budget 40,000.00" — read back before joining. */
        public fun summary(): String {
            val budget = budgetMinor
                ?.let { " · budget " + Money.ofMinor(it).format(baseCurrency) }
                ?: ""
            return "${start.iso} – ${end.iso} · ${baseCurrency.code}$budget"
        }
    }

    /** A code that could not be read, and why, in words a person can act on. */
    public sealed interface Outcome<out T> {
        public data class Ok<T>(public val value: T) : Outcome<T>
        public data class Failed(public val reason: String) : Outcome<Nothing>
    }

    /** One confirmed figure, as another phone will read it. */
    public data class Entry(
        public val day: LocalDay,
        public val amountMinor: Long,
        public val currency: CurrencyCode,
        public val category: Category? = null,
        /** Two letters, so "Nilgiri Provisions" arrives as "NP" and nothing more. */
        public val merchantInitials: String? = null,
    )

    /** One traveller's figures, handed back. */
    public data class Ledger(
        public val traveller: String,
        public val entries: List<Entry>,
    ) {
        /** The local days these figures belong to, in order. */
        public val days: List<LocalDay> get() = entries.map { it.day }.distinct().sortedBy { it.iso }
    }

    // -- a trip plan -------------------------------------------------------

    /**
     * Encodes a trip for another phone.
     *
     * A budget outside the thirty-two bits a code can carry is left out rather than
     * clamped: a budget that silently became a different number would be worse than none.
     * [Plan.budgetMinor] is then null, which the sharing page reports rather than hides.
     */
    public fun encodeTrip(plan: Plan): String {
        val startDay = LocalDay.toEpochDay(plan.start) - LocalDay.toEpochDay(DAY_ZERO)
        require(startDay in 0..MAX_START_DAY) { "a trip code runs from 2020 to 2064" }
        val nights = LocalDay.toEpochDay(plan.end) - LocalDay.toEpochDay(plan.start)
        require(nights in 0..MAX_NIGHTS) { "a trip code carries at most 255 nights" }
        val code = plan.baseCurrency.code
        require(code.length == 3 && code.all { it in 'A'..'Z' }) { "a currency is three capital letters" }

        val budget = plan.budgetMinor?.takeIf { it in 0..MAX_BUDGET }
        val writer = BitWriter()
        writer.write(startDay, 14)
        writer.write(nights, 8)
        code.forEach { letter -> writer.write((letter - 'A').toLong(), 5) }
        writer.write(if (budget == null) 0L else 1L, 1)
        if (budget != null) writer.write(budget, 32)
        return format(TRIP_PREFIX, writer.finish())
    }

    /** Reads a trip code, with a reason when it cannot be read. */
    public fun decodeTrip(text: String): Outcome<Plan> {
        val symbols = symbolsOf(text, TRIP_PREFIX) ?: return Outcome.Failed(
            if (text.isBlank()) "Type or paste a code first." else "A trip code starts with AR1.",
        )
        if (symbols.size != TRIP_LENGTH) {
            return Outcome.Failed(
                "A trip code is sixteen characters after AR1 — this one has ${symbols.size}.",
            )
        }
        unknownSymbol(text, TRIP_PREFIX)?.let { bad ->
            return Outcome.Failed("'$bad' is not one of a code's characters.")
        }
        val payload = splitChecksum(symbols) ?: return Outcome.Failed(MISMATCH)
        return try {
            val reader = BitReader(payload)
            val startDay = reader.read(14)
            val nights = reader.read(8)
            val currency = buildString(3) { repeat(3) { append('A' + reader.read(5).toInt()) } }
            val hasBudget = reader.read(1) == 1L
            val budget = if (hasBudget) reader.read(32) else null
            if (nights > MAX_NIGHTS) return Outcome.Failed(MISMATCH)
            val code = runCatching { CurrencyCode.of(currency) }.getOrNull()
                ?: return Outcome.Failed("The code names a currency that is not three letters.")
            val start = LocalDay.fromEpochDay(LocalDay.toEpochDay(DAY_ZERO) + startDay)
            Outcome.Ok(Plan(start, start.plusDays(nights.toInt()), code, budget))
        } catch (short: CodeTooShort) {
            Outcome.Failed(MISMATCH)
        }
    }

    // -- a day's ledger ----------------------------------------------------

    /**
     * Encodes one traveller's figures.
     *
     * The name is capped at nineteen characters because it is spoken back on the other
     * phone ("Priya's ledger"), and a name that will not fit in a line is a name that does
     * not belong in a code.
     */
    public fun encodeLedger(ledger: Ledger): String {
        require(ledger.entries.size <= MAX_ENTRIES) { "a ledger code carries at most $MAX_ENTRIES entries" }
        require(ledger.entries.isNotEmpty()) { "there is nothing to hand over yet" }
        val name = ledger.traveller.trim().take(MAX_NAME_CHARS)
        require(name.isNotEmpty()) { "a ledger needs a name on it" }
        val nameBytes = name.encodeToByteArray().take(MAX_NAME_CHARS)
        val writer = BitWriter()
        writer.write(nameBytes.size.toLong(), 5)
        nameBytes.forEach { writer.write(it.toLong() and 0xFF, 8) }
        writer.write(ledger.entries.size.toLong(), 8)
        ledger.entries.forEach { entry ->
            val day = LocalDay.toEpochDay(entry.day) - LocalDay.toEpochDay(DAY_ZERO)
            require(day in 0..MAX_START_DAY) { "a ledger entry is dated 2020 to 2064" }
            require(entry.amountMinor in Int.MIN_VALUE..Int.MAX_VALUE) {
                "an amount in a ledger code is under twenty crore in minor units"
            }
            require(entry.currency.code.length == 3) { "a currency is three letters" }
            writer.write(day, 14)
            writer.write(entry.amountMinor and 0xFFFFFFFFL, 32)
            entry.currency.code.forEach { letter -> writer.write((letter - 'A').toLong(), 5) }
            writer.write((entry.category?.let { Category.entries.indexOf(it) + 1 } ?: 0).toLong(), 3)
            writer.write(initialsBits(entry.merchantInitials), 10)
            writer.write(entryDigest(entry).toLong(), 8)
        }
        return format(LEDGER_PREFIX, writer.finish())
    }

    /** Reads a ledger code, with a reason when it cannot be read. */
    public fun decodeLedger(text: String): Outcome<Ledger> {
        val symbols = symbolsOf(text, LEDGER_PREFIX) ?: return Outcome.Failed(
            if (text.isBlank()) "Paste the line they sent you." else "A ledger code starts with AR2.",
        )
        unknownSymbol(text, LEDGER_PREFIX)?.let { bad ->
            return Outcome.Failed("'$bad' is not one of a code's characters.")
        }
        val payload = splitChecksum(symbols) ?: return Outcome.Failed(MISMATCH)
        return try {
            val reader = BitReader(payload)
            val nameLength = reader.read(5).toInt()
            if (nameLength == 0 || nameLength > MAX_NAME_CHARS) return Outcome.Failed(MISMATCH)
            val nameBytes = ByteArray(nameLength) { (reader.read(8) and 0xFF).toByte() }
            val name = nameBytes.decodeToString()
            if (name.isBlank()) return Outcome.Failed(MISMATCH)
            val count = reader.read(8).toInt()
            if (count == 0 || count > MAX_ENTRIES) return Outcome.Failed(MISMATCH)
            val entries = ArrayList<Entry>(count)
            repeat(count) {
                val day = reader.read(14)
                val amount = reader.read(32)
                val currency = buildString(3) { repeat(3) { append('A' + reader.read(5).toInt()) } }
                val categoryOrdinal = reader.read(3).toInt()
                val initials = initialsOf(reader.read(10).toInt())
                val digest = reader.read(8).toInt()
                val code = runCatching { CurrencyCode.of(currency) }.getOrNull()
                    ?: return Outcome.Failed("An entry names a currency that is not three letters.")
                val entry = Entry(
                    day = LocalDay.fromEpochDay(LocalDay.toEpochDay(DAY_ZERO) + day),
                    amountMinor = if (amount >= 0x80000000L) amount - 0x100000000L else amount,
                    currency = code,
                    category = if (categoryOrdinal == 0) null else {
                        Category.entries.getOrNull(categoryOrdinal - 1)
                    },
                    merchantInitials = initials,
                )
                if (entryDigest(entry) != digest) {
                    return Outcome.Failed("Line ${entries.size + 1} of the ledger is damaged.")
                }
                entries.add(entry)
            }
            Outcome.Ok(Ledger(name, entries))
        } catch (short: CodeTooShort) {
            Outcome.Failed("The line stops before its last entry — paste all of it.")
        }
    }

    // -- shared plumbing ---------------------------------------------------

    /**
     * A stable name for a code, so the same ledger pasted twice is recognised rather than
     * added twice.
     *
     * Content, not identity: it is not a security measure, and nothing in this codec is.
     */
    public fun fingerprint(text: String): String {
        val normalized = text.uppercase().filter { it.isLetterOrDigit() }
        var hash = 2166136261L
        normalized.forEach { char ->
            hash = hash xor (char.code.toLong() and 0xFF)
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        return hash.toString(16).padStart(8, '0')
    }

    /** The code, in groups of four, with its prefix in front. */
    private fun format(prefix: String, payload: IntArray): String {
        val checksum = checksumOf(payload)
        val full = payload + intArrayOf((checksum shr 5) and 0x1F, checksum and 0x1F)
        val text = full.joinToString("") { ALPHABET[it].toString() }
        return prefix + "-" + text.chunked(4).joinToString("-")
    }

    /** The symbols after the prefix, or null when the text is not this kind of code. */
    private fun symbolsOf(text: String, prefix: String): IntArray? {
        val normalized = text.uppercase().filter { it.isLetterOrDigit() }
        if (!normalized.startsWith(prefix)) return null
        val body = normalized.removePrefix(prefix)
        return IntArray(body.length) { index -> ALPHABET.indexOf(body[index]) }
    }

    /** The first character that is not in the alphabet, so the message can name it. */
    private fun unknownSymbol(text: String, prefix: String): Char? {
        val body = text.uppercase().filter { it.isLetterOrDigit() }.removePrefix(prefix)
        return body.firstOrNull { ALPHABET.indexOf(it) < 0 }
    }

    /** Splits the last two symbols off as the checksum, or null when it does not match. */
    private fun splitChecksum(symbols: IntArray): IntArray? {
        if (symbols.size < 3) return null
        val payload = symbols.copyOfRange(0, symbols.size - 2)
        val expected = checksumOf(payload)
        val actual = (symbols[symbols.size - 2] shl 5) or symbols[symbols.size - 1]
        return if (expected == actual) payload else null
    }

    private fun checksumOf(payload: IntArray): Int {
        var hash = 2166136261L
        payload.forEach { symbol ->
            hash = hash xor (symbol.toLong() and 0xFF)
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        return (hash and 0x3FFL).toInt()
    }

    /** Two initials, five bits each; zero for no merchant. */
    private fun initialsBits(initials: String?): Long {
        val letters = initials.orEmpty().uppercase().filter { it in 'A'..'Z' }.take(2)
        if (letters.isEmpty()) return 0
        var bits = 0L
        letters.forEach { bits = (bits shl 5) or ((it - 'A') + 1).toLong() }
        return if (letters.length == 1) bits shl 5 else bits
    }

    private fun initialsOf(bits: Int): String? {
        if (bits == 0) return null
        val first = (bits shr 5) and 0x1F
        val second = bits and 0x1F
        val letters = buildString {
            if (first in 1..26) append('A' + (first - 1))
            if (second in 1..26) append('A' + (second - 1))
        }
        return letters.ifEmpty { null }
    }

    /** The per-entry digest that makes a damaged line say so. */
    private fun entryDigest(entry: Entry): Int {
        var hash = 2166136261L
        fun mix(value: Long) {
            hash = hash xor (value and 0xFFFFFFFFL)
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        mix(LocalDay.toEpochDay(entry.day))
        mix(entry.amountMinor)
        entry.currency.code.forEach { letter -> mix(letter.code.toLong()) }
        return (hash and 0xFFL).toInt()
    }

    /** Writes values most-significant bit first into five-bit symbols. */
    private class BitWriter {
        private val symbols = ArrayList<Int>()
        private var accumulator = 0
        private var bits = 0

        fun write(value: Long, width: Int) {
            var remaining = width
            while (remaining > 0) {
                val take = minOf(remaining, 5 - bits)
                val shift = remaining - take
                val mask = (1L shl take) - 1L
                val chunk = ((value shr shift) and mask).toInt()
                accumulator = (accumulator shl take) or chunk
                bits += take
                remaining -= take
                if (bits == 5) {
                    symbols.add(accumulator)
                    accumulator = 0
                    bits = 0
                }
            }
        }

        fun finish(): IntArray {
            if (bits > 0) {
                symbols.add(accumulator shl (5 - bits))
                accumulator = 0
                bits = 0
            }
            return symbols.toIntArray()
        }
    }

    /** Reads values most-significant bit first out of five-bit symbols. */
    private class BitReader(private val symbols: IntArray) {
        private var at = 0
        private var current = 0
        private var bits = 0

        fun read(width: Int): Long {
            var result = 0L
            var remaining = width
            while (remaining > 0) {
                if (bits == 0) {
                    if (at >= symbols.size) throw CodeTooShort()
                    current = symbols[at++]
                    bits = 5
                }
                val take = minOf(remaining, bits)
                val shift = bits - take
                val chunk = (current shr shift) and ((1 shl take) - 1)
                result = (result shl take) or chunk.toLong()
                bits -= take
                remaining -= take
            }
            return result
        }
    }

    /** A code that ran out of symbols where a value was promised. */
    private class CodeTooShort : Exception("the code ended early")
}
