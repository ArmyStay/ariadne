plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvm()
    iosX64(); iosArm64(); iosSimulatorArm64()
    wasmJs { browser(); nodejs() }
    androidLibrary {
        namespace = "com.ariadne.core.model"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
