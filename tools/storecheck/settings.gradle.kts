// A JVM-only project whose sole job is to run SQLDelight's code generator against
// :core:store's schema. It exists because the full KMP build needs more memory
// than some machines have, and because a schema that has never been compiled is
// not a schema.
rootProject.name = "ariadne-storecheck"

pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
