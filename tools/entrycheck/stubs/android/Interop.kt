package androidx.compose.ui.platform

/** The Android context of the current composition. */
val LocalContext: androidx.compose.runtime.ProvidableCompositionLocal<android.content.Context> =
    androidx.compose.runtime.staticCompositionLocalOf { android.content.Context() }
