package androidx.camera.view

import android.content.Context
import android.view.View

/** PreviewView, reduced to the properties the binding sets. */
class PreviewView(context: Context) : View(context) {
    var scaleType: ScaleType = ScaleType.FILL_CENTER
    var implementationMode: ImplementationMode = ImplementationMode.COMPATIBLE

    val surfaceProvider: Any get() = Any()

    enum class ScaleType { FILL_CENTER, FIT_CENTER, FILL_START }
    enum class ImplementationMode { PERFORMANCE, COMPATIBLE }
}
