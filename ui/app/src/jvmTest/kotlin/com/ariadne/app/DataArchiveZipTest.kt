package com.ariadne.app

import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Settings
import com.ariadne.model.Trip
import com.ariadne.model.Traveller
import com.ariadne.model.TravellerEntry
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataArchiveZipTest {
    @Test
    fun writesReadableZipEntriesWithUtf8NamesAndContents() {
        val archive = DataArchiveZip.encode(
            listOf(
                ArchiveFile("data/captures.csv", "id,merchant\n1,\"Café\"".encodeToByteArray()),
                ArchiveFile("photos/trip/receipt.jpg", byteArrayOf(0, 1, 2, 3)),
            ),
        )

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }

        assertEquals("id,merchant\n1,\"Café\"", assertNotNull(entries["data/captures.csv"]).decodeToString())
        assertEquals(listOf<Byte>(0, 1, 2, 3), assertNotNull(entries["photos/trip/receipt.jpg"]).toList())
    }

    @Test
    fun exportsDaysAndConfirmedManualEntriesWithoutInventingPhotos() {
        val date = LocalDay.of(2026, 11, 27)
        val currency = CurrencyCode.of("INR")
        val trip = Trip("trip-1", "Delhi", date, date, currency, currency)
        val day = DayCard("day-1", trip.id, date, closedAt = 123L)
        val manualEntry = Capture(
            id = "capture-1",
            tripId = trip.id,
            localDate = date,
            photoPath = "",
            createdAt = 456L,
            state = CaptureState.CONFIRMED,
            amountMinor = 1250L,
            currency = currency,
            merchant = "Chai & Co",
            confirmedAt = 789L,
        )

        val manifest = DataArchiveZip.manifest(
            Settings(),
            listOf(trip),
            listOf(day),
            listOf(manualEntry),
            emptyList(),
            emptyMap(),
        )

        assertTrue(manifest.contains("\"date\":\"2026-11-27\",\"closedAt\":123"))
        assertTrue(manifest.contains("\"state\":\"CONFIRMED\""))
        assertTrue(manifest.contains("\"merchant\":\"Chai & Co\""))
        assertTrue(manifest.contains("\"photo\":null"))
        // The setting that can speak after the app is closed travels with the rest.
        assertTrue(manifest.contains("\"reminder\":{\"enabled\":false,\"hour\":20,\"minute\":0}"))
        assertTrue(DataArchiveZip.capturesCsv(listOf(manualEntry), emptyMap()).contains("capture-1"))
        assertTrue(DataArchiveZip.daysCsv(listOf(day)).contains("day-1"))
    }
    @Test
    fun csvNeutralizesSpreadsheetFormulasButJsonPreservesOriginals() {
        val date = LocalDay.of(2026, 11, 27)
        val currency = CurrencyCode.of("INR")
        val trip = Trip("trip-1", "=1+1", date, date, currency, currency)
        val capture = Capture(
            id = "capture-1", tripId = trip.id, localDate = date, photoPath = "",
            createdAt = 1L, state = CaptureState.CONFIRMED, amountMinor = -1250L,
            currency = currency, merchant = "  @evil", note = "+cmd",
        )
        val csv = DataArchiveZip.capturesCsv(listOf(capture), emptyMap())
        assertTrue(csv.contains("\"'  @evil\""))
        assertTrue(csv.contains("\"'+cmd\""))
        assertTrue(csv.contains("\"-1250\""))
        assertTrue(DataArchiveZip.tripsCsv(listOf(trip)).contains("\"'=1+1\""))
        val json = DataArchiveZip.manifest(Settings(), listOf(trip), emptyList(), listOf(capture), emptyList(), emptyMap())
        assertTrue(json.contains("\"name\":\"=1+1\""))
        assertTrue(json.contains("\"merchant\":\"  @evil\""))
    }

    @Test
    fun manifestKeepsTravellerFiguresAndReminderOffer() {
        val day = LocalDay.of(2026, 11, 27)
        val traveller = Traveller("person-1", "trip-1", "Friend", 1, "fingerprint", listOf(
            TravellerEntry(day, 1200, CurrencyCode.of("INR"), merchantInitials = "AB", digest = 42),
        ))
        val json = DataArchiveZip.manifest(Settings(reminderAsked = true), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyMap(), listOf(traveller))
        assertTrue(json.contains("\"reminderAsked\":true"))
        assertTrue(json.contains("\"codeFingerprint\":\"fingerprint\""))
        assertTrue(json.contains("\"merchantInitials\":\"AB\""))
        assertTrue(json.contains("\"amountMinor\":1200"))
    }

}
