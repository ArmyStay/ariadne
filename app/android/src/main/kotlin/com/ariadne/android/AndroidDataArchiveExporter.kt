package com.ariadne.android

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ariadne.app.DataArchiveExporter
import kotlinx.coroutines.CompletableDeferred

@Composable
fun rememberAndroidDataArchiveExporter(context: Context): DataArchiveExporter {
    val pending = remember { mutableStateOf<PendingDataArchiveExport?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        val request = pending.value
        pending.value = null
        if (request != null) {
            if (uri == null) {
                request.completion.complete(false)
            } else {
                Thread(
                    {
                        try {
                            val output = context.contentResolver.openOutputStream(uri)
                                ?: error("The selected document could not be opened for writing.")
                            output.use { it.write(request.bytes) }
                            request.completion.complete(true)
                        } catch (failure: Exception) {
                            request.completion.completeExceptionally(failure)
                        }
                    },
                    "ariadne-data-export",
                ).start()
            }
        }
    }

    return remember(context, launcher) {
        object : DataArchiveExporter {
            override suspend fun saveZip(fileName: String, bytes: ByteArray): Boolean {
                check(pending.value == null) { "A data export is already in progress." }
                val request = PendingDataArchiveExport(bytes, CompletableDeferred())
                pending.value = request
                launcher.launch(fileName)
                return request.completion.await()
            }
        }
    }
}

private data class PendingDataArchiveExport(
    val bytes: ByteArray,
    val completion: CompletableDeferred<Boolean>,
)
