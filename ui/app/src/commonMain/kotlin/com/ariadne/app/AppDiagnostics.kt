package com.ariadne.app

/**
 * App diagnostics surfaced to the user — a review-ready section of the Settings page.
 *
 * Follows the KMP contest starter kit: a small, plain "Diagnostics" view of your logs
 * with a Refresh button, so the user can see what the app is doing.
 */
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ariadne.design.Paper

/**
 * The user-facing view of app diagnostics — logs you can review and clear.
 * Draw [Paper] as the card around it.
 */
@Composable
public fun DiagnosticsCard(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    var logs by remember { mutableStateOf(AppDiagnostics.current()) }
    Paper(modifier = modifier) {
        LazyColumn {
            items(logs) { line ->
                Text(
                    line,
                    style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold).let { it },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OutlinedButton({ logs = AppDiagnostics.current(); AppDiagnostics.refresh() }) {
            Text("Refresh logs")
        }
    }
}

/** The provider/refresher — reads the RingLogger from AppLogger when available. */
public object AppDiagnostics {
    public fun current(): List<String> =
        AppLogger.sinks.filterIsInstance<RingLogger>().flatMap { it.snapshot() }

    public fun refresh(): List<String> = current()

    public fun clearAll() {
        AppLogger.sinks.filterIsInstance<RingLogger>().forEach { it.clear() }
    }
}
