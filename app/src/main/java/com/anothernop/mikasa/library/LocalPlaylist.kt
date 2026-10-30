package com.anothernop.mikasa.library

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class LocalPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<Long>
)

data class PlaylistLoadResult(
    val playlists: List<LocalPlaylist>,
    val errorMessage: String?
)

object PlaylistStore {
    private const val PREFERENCES = "mikasa_library"
    private const val PLAYLISTS_KEY = "playlists"

    fun load(context: Context): PlaylistLoadResult {
        val json = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(PLAYLISTS_KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            val playlists = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val trackIds = item.getJSONArray("trackIds")
                    add(
                        LocalPlaylist(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            trackIds = buildList {
                                for (trackIndex in 0 until trackIds.length()) {
                                    add(trackIds.getLong(trackIndex))
                                }
                            }
                        )
                    )
                }
            }
            PlaylistLoadResult(playlists, null)
        } catch (error: JSONException) {
            Log.e(TAG, "Saved playlists could not be read", error)
            PlaylistLoadResult(emptyList(), "Saved playlists could not be read: ${error.message}")
        }
    }

    fun save(context: Context, playlists: List<LocalPlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val trackIds = JSONArray()
            playlist.trackIds.forEach(trackIds::put)
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("trackIds", trackIds)
            )
        }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(PLAYLISTS_KEY, array.toString())
            .apply()
    }

    private const val TAG = "PlaylistStore"
}
