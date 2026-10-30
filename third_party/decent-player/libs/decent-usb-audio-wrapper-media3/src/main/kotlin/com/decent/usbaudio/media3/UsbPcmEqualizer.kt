package com.decent.usbaudio.media3

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal class UsbPcmEqualizer {
    private data class Coefficients(
        val b0: Float,
        val b1: Float,
        val b2: Float,
        val a1: Float,
        val a2: Float
    )

    @Volatile
    private var gainsDb = FloatArray(CENTERS_HZ.size)
    private var coefficients = emptyList<Coefficients>()
    private var states = emptyArray<FloatArray>()
    private var configuredRate = 0
    private var configuredChannels = 0
    private var configuredGains = FloatArray(0)

    val isEnabled: Boolean
        get() = gainsDb.any { it != 0f }

    fun setGainsDb(gains: FloatArray) {
        require(gains.size == CENTERS_HZ.size) { "Expected five equalizer bands." }
        require(gains.all(Float::isFinite)) { "Equalizer gains must be finite." }
        gainsDb = gains.map { it.coerceIn(-12f, 12f) }.toFloatArray()
    }

    fun process(samples: FloatArray, sampleRate: Int, channels: Int) {
        if (sampleRate <= 0 || channels <= 0 || samples.size % channels != 0) return
        val currentGains = gainsDb
        if (configuredRate != sampleRate ||
            configuredChannels != channels ||
            !configuredGains.contentEquals(currentGains)
        ) {
            coefficients = currentGains.mapIndexed { index, gain ->
                peakingFilter(CENTERS_HZ[index], gain, sampleRate)
            }
            states = Array(CENTERS_HZ.size * channels) { FloatArray(2) }
            configuredRate = sampleRate
            configuredChannels = channels
            configuredGains = currentGains.copyOf()
        }

        for (frame in samples.indices step channels) {
            for (channel in 0 until channels) {
                var sample = samples[frame + channel]
                for (band in coefficients.indices) {
                    val filter = coefficients[band]
                    val state = states[band * channels + channel]
                    val output = filter.b0 * sample + state[0]
                    state[0] = filter.b1 * sample - filter.a1 * output + state[1]
                    state[1] = filter.b2 * sample - filter.a2 * output
                    sample = output
                }
                samples[frame + channel] = sample
            }
        }
    }

    private fun peakingFilter(centerHz: Int, gainDb: Float, sampleRate: Int): Coefficients {
        val frequency = centerHz.coerceAtMost((sampleRate * 0.45f).toInt()).coerceAtLeast(1)
        val omega = 2.0 * PI * frequency / sampleRate
        val alpha = sin(omega) / (2.0 * QUALITY)
        val amplitude = 10.0.pow(gainDb / 40.0)
        val a0 = 1.0 + alpha / amplitude
        return Coefficients(
            b0 = ((1.0 + alpha * amplitude) / a0).toFloat(),
            b1 = ((-2.0 * cos(omega)) / a0).toFloat(),
            b2 = ((1.0 - alpha * amplitude) / a0).toFloat(),
            a1 = ((-2.0 * cos(omega)) / a0).toFloat(),
            a2 = ((1.0 - alpha / amplitude) / a0).toFloat()
        )
    }

    private companion object {
        val CENTERS_HZ = intArrayOf(60, 250, 1_000, 4_000, 16_000)
        const val QUALITY = 1.0
    }
}
