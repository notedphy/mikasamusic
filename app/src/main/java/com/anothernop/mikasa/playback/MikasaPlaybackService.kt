package com.anothernop.mikasa.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.decent.usbaudio.media3.UsbAudioSink

@OptIn(UnstableApi::class)
class MikasaPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var usbAudioSink: UsbAudioSink? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableOffload: Boolean
            ): AudioSink {
                val hasNativeFlacDecoder = try {
                    Class.forName("androidx.media3.decoder.flac.LibflacAudioRenderer")
                    true
                } catch (_: ClassNotFoundException) {
                    false
                }
                val delegate = DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(!hasNativeFlacDecoder)
                    .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                    .build()

                return UsbAudioSink(delegate, context).also { usbAudioSink = it }
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val loadControl = UsbAudioSink.wrapLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(5_000, 15_000, 2_000, 3_000)
                .build()
        ) {
            usbAudioSink?.isNativeEngineActive == true
        }

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .build()
        usbAudioSink?.attachToPlayer(player)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        player.release()
        super.onDestroy()
    }
}
