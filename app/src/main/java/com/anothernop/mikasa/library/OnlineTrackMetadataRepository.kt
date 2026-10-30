package com.anothernop.mikasa.library

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

data class OnlineTrackMetadata(
    val artist: String,
    val album: String,
    val albumArtUrl: String?,
    val artistImageUrl: String?
)

object OnlineTrackMetadataRepository {
    private const val TAG = "OnlineTrackMetadata"
    private const val CACHE_NAME = "mikasa_online_metadata"
    private const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1_000
    private const val NEGATIVE_CACHE_TTL_MS = 24L * 60 * 60 * 1_000

    suspend fun find(context: Context, track: LocalTrack): OnlineTrackMetadata? =
        withContext(Dispatchers.IO) {
            if (track.id <= 0L || track.title.isBlank()) return@withContext null

            val preferences = context.applicationContext
                .getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
            val cacheKey = track.id.toString()
            val cached = preferences.getString(cacheKey, null)
            if (cached != null) {
                try {
                    val json = JSONObject(cached)
                    val ageMs = System.currentTimeMillis() - json.optLong("cachedAt")
                    val ttl = if (json.optBoolean("found")) CACHE_TTL_MS else NEGATIVE_CACHE_TTL_MS
                    if (ageMs in 0..ttl) {
                        return@withContext json.toMetadata()
                    }
                } catch (error: JSONException) {
                    Log.w(TAG, "Ignoring invalid cached metadata for track ${track.id}", error)
                }
            }

            try {
                val metadata = requestMetadata(track)
                val cacheValue = JSONObject()
                    .put("found", metadata != null)
                    .put("cachedAt", System.currentTimeMillis())
                if (metadata != null) {
                    cacheValue
                        .put("artist", metadata.artist)
                        .put("album", metadata.album)
                        .put("albumArtUrl", metadata.albumArtUrl)
                        .put("artistImageUrl", metadata.artistImageUrl)
                }
                if (!preferences.edit().putString(cacheKey, cacheValue.toString()).commit()) {
                    Log.w(TAG, "Could not cache online metadata for track ${track.id}")
                }
                metadata
            } catch (error: IOException) {
                Log.w(TAG, "Online metadata lookup failed for track ${track.id}", error)
                null
            } catch (error: JSONException) {
                Log.w(TAG, "Could not parse online metadata for track ${track.id}", error)
                null
            }
        }

    private fun requestMetadata(track: LocalTrack): OnlineTrackMetadata? {
        val query = listOf(track.title, track.artist)
            .filter { it.isNotBlank() && !it.startsWith("Unknown", ignoreCase = true) }
            .joinToString(" ")
        if (query.isBlank()) return null

        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val connection = URL("https://api.deezer.com/search/track?q=$encodedQuery&limit=10")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MikasaMusic/1.0 (Android)")
            if (connection.responseCode !in 200..299) {
                throw IOException("Deezer returned HTTP ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val results = JSONObject(response).optJSONArray("data") ?: return null
            val expectedTitle = normalize(track.title)
            val expectedArtist = if (track.artist.startsWith("Unknown", ignoreCase = true)) {
                ""
            } else {
                normalize(track.artist)
            }

            return (0 until results.length())
                .asSequence()
                .mapNotNull { index -> results.optJSONObject(index) }
                .mapNotNull { result ->
                    val remoteTitle = result.optString("title")
                    val artistObject = result.optJSONObject("artist") ?: return@mapNotNull null
                    val remoteArtist = artistObject.optString("name")
                    if (!matches(expectedTitle, normalize(remoteTitle)) ||
                        (expectedArtist.isNotBlank() &&
                            !matches(expectedArtist, normalize(remoteArtist)))
                    ) {
                        return@mapNotNull null
                    }

                    val albumObject = result.optJSONObject("album")
                    OnlineTrackMetadata(
                        artist = remoteArtist,
                        album = albumObject?.optString("title").orEmpty(),
                        albumArtUrl = albumObject?.optString("cover_xl")
                            ?.takeIf(String::isNotBlank),
                        artistImageUrl = artistObject.optString("picture_big")
                            .takeIf(String::isNotBlank)
                    )
                }
                .firstOrNull()
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toMetadata(): OnlineTrackMetadata? {
        if (!optBoolean("found")) return null
        return OnlineTrackMetadata(
            artist = optString("artist"),
            album = optString("album"),
            albumArtUrl = optString("albumArtUrl").takeIf(String::isNotBlank),
            artistImageUrl = optString("artistImageUrl").takeIf(String::isNotBlank)
        )
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]"), "")

    private fun matches(expected: String, actual: String): Boolean =
        expected.isNotBlank() && actual.isNotBlank() &&
            (expected == actual ||
                (minOf(expected.length, actual.length) >= 8 &&
                    (expected.startsWith(actual) || actual.startsWith(expected))))
}
