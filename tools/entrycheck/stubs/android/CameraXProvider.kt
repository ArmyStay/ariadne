package androidx.camera.lifecycle

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner

/**
 * ProcessCameraProvider, standing on a real ListenableFuture so the code under
 * test is the code that ships: if the app calls a method the future does not
 * have, this compile catches it.
 */
abstract class ProcessCameraProvider {
    abstract fun unbindAll()
    abstract fun bindToLifecycle(
        owner: LifecycleOwner,
        selector: CameraSelector,
        vararg useCases: Any,
    ): Camera

    companion object {
        fun getInstance(context: Context): com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider> =
            com.google.common.util.concurrent.Futures.immediateFailedFuture(
                UnsupportedOperationException("stub provider"),
            )
    }
}
