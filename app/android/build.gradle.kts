plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ariadne.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ariadne.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        // Meta Wearables DAT identity. 0/0 is the documented developer-mode pair: the
        // glasses accept an unregistered app while the developer switch is on in Meta
        // AI. A release build overrides both from the Wearables Developer Center.
        manifestPlaceholders["mwdatApplicationId"] = "0"
        manifestPlaceholders["mwdatClientToken"] = "0"
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
    // The rate-source contract: this module supplies the transport, :core:fx owns
    // the policy and parsing.
    implementation(project(":core:fx"))
    implementation(libs.sqldelight.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    // System splash until the app is up (Theme.App.Starting).
    implementation(libs.androidx.core.splashscreen)
    // Trusted in-app review (Play Core Review).
    implementation(libs.android.inappreview)
    // Meta Wearable Device Access Toolkit (DAT) - experimental surface, analytics opted out
    implementation(libs.mwdat.core)
    implementation(libs.mwdat.camera)
    debugImplementation(libs.mwdat.mock)
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
