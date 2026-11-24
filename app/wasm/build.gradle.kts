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
            implementation(libs.sqldelight.web)
            implementation(libs.kotlinx.browser)
        }
    }
}
