package com.ariadne.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ariadne.app.ui.pages.LocalPhotoPicker
import com.ariadne.app.ui.pages.PhotoPicker
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
    override suspend fun pick(): ByteArray? {
        val chosen = withContext(Dispatchers.Main) {
            val dialog = FileDialog(owner, "Choose a photograph", FileDialog.LOAD).apply {
                // A filter is a hint, not a guarantee: every platform lets the
                // person pick anything anyway, so the bytes are checked below.
                setFilenameFilter { _, name -> name.hasImageExtension() }
                isVisible = true
            }
            val dir = dialog.directory
            val file = dialog.file
            if (dir == null || file == null) null else File(dir, file)
        } ?: return null

        return withContext(Dispatchers.IO) {
            runCatching { chosen.readBytes() }.getOrNull()?.takeIf { it.isNotEmpty() }
        }
    }
}

private fun String.hasImageExtension(): Boolean {
    val lower = lowercase()
    return listOf(".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif", ".bmp", ".gif")
        .any { lower.endsWith(it) }
}

/** Supplies the picker to the shared UI, the same way the camera is supplied on Android. */
@Composable
fun ProvideDesktopPhotoPicker(owner: Frame?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPhotoPicker provides DesktopPhotoPicker(owner), content = content)
}
