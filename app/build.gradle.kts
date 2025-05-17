plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "app.atomofiron.cheatpeek"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.atomofiron.cheatpeek"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // 🦗🦗🦗
}