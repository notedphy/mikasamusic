package com.anothernop.mikasa.playback

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
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
    companion object {
        private const val TAG = "MikasaPlaybackService"
    }

    private var mediaSession: MediaSession? = null
    private var usbAudioSink: UsbAudioSink? = null
    private var equalizerEffect: Equalizer? = null
    private var equalizerSessionId = 0
    private lateinit var player: ExoPlayer
    private val equalizerListener: (FloatArray) -> Unit = ::applyEqualizerSettings
    private val playerListener = object : androidx.media3.common.Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) applyEqualizerSettings(PlaybackEqualizer.currentGains())
        }
    }

    override fun onCreate() {
        super.onCreate()
        PlaybackEqualizer.load(this)

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
        player.addListener(playerListener)
        PlaybackEqualizer.addListener(equalizerListener)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun applyEqualizerSettings(gainsDb: FloatArray) {
        usbAudioSink?.setEqualizerGainsDb(gainsDb)
        val sessionId = player.audioSessionId
        if (sessionId <= 0) return
        if (equalizerSessionId != sessionId) {
            equalizerEffect?.release()
            equalizerEffect = null
            equalizerSessionId = 0
            try {
                equalizerEffect = Equalizer(0, sessionId).also { effect ->
                    effect.enabled = true
                }
                equalizerSessionId = sessionId
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "System equalizer rejected audio session $sessionId", error)
            } catch (error: IllegalStateException) {
                Log.w(TAG, "System equalizer could not initialize audio session $sessionId", error)
            } catch (error: UnsupportedOperationException) {
                Log.w(TAG, "System equalizer is not supported on this device", error)
            } catch (error: SecurityException) {
                Log.w(TAG, "System equalizer permission was denied", error)
            }
        }
        val effect = equalizerEffect ?: return
        try {
            val range = effect.bandLevelRange
            val bandCount = effect.numberOfBands.toInt()
            for (band in 0 until bandCount) {
                val centerHz = effect.getCenterFreq(band.toShort()) / 1_000
                val target = PlaybackEqualizer.frequenciesHz.indices.minBy { index ->
                    kotlin.math.abs(PlaybackEqualizer.frequenciesHz[index] - centerHz)
                }
                val level = (gainsDb[target] * 100f)
                    .toInt()
                    .coerceIn(range[0].toInt(), range[1].toInt())
                    .toShort()
                effect.setBandLevel(band.toShort(), level)
            }
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Could not apply equalizer settings", error)
        } catch (error: IllegalStateException) {
            Log.w(TAG, "System equalizer became unavailable", error)
        } catch (error: UnsupportedOperationException) {
            Log.w(TAG, "System equalizer settings are unsupported", error)
        } catch (error: SecurityException) {
            Log.w(TAG, "System equalizer permission was denied while applying settings", error)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        PlaybackEqualizer.removeListener(equalizerListener)
        player.removeListener(playerListener)
        equalizerEffect?.release()
        equalizerEffect = null
        mediaSession?.release()
        player.release()
        super.onDestroy()
    }
}
