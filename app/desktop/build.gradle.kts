import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    sourceSets {
        jvmMain {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                implementation(project(":ui:app"))
                implementation(project(":core:store"))
                implementation(project(":core:time"))
                implementation(project(":core:queue"))
                implementation(project(":core:extract"))
                implementation(libs.sqldelight.jvm)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ariadne.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Ariadne"
            packageVersion = "1.0.0"
        }
    }
}
