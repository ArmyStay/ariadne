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
            // The tables are declared once, in 1.sqm, and everything else is
            // derived from the migrations. The alternative - CREATE statements in
            // Ariadne.sq *and* ALTERs in .sqm - meant `verifyMigrations` compiled
            // each migration against an empty derived schema and failed with
            // "No table found with name capture", while the local gate (which
            // never raised this flag) passed. Two declarations, one of them
            // silently wrong.
            deriveSchemaFromMigrations.set(true)
            verifyMigrations.set(true)
        }
    }
}
