package androidx.activity.result.contract

abstract class ActivityResultContract<I, O>

/** RequestPermission: takes a permission string, returns a boolean. */
class RequestPermissionContract : ActivityResultContract<String, Boolean>()

object ActivityResultContracts {
    class RequestPermission : ActivityResultContract<String, Boolean>()
}
