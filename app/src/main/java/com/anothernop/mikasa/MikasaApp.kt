package com.anothernop.mikasa

import android.Manifest
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.anothernop.mikasa.library.LibraryRepository
import com.anothernop.mikasa.library.LocalPlaylist
import com.anothernop.mikasa.library.LocalTrack
import com.anothernop.mikasa.library.OnlineTrackMetadata
import com.anothernop.mikasa.library.OnlineTrackMetadataRepository
import com.anothernop.mikasa.library.PlaylistStore
import com.anothernop.mikasa.library.TrackLyrics
import com.anothernop.mikasa.library.TrackLyricsStore
import com.anothernop.mikasa.playback.PlaybackEqualizer
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.json.JSONException
import java.util.UUID

private val Ink = Color(0xFFFAF9F7)
private val SurfaceColor = Color(0xFFFFFFFF)
private val SurfaceRaised = Color(0xFFFFFFFF)
private val Accent = Color(0xFFE54862)
private val Muted = Color(0xFF77747A)
private val TextPrimary = Color(0xFF211F23)

private enum class MainTab(val label: String) {
    Listen("Listen"),
    Search("Search"),
    Library("Library"),
    Dac("DAC")
}

private enum class LibraryMode(val label: String) {
    Songs("Songs"),
    Artists("Artists"),
    Albums("Albums"),
    Favorites("Favorites"),
    Playlists("Playlists")
}

private data class LibraryGroup(
    val key: String,
    val title: String,
    val detail: String,
    val tracks: List<LocalTrack>,
    val artistArtwork: Boolean = false
)

private data class PlayerSnapshot(
    val title: String,
    val artist: String,
    val hasTrack: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int
)

private data class DacStatus(
    val name: String,
    val supportsUac2: Boolean
)

