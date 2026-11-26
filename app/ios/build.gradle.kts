plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin { iosArm64(); iosSimulatorArm64()
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Ariadne"
            isStatic = true
        }
    }
    sourceSets {
        iosMain.dependencies {
            implementation(project(":ui:app"))
            implementation(project(":core:store"))
            implementation(libs.sqldelight.native)
        }
    }
}
