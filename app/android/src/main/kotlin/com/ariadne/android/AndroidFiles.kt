package com.ariadne.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.ariadne.app.Clipboard
import com.ariadne.app.PhotoFiles
import com.ariadne.app.PhotoNaming
import com.ariadne.model.LocalDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Photographs in the app's private folder.
 *
 * `filesDir/photographs/<tripId>/<day>/<captureId>.jpg` — never the shared
 * gallery, never external storage, so no other app can read them without root and
 * an uninstall takes them with it. That is a deliberate cost: the photographs are
 * not backed up by the phone's photo backup, because backing them up is exactly
 * the thing this app promises not to do.
 */
public class AndroidFiles(private val context: Context) : PhotoFiles {

    private val root: File get() = File(context.filesDir, "photographs").apply { mkdirs() }

    override suspend fun write(
        tripId: String,
        day: LocalDay,
        id: String,
        bytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val relative = PhotoNaming.relative(tripId, day, id)
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        // Written to a temporary name first so a kill mid-write cannot leave a
        // half-JPEG under the real name, which would decode as nothing.
        val temp = File(file.parentFile, "${file.name}.part")
        temp.writeBytes(bytes)
        if (!temp.renameTo(file)) {
            temp.delete()
            file.writeBytes(bytes)
        }
        file.absolutePath
    }

    override suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isFile) file.readBytes() else null
    }

    override suspend fun delete(path: String): Unit = withContext(Dispatchers.IO) {
        val file = File(path)
        file.delete()
        // Tidy the day folder and the trip folder when they empty, so a deleted
        // trip leaves nothing behind but the root.
        var parent = file.parentFile
        while (parent != null && parent != root && parent.list()?.isEmpty() == true) {
            val next = parent.parentFile
            parent.delete()
            parent = next
        }
        Unit
    }

    override suspend fun footprintBytes(): Long = withContext(Dispatchers.IO) {
        root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}

/** The system clipboard, used only by the trip card's share action. */
public class AndroidClipboard(private val context: Context) : Clipboard {
    override fun copy(text: String): Boolean = try {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Ariadne trip card", text))
        true
    } catch (e: Exception) {
        false
    }
}
