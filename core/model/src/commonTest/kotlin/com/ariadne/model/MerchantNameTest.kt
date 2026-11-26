package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The card that was titled "ction View Go Run Terminal Help".
 *
 * That line came from a screenshot of a code editor's menu bar, was the first line
 * with letters on it, and was accepted as a merchant name. These tests are the
 * specification for what a name has to look like to be printed as one.
 */
class MerchantNameTest {

    @Test
    fun a_menu_bar_is_not_a_merchant() {
        assertTrue(MerchantName.isUiChrome("ction View Go Run Terminal Help"))
        assertTrue(MerchantName.isUiChrome("File Edit View Window Help"))
        assertTrue(MerchantName.isUiChrome("View  Run  Terminal"))
        assertNull(MerchantName.clean("ction View Go Run Terminal Help"))
    }

    @Test
    fun a_real_shop_name_survives_even_when_one_word_looks_like_a_menu() {
        // "View" alone is a coincidence, not a screen: this is a restaurant.
        assertFalse(MerchantName.isUiChrome("Food View Restaurant"))
        assertEquals("Food View Restaurant", MerchantName.clean("Food View Restaurant"))
        assertEquals("Terminal 21 Shopping Mall", MerchantName.clean("Terminal 21 Shopping Mall"))
        assertEquals("Chatuchak Market", MerchantName.clean("Chatuchak Market"))
        assertEquals("7-Eleven", MerchantName.clean("7-Eleven"))
        assertEquals("Café Amazon", MerchantName.clean("Café Amazon"))
    }

    @Test
    fun a_receipt_word_alone_is_not_a_name() {
        assertNull(MerchantName.clean("RECEIPT"))
        assertNull(MerchantName.clean("Receipt"))
        assertNull(MerchantName.clean("TAX INVOICE"))
        assertNull(MerchantName.clean("Untitled"))
        assertNull(MerchantName.clean("  "))
        assertNull(MerchantName.clean(null))
    }

    @Test
    fun a_receipt_number_is_not_a_name() {
        assertNull(MerchantName.clean("45 12 9981"))
        assertNull(MerchantName.clean("#2026-0012"))
        assertNull(MerchantName.clean("4901234567894"))
    }

    @Test
    fun a_leading_bullet_or_bar_is_stripped_rather_than_filed() {
        assertEquals("Blue Bottle Coffee", MerchantName.clean("* Blue Bottle Coffee"))
        assertEquals("Blue Bottle Coffee", MerchantName.clean("| Blue Bottle Coffee"))
    }

    @Test
    fun a_line_too_long_to_be_a_shop_name_is_refused_rather_than_cut() {
        // Refused, not truncated: half a name is not a name, and "Receipt · 21:09"
        // says more than the first 48 characters of a paragraph.
        assertNull(MerchantName.clean("The Extremely Long Name Of A Market Stall Near The River In Bangkok"))
        // A long-but-plausible name is kept whole.
        assertEquals(
            "Chatuchak Weekend Market, Section 7",
            MerchantName.clean("Chatuchak Weekend Market, Section 7"),
        )
    }

    @Test
    fun a_title_falls_back_to_the_receipt_and_its_time_never_to_raw_ocr() {
        assertEquals("Chatuchak Market", MerchantName.receiptTitle("Chatuchak Market", "21:09"))
        assertEquals("Receipt · 21:09", MerchantName.receiptTitle(null, "21:09"))
        assertEquals("Receipt · 21:09", MerchantName.receiptTitle("View Go Run Terminal Help", "21:09"))
        assertEquals("Receipt · 21:09", MerchantName.receiptTitle("RECEIPT", "21:09"))
        assertEquals("Receipt", MerchantName.receiptTitle(null, null))
    }

    @Test
    fun a_name_that_is_already_clean_is_returned_unchanged() {
        assertEquals("Shree Ganesh Chai", MerchantName.receiptTitle("  Shree   Ganesh Chai  ", "08:15"))
    }
}
