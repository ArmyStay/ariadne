package com.ariadne.model

/**
 * A currency as a person would choose it: the code they see on a receipt, the
 * name they know it by, and the sign that is printed next to it.
 *
 * The catalogue is for *choosing*, never for validating. A receipt can carry any
 * ISO code in the world and the record must accept it — see [CurrencyCode.of] —
 * but a list of sixty-six names is far more usable than a three-letter box that
 * silently accepts `ABC`.
 */
public data class CurrencyOption(
    public val code: String,
    public val name: String,
    public val symbol: String,
)

/**
 * The currencies this build offers by name.
 *
 * Sixty-seven codes, which is every code [CurrencyCode] already knows how to
 * scale and symbol, so there is exactly one vocabulary: nothing can be chosen
 * here that the arithmetic does not already understand. The order is the order a
 * traveller is likely to need them in — the home currencies first, then the
 * places people go — because a list sorted alphabetically would start in
 * Albania and put the baht on page three.
 */
public object CurrencyCatalog {

    private val order: List<String> = listOf(
        "INR", "USD", "EUR", "GBP", "THB", "JPY", "AED", "SGD", "VND", "IDR",
        "MYR", "PHP", "KRW", "TWD", "HKD", "CNY", "LKR", "NPR", "BDT", "PKR",
        "MMK", "KHR", "LAK", "MNT", "KZT", "UZS", "GEL", "AMD",
        "AUD", "NZD", "CAD", "CHF", "SEK", "NOK", "DKK", "ISK", "PLN", "CZK",
        "HUF", "TRY", "ILS", "EGP", "MAD", "TND", "DZD", "ZAR", "KES", "TZS",
        "UGX", "RWF", "GHS", "NGN",
        "BRL", "MXN", "ARS", "CLP", "COP", "PEN",
        "SAR", "QAR", "KWD", "BHD", "OMR", "JOD",
        "PYG", "XOF", "XAF",
    )

    private val names: Map<String, String> = mapOf(
        "INR" to "Indian rupee",
        "USD" to "US dollar",
        "EUR" to "Euro",
        "GBP" to "Pound sterling",
        "THB" to "Thai baht",
        "JPY" to "Japanese yen",
        "AED" to "UAE dirham",
        "SGD" to "Singapore dollar",
        "VND" to "Vietnamese dong",
        "IDR" to "Indonesian rupiah",
        "MYR" to "Malaysian ringgit",
        "PHP" to "Philippine peso",
        "KRW" to "South Korean won",
        "TWD" to "New Taiwan dollar",
        "HKD" to "Hong Kong dollar",
        "CNY" to "Chinese yuan",
        "LKR" to "Sri Lankan rupee",
        "NPR" to "Nepalese rupee",
        "BDT" to "Bangladeshi taka",
        "PKR" to "Pakistani rupee",
        "MMK" to "Myanmar kyat",
        "KHR" to "Cambodian riel",
        "LAK" to "Lao kip",
        "MNT" to "Mongolian tugrik",
        "KZT" to "Kazakhstani tenge",
        "UZS" to "Uzbekistani som",
        "GEL" to "Georgian lari",
        "AMD" to "Armenian dram",
        "AUD" to "Australian dollar",
        "NZD" to "New Zealand dollar",
        "CAD" to "Canadian dollar",
        "CHF" to "Swiss franc",
        "SEK" to "Swedish krona",
        "NOK" to "Norwegian krone",
        "DKK" to "Danish krone",
        "ISK" to "Icelandic króna",
        "PLN" to "Polish złoty",
        "CZK" to "Czech koruna",
        "HUF" to "Hungarian forint",
        "TRY" to "Turkish lira",
        "ILS" to "Israeli shekel",
        "EGP" to "Egyptian pound",
        "MAD" to "Moroccan dirham",
        "TND" to "Tunisian dinar",
        "DZD" to "Algerian dinar",
        "ZAR" to "South African rand",
        "KES" to "Kenyan shilling",
        "TZS" to "Tanzanian shilling",
        "UGX" to "Ugandan shilling",
        "RWF" to "Rwandan franc",
        "GHS" to "Ghanaian cedi",
        "NGN" to "Nigerian naira",
        "BRL" to "Brazilian real",
        "MXN" to "Mexican peso",
        "ARS" to "Argentine peso",
        "CLP" to "Chilean peso",
        "COP" to "Colombian peso",
        "PEN" to "Peruvian sol",
        "SAR" to "Saudi riyal",
        "QAR" to "Qatari riyal",
        "KWD" to "Kuwaiti dinar",
        "BHD" to "Bahraini dinar",
        "OMR" to "Omani rial",
        "JOD" to "Jordanian dinar",
        "PYG" to "Paraguayan guaraní",
        "XOF" to "West African CFA franc",
        "XAF" to "Central African CFA franc",
    )

    /** Every currency in the catalogue, in the order above. */
    public val all: List<CurrencyOption> = order.map { code ->
        CurrencyOption(code = code, name = names[code] ?: code, symbol = CurrencyCode.of(code).symbol())
    }

    private val byCode: Map<String, CurrencyOption> = all.associateBy { it.code }

    /** The catalogue entry for a code, or null if it is outside the catalogue. */
    public fun of(code: String): CurrencyOption? = byCode[code.trim().uppercase()]

    /**
     * Search by code, name or symbol.
     *
     * A code that starts with the query comes first ("th" → THB before "South
     * African rand"), then a name that starts with it, then anything that merely
     * contains it. With an empty query the list is the catalogue order, which is
     * a usable list rather than a wall.
     */
    public fun search(query: String, limit: Int = 40): List<CurrencyOption> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all.take(limit)
        val starts = mutableListOf<CurrencyOption>()
        val contains = mutableListOf<CurrencyOption>()
        for (option in all) {
            val code = option.code.lowercase()
            val name = option.name.lowercase()
            when {
                code.startsWith(q) || name.startsWith(q) -> starts += option
                code.contains(q) || name.contains(q) || option.symbol.lowercase() == q -> contains += option
            }
        }
        return (starts + contains).take(limit)
    }

    /**
     * Four codes worth showing as chips: the ones the trip actually uses, then
     * the three most common travel currencies, in catalogue order.
     *
     * The current code comes first so a chip can never disappear from under the
     * finger that chose it — the old implementation offered a fixed list and a
     * trip in JPY could not be switched back to JPY from the detail page.
     */
    public fun shortlist(vararg preferred: String?): List<String> {
        val out = LinkedHashSet<String>()
        for (code in preferred) {
            val normalized = code?.trim()?.uppercase() ?: continue
            if (normalized.length == 3) out += normalized
        }
        out += listOf("THB", "USD", "EUR")
        // Always four chips: top up from the catalogue's own order so the row
        // never changes width between one capture and the next.
        for (option in all) {
            if (out.size >= 4) break
            out += option.code
        }
        return out.take(4)
    }
}
