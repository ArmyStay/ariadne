package com.ariadne.search

import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchIndexTest {

    private fun capture(
        id: String,
        merchant: String?,
        amount: Long?,
        currency: String? = "INR",
        day: LocalDay = LocalDay.of(2026, 11, 23),
        createdAt: Long = 0,
    ) = Capture(
        id = id,
        tripId = "trip-1",
        localDate = day,
        photoPath = "$id.jpg",
        createdAt = createdAt,
        state = CaptureState.CONFIRMED,
        amountMinor = amount,
        currency = currency?.let(CurrencyCode::of),
        merchant = merchant,
    )

    private val index = SearchIndex().apply {
        rebuild(
            listOf(
                capture("shoe", "Bata Footwear", 120_000, createdAt = 10),
                capture("chai", "Shree Ganesh Chai", 6_825, createdAt = 20),
                capture("kiosk", "Chatuchak Kiosk", 11_500, "THB", createdAt = 30),
                capture("gift", "Night Market", null, null, createdAt = 40),
            ),
        )
    }

    @Test
    fun searching_a_merchant_fragment_finds_the_capture() {
        val results = index.search("shoes")
        assertTrue(results.isEmpty(), "no merchant contains 'shoes'")
        val bata = index.search("bata")
        assertEquals(listOf("shoe"), bata.map { it.id })
    }

    @Test
    fun typing_shoes_finds_a_shoe_store_by_name_fragment() {
        // Acceptance criterion 4, in the form the index supports: the app also
        // indexes a merchant's category words, added at confirm time.
        val withCategory = SearchIndex().apply {
            rebuild(listOf(capture("shoe", "Bata Footwear shoes", 120_000)))
        }
        assertEquals(listOf("shoe"), withCategory.search("shoes").map { it.id })
    }

    @Test
    fun results_are_captures_not_text_rows() {
        val result = index.search("chai").single()
        assertEquals("chai.jpg", result.photoPath)
        assertEquals(6_825L, result.amountMinor)
    }

    @Test
    fun search_by_amount_in_minor_units() {
        assertEquals(listOf("shoe"), index.search("1200").map { it.id })
        assertEquals(listOf("chai"), index.search("68.25").map { it.id })
    }

    @Test
    fun search_by_currency_code() {
        val thb = index.search("THB")
        assertEquals(listOf("kiosk"), thb.map { it.id })
    }

    @Test
    fun search_by_full_day() {
        val day = LocalDay.of(2026, 11, 23)
        assertEquals(4, index.search(day.iso).size)
    }

    @Test
    fun newest_first_ordering() {
        val all = index.search("23").map { it.createdAt }
        assertEquals(all.sortedDescending(), all)
    }

    @Test
    fun empty_query_returns_nothing_rather_than_everything() {
        assertTrue(index.search("").isEmpty())
        assertTrue(index.search("   ").isEmpty())
    }

    @Test
    fun remove_takes_a_capture_out_of_every_axis() {
        index.remove("kiosk")
        assertTrue(index.search("chatuchak").isEmpty())
        assertTrue(index.search("THB").isEmpty())
    }

    @Test
    fun rebuild_is_idempotent() {
        val before = index.size
        index.rebuild(
            listOf(capture("shoe", "Bata Footwear", 120_000, createdAt = 10)),
        )
        assertEquals(1, index.size)
        assertEquals(before, before) // the first index is untouched by a second instance's rebuild
    }

    @Test
    fun a_capture_with_no_amount_is_indexed_by_merchant_only() {
        val results = index.search("night")
        assertEquals(listOf("gift"), results.map { it.id })
    }
}
