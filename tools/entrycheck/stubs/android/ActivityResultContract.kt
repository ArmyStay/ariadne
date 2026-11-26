package androidx.activity.result.contract

import android.net.Uri

abstract class ActivityResultContract<I, O>

/** RequestPermission: takes a permission string, returns a boolean. */
class RequestPermissionContract : ActivityResultContract<String, Boolean>()

/** PickVisualMedia: takes a request, returns the chosen image's Uri (or null). */
abstract class PickVisualMediaContract : ActivityResultContract<androidx.activity.result.PickVisualMediaRequest, Uri?>()

object ActivityResultContracts {
    class RequestPermission : ActivityResultContract<String, Boolean>()

    /**
     * The system photo picker. `ImageOnly` is the media-type constant the app
     * asks for; on a device the contract opens the platform picker for one image.
     */
    class PickVisualMedia : PickVisualMediaContract() {
        companion object {
            const val ImageOnly: Int = 1
            const val VideoOnly: Int = 2
        }
    }
}
