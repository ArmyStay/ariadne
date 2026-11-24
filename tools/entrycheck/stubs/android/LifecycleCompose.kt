package androidx.lifecycle.compose

import androidx.lifecycle.LifecycleOwner

/** The current lifecycle owner, as the real library supplies it. */
val LocalLifecycleOwner: androidx.compose.runtime.ProvidableCompositionLocal<LifecycleOwner> =
    androidx.compose.runtime.staticCompositionLocalOf { object : LifecycleOwner {} }
