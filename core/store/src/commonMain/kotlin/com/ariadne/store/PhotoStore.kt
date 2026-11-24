package com.ariadne.store

import com.ariadne.model.LocalDay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Photographs, written before anything else.
 *
 * Ordering matters and is enforced here: [writeCapturePhoto] returns a path that
 * is already durable, and only then does the caller insert the capture row. A
 * row with no photo would be a hole; a photo with no row is invisible junk that
 * a launch sweep can collect.
 */
public interface PhotoStore {
    /** Persists bytes under a trip/day path and returns the app-private path. */
    public suspend fun writeCapturePhoto(tripId: String, day: LocalDay, id: String, bytes: ByteArray): String

    public suspend fun read(path: String): ByteArray?

    public suspend fun delete(path: String)

    /** Total bytes of stored photos, for the settings footprint line. */
    public suspend fun footprintBytes(): Long

    /** Removes photos with no capture row. Called on launch, idempotent. */
    public suspend fun collectOrphans(knownPaths: Set<String>): Int
}

/**
 * The queue's durability guarantee, stated once:
 * photo first, then row, then the shutter may return.
 */
public class CaptureCommitter(
    private val photos: PhotoStore,
    private val store: Store,
    private val io: CoroutineDispatcher,
) {
    public suspend fun commit(
        tripId: String,
        day: LocalDay,
        id: String,
        createdAt: Long,
        bytes: ByteArray,
    ): String = withContext(io) {
        val path = photos.writeCapturePhoto(tripId, day, id, bytes)
        store.insert(
            com.ariadne.model.Capture(
                id = id,
                tripId = tripId,
                localDate = day,
                photoPath = path,
                createdAt = createdAt,
                state = com.ariadne.model.CaptureState.QUEUED,
            ),
        )
        path
    }
}
