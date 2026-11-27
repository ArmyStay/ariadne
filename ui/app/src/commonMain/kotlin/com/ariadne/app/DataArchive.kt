package com.ariadne.app

import com.ariadne.model.Capture
import com.ariadne.model.DayCard
import com.ariadne.model.Rate
import com.ariadne.model.Settings
import com.ariadne.model.Trip
import com.ariadne.model.Traveller

/** Saves a ZIP archive through the platform's native file/share surface. */
public interface DataArchiveExporter {
    /** False means the person cancelled the platform's save/share surface. */
    public suspend fun saveZip(fileName: String, bytes: ByteArray): Boolean
}

internal data class ArchiveFile(val name: String, val bytes: ByteArray)

/** A minimal, uncompressed ZIP writer so the export format is available in common code. */
internal object DataArchiveZip {
    fun encode(files: List<ArchiveFile>): ByteArray {
        require(files.size <= 65_535) { "The archive contains too many files." }
        val prepared = files.map { file ->
            require(file.bytes.size.toLong() <= 0xFFFF_FFFFL) { "An archive entry is too large." }
            val name = file.name.encodeToByteArray()
            require(name.size <= 65_535) { "An archive filename is too long." }
            Prepared(file, name, crc32(file.bytes))
        }
        val localSize = prepared.sumOf { 30L + it.name.size + it.file.bytes.size }
        val centralSize = prepared.sumOf { 46L + it.name.size }
        val total = localSize + centralSize + 22
        require(total <= Int.MAX_VALUE) { "The export is too large to prepare in memory." }

        val output = ByteArray(total.toInt())
        var offset = 0
        val centralOffset = localSize.toInt()
        prepared.forEach { entry ->
            entry.offset = offset
            write32(output, offset, 0x04034b50L); offset += 4
            write16(output, offset, 20); offset += 2
            write16(output, offset, 0x0800); offset += 2
            write16(output, offset, 0); offset += 2
            write16(output, offset, 0); offset += 2
            write16(output, offset, 0x0021); offset += 2
            write32(output, offset, entry.crc); offset += 4
            write32(output, offset, entry.file.bytes.size.toLong()); offset += 4
            write32(output, offset, entry.file.bytes.size.toLong()); offset += 4
            write16(output, offset, entry.name.size); offset += 2
            write16(output, offset, 0); offset += 2
            entry.name.copyInto(output, offset); offset += entry.name.size
            entry.file.bytes.copyInto(output, offset); offset += entry.file.bytes.size
        }
        prepared.forEach { entry ->
            write32(output, offset, 0x02014b50L); offset += 4
            write16(output, offset, 20); offset += 2
            write16(output, offset, 20); offset += 2
            write16(output, offset, 0x0800); offset += 2
            write16(output, offset, 0); offset += 2
            write16(output, offset, 0); offset += 2
            write16(output, offset, 0x0021); offset += 2
            write32(output, offset, entry.crc); offset += 4
            write32(output, offset, entry.file.bytes.size.toLong()); offset += 4
            write32(output, offset, entry.file.bytes.size.toLong()); offset += 4
            write16(output, offset, entry.name.size); offset += 2
            write16(output, offset, 0); offset += 2
            write16(output, offset, 0); offset += 2
            write16(output, offset, 0); offset += 2
            write16(output, offset, 0); offset += 2
            write32(output, offset, 0); offset += 4
            write32(output, offset, entry.offset.toLong()); offset += 4
            entry.name.copyInto(output, offset); offset += entry.name.size
        }
        val centralLength = offset - centralOffset
        write32(output, offset, 0x06054b50L); offset += 4
        write16(output, offset, 0); offset += 2
        write16(output, offset, 0); offset += 2
        write16(output, offset, prepared.size); offset += 2
        write16(output, offset, prepared.size); offset += 2
        write32(output, offset, centralLength.toLong()); offset += 4
        write32(output, offset, centralOffset.toLong()); offset += 4
        write16(output, offset, 0)
        return output
    }

