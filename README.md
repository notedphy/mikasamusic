# MikasaMusic

MikasaMusic is an offline Android music player for local audio files. It uses a custom dark interface, local search and library browsing, favorites, playlists, a playback queue, lock-screen controls, and direct USB DAC output.

## Requirements

- Android Studio with JDK 21
- Android SDK 36
- Android NDK `29.0.14206865` and CMake 3.22.1
- Android 10 (API 29) or newer

Open this directory in Android Studio and sync the Gradle project. The app ID is `com.anothernop.mikasa`.

## Local playback and USB DAC

The app scans the device's local audio library after the user grants audio access. It does not request network access or provide music streaming.

USB direct output is provided by the MIT-licensed [Decent USB Audio libraries](https://github.com/Ma145/decent-player), pinned in `third_party/decent-player`. Its native decoder uses Xiph FLAC 1.5.0, pinned in the same directory. The current driver targets USB Audio Class 2.0 DACs on Android 10+. Output capability, sample-rate support, and bit-perfect operation depend on the connected DAC and device. Without a compatible DAC, playback uses Android's standard audio output.

The Library includes song, artist, album, favorite, and playlist browsing. Lyrics, translations, romaji, and kanji can be entered and saved offline per track; translation and romanization are not generated automatically. The five-band equalizer is available from the DAC tab and is saved on the device. Equalization applies software DSP to both Android and USB output, so bit-perfect playback is available only when all EQ bands are set to 0 dB. The USB path automatically avoids its native direct FLAC engine while EQ is active.

When internet access is available, MikasaMusic searches Deezer's public track catalog using the local song title and artist to show matching album covers, artist photos, and catalog artist/album names. Matches and downloaded artwork are cached on the device; playback remains local. Artwork appears only when a catalog match is available.

The driver and Media3 wrapper are included as local Gradle modules because they are not published to Maven Central. Their upstream `LICENSE` and `NOTICE.md` are kept alongside the vendored source. The optional native FLAC decoder module is included because the USB driver's native build links against it; its bundled FLAC sources retain their upstream notices.

## Build

From Android Studio, use **Build > Make Project**. The first build downloads Android/Gradle dependencies and requires the listed SDK, NDK, and CMake versions. USB DAC behavior should be verified on each target phone and DAC; it cannot be validated by an emulator alone.

GitHub Actions runs the debug APK build on pushes, pull requests, and manual workflow dispatch. Successful builds upload the APK as the `mikasa-debug` workflow artifact.
