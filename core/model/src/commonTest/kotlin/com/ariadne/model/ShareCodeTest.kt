package com.ariadne.model

import com.ariadne.model.ShareCodes.Outcome
import kotlin.test.Test

class ShareCodeTest {

    private val start = LocalDay.of(2026, 12, 3)

    private fun plan(budget: Long? = 4_000_000L) = ShareCodes.Plan(
        start = start,
        end = start.plusDays(5),
        baseCurrency = CurrencyCode.of("INR"),
        budgetMinor = budget,
    )

    // -- the trip code -----------------------------------------------------

    @Test
    fun a_trip_code_round_trips() {
        val code = ShareCodes.encodeTrip(plan())
        val decoded = ShareCodes.decodeTrip(code)
        check(decoded is Outcome.Ok) { "expected a plan, got $decoded" }
        val value = (decoded as Outcome.Ok).value
        check(value.start == start)
        check(value.end == start.plusDays(5))
        check(value.baseCurrency == CurrencyCode.of("INR"))
        check(value.budgetMinor == 4_000_000L)
    }

    @Test
    fun a_trip_without_a_budget_round_trips() {
        val decoded = ShareCodes.decodeTrip(ShareCodes.encodeTrip(plan(budget = null)))
        check(decoded is Outcome.Ok)
        check((decoded as Outcome.Ok).value.budgetMinor == null)
    }

    @Test
    fun a_code_is_sixteen_symbols_after_its_prefix() {
        val code = ShareCodes.encodeTrip(plan())
        check(code.startsWith("AR1-")) { code }
        val symbols = code.removePrefix("AR1").filter { it.isLetterOrDigit() }
        check(symbols.length == ShareCodes.TRIP_LENGTH) { "was ${symbols.length}: $code" }
    }

    @Test
    fun a_code_avoids_the_four_confusable_characters() {
        // Every trip in a decade of dates, so this is not one lucky code.
        //
        // The prefix is exempt and deliberately so: `AR1` and `AR2` are version
        // numerals, printed as such and never mistyped for anything else. The
        // payload — the part that is read aloud and typed back — uses none of them.
        var day = LocalDay.of(2020, 1, 1)
        repeat(400) {
            val code = ShareCodes.encodeTrip(
                ShareCodes.Plan(day, day.plusDays(it % 30), CurrencyCode.of("THB"), null),
            )
            check(code.startsWith("AR1-")) { code }
            val payload = code.removePrefix("AR1").filter { it.isLetterOrDigit() }
            check(payload.none { it == '0' || it == '1' || it == 'I' || it == 'O' }) { code }
            day = day.plusDays(9)
        }
    }

    @Test
    fun a_ledger_avoids_the_same_four_characters_in_its_payload() {
        val code = ShareCodes.encodeLedger(ledger())
        val payload = code.removePrefix("AR2").filter { it.isLetterOrDigit() }
        check(payload.none { it == '0' || it == '1' || it == 'I' || it == 'O' }) { code }
    }

    @Test
    fun case_spaces_and_hyphens_do_not_matter() {
        val code = ShareCodes.encodeTrip(plan())
        val readBack = code.lowercase().replace("-", " ")
        check(ShareCodes.decodeTrip(readBack) is Outcome.Ok)
    }

    @Test
    fun one_wrong_character_is_caught() {
        val code = ShareCodes.encodeTrip(plan())
        // Change one symbol to a different one that is still in the alphabet.
        val index = code.lastIndex
        val original = code[index]
        val replacement = if (original == 'X') 'Y' else 'X'
        val wrong = code.substring(0, index) + replacement
        val outcome = ShareCodes.decodeTrip(wrong)
        check(outcome is Outcome.Failed) { "a mistyped code was accepted" }
    }

    @Test
    fun a_short_code_says_how_long_it_is() {
        val outcome = ShareCodes.decodeTrip("AR1-K7QM")
        check(outcome is Outcome.Failed)
        check((outcome as Outcome.Failed).reason.contains("sixteen"))
    }

