package android.content

open class Context {
    open val packageManager: PackageManager = PackageManager()
    open val contentResolver: ContentResolver = ContentResolver()
    open val filesDir: java.io.File = java.io.File(System.getProperty("java.io.tmpdir"), "ariadne-stub")
    open fun getSystemService(name: String): Any? = null
    companion object {
        const val CLIPBOARD_SERVICE: String = "clipboard"
    }
}
