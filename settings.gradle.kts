rootProject.name = "ariadne"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

// ---------------------------------------------------------------------------
// Targets. iOS host modules are only wired in when a Darwin host is present,
// so that the Linux/Windows CI legs stay green without a macOS runner.
// ---------------------------------------------------------------------------
val onMac = System.getProperty("os.name").startsWith("Mac")

include(
    ":core:model",
    ":core:time",
    ":core:queue",
    ":core:fx",
    ":core:extract",
    ":core:search",
    ":core:design",
    ":core:store",
    ":ui:app",
    ":app:android",
    ":app:desktop",
    ":app:wasm",
)

if (onMac) {
    include(":app:ios")
}
include(":shared")
