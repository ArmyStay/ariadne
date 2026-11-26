package com.ariadne.app.ui.pages

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** Saves the locally rendered Trip Card image using the platform's save/share surface. */
public interface TripCardImageExporter {
    /** False means the person cancelled the platform's save or share surface. */
    public suspend fun savePng(fileName: String, bytes: ByteArray): Boolean
}

public val LocalTripCardImageExporter: ProvidableCompositionLocal<TripCardImageExporter?> =
    staticCompositionLocalOf { null }
