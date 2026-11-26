plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm()
    androidTarget { withHostTest() }
    iosArm64()
    iosSimulatorArm64()
    wasmJs { browser(); nodejs() }

    sourceSets {
        commonTest.dependencies { implementation(libs.kotlin.test) }
        commonMain.dependencies { implementation(project(":core:model")); implementation(project(":core:queue")); implementation(libs.kotlinx.coroutines.core) }
        commonTest.dependencies { implementation(libs.kotlinx.coroutines.test) }
    }
}

android {
    namespace = "com.ariadne.core.extract"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig.minSdk = libs.versions.android.minSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
