package androidx.activity.compose

import androidx.activity.result.contract.ActivityResultContract

/** rememberLauncherForActivityResult, reduced to launch(). */
class Launcher(private val onResult: (Boolean) -> Unit) {
    fun launch(input: String) { onResult(true) }
}

@androidx.compose.runtime.Composable
fun rememberLauncherForActivityResult(
    contract: ActivityResultContract<String, Boolean>,
    onResult: (Boolean) -> Unit,
): Launcher = Launcher(onResult)
