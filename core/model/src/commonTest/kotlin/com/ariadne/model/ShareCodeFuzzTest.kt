package com.ariadne.model

import com.ariadne.model.ShareCodes.Outcome
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Seeded, reproducible fuzz/property coverage, shared by JVM, iOS and Wasm tests. */
class ShareCodeFuzzTest {
    private val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val zero = LocalDay.of(2020, 1, 1)
    private val currency = CurrencyCode.of("INR")

    @Test
    fun arbitrary_utf16_and_separators_never_throw() {
        val random = Random(0xA21AD)
        val corpus = listOf("", "AR2", "AR2-", "AR1", "ar2\u0000", "AR2-💰", "AR2-\uD800", "AR2-\uDC00",
            "AR2-" + "- \n\t".repeat(2000), "AR2-" + "ß".repeat(2000))
        corpus.forEach(::checkDecoders)
        repeat(4000) {
            val text = buildString {
                append(listOf("", "AR1-", "AR2-")[random.nextInt(3)])
                repeat(random.nextInt(512)) { append(random.nextInt(65536).toChar()) }
            }
            checkDecoders(text)
        }
    }

    @Test
    fun valid_checksums_do_not_bypass_parser_bounds() {
        // Random garbage alone mostly stops at the prefix/checksum. Recompute the
        // public checksum so these inputs reach name length, count and entry decoding.
        val random = Random(0xC0DE)
        repeat(4000) {
            val payload = IntArray(random.nextInt(1, 350)) { random.nextInt(32) }
            checkDecoders(withChecksum("AR2", payload))
            checkDecoders(withChecksum("AR1", IntArray(14) { random.nextInt(32) }))
        }
    }

    @Test
    fun valid_ledgers_round_trip_and_mutations_are_safe() {
        val random = Random(0xF022)
        repeat(250) {
            val ledger = ShareCodes.Ledger("Traveller", List(random.nextInt(1, 201)) {
                ShareCodes.Entry(
                    day = zero.plusDays(random.nextInt(16384)),
                    amountMinor = random.nextInt().toLong(),
                    currency = currency,
                    category = if (random.nextBoolean()) null else Category.entries.random(random),
                    merchantInitials = listOf(null, "A", "NP").random(random),
                )
            })
            val encoded = ShareCodes.encodeLedger(ledger)
            assertEquals(Outcome.Ok(ledger), ShareCodes.decodeLedger(encoded))
            assertEquals(Outcome.Ok(ledger), ShareCodes.decodeLedger(encoded.lowercase().replace("-", " \n")))
            val payload = encoded.drop(4).filter { it != '-' }.dropLast(2).map(alphabet::indexOf).toIntArray()
            repeat(8) {
                val mutant = payload.copyOf()
                mutant[random.nextInt(mutant.size)] = random.nextInt(32)
                // A malicious sender can repair a checksum; this is not authentication.
                checkDecoders(withChecksum("AR2", mutant))
                checkDecoders(encoded.take(random.nextInt(encoded.length)))
            }
        }
    }

    @Test
    fun maximum_ledger_fits_but_giant_pastes_are_rejected_before_normalizing() {
        val ledger = ShareCodes.Ledger("N".repeat(19), List(200) {
            ShareCodes.Entry(zero.plusDays(16383), Int.MAX_VALUE.toLong(), currency)
        })
        assertEquals(Outcome.Ok(ledger), ShareCodes.decodeLedger(ShareCodes.encodeLedger(ledger)))
        for (prefix in listOf("AR1-", "AR2-")) {
            for (body in listOf("2", "-", "ß", "\uD800")) {
                val text = prefix + body.repeat(100_000)
                assertTrue(ShareCodes.decodeTrip(text) is Outcome.Failed)
                assertTrue(ShareCodes.decodeLedger(text) is Outcome.Failed)
            }
        }
    }

    private fun checkDecoders(text: String) {
        when (val result = ShareCodes.decodeLedger(text)) {
            is Outcome.Failed -> assertTrue(result.reason.isNotBlank())
            is Outcome.Ok -> {
                assertTrue(result.value.traveller.isNotBlank())
                assertTrue(result.value.traveller.length <= 19)
                assertTrue(result.value.entries.size in 1..200)
                result.value.entries.forEach { entry ->
                    assertTrue(entry.day >= zero && entry.day <= zero.plusDays(16383))
                    assertTrue(entry.amountMinor in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
                    assertTrue(entry.currency.code.matches(Regex("[A-Z]{3}")))
                }
            }
        }
        when (val result = ShareCodes.decodeTrip(text)) {
            is Outcome.Failed -> assertTrue(result.reason.isNotBlank())
            is Outcome.Ok -> {
                val plan = result.value
                assertTrue(plan.start >= zero && plan.start <= zero.plusDays(16383))
                assertTrue(plan.end >= plan.start && plan.end <= plan.start.plusDays(255))
                assertTrue(plan.budgetMinor == null || plan.budgetMinor in 0L..0xFFFFFFFFL)
            }
        }
    }

    private fun withChecksum(prefix: String, payload: IntArray): String {
        var hash = 2166136261L
        payload.forEach { hash = ((hash xor it.toLong()) * 16777619L) and 0xFFFFFFFFL }
        val checksum = (hash and 1023).toInt()
        return prefix + "-" + (payload + intArrayOf(checksum shr 5, checksum and 31))
            .joinToString("") { alphabet[it].toString() }
    }
}
