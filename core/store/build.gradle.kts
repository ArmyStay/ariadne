plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    wasmJs { browser(); nodejs() }

    sourceSets {
        commonTest.dependencies { implementation(libs.kotlin.test) }
        commonMain.dependencies { implementation(project(":core:model")); implementation(project(":core:queue")); implementation(project(":core:fx")); implementation(libs.sqldelight.runtime); implementation(libs.sqldelight.coroutines); implementation(libs.kotlinx.coroutines.core); implementation(libs.okio) }
        androidMain.dependencies { implementation(libs.sqldelight.android) }
        jvmMain.dependencies { implementation(libs.sqldelight.jvm) }
        iosMain.dependencies { implementation(libs.sqldelight.native) }
        wasmJsMain.dependencies { implementation(libs.sqldelight.web) }
    }
}

android {
    namespace = "com.ariadne.core.store"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig.minSdk = libs.versions.android.minSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("AriadneDb") {
            packageName.set("com.ariadne.store.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            // On. The schema is versioned through .sqm files and every migration
            // is checked against the generated schema at build time — the failure
            // this guards against (a table that grew while its version did not) is
            // exactly the one that made "Open the trip" do nothing on a device.
            verifyMigrations.set(true)
        }
    }
}
