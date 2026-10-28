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
        versionCode = 2
        versionName = "0.2"
    }

    // Fixed debug key so every build can update the previous install
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
