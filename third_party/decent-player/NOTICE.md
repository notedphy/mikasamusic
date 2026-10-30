# Third-party notices

## Decent USB Audio

The USB Audio driver, Media3 wrapper, and FLAC decoder integration in `libs/`
come from [Ma145/decent-player](https://github.com/Ma145/decent-player), pinned
at commit `d039f9d6b1ec30901a5cd6edf72c29cde362a549`. They are distributed
under the MIT License in `LICENSE`.

MikasaMusic adapts the driver's and wrapper's Gradle plugin declarations to
apply the Kotlin Android plugin with Android Gradle Plugin 8.x.

## Xiph FLAC

`libs/decent-media3-decoder-flac/src/main/jni/libflac/` contains the upstream
[xiph/flac](https://github.com/xiph/flac) source, pinned at commit
`1507800de4b70e21be71f38caa0d9079d0bc6e45` (version 1.5.0). Its license and
other upstream notices are preserved in that directory, including
`COPYING.Xiph`.
