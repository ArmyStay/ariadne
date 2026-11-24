package com.ariadne.store

import com.ariadne.model.LocalDay

/** Path layout shared by every target so a photo survives a platform move. */
public object PhotoPaths {
    public fun capturePath(tripId: String, day: LocalDay, id: String): String =
        "$tripId/${day.iso}/$id.jpg"
}

