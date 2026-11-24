package android.provider
object Settings {
    object Global {
        const val ANIMATOR_DURATION_SCALE: String = "animator_duration_scale"
        fun getFloat(resolver: Any?, name: String, def: Float): Float = 1f
    }
    class SettingNotFoundException(msg: String) : Exception(msg)
}
