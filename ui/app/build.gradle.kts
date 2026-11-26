plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm(); iosArm64(); iosSimulatorArm64()
    wasmJs { browser(); nodejs() }
    androidLibrary {
        namespace = "com.ariadne.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        jvmTest {
            // Headless Compose UI tests run on JVM — the KMP contest kit's
            // "runComposeUiTest for headless UI tests" feature.
            dependencies { implementation(libs.compose.uiTest) }
        }
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:time"))
            implementation(project(":core:queue"))
            implementation(project(":core:extract"))
            implementation(project(":core:fx"))
            implementation(project(":core:search"))
            implementation(project(":core:store"))
            implementation(project(":core:design"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.napier)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
