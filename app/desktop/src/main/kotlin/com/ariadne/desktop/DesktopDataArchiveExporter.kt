package com.ariadne.desktop

import com.ariadne.app.DataArchiveExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

internal object DesktopDataArchiveExporter : DataArchiveExporter {
    override suspend fun saveZip(fileName: String, bytes: ByteArray): Boolean {
        val selected = AtomicReference<File?>()
        withContext(Dispatchers.IO) {
            SwingUtilities.invokeAndWait {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Save Ariadne data archive"
                    selectedFile = File(fileName)
                    fileFilter = FileNameExtensionFilter("ZIP archive", "zip")
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selected.set(chooser.selectedFile)
                }
            }
        }
        val picked = selected.get() ?: return false
        val target = picked.let {
            if (it.extension.equals("zip", ignoreCase = true)) it else File(it.parentFile, "${it.name}.zip")
        }
        withContext(Dispatchers.IO) { target.writeBytes(bytes) }
        return true
    }
}
