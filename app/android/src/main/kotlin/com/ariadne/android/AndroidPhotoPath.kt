package com.ariadne.android

import android.content.Context
import java.io.File

internal fun safeAndroidPhotoFile(context: Context, path: String): File? = runCatching {
    val root = File(context.filesDir, "photographs").apply { mkdirs() }
    val rootPath = root.canonicalFile.path + File.separator
    File(path).canonicalFile.takeIf { it.path.startsWith(rootPath) }
}.getOrNull()
