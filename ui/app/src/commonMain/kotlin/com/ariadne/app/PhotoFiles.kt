package com.ariadne.app

import com.ariadne.model.LocalDay

/**
 * Where a photograph goes.
 *
 * The record stores a path; the bytes live in the app's own private folder, never
 * in the database and never on a server. One photo, one file, named by trip, day
 * and capture id so a backup, a restore or a move to another target can put the
 * record back together from the folder alone.
 *
 * The interface is tiny on purpose: four methods, no listing, no metadata. A
 * platform that can write a file can implement it, and the desktop preview
 * implements it against a temp folder so the whole capture path is exercised
 * without a device.
 */
public interface PhotoFiles {
    /** Writes [bytes] and returns the durable path to store in the capture row. */
    public suspend fun write(tripId: String, day: LocalDay, id: String, bytes: ByteArray): String

    /** Null when the file is gone: a missing photograph is a state, not a crash. */
    public suspend fun read(path: String): ByteArray?

    public suspend fun delete(path: String)

    /** For the settings footprint line. */
    public suspend fun footprintBytes(): Long
}

/** Shared arithmetic so every target reports the same number. */
public object PhotoNaming {
    public fun relative(tripId: String, day: LocalDay, id: String): String =
        "$tripId/${day.iso}/$id.jpg"
}