@Composable
fun MikasaApp(
    player: Player?,
    playbackConnectionError: String?
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    var hasAudioPermission by remember {
        mutableStateOf(context.checkSelfPermission(permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }
    var tracks by remember { mutableStateOf(emptyList<LocalTrack>()) }
    var loading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var scanKey by remember { mutableIntStateOf(0) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Listen) }
    var libraryMode by rememberSaveable { mutableStateOf(LibraryMode.Songs) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibraryGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var trackToAdd by remember { mutableStateOf<LocalTrack?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var lyricsError by remember { mutableStateOf<String?>(null) }
    val playlistLoadResult = remember { PlaylistStore.load(context) }
    var playlistError by remember { mutableStateOf(playlistLoadResult.errorMessage) }
    var playlists by remember { mutableStateOf(playlistLoadResult.playlists) }
    var favorites by remember {
        mutableStateOf(
            context.getSharedPreferences("mikasa_library", Context.MODE_PRIVATE)
                .getStringSet("favorites", emptySet())
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet()
        )
    }
    val snapshot = rememberPlayerSnapshot(player)
    LaunchedEffect(hasAudioPermission, scanKey) {
        if (hasAudioPermission) {
            loading = true
            scanError = null
            try {
                tracks = LibraryRepository(context).scan()
            } catch (error: SecurityException) {
                scanError = "Music access was revoked. Grant access and scan again."
            } catch (error: IllegalArgumentException) {
                scanError = "The music library could not be read: ${error.message}"
            } finally {
                loading = false
            }
        } else {
            tracks = emptyList()
        }
    }

    fun toggleFavorite(trackId: Long) {
        val next = if (trackId in favorites) favorites - trackId else favorites + trackId
        favorites = next
        context.getSharedPreferences("mikasa_library", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("favorites", next.map(Long::toString).toSet())
            .apply()
    }

    fun savePlaylists(next: List<LocalPlaylist>) {
        try {
            PlaylistStore.save(context, next)
            playlists = next
            playlistError = null
        } catch (error: JSONException) {
            playlistError = "Could not save this playlist: ${error.message}"
        }
    }

    fun playQueue(queue: List<LocalTrack>, startTrack: LocalTrack) {
        val activePlayer = player ?: return
        val mediaItems = queue.map { it.toMediaItem() }
        val startIndex = queue.indexOfFirst { it.id == startTrack.id }.coerceAtLeast(0)
        activePlayer.setMediaItems(mediaItems, startIndex, 0L)
        activePlayer.prepare()
        activePlayer.play()
    }

    val filteredSearch = remember(tracks, searchText) {
        val query = searchText.trim()
        if (query.isEmpty()) {
            emptyList()
        } else {
            tracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
            }
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            secondary = Color(0xFF8F536C),
            background = Ink,
            surface = SurfaceColor,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    ) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                Column {
                    if (snapshot.hasTrack) {
                        MiniPlayer(
                            player = player,
                            snapshot = snapshot,
                            onExpand = { showNowPlaying = true }
                        )
                    }
                    NavigationBar(
                        containerColor = SurfaceColor,
                        tonalElevation = 8.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val icon = when (tab) {
                                MainTab.Listen -> Icons.Filled.Home
                                MainTab.Search -> Icons.Filled.Search
                                MainTab.Library -> Icons.Filled.LibraryMusic
                                MainTab.Dac -> Icons.Filled.Usb
                            }
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = {
                                    selectedTab = tab
                                    if (tab != MainTab.Library) {
                                        selectedPlaylistId = null
                                        selectedLibraryGroup = null
                                    }
                                },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = Accent,
                                    selectedTextColor = Accent,
                                    indicatorColor = Accent.copy(alpha = 0.12f),
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (playbackConnectionError != null) {
                    ErrorBanner(playbackConnectionError)
                }
                if (scanError != null) {
                    ErrorBanner(scanError!!)
                }
                if (playlistError != null) {
                    ErrorBanner(playlistError!!)
                }
                if (lyricsError != null) {
                    ErrorBanner(lyricsError!!)
                }

                when (selectedTab) {
                    MainTab.Listen -> ListenNowScreen(
                        tracks = tracks,
                        favorites = favorites,
                        hasAudioPermission = hasAudioPermission,
                        loading = loading,
                        onGrantAccess = { permissionLauncher.launch(permission) },
                        onRefresh = { scanKey++ },
                        onPlay = { playQueue(tracks, it) },
                        onFavorite = ::toggleFavorite
                    )

                    MainTab.Search -> SearchScreen(
                        query = searchText,
                        onQueryChange = { searchText = it },
                        tracks = filteredSearch,
                        favorites = favorites,
                        hasAudioPermission = hasAudioPermission,
                        onGrantAccess = { permissionLauncher.launch(permission) },
                        onPlay = { playQueue(filteredSearch, it) },
                        onFavorite = ::toggleFavorite,
                        onAddToPlaylist = { trackToAdd = it }
                    )

                    MainTab.Library -> LibraryScreen(
                        tracks = tracks,
                        favorites = favorites,
                        playlists = playlists,
                        hasAudioPermission = hasAudioPermission,
                        loading = loading,
                        mode = libraryMode,
                        onModeChange = {
                            libraryMode = it
                            selectedPlaylistId = null
                            selectedLibraryGroup = null
                        },
                        selectedGroupKey = selectedLibraryGroup,
                        onSelectGroup = { selectedLibraryGroup = it },
                        selectedPlaylistId = selectedPlaylistId,
                        onSelectPlaylist = { selectedPlaylistId = it },
                        onCreatePlaylist = { showCreatePlaylist = true },
                        onGrantAccess = { permissionLauncher.launch(permission) },
                        onRefresh = { scanKey++ },
                        onPlay = { queue, start -> playQueue(queue, start) },
                        onFavorite = ::toggleFavorite,
                        onAddToPlaylist = { trackToAdd = it }
                    )

                    MainTab.Dac -> DacScreen()
                }
            }
        }
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylist = false },
            onCreate = { name ->
                val playlist = LocalPlaylist(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    trackIds = emptyList()
                )
                savePlaylists(playlists + playlist)
                selectedPlaylistId = playlist.id
                showCreatePlaylist = false
            }
        )
    }

    val pendingTrack = trackToAdd
    if (pendingTrack != null) {
        AddToPlaylistDialog(
            track = pendingTrack,
            playlists = playlists,
            onDismiss = { trackToAdd = null },
            onSelect = { playlistId ->
                val next = playlists.map { playlist ->
                    if (playlist.id == playlistId) {
                        playlist.copy(trackIds = (playlist.trackIds + pendingTrack.id).distinct())
                    } else {
                        playlist
                    }
                }
                savePlaylists(next)
                trackToAdd = null
            }
        )
    }

    if (showNowPlaying) {
        val currentTrackId = player?.currentMediaItem?.mediaId?.toLongOrNull()
        var currentLyrics by remember(currentTrackId) { mutableStateOf(TrackLyrics()) }
        LaunchedEffect(currentTrackId) {
            lyricsError = null
            if (currentTrackId != null) {
                try {
                    currentLyrics = TrackLyricsStore.load(context, currentTrackId)
                } catch (error: JSONException) {
                    lyricsError = "Could not read saved lyrics: ${error.message}"
                }
            } else {
                currentLyrics = TrackLyrics()
            }
        }
        NowPlayingDialog(
            player = player,
            snapshot = snapshot,
            lyrics = currentLyrics,
            onSaveLyrics = { updated ->
                if (currentTrackId != null) {
                    try {
                        TrackLyricsStore.save(context, currentTrackId, updated)
                        currentLyrics = updated
                        lyricsError = null
                    } catch (error: JSONException) {
                        lyricsError = "Could not save lyrics: ${error.message}"
                    }
                }
            },
            onDismiss = { showNowPlaying = false }
        )
    }
}

@Composable
private fun HomeScreen(
    tracks: List<LocalTrack>,
    favorites: Set<Long>,
    hasAudioPermission: Boolean,
    loading: Boolean,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (LocalTrack) -> Unit,
    onFavorite: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandHeader("Good music, no connection required.") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3B1D37), Color(0xFF17131A), Color(0xFF251A2B))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Text(
                        "MIKASA MIX",
                        color = Accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your library,\nyour own way.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (hasAudioPermission) "${tracks.size} songs stored on this device"
                        else "Listen to the music already on your device",
                        color = Muted
                    )
                    Spacer(Modifier.height(16.dp))
                    if (!hasAudioPermission) {
                        Button(onClick = onGrantAccess) {
                            Text("Allow music access")
                        }
                    } else {
                        OutlinedButton(onClick = onRefresh, enabled = !loading) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (loading) "Scanning..." else "Scan library")
                        }
                    }
                }
            }
        }
        item { SectionTitle("Your library", "${tracks.size} songs") }
        if (tracks.isEmpty() && hasAudioPermission && !loading) {
            item {
                EmptyState(
                    title = "Your library is ready",
                    detail = "Add audio files to your device, then scan your library."
                )
            }
        } else {
            items(tracks.take(8), key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isFavorite = track.id in favorites,
                    onPlay = { onPlay(track) },
                    onFavorite = { onFavorite(track.id) }
                )
            }
        }
    }
}

