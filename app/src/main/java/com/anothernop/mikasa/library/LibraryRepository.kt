package com.anothernop.mikasa.library

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryRepository(private val context: Context) {
    suspend fun scan(): List<LocalTrack> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val collection = MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
        val tracks = ArrayList<LocalTrack>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                tracks += LocalTrack(
                    id = id,
                    title = cursor.getString(titleColumn)?.takeIf(String::isNotBlank)
                        ?: "Unknown title",
                    artist = cursor.getString(artistColumn)?.takeIf(String::isNotBlank)
                        ?: "Unknown artist",
                    album = cursor.getString(albumColumn)?.takeIf(String::isNotBlank)
                        ?: "Unknown album",
                    durationMs = cursor.getLong(durationColumn),
                    uri = ContentUris.withAppendedId(collection, id)
                )
            }
        }
        tracks
    }
}
