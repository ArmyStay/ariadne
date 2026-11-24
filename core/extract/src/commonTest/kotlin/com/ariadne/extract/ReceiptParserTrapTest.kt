package com.ariadne.extract

import com.ariadne.model.CurrencyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Trap-named parser fixtures (parser half, runnable without OCR engines/devices).
 * Raw OCR text dumps for these cases live in `commonTest/resources/fixtures/text/`.
 */
class ReceiptParserTrapTest {
    private val parser = ReceiptParser(fallbackCurrency = CurrencyCode.of("INR"))

    private data class TextFixture(
        val id: String,
        val raw: String,
        val amountMinor: Long?,
        val merchant: String?,
        val currency: CurrencyCode?,
    )

    private fun fixture(id: String): TextFixture = when (id) {
        "gstin-and-phone" -> TextFixture(
            id = id,
            raw = """
                SHREE GANESH CHAI
                Shop 4, MG Road
                GSTIN 27AAACB1234C1Z5
                Phone 9876543210
                2 x Masala Chai      40.00
                1 x Bun Butter       25.00
                Subtotal             65.00
                GST 5%               3.25
                Grand Total       ₹ 68.25
            """.trimIndent(),
            amountMinor = 6825L,
            merchant = "SHREE GANESH CHAI",
            currency = CurrencyCode.of("INR"),
        )
        "detached-decimal" -> TextFixture(
            id = id,
            raw = """
                ANAND FOOTWEAR
                Total
                1,200
                .
                00
                Cash 1200.00
            """.trimIndent(),
            amountMinor = 120000L, // parser should still recover amount from available number lines
            merchant = "ANAND FOOTWEAR",
            currency = CurrencyCode.of("INR"),
        )
        "total-beats-tax-and-change" -> TextFixture(
            id = id,
            raw = """
                CAFE CENTRAL
                Americano  180.00
                Tax         18.00
                Service      0.00
                Change     200.00
                TOTAL      198.00
            """.trimIndent(),
            amountMinor = 19800L,
            merchant = "CAFE CENTRAL",
            currency = CurrencyCode.of("INR"),
        )
        "european-comma-decimal" -> TextFixture(
            id = id,
            raw = """
                MUSEU DA CIDADE
                Entrada adulto   €12,50
                TOTAL            €12,50
            """.trimIndent(),
            amountMinor = 1250L,
            merchant = "MUSEU DA CIDADE",
            currency = CurrencyCode.of("EUR"),
        )
        "merchant-only-no-amount" -> TextFixture(
            id = id,
            raw = """
                PHUKET BOAT TOURS
                Banyan Tree Jetty
                Ask at counter
            """.trimIndent(),
            amountMinor = null,
            merchant = null,
            currency = null,
        )
        else -> error("unknown fixture: $id")
    }

    private fun ReceiptParser.parse(raw: String): ExtractionOutcome = parse(raw.lines())

    @Test
    fun `amount survives a GSTIN and phone-number trap`() {
        val f = fixture("gstin-and-phone")
        val out = parser.parse(f.raw)
        assertEquals(f.amountMinor, out.amountMinorOrNull)
        assertEquals(f.currency?.code, out.currencyOrNull?.code)
        val merchant = out.merchantOrNull
        assertNotNull(merchant)
        assertTrue(merchant.contains("GANESH", ignoreCase = true), "merchant was '$merchant'")
    }

    @Test
    fun `amount survives a detached decimal line split`() {
        val f = fixture("detached-decimal")
        val out = parser.parse(f.raw)
        // OCR often splits grouped/decimal fragments across lines; parser must not pick noise and should recover a usable total.
        assertEquals(f.amountMinor, out.amountMinorOrNull)
        assertEquals(f.currency?.code, out.currencyOrNull?.code)
    }

    @Test
    fun `total line wins over tax service and change traps`() {
        val f = fixture("total-beats-tax-and-change")
        val out = parser.parse(f.raw)
        assertEquals(f.amountMinor, out.amountMinorOrNull)
    }

    @Test
    fun `european comma decimal and currency symbol parse correctly`() {
        val f = fixture("european-comma-decimal")
        val out = parser.parse(f.raw)
        assertEquals(f.amountMinor, out.amountMinorOrNull)
        assertEquals("EUR", out.currencyOrNull?.code)
    }

    @Test
    fun `merchant-only OCR text stays blank instead of guessed`() {
        val f = fixture("merchant-only-no-amount")
        val out = parser.parse(f.raw)
        assertIs<ExtractionOutcome.Nothing>(out)
        assertEquals(null, out.amountMinorOrNull)
    }
}