package android.graphics

class Bitmap {
    enum class CompressFormat { JPEG, PNG, WEBP }
    var width: Int = 100
    var height: Int = 100
    fun recycle(): Unit = Unit
    fun compress(format: Bitmap.CompressFormat, quality: Int, out: java.io.OutputStream): Boolean = true
    companion object {
        fun createBitmap(
            source: Bitmap, x: Int, y: Int, width: Int, height: Int, matrix: Matrix?, filter: Boolean,
        ): Bitmap = Bitmap()
        @Suppress("UNUSED_PARAMETER")
        fun createScaledBitmap(source: Bitmap, width: Int, height: Int, filter: Boolean): Bitmap = Bitmap()
    }
}

class Matrix {
    fun postRotate(degrees: Float): Matrix = this
}

class BitmapFactory {
    companion object {
        fun decodeByteArray(data: ByteArray, offset: Int, length: Int): Bitmap? = Bitmap()
        fun decodeStream(stream: java.io.InputStream): Bitmap? = Bitmap()
    }
}
