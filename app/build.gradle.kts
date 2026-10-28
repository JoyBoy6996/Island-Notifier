plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Code package (can be anything)
    namespace = "com.joyboy.islandnotifier"
    compileSdk = 34

    defaultConfig {
        // The ID Origin Island recognises (YT Music)
        applicationId = "com.google.android.apps.youtube.music"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
