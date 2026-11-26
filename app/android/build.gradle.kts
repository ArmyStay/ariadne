plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ariadne.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ariadne.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":ui:app"))
    implementation(project(":core:store"))
    implementation(project(":core:model"))
    implementation(project(":core:time"))
    implementation(project(":core:extract"))
    implementation(project(":core:design"))
    implementation(libs.sqldelight.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    // The camera, bound to this activity's lifecycle.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.compose)
    // Bundled on-device ML Kit recognizers (no Play Services runtime download). Latin is primary;
    // additional script modules improve coverage for some trip destinations but not all scripts (e.g. Thai/Arabic).
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.devanagari)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)

    androidTestImplementation(project(":core:extract"))
    androidTestImplementation(project(":core:model"))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlin.test)
}
