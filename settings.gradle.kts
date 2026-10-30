pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MikasaMusic"
include(":app")
include(":decent-usb-audio-driver")
include(":decent-usb-audio-wrapper-media3")
include(":decent-media3-decoder-flac")

project(":decent-usb-audio-driver").projectDir =
    file("third_party/decent-player/libs/decent-usb-audio-driver")
project(":decent-usb-audio-wrapper-media3").projectDir =
    file("third_party/decent-player/libs/decent-usb-audio-wrapper-media3")
project(":decent-media3-decoder-flac").projectDir =
    file("third_party/decent-player/libs/decent-media3-decoder-flac")
