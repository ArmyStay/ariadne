// CameraX's resolution selector API. Kotlin does not require the directory to
// match the package, and the entry check globs one level deep, so this lives
// here rather than under camera/core/resolutionselector.
package androidx.camera.core.resolutionselector

import android.util.Size

class ResolutionStrategy private constructor() {
    constructor(targetResolution: Size, fallbackRule: Int) : this()

    companion object {
        const val FALLBACK_RULE_NONE: Int = 0
        const val FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER: Int = 1
        const val FALLBACK_RULE_CLOSEST_HIGHER: Int = 2
        const val FALLBACK_RULE_CLOSEST_LOWER: Int = 3
    }
}

class ResolutionSelector private constructor() {
    class Builder {
        fun setResolutionStrategy(strategy: ResolutionStrategy): Builder = this
        fun build(): ResolutionSelector = ResolutionSelector()
    }
}
