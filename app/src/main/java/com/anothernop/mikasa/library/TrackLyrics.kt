package com.anothernop.mikasa.library

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

data class TrackLyrics(
    val lyrics: String = "",
    val translation: String = "",
    val romaji: String = "",
    val kanji: String = ""
)

object TrackLyricsStore {
    fun load(context: Context, trackId: Long): TrackLyrics {
        val stored = context.getSharedPreferences("mikasa_lyrics", Context.MODE_PRIVATE)
            .getString(trackId.toString(), null) ?: return TrackLyrics()
        val json = JSONObject(stored)
        return TrackLyrics(
            lyrics = json.optString("lyrics"),
            translation = json.optString("translation"),
            romaji = json.optString("romaji"),
            kanji = json.optString("kanji")
        )
    }

    @Throws(JSONException::class)
    fun save(context: Context, trackId: Long, lyrics: TrackLyrics) {
        val json = JSONObject()
            .put("lyrics", lyrics.lyrics)
            .put("translation", lyrics.translation)
            .put("romaji", lyrics.romaji)
            .put("kanji", lyrics.kanji)
        val saved = context.getSharedPreferences("mikasa_lyrics", Context.MODE_PRIVATE)
            .edit()
            .putString(trackId.toString(), json.toString())
            .commit()
        if (!saved) throw JSONException("The lyrics could not be written to local storage.")
    }
}
