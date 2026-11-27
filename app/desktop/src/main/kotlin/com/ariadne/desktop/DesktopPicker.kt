package com.ariadne.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ariadne.app.ui.pages.LocalPhotoPicker
import com.ariadne.app.ui.pages.LocalTripCardImageExporter
import com.ariadne.app.ui.pages.PhotoPicker
import com.ariadne.app.ui.pages.TripCardImageExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * The desktop picker: the system file dialog.
 *
 * AWT's `FileDialog` rather than Swing's `JFileChooser` because it is the real
 * system dialog on every platform - the one a person already knows, with their
 * recent folders and their own language in it. The alternative is a window that
 * looks like Java, which is the one thing a page this restrained cannot survive.
 *
 * Two details that matter more than they look:
 *
 *  - it runs on the AWT event thread, because showing a dialog on a coroutine
 *    worker is undefined behaviour on macOS and janky everywhere else;
 *  - the read happens off that thread, so a 40MB photograph does not freeze the
 *    window while it is being copied into the record.
 */
private class DesktopPhotoPicker(private val owner: Frame?) : PhotoPicker {
    override suspend fun pick(): ByteArray? = pickMany(1).firstOrNull()

    override suspend fun pickMany(limit: Int): List<ByteArray> {
        val chosen = withContext(Dispatchers.Main) {
            val dialog = FileDialog(owner, "Choose photographs", FileDialog.LOAD).apply {
                setFilenameFilter { _, name -> name.hasImageExtension() }
                isMultipleMode = limit > 1
                isVisible = true
            }
            if (dialog.isMultipleMode) {
                dialog.files.take(limit.coerceAtLeast(1))
            } else {
                val dir = dialog.directory
                val file = dialog.file
                if (dir == null || file == null) emptyList() else listOf(File(dir, file))
            }
        }
        return withContext(Dispatchers.IO) {
            chosen.mapNotNull { file -> file.readBoundedImage() }
        }
    }
}

private class DesktopTripCardImageExporter(private val owner: Frame?) : TripCardImageExporter {
    override suspend fun savePng(fileName: String, bytes: ByteArray): Boolean {
        val destination = withContext(Dispatchers.Main) {
            val dialog = FileDialog(owner, "Save Trip Card as PNG", FileDialog.SAVE).apply {
                file = fileName
                isVisible = true
            }
            val directory = dialog.directory
            val selected = dialog.file
            if (directory == null || selected == null) {
                null
            } else {
                File(directory, selected).let { file ->
                    if (file.extension.equals("png", ignoreCase = true)) file
                    else File(file.parentFile, "${file.name}.png")
                }
            }
        } ?: return false
        withContext(Dispatchers.IO) { destination.writeBytes(bytes) }
        return true
    }
}

private fun String.hasImageExtension(): Boolean {
    val lower = lowercase()
    return listOf(".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif", ".bmp", ".gif")
        .any { lower.endsWith(it) }
}

/** Supplies the platform's photo picker and local Trip Card export surface. */
@Composable
fun ProvideDesktopPhotoPicker(owner: Frame?, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPhotoPicker provides DesktopPhotoPicker(owner),
        LocalTripCardImageExporter provides DesktopTripCardImageExporter(owner),
        content = content,
    )
}
