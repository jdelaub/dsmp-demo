plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release key outside the repo, as for the other apps, in ~/.gradle/gradle.properties:
//   SAFARI_NATURE_KEYSTORE=~/.android-keys/safari-nature-release.jks
//   SAFARI_NATURE_KEYSTORE_PASSWORD=…  SAFARI_NATURE_KEY_ALIAS=safari-nature  SAFARI_NATURE_KEY_PASSWORD=…
// Without it, the release APK comes out unsigned.
val releaseKeystore = providers.gradleProperty("SAFARI_NATURE_KEYSTORE").orNull

android {
    namespace = "app.safarinature"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.safarinature" // fixed once installed on a phone: the album lives under this id
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        // Committed debug key (not a secret): a CI build installs over a local one without losing the album.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null) create("release") {
            storeFile = file(releaseKeystore)
            storePassword = providers.gradleProperty("SAFARI_NATURE_KEYSTORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("SAFARI_NATURE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("SAFARI_NATURE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev" // lives next to the child's everyday copy
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // Not shrunk: the release runs the same code as the tested debug build.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Reference photos are already WebP: storing them again compressed gains nothing.
    androidResources { noCompress += "webp" }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 1
    maxHeapSize = "2g"
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    // The child's camera, inside the app: one big button, no settings to get lost in. Photos stay on the phone.
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    // Renders every screen on the JVM (Robolectric native graphics): the UI is checked without an emulator.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
