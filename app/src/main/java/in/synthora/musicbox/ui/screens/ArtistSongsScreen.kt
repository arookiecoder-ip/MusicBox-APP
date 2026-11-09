package `in`.synthora.musicbox.ui.screens

import `in`.synthora.musicbox.ui.components.stableStatusBarsPadding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.synthora.musicbox.ui.components.*
import `in`.synthora.musicbox.viewmodels.MusicViewModel
import `in`.synthora.musicbox.viewmodels.SearchViewModel

@Composable
fun ArtistSongsScreen(artistId: String, searchViewModel: SearchViewModel, musicViewModel: MusicViewModel,
    onNavigateBack: () -> Unit, bottomPadding: Dp = 0.dp, videos: Boolean = false) {
    val state by searchViewModel.artistDetailState.collectAsStateWithLifecycle()
    LaunchedEffect(artistId, state.artist?.id, state.isLoading) {
        if (state.artist?.id != artistId) searchViewModel.loadArtistDetailsById(artistId)
        else if (!state.isLoading) { if (videos) searchViewModel.loadAllArtistVideos() else if(state.allTracks.isEmpty()) searchViewModel.loadAllArtistSongs() }
    }
    val songs = if(videos) state.videos else state.allTracks.ifEmpty { state.topTracks }
    val loading=if(videos) state.videosLoading else state.songsLoading
    fun loadMore() { if(videos) searchViewModel.loadAllArtistVideos() else searchViewModel.loadAllArtistSongs() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().stableStatusBarsPadding(), contentPadding = PaddingValues(top = 60.dp, bottom = bottomPadding + 16.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(if(videos) "Music videos" else "Songs", style = MaterialTheme.typography.headlineMedium)
                    Text(state.artist?.title.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = ::loadMore) { Text("Retry") }
                    }
                }
            }
            if ((state.isLoading || loading) && songs.isEmpty()) item { TrackRowsSkeleton() }
            itemsIndexed(songs, key = { index, track -> "$index-${track.ytVideoId}" }) { index, track ->
                if (index >= songs.size - 5 && !(if(videos) state.allVideosLoaded else state.allSongsLoaded) && state.error == null) {
                    LaunchedEffect(songs.size, loading) { if (!loading) loadMore() }
                }
                SwipeToAddNextContainer(onAddNext = { musicViewModel.addNext(track) }, onAddToQueue = { musicViewModel.addToQueue(listOf(track)) }) {
                    FlatTrackRow(track.thumbnailUri, track.title, track.artist,
                        if (track.durationSec > 0) "%d:%02d".format(track.durationSec / 60, track.durationSec % 60) else "",
                        onClick = { musicViewModel.startRadio(track) }, modifier = Modifier.padding(horizontal = 20.dp), track = track)
                }
            }
            if (loading && songs.isNotEmpty()) item { TrackRowsSkeleton(3) }
        }
        DetailBackButton(onNavigateBack, Modifier.align(Alignment.TopStart))
    }
}
