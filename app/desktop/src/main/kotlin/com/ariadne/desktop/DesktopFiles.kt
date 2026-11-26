package com.ariadne.desktop

import com.ariadne.app.PhotoFiles
import com.ariadne.app.PhotoNaming
import com.ariadne.model.LocalDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Receipt photographs stored beside the desktop database in Ariadne's private folder. */
internal class DesktopFiles(private val root: File) : PhotoFiles {
    override suspend fun write(
        tripId: String,
        day: LocalDay,
        id: String,
        bytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val file = File(root, PhotoNaming.relative(tripId, day, id))
        file.parentFile?.mkdirs()
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
        var parent = file.parentFile
        while (parent != null && parent != root && parent.list()?.isEmpty() == true) {
            val next = parent.parentFile
            parent.delete()
            parent = next
        }
    }

    override suspend fun footprintBytes(): Long = withContext(Dispatchers.IO) {
        if (!root.exists()) 0L else root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
