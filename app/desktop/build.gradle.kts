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
                implementation(project(":core:model"))
                implementation(libs.sqldelight.jvm)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.androidx.datastore.preferences.core)
                implementation(compose.desktop.currentOs)
                implementation("net.sourceforge.tess4j:tess4j:5.12.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(project(":core:extract"))
                implementation(project(":core:model"))
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
