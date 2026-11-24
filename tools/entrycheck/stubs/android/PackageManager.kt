package android.content

/** Stub: enough for the camera's hardware and permission checks. */
class PackageManager {
    fun hasSystemFeature(name: String): Boolean = true
    fun checkPermission(permission: String, packageName: String): Int = 0
}

class ClipData private constructor() {
    companion object {
        fun newPlainText(label: CharSequence, text: CharSequence): ClipData = ClipData()
    }
}

interface ClipboardManager {
    fun setPrimaryClip(clip: ClipData)
}

// androidx.core, as much of it as the camera touches.
// ----------------------------------------------------------------
