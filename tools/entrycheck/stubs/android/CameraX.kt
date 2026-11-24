// CameraX and the compose interop the Android camera binding touches. These are
// compile-only stand-ins: on a machine without the Android SDK, the point is to
// prove that every API the camera calls exists with the shape it is called with,
// not to run it. Signatures are copied from CameraX 1.4.
package androidx.camera.core

import android.content.Context
import android.graphics.Matrix
import java.util.concurrent.Executor

@Suppress("UNUSED_PARAMETER")
class Preview private constructor() {
    fun setSurfaceProvider(provider: Any?) {}
    class Builder {
        fun build(): Preview = Preview()
    }
}

class ImageCapture private constructor() {
    fun takePicture(executor: Executor, callback: OnImageCapturedCallback) {}

    abstract class OnImageCapturedCallback {
        open fun onCaptureSuccess(image: ImageProxy) {}
        open fun onError(exception: ImageCaptureException) {}
    }

    class Builder {
        fun setTargetResolution(size: android.util.Size): Builder = this
        fun setResolutionSelector(
            selector: androidx.camera.core.resolutionselector.ResolutionSelector,
        ): Builder = this
        fun setCaptureMode(mode: Int): Builder = this
        fun build(): ImageCapture = ImageCapture()
    }

    companion object {
        const val CAPTURE_MODE_MINIMIZE_LATENCY: Int = 1
    }
}

class ImageCaptureException(val code: Int, message: String, cause: Throwable?) : Exception(message, cause)

class ImageProxy {
    val format: Int get() = android.graphics.ImageFormat.JPEG
    val planes: Array<Plane> get() = arrayOf(Plane())
    val imageInfo: ImageInfo get() = ImageInfo()
    fun close() {}

    class Plane {
        val buffer: java.nio.ByteBuffer = java.nio.ByteBuffer.allocate(0)
    }
}

class ImageInfo {
    val rotationDegrees: Int = 0
}

class CameraSelector private constructor() {
    companion object {
        val DEFAULT_BACK_CAMERA: CameraSelector = CameraSelector()
        val DEFAULT_FRONT_CAMERA: CameraSelector = CameraSelector()
    }
}

class Camera {
    // Nothing the app calls yet.
}