    fun manifest(
        settings: Settings,
        trips: List<Trip>,
        days: List<DayCard>,
        captures: List<Capture>,
        rates: List<Rate>,
        photoEntries: Map<String, String>,
        travellers: List<Traveller> = emptyList(),
    ): String = buildString {
        append("{\"format\":\"ariadne-export-v1\",\"settings\":{")
        append("\"baseCurrency\":").append(json(settings.baseCurrency.code)).append(',')
        append("\"homeCurrency\":").append(json(settings.homeCurrency.code)).append(',')
        append("\"theme\":").append(json(settings.theme.name)).append(',')
        append("\"ocrProvider\":").append(json(settings.ocrProvider.name)).append(',')
        append("\"displayName\":").append(json(settings.displayName)).append(',')
        // The reminder travels with the rest of the settings: it is a preference of the
        // person, and an export that forgot it would restore a trip with the evening
        // half-missing.
        append("\"reminder\":{\"enabled\":").append(settings.reminder.enabled)
        append(",\"hour\":").append(settings.reminder.hour)
        append(",\"minute\":").append(settings.reminder.minute).append('}')
        append(",\"reminderAsked\":").append(settings.reminderAsked)
        append("},\"trips\":[")
        trips.forEachIndexed { index, trip ->
            if (index > 0) append(',')
            append("{\"id\":").append(json(trip.id))
            append(",\"name\":").append(json(trip.name))
            append(",\"place\":").append(json(trip.place))
            append(",\"startDate\":").append(json(trip.startDate.iso))
            append(",\"endDate\":").append(json(trip.endDate.iso))
            append(",\"baseCurrency\":").append(json(trip.baseCurrency.code))
            append(",\"homeCurrency\":").append(json(trip.homeCurrency.code))
            append(",\"budgetMinor\":").append(trip.budgetMinor ?: "null")
            append(",\"closedAt\":").append(trip.closedAt ?: "null").append('}')
        }
        append("],\"days\":[")
        days.forEachIndexed { index, day ->
            if (index > 0) append(',')
            append("{\"id\":").append(json(day.id))
            append(",\"tripId\":").append(json(day.tripId))
            append(",\"date\":").append(json(day.localDate.iso))
            append(",\"closedAt\":").append(day.closedAt ?: "null").append('}')
        }
        append("],\"captures\":[")
        captures.forEachIndexed { index, capture ->
            if (index > 0) append(',')
            append("{\"id\":").append(json(capture.id))
            append(",\"tripId\":").append(json(capture.tripId))
            append(",\"date\":").append(json(capture.localDate.iso))
            append(",\"createdAt\":").append(capture.createdAt)
            append(",\"state\":").append(json(capture.state.name))
            append(",\"amountMinor\":").append(capture.amountMinor ?: "null")
            append(",\"currency\":").append(json(capture.currency?.code))
            append(",\"merchant\":").append(json(capture.merchant))
            append(",\"category\":").append(json(capture.category?.name))
            append(",\"note\":").append(json(capture.note))
            append(",\"extractionConfidence\":").append(capture.extractionConfidence ?: "null")
            append(",\"confirmedAt\":").append(capture.confirmedAt ?: "null")
            append(",\"markedNoAmount\":").append(capture.markedNoAmount)
            append(",\"rateScaled\":").append(capture.rateScaled ?: "null")
            append(",\"rateDate\":").append(json(capture.rateDate?.iso))
            append(",\"photo\":").append(json(photoEntries[capture.id]))
            append('}')
        }
        append("],\"rates\":[")
        rates.forEachIndexed { index, rate ->
            if (index > 0) append(',')
            append("{\"base\":").append(json(rate.base.code))
            append(",\"quote\":").append(json(rate.quote.code))
            append(",\"date\":").append(json(rate.date.iso))
            append(",\"rateScaled\":").append(rate.rateScaled).append('}')
        }
        append("],\"travellers\":[")
        travellers.forEachIndexed { index, traveller ->
            if (index > 0) append(',')
            append("{\"id\":").append(json(traveller.id))
            append(",\"tripId\":").append(json(traveller.tripId))
            append(",\"name\":").append(json(traveller.name))
            append(",\"addedAt\":").append(traveller.addedAt)
            append(",\"codeFingerprint\":").append(json(traveller.codeFingerprint))
            append(",\"entries\":[")
            traveller.entries.forEachIndexed { entryIndex, entry ->
                if (entryIndex > 0) append(',')
                append("{\"day\":").append(json(entry.day.iso))
                append(",\"amountMinor\":").append(entry.amountMinor)
                append(",\"currency\":").append(json(entry.currency.code))
                append(",\"category\":").append(json(entry.category?.name))
                append(",\"merchantInitials\":").append(json(entry.merchantInitials))
                append(",\"digest\":").append(entry.digest).append('}')
            }
            append("]}")
        }
        append("]}")
    }

