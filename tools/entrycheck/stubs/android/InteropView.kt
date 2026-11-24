package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable

/** AndroidView, as much of it as the camera preview needs. */
@Composable
fun <T : android.view.View> AndroidView(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    factory: (android.content.Context) -> T,
    update: (T) -> Unit = {},
) {
}
