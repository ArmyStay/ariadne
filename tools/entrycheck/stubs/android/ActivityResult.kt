package androidx.activity.compose

import androidx.activity.result.contract.ActivityResultContract

/**
 * rememberLauncherForActivityResult, reduced to launch().
 *
 * Generic over the contract's input and output, because the app now registers two
 * of them: the camera permission (`String` → `Boolean`) and the system photo
 * picker (`PickVisualMediaRequest` → `Uri?`). The gate below only has to check
 * that the call sites type-check, not that a launcher actually launches.
 */
class Launcher<I>(private val onLaunch: (I) -> Unit) {
    fun launch(input: I) { onLaunch(input) }
}

@androidx.compose.runtime.Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): Launcher<I> = Launcher { }
