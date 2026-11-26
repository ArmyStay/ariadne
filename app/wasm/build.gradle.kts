plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "ariadne.js"
            }
            binaries.executable()
        }
    }
    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":ui:app"))
            implementation(project(":core:store"))
            implementation(project(":core:queue"))
            implementation(project(":core:extract"))
            implementation(project(":core:time"))
            implementation(libs.sqldelight.web)
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}