@Composable
private fun ListenNowScreen(
    tracks: List<LocalTrack>,
    favorites: Set<Long>,
    hasAudioPermission: Boolean,
    loading: Boolean,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (LocalTrack) -> Unit,
    onFavorite: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "MIKASA MUSIC",
                        color = Accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        "Listen Now",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Local music · artwork when online",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Accent, Color(0xFFF3A17D)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (!hasAudioPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            "Your music, all in one place",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Allow access to browse and play songs stored on this device.",
                            color = Muted
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = onGrantAccess) { Text("Allow music access") }
                    }
                }
            }
        } else if (tracks.isEmpty()) {
            item {
                EmptyState(
                    title = if (loading) "Finding your music..." else "Your library is ready",
                    detail = if (loading) "Your on-device songs will appear here."
                    else "Add audio files to your device, then scan your library.",
                    actionLabel = if (loading) null else "Scan library",
                    onAction = if (loading) null else onRefresh
                )
            }
        } else {
            item {
                HomeSectionHeading(
                    title = "Top Picks",
                    detail = "${tracks.size} songs in your library",
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(tracks.take(10), key = { it.id }) { track ->
                        FeaturedTrackCard(
                            track = track,
                            isFavorite = track.id in favorites,
                            onClick = { onPlay(track) },
                            onFavorite = { onFavorite(track.id) }
                        )
                    }
                }
            }
            item {
                HomeSectionHeading(
                    title = "Quick Picks",
                    detail = "From your library",
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(tracks.take(12), key = { "recent-${it.id}" }) { track ->
                        RecentTrackCard(track = track, onClick = { onPlay(track) })
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSectionHeading(
                        title = "Your Songs",
                        detail = "${tracks.size} tracks",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRefresh, enabled = !loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (loading) "Scanning" else "Scan")
                    }
                }
            }
            items(tracks.take(8), key = { "song-${it.id}" }) { track ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    TrackRow(
                        track = track,
                        isFavorite = track.id in favorites,
                        onPlay = { onPlay(track) },
                        onFavorite = { onFavorite(track.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeading(
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.weight(1f))
        Text(detail, color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FeaturedTrackCard(
    track: LocalTrack,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    val palette = coverPalette(track.id)
    val metadata = rememberOnlineTrackMetadata(track)
    Box(
        modifier = Modifier
            .width(258.dp)
            .height(292.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(188.dp)
                .align(Alignment.TopEnd)
                .padding(top = 22.dp, end = 18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.13f))
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Album,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(58.dp)
            )
        }
        metadata?.albumArtUrl?.let { artworkUrl ->
            RemoteArtworkImage(artworkUrl, "Album art for ${track.title}", Modifier.fillMaxSize())
        }
        IconButton(
            onClick = onFavorite,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.16f))
        ) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
                .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 18.dp)
        ) {
            Text(
                "FROM YOUR LIBRARY",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                track.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                metadata?.artist?.takeIf(String::isNotBlank) ?: track.artist,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            metadata?.album?.takeIf(String::isNotBlank)?.let { album ->
                Text(
                    album,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentTrackCard(track: LocalTrack, onClick: () -> Unit) {
    val metadata = rememberOnlineTrackMetadata(track)
    Column(
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onClick)
    ) {
        AlbumTile(track, 132.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            metadata?.artist?.takeIf(String::isNotBlank) ?: track.artist,
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun coverPalette(id: Long): List<Color> = when (Math.floorMod(id, 5L).toInt()) {
    0 -> listOf(Color(0xFFFB9D42), Color(0xFFEB5D40), Color(0xFF783D53))
    1 -> listOf(Color(0xFF3B93A1), Color(0xFF286176), Color(0xFF202C51))
    2 -> listOf(Color(0xFFD66E8B), Color(0xFF914D7E), Color(0xFF49396A))
    3 -> listOf(Color(0xFF9BB14A), Color(0xFF398A72), Color(0xFF245665))
    else -> listOf(Color(0xFFF1B644), Color(0xFFE78348), Color(0xFFAD4C60))
}

@Composable
private fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    tracks: List<LocalTrack>,
    favorites: Set<Long>,
    hasAudioPermission: Boolean,
    onGrantAccess: () -> Unit,
    onPlay: (LocalTrack) -> Unit,
    onFavorite: (Long) -> Unit,
    onAddToPlaylist: (LocalTrack) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        BrandHeader("Find a song, artist, or album.")
        if (!hasAudioPermission) {
            EmptyState(
                title = "Search your music",
                detail = "Allow access to search audio stored on this device.",
                actionLabel = "Allow access",
                onAction = onGrantAccess
            )
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Songs, artists, albums") },
                shape = RoundedCornerShape(18.dp)
            )
            Text(
                if (query.isBlank()) "Your offline library"
                else "${tracks.size} results",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (query.isBlank()) {
                EmptyState(
                    title = "Start typing to search",
                    detail = "Search stays on this device. Nothing is uploaded."
                )
            } else {
                TrackList(
                    tracks = tracks,
                    favorites = favorites,
                    emptyTitle = "No matches",
                    emptyDetail = "Try another title, artist, or album.",
                    onPlay = onPlay,
                    onFavorite = onFavorite,
                    onAddToPlaylist = onAddToPlaylist
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<LocalTrack>,
    favorites: Set<Long>,
    playlists: List<LocalPlaylist>,
    hasAudioPermission: Boolean,
    loading: Boolean,
    mode: LibraryMode,
    onModeChange: (LibraryMode) -> Unit,
    selectedGroupKey: String?,
    onSelectGroup: (String?) -> Unit,
    selectedPlaylistId: String?,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: () -> Unit,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (List<LocalTrack>, LocalTrack) -> Unit,
    onFavorite: (Long) -> Unit,
    onAddToPlaylist: (LocalTrack) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        BrandHeader("Everything you keep close.")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                LibraryMode.entries.forEach { item ->
                    FilterChip(
                        selected = item == mode,
                        onClick = { onModeChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }
            IconButton(onClick = onRefresh, enabled = !loading && hasAudioPermission) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Scan library")
            }
        }

        if (!hasAudioPermission) {
            EmptyState(
                title = "Your music belongs here",
                detail = "Allow access to browse songs stored on this device.",
                actionLabel = "Allow access",
                onAction = onGrantAccess
            )
        } else {
            when (mode) {
                LibraryMode.Songs -> TrackList(
                    tracks = tracks,
                    favorites = favorites,
                    emptyTitle = if (loading) "Scanning your library..." else "No songs yet",
                    emptyDetail = "Add audio to your device and scan again.",
                    onPlay = { onPlay(tracks, it) },
                    onFavorite = onFavorite,
                    onAddToPlaylist = onAddToPlaylist
                )

                LibraryMode.Artists -> {
                    val groups = tracks.groupBy { it.artist }
                        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                        .map { (artist, songs) ->
                            LibraryGroup(
                                artist,
                                artist,
                                "${songs.size} songs",
                                songs,
                                artistArtwork = true
                            )
                        }
                    LibraryGroupsScreen(
                        groups = groups,
                        selectedKey = selectedGroupKey,
                        onSelectGroup = onSelectGroup,
                        favorites = favorites,
                        onPlay = { queue, start -> onPlay(queue, start) },
                        onFavorite = onFavorite,
                        onAddToPlaylist = onAddToPlaylist,
                        emptyTitle = "No artists yet"
                    )
                }

                LibraryMode.Albums -> {
                    val groups = tracks.groupBy { it.album to it.artist }
                        .entries
                        .sortedWith(
                            compareBy<Map.Entry<Pair<String, String>, List<LocalTrack>>> {
                                it.key.first.lowercase()
                            }.thenBy { it.key.second.lowercase() }
                        )
                        .map { (key, songs) ->
                            val albumKey = "${key.first}\u0000${key.second}"
                            LibraryGroup(
                                albumKey,
                                key.first,
                                "${key.second} · ${songs.size} songs",
                                songs
                            )
                        }
                    LibraryGroupsScreen(
                        groups = groups,
                        selectedKey = selectedGroupKey,
                        onSelectGroup = onSelectGroup,
                        favorites = favorites,
                        onPlay = { queue, start -> onPlay(queue, start) },
                        onFavorite = onFavorite,
                        onAddToPlaylist = onAddToPlaylist,
                        emptyTitle = "No albums yet"
                    )
                }

                LibraryMode.Favorites -> {
                    val favoriteTracks = tracks.filter { it.id in favorites }
                    TrackList(
                        tracks = favoriteTracks,
                        favorites = favorites,
                        emptyTitle = "Your favorites will live here",
                        emptyDetail = "Tap the heart beside a song to save it.",
                        onPlay = { onPlay(favoriteTracks, it) },
                        onFavorite = onFavorite,
                        onAddToPlaylist = onAddToPlaylist
                    )
                }

                LibraryMode.Playlists -> {
                    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }
                    if (selectedPlaylist != null) {
                        val playlistTracks = selectedPlaylist.trackIds.mapNotNull { id ->
                            tracks.find { it.id == id }
                        }
                        Column(Modifier.fillMaxSize()) {
                            TextButton(onClick = { onSelectPlaylist(null) }) {
                                Text("Back to playlists")
                            }
                            SectionTitle(selectedPlaylist.name, "${playlistTracks.size} songs")
                            TrackList(
                                tracks = playlistTracks,
                                favorites = favorites,
                                emptyTitle = "This playlist is waiting for a song",
                                emptyDetail = "Add songs from your library using the playlist icon.",
                                onPlay = { onPlay(playlistTracks, it) },
                                onFavorite = onFavorite,
                                onAddToPlaylist = onAddToPlaylist
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            OutlinedButton(
                                onClick = onCreatePlaylist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create playlist")
                            }
                            if (playlists.isEmpty()) {
                                EmptyState(
                                    title = "Make it yours",
                                    detail = "Create a playlist, then add songs from your library."
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 20.dp)
                                ) {
                                    items(playlists, key = { it.id }) { playlist ->
                                        PlaylistRow(
                                            playlist = playlist,
                                            onClick = { onSelectPlaylist(playlist.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryGroupsScreen(
    groups: List<LibraryGroup>,
    selectedKey: String?,
    onSelectGroup: (String?) -> Unit,
    favorites: Set<Long>,
    onPlay: (List<LocalTrack>, LocalTrack) -> Unit,
    onFavorite: (Long) -> Unit,
    onAddToPlaylist: (LocalTrack) -> Unit,
    emptyTitle: String
) {
    val selected = groups.find { it.key == selectedKey }
    if (selected != null) {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = { onSelectGroup(null) }) { Text("Back") }
            SectionTitle(selected.title, selected.detail)
            TrackList(
                tracks = selected.tracks,
                favorites = favorites,
                emptyTitle = "No songs in this collection",
                emptyDetail = "This collection has no playable songs.",
                onPlay = { onPlay(selected.tracks, it) },
                onFavorite = onFavorite,
                onAddToPlaylist = onAddToPlaylist
            )
        }
    } else if (groups.isEmpty()) {
        EmptyState(emptyTitle, "Scan your local music library to browse this collection.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(groups, key = { it.key }) { group ->
                LibraryGroupRow(group, onClick = { onSelectGroup(group.key) })
            }
        }
    }
}

@Composable
private fun LibraryGroupRow(group: LibraryGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumTile(group.tracks.first(), 52.dp, artistArtwork = group.artistArtwork)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(group.title, fontWeight = FontWeight.SemiBold)
            Text(group.detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DacScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var devices by remember { mutableStateOf(findUsbDacs(context)) }
    var equalizerGains by remember {
        mutableStateOf(PlaybackEqualizer.load(context).toList())
    }
    LaunchedEffect(Unit) {
        while (true) {
            devices = findUsbDacs(context)
            delay(1_500)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandHeader("A direct path to your DAC.") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Usb,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "USB audio output",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "On a compatible USB Audio Class 2.0 DAC, MikasaMusic sends audio directly to the device without Android mixer processing.",
                        color = Muted
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "EQ is off by default. Enabling it processes audio in software and disables bit-perfect output.",
                        color = Muted
                    )
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text("Equalizer", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Five-band EQ · changes are saved on this device",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    PlaybackEqualizer.frequenciesHz.forEachIndexed { index, frequency ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (frequency >= 1_000) "${frequency / 1_000} kHz" else "$frequency Hz",
                                modifier = Modifier.width(58.dp),
                                color = Muted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = equalizerGains[index],
                                onValueChange = { value ->
                                    PlaybackEqualizer.setGain(context, index, value)
                                    equalizerGains = equalizerGains.toMutableList()
                                        .also { it[index] = value }
                                },
                                valueRange = -12f..12f,
                                steps = 47,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${equalizerGains[index].toInt()} dB",
                                modifier = Modifier.width(42.dp),
                                color = Muted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            PlaybackEqualizer.frequenciesHz.indices.forEach {
                                PlaybackEqualizer.setGain(context, it, 0f)
                            }
                            equalizerGains = List(PlaybackEqualizer.frequenciesHz.size) { 0f }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset EQ")
                    }
                }
            }
        }
        if (devices.isEmpty()) {
            item {
                EmptyState(
                    title = "No USB audio device connected",
                    detail = "Connect a USB DAC. The device must support USB Audio Class 2.0 for direct output."
                )
            }
        } else {
            items(devices, key = { it.name }) { device ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (device.supportsUac2) Icons.Filled.Usb else Icons.Outlined.Usb,
                            contentDescription = null,
                            tint = if (device.supportsUac2) Accent else Muted
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(device.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (device.supportsUac2) "UAC 2.0 device detected"
                                else "Audio device detected; UAC 2.0 required",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Direct output depends on the phone, USB permissions, and DAC format support. Other output uses Android's standard audio path.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<LocalTrack>,
    favorites: Set<Long>,
    emptyTitle: String,
    emptyDetail: String,
    onPlay: (LocalTrack) -> Unit,
    onFavorite: (Long) -> Unit,
    onAddToPlaylist: ((LocalTrack) -> Unit)?
) {
    if (tracks.isEmpty()) {
        EmptyState(emptyTitle, emptyDetail)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                isFavorite = track.id in favorites,
                onPlay = { onPlay(track) },
                onFavorite = { onFavorite(track.id) },
                onAddToPlaylist = onAddToPlaylist?.let { add -> { add(track) } }
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: LocalTrack,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null
) {
    val metadata = rememberOnlineTrackMetadata(track)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumTile(track, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${metadata?.artist?.takeIf(String::isNotBlank) ?: track.artist}  ·  " +
                    (metadata?.album?.takeIf(String::isNotBlank) ?: track.album),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                tint = if (isFavorite) Accent else Muted,
                modifier = Modifier.size(20.dp)
            )
        }
        if (onAddToPlaylist != null) {
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    Icons.Outlined.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = Muted
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: LocalPlaylist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4A294C), Accent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.LibraryMusic, contentDescription = null)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(playlist.name, fontWeight = FontWeight.SemiBold)
            Text("${playlist.trackIds.size} songs", color = Muted)
        }
    }
}

@Composable
private fun AlbumTile(
    track: LocalTrack,
    size: androidx.compose.ui.unit.Dp,
    artistArtwork: Boolean = false
) {
    val colors = when ((track.id % 4).toInt()) {
        0 -> listOf(Color(0xFF713953), Color(0xFFE18892))
        1 -> listOf(Color(0xFF244C5B), Color(0xFF8AC1B7))
        2 -> listOf(Color(0xFF59436E), Color(0xFFD68B9E))
        else -> listOf(Color(0xFF6D482C), Color(0xFFE3AE69))
    }
    val metadata = rememberOnlineTrackMetadata(track)
    val imageUrl = if (artistArtwork) metadata?.artistImageUrl else metadata?.albumArtUrl
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Album,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.42f)
        )
        if (imageUrl != null) {
            RemoteArtworkImage(
                imageUrl,
                if (artistArtwork) "Artist photo for ${track.artist}" else "Album art for ${track.title}",
                Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun rememberOnlineTrackMetadata(track: LocalTrack): OnlineTrackMetadata? {
    val context = androidx.compose.ui.platform.LocalContext.current
    var metadata by remember(track.id) { mutableStateOf<OnlineTrackMetadata?>(null) }
    LaunchedEffect(track.id, track.title, track.artist) {
        metadata = OnlineTrackMetadataRepository.find(context, track)
    }
    return metadata
}

@Composable
private fun RemoteArtworkImage(url: String, description: String, modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = description,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun BrandHeader(subtitle: String) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Text("M", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "mikasamusic",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyState(
    title: String,
    detail: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(detail, color = Muted, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Text(
        message,
        color = Color(0xFF9D243A),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE9ED))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Playlist name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = SurfaceRaised
    )
}

@Composable
private fun AddToPlaylistDialog(
    track: LocalTrack,
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text("Create a playlist in your Library first.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        track.title,
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onSelect(playlist.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                playlist.name,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        containerColor = SurfaceRaised
    )
}

@Composable
private fun MiniPlayer(
    player: Player?,
    snapshot: PlayerSnapshot,
    onExpand: () -> Unit
) {
    val progress = if (snapshot.durationMs > 0) {
        (snapshot.positionMs.toFloat() / snapshot.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(onClick = onExpand)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 9.dp, end = 4.dp, top = 7.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumTile(
                    LocalTrack(
                        id = player?.currentMediaItem?.mediaId?.toLongOrNull() ?: 0L,
                        title = snapshot.title,
                        artist = snapshot.artist,
                        album = "",
                        durationMs = snapshot.durationMs,
                        uri = android.net.Uri.EMPTY
                    ),
                    42.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        snapshot.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        snapshot.artist,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { player?.seekToPreviousMediaItem() }) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = {
                    if (snapshot.isPlaying) player?.pause() else player?.play()
                }) {
                    Icon(
                        if (snapshot.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                        tint = Accent
                    )
                }
                IconButton(onClick = { player?.seekToNextMediaItem() }) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Next")
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFEDE9E9))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(Accent)
                )
            }
        }
    }
}

@Composable
private fun NowPlayingDialog(
    player: Player?,
    snapshot: PlayerSnapshot,
    lyrics: TrackLyrics,
    onSaveLyrics: (TrackLyrics) -> Unit,
    onDismiss: () -> Unit
) {
    var showLyricsEditor by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AlbumTile(
                    LocalTrack(
                        id = player?.currentMediaItem?.mediaId?.toLongOrNull() ?: 0L,
                        title = snapshot.title,
                        artist = snapshot.artist,
                        album = "",
                        durationMs = snapshot.durationMs,
                        uri = android.net.Uri.EMPTY
                    ),
                    190.dp
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    snapshot.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(snapshot.artist, color = Muted)
                Spacer(Modifier.height(14.dp))
                val duration = snapshot.durationMs.coerceAtLeast(1L)
                Slider(
                    value = (snapshot.positionMs.toFloat() / duration).coerceIn(0f, 1f),
                    onValueChange = { fraction ->
                        player?.seekTo((fraction * duration).toLong())
                    },
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = Accent,
                        activeTrackColor = Accent
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(snapshot.positionMs), color = Muted)
                    Text(formatTime(snapshot.durationMs), color = Muted)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player?.shuffleModeEnabled = !snapshot.shuffleEnabled }) {
                        Icon(
                            Icons.Outlined.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (snapshot.shuffleEnabled) Accent else Muted
                        )
                    }
                    IconButton(onClick = { player?.seekToPreviousMediaItem() }) {
                        Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
                    }
                    IconButton(
                        onClick = {
                            if (snapshot.isPlaying) player?.pause() else player?.play()
                        },
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Accent)
                    ) {
                        Icon(
                            if (snapshot.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { player?.seekToNextMediaItem() }) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
                    }
                    IconButton(
                        onClick = {
                            player?.repeatMode = when (snapshot.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Repeat,
                            contentDescription = "Repeat",
                            tint = if (snapshot.repeatMode == Player.REPEAT_MODE_OFF) Muted else Accent
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showLyricsEditor = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lyrics · translation · romaji · kanji")
                }
            }
        }
    }
    if (showLyricsEditor) {
        LyricsEditorDialog(
            title = snapshot.title,
            lyrics = lyrics,
            onDismiss = { showLyricsEditor = false },
            onSave = {
                onSaveLyrics(it)
                showLyricsEditor = false
            }
        )
    }
}

@Composable
private fun LyricsEditorDialog(
    title: String,
    lyrics: TrackLyrics,
    onDismiss: () -> Unit,
    onSave: (TrackLyrics) -> Unit
) {
    var original by remember(lyrics) { mutableStateOf(lyrics.lyrics) }
    var translation by remember(lyrics) { mutableStateOf(lyrics.translation) }
    var romaji by remember(lyrics) { mutableStateOf(lyrics.romaji) }
    var kanji by remember(lyrics) { mutableStateOf(lyrics.kanji) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = SurfaceRaised) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Lyrics for $title", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Enter or paste these offline. Translation and romanization are not generated automatically.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                LyricsTextField("Original lyrics", original) { original = it }
                LyricsTextField("Translation", translation) { translation = it }
                LyricsTextField("Romaji", romaji) { romaji = it }
                LyricsTextField("Kanji", kanji) { kanji = it }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            onSave(
                                TrackLyrics(
                                    lyrics = original,
                                    translation = translation,
                                    romaji = romaji,
                                    kanji = kanji
                                )
                            )
                        }
                    ) {
                        Text("Save offline")
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6
    )
}

@Composable
private fun rememberPlayerSnapshot(player: Player?): PlayerSnapshot {
    var snapshot by remember(player) { mutableStateOf(player.toSnapshot()) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = player.toSnapshot()
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (player != null) {
            snapshot = player.toSnapshot()
            delay(400)
        }
    }
    return snapshot
}

private fun Player?.toSnapshot(): PlayerSnapshot {
    val metadata = this?.mediaMetadata
    val title = metadata?.title?.toString()?.takeIf(String::isNotBlank)
    val artist = metadata?.artist?.toString()?.takeIf(String::isNotBlank)
    val duration = this?.duration?.takeIf { it > 0L } ?: 0L
    return PlayerSnapshot(
        title = title ?: "Nothing playing",
        artist = artist ?: "Choose a song from your library",
        hasTrack = this?.currentMediaItem != null,
        isPlaying = this?.isPlaying == true,
        positionMs = this?.currentPosition?.coerceAtLeast(0L) ?: 0L,
        durationMs = duration,
        shuffleEnabled = this?.shuffleModeEnabled == true,
        repeatMode = this?.repeatMode ?: Player.REPEAT_MODE_OFF
    )
}

private fun LocalTrack.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()

private fun findUsbDacs(context: Context): List<DacStatus> {
    val usbManager = context.getSystemService(UsbManager::class.java)
    return usbManager.deviceList.values.mapNotNull { device ->
        val audioInterfaces = (0 until device.interfaceCount)
            .map(device::getInterface)
            .filter { it.interfaceClass == UsbConstants.USB_CLASS_AUDIO }
        if (audioInterfaces.isEmpty()) {
            null
        } else {
            DacStatus(
                name = device.productName?.takeIf(String::isNotBlank) ?: "USB audio device",
                supportsUac2 = audioInterfaces.any { it.interfaceProtocol == 0x20 }
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
