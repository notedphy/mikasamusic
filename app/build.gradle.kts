plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.anothernop.mikasa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anothernop.mikasa"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    implementation(project(":decent-usb-audio-driver"))
    implementation(project(":decent-usb-audio-wrapper-media3"))
    implementation(project(":decent-media3-decoder-flac"))

    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.9.3")
    implementation("androidx.media3:media3-session:1.9.3")
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