    @Test
    fun text_that_is_not_a_code_says_what_a_code_starts_with() {
        val outcome = ShareCodes.decodeTrip("dinner with Priya")
        check(outcome is Outcome.Failed)
        check((outcome as Outcome.Failed).reason.contains("AR1"))
    }

    @Test
    fun a_budget_too_large_for_a_code_is_left_out_rather_than_clamped() {
        val huge = 5_000_000_000L // fifty crore rupees in minor units
        val decoded = ShareCodes.decodeTrip(ShareCodes.encodeTrip(plan(budget = huge)))
        check(decoded is Outcome.Ok)
        check((decoded as Outcome.Ok).value.budgetMinor == null) {
            "a budget that does not fit must arrive as no budget, not as a different one"
        }
    }

    // -- the ledger code ---------------------------------------------------

    private fun ledger() = ShareCodes.Ledger(
        traveller = "Priya",
        entries = listOf(
            ShareCodes.Entry(
                day = start,
                amountMinor = 412_000L,
                currency = CurrencyCode.of("INR"),
                category = Category.Food,
                merchantInitials = "NP",
            ),
            ShareCodes.Entry(
                day = start,
                amountMinor = 2_555L,
                currency = CurrencyCode.of("THB"),
                category = null,
                merchantInitials = null,
            ),
            ShareCodes.Entry(
                day = start.plusDays(1),
                amountMinor = -9_900L, // a refund at the counter
                currency = CurrencyCode.of("INR"),
                category = Category.Shopping,
                merchantInitials = "A",
            ),
        ),
    )

    @Test
    fun a_ledger_round_trips() {
        val code = ShareCodes.encodeLedger(ledger())
        val decoded = ShareCodes.decodeLedger(code)
        check(decoded is Outcome.Ok) { "expected a ledger, got $decoded" }
        val value = (decoded as Outcome.Ok).value
        check(value.traveller == "Priya")
        check(value.entries.size == 3)
        check(value.entries[0].amountMinor == 412_000L)
        check(value.entries[0].currency == CurrencyCode.of("INR"))
        check(value.entries[0].category == Category.Food)
        check(value.entries[0].merchantInitials == "NP")
        check(value.entries[1].currency == CurrencyCode.of("THB"))
        check(value.entries[1].category == null)
        check(value.entries[2].amountMinor == -9_900L) { "a refund must survive the round trip" }
        check(value.entries[2].merchantInitials == "A")
    }

    @Test
    fun a_ledger_names_its_days() {
        val decoded = ShareCodes.decodeLedger(ShareCodes.encodeLedger(ledger()))
        check(decoded is Outcome.Ok)
        check((decoded as Outcome.Ok).value.days == listOf(start, start.plusDays(1)))
    }

    @Test
    fun a_damaged_ledger_is_refused() {
        val code = ShareCodes.encodeLedger(ledger())
        // Flip a symbol in the middle of the entry block.
        val body = code.drop(ShareCodes.LEDGER_PREFIX.length + 1).filter { it != '-' }
        val position = body.length / 2
        val replacement = if (body[position] == 'X') 'Y' else 'X'
        val damaged = ShareCodes.LEDGER_PREFIX + "-" +
            (body.substring(0, position) + replacement + body.substring(position + 1))
        val outcome = ShareCodes.decodeLedger(damaged)
        check(outcome is Outcome.Failed) { "a damaged ledger was accepted" }
    }

    @Test
    fun a_ledger_with_no_entries_refuses_to_encode() {
        val empty = runCatching { ShareCodes.encodeLedger(ShareCodes.Ledger("Priya", emptyList())) }
        check(empty.isFailure)
    }

    @Test
    fun a_fingerprint_ignores_case_and_separators() {
        val code = ShareCodes.encodeLedger(ledger())
        check(ShareCodes.fingerprint(code) == ShareCodes.fingerprint(code.lowercase().replace("-", " ")))
        check(ShareCodes.fingerprint(code) != ShareCodes.fingerprint(ShareCodes.encodeLedger(ledger().copy(traveller = "Sam"))))
    }
}
