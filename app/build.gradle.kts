plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigning = listOf(
    providers.environmentVariable("MIKASA_RELEASE_STORE_FILE").orNull,
    providers.environmentVariable("MIKASA_RELEASE_STORE_PASSWORD").orNull,
    providers.environmentVariable("MIKASA_RELEASE_KEY_ALIAS").orNull,
    providers.environmentVariable("MIKASA_RELEASE_KEY_PASSWORD").orNull
)
val hasReleaseSigning = releaseSigning.any { it != null }
if (hasReleaseSigning && releaseSigning.any { it.isNullOrBlank() }) {
    throw GradleException("Set all MIKASA_RELEASE_* signing environment variables to build a signed release.")
}

android {
    namespace = "com.anothernop.mikasa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anothernop.mikasa"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
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

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseSigning[0]!!)
                storePassword = releaseSigning[1]!!
                keyAlias = releaseSigning[2]!!
                keyPassword = releaseSigning[3]!!
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
