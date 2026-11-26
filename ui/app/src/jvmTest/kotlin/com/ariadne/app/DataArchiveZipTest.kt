package com.ariadne.app

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Settings
import com.ariadne.model.Trip
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
        assertTrue(DataArchiveZip.capturesCsv(listOf(manualEntry), emptyMap()).contains("capture-1"))
        assertTrue(DataArchiveZip.daysCsv(listOf(day)).contains("day-1"))
    }
}
