package androidx.activity
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
open class ComponentActivity : Context() {
    open val contentResolver: ContentResolver? = null
    protected open fun onCreate(savedInstanceState: Bundle?) {}
    protected open fun onDestroy() {}
}
fun ComponentActivity.enableEdgeToEdge() {}