    fun capturesCsv(captures: List<Capture>, photoEntries: Map<String, String>): String = buildString {
        appendLine(
            "id,trip_id,date,created_at,state,amount_minor,currency,merchant,extraction_confidence," +
                "confirmed_at,category,note,marked_no_amount,rate_scaled,rate_date,photo",
        )
        captures.forEach { capture ->
            appendLine(
                listOf(
                    capture.id,
                    capture.tripId,
                    capture.localDate.iso,
                    capture.createdAt.toString(),
                    capture.state.name,
                    capture.amountMinor?.toString().orEmpty(),
                    capture.currency?.code.orEmpty(),
                    capture.merchant.orEmpty(),
                    capture.extractionConfidence?.toString().orEmpty(),
                    capture.confirmedAt?.toString().orEmpty(),
                    capture.category?.name.orEmpty(),
                    capture.note.orEmpty(),
                    capture.markedNoAmount.toString(),
                    capture.rateScaled?.toString().orEmpty(),
                    capture.rateDate?.iso.orEmpty(),
                    photoEntries[capture.id].orEmpty(),
                ).mapIndexed { index, value -> csv(value, index !in setOf(3, 5, 8, 9, 13)) }.joinToString(","),
            )
        }
    }

    fun tripsCsv(trips: List<Trip>): String = buildString {
        appendLine("id,name,place,start_date,end_date,base_currency,home_currency,budget_minor,closed_at")
        trips.forEach { trip ->
            appendLine(
                listOf(
                    trip.id,
                    trip.name,
                    trip.place.orEmpty(),
                    trip.startDate.iso,
                    trip.endDate.iso,
                    trip.baseCurrency.code,
                    trip.homeCurrency.code,
                    trip.budgetMinor?.toString().orEmpty(),
                    trip.closedAt?.toString().orEmpty(),
                ).mapIndexed { index, value -> csv(value, index !in setOf(7, 8)) }.joinToString(","),
            )
        }
    }

    fun daysCsv(days: List<DayCard>): String = buildString {
        appendLine("id,trip_id,date,closed_at")
        days.forEach { day ->
            appendLine(
                listOf(
                    day.id,
                    day.tripId,
                    day.localDate.iso,
                    day.closedAt?.toString().orEmpty(),
                ).joinToString(",") { csv(it) },
            )
        }
    }

    private data class Prepared(
        val file: ArchiveFile,
        val name: ByteArray,
        val crc: Long,
        var offset: Int = 0,
    )

    private fun write16(out: ByteArray, offset: Int, value: Int) {
        out[offset] = value.toByte()
        out[offset + 1] = (value ushr 8).toByte()
    }

    private fun write32(out: ByteArray, offset: Int, value: Long) {
        write16(out, offset, value.toInt() and 0xFFFF)
        write16(out, offset + 2, ((value ushr 16) and 0xFFFF).toInt())
    }

    private fun crc32(bytes: ByteArray): Long {
        var crc = 0xFFFF_FFFFL
        bytes.forEach { byte ->
            crc = crc xor (byte.toLong() and 0xFF)
            repeat(8) {
                crc = if ((crc and 1L) != 0L) (crc ushr 1) xor 0xEDB8_8320L else crc ushr 1
            }
        }
        return crc xor 0xFFFF_FFFFL
    }

    private fun json(value: String?): String =
        value?.let {
            buildString {
                append('"')
                it.forEach { char ->
                    when (char) {
                        '"' -> append("\\\"")
                        '\\' -> append("\\\\")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> if (char.code < 0x20) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else append(char)
                    }
                }
                append('"')
            }
        } ?: "null"

    // Spreadsheet programs can evaluate formulas even in quoted CSV cells. Neutralize
    // untrusted text, including formulas preceded by whitespace or control characters.
    private fun csv(value: String, untrustedText: Boolean = false): String {
        val dangerous = untrustedText && (value.firstOrNull() in listOf('\t', '\r', '\n') ||
            value.trimStart().firstOrNull() in listOf('=', '+', '-', '@'))
        val safe = if (dangerous) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}
