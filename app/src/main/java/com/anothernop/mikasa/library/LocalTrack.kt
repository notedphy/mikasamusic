package com.anothernop.mikasa.library

import android.net.Uri

data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri
)
