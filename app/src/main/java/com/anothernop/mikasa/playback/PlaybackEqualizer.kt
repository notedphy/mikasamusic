package com.anothernop.mikasa.playback

import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

object PlaybackEqualizer {
    val frequenciesHz = listOf(60, 250, 1_000, 4_000, 16_000)
    private val listeners = CopyOnWriteArrayList<(FloatArray) -> Unit>()
    @Volatile private var gains = FloatArray(frequenciesHz.size)

    fun load(context: Context): FloatArray {
        val values = context.getSharedPreferences("mikasa_equalizer", Context.MODE_PRIVATE)
            .getString("gains", null)
            ?.split(",")
            ?.mapNotNull(String::toFloatOrNull)
        if (values != null && values.size == frequenciesHz.size) {
            gains = values.map { it.coerceIn(-12f, 12f) }.toFloatArray()
        }
        return gains.copyOf()
    }

    fun currentGains(): FloatArray = gains.copyOf()

    fun setGain(context: Context, band: Int, gainDb: Float) {
        val next = currentGains()
        next[band] = gainDb.coerceIn(-12f, 12f)
        gains = next
        context.getSharedPreferences("mikasa_equalizer", Context.MODE_PRIVATE)
            .edit()
            .putString("gains", next.joinToString(","))
            .apply()
        listeners.forEach { it(next.copyOf()) }
    }

    fun addListener(listener: (FloatArray) -> Unit) {
        listeners += listener
        listener(currentGains())
    }

    fun removeListener(listener: (FloatArray) -> Unit) {
        listeners -= listener
    }
}
