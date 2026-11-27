package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpokenExpenseTest {

    private val eur = CurrencyCode.of("EUR")

    private fun minor(text: String, currency: CurrencyCode? = eur): Long? =
        SpokenExpense.parse(text, currency)?.minor

    @Test
    fun digitsWithADot() {
        assertEquals(1250L, minor("12.50"))
        assertEquals(500L, minor("5"))
        assertEquals(500L, minor("€5"))
    }

    @Test
    fun twoNumberWordsAreWholeAndCents() {
        assertEquals(1250L, minor("twelve fifty"))
        assertEquals(550L, minor("five fifty"))
        assertEquals(2350L, minor("twenty three fifty"))
    }

    @Test
    fun combinedWordsAreOneNumber() {
        assertEquals(2300L, minor("twenty three"))
        assertEquals(12000L, minor("one hundred twenty"))
        assertEquals(12300L, minor("one hundred twenty three"))
    }

    @Test
    fun theCurrencyIsTheOneSaid() {
        assertEquals("INR", SpokenExpense.parse("450 rupees", eur)?.currency?.code)
        assertEquals("USD", SpokenExpense.parse("twelve dollars fifty", eur)?.currency?.code)
        assertEquals("EUR", SpokenExpense.parse("twelve fifty", eur)?.currency?.code)
    }

    @Test
    fun aCurrencyWordIsNotAMerchant() {
        val said = SpokenExpense.parse("five euros fifty", eur)
        assertEquals(550L, said?.minor)
        assertEquals("", said?.remainder)
    }

    @Test
    fun theOtherWordsAreTheMerchant() {
        val said = SpokenExpense.parse("coffee twelve fifty", eur)
        assertEquals(1250L, said?.minor)
        assertEquals("coffee", said?.remainder)
    }

    @Test
    fun aCurrencyWithNoMinorUnitKeepsWholeNumbers() {
        val jpy = CurrencyCode.of("JPY")
        assertEquals(12L, SpokenExpense.parse("twelve fifty", jpy)?.minor)
        assertEquals(12L, SpokenExpense.parse("twelve", jpy)?.minor)
    }

    @Test
    fun nothingSaidIsNothing() {
        assertNull(SpokenExpense.parse("", eur))
        assertNull(SpokenExpense.parse("nice coffee", eur))
        assertNull(SpokenExpense.parse("zero", eur))
    }
}
