package `in`.synthora.musicbox

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.network.BrowseParser
import `in`.synthora.musicbox.services.*
import `in`.synthora.musicbox.ui.components.*
import `in`.synthora.musicbox.ui.components.player.QueueBottomSheetContent
import `in`.synthora.musicbox.ui.screens.HomeContent
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.HomeUiState
import `in`.synthora.musicbox.viewmodels.MusicUiState
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@UnstableApi
class QueueSelectionRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val songs = (0..2).map { Track("song$it", "Song $it", "Artist", durationSec=180, ytVideoId="video00000$it") }

    @Test fun queueEntrySnapshotCannotAttachThePreviousAggregateTitleToTheNewSong() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val player=ExoPlayer.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
            try {
                player.setMediaItems(songs.map { song -> MediaItem.Builder().setMediaId(song.uuid).setUri("file:///missing-${song.uuid}.wav")
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()).build() })
                player.seekTo(1,0)
                val lagging=object:ForwardingPlayer(player) {
                    override fun getMediaMetadata(): MediaMetadata = MediaMetadata.Builder().setTitle("Song 0").build()
                }
                val snapshot=phonePlaybackSnapshot(lagging)
                assertEquals("song1",snapshot.currentId)
                assertEquals("Song 1",snapshot.currentMetadata?.title)
                player.pause()
                assertTrue(playQueueEntry(player,1))
                assertTrue(player.playWhenReady)
                player.pause()
                assertTrue(playQueueEntry(player,2))
                assertTrue(player.playWhenReady)
                player.pause()
                assertFalse(playQueueEntry(player,99))
                assertFalse(player.playWhenReady)
            } finally { player.release() }
        }
    }

    @Test fun stalePreparedRowsCannotReplaceTitlesOrBlockRepeatedTaps() {
        var rows by mutableStateOf(songs)
        var selected by mutableIntStateOf(0)
        var clicks=0
        val stale=PreparedQueueRows.prepare(songs)
        compose.setContent { JUKETheme { Surface(Modifier.fillMaxWidth().height(480.dp)) {
            QueueBottomSheetContent(rows[selected],rows,selected,MusicUiState(),{}, {_,_->}, {_,_->}, {},
                preparedRows=stale,onPlayEntry={_,index-> selected=index;clicks++ })
        } } }
        compose.runOnIdle { rows=songs.map { it.copy(title="Updated ${it.title}") } }
        compose.onNodeWithText("Updated Song 1").assertExists()
        repeat(6) {
            val index=1+it%2
            compose.onNodeWithTag("Queue row song$index").performTouchInput {
                down(center);moveBy(Offset(1f,1f));up()
            }
            compose.onNodeWithTag("Queue row song$index").assertIsSelected()
        }
        compose.runOnIdle { assertEquals(6,clicks) }
        compose.onNodeWithText("Song 0",substring=false).assertDoesNotExist()
    }

    @Test fun speedDialHoldUsesSongActionsAndDismissThenTapStillPlays() {
        val menu=MediaMenuController()
        var plays=0
        compose.setContent { JUKETheme { CompositionLocalProvider(LocalMediaMenu provides menu) {
            HomeContent(HomeUiState(recentTracks=songs,isLoading=false),"",onPlayTracks={_,_->plays++})
        } } }
        compose.onNodeWithTag("Speed dial video000001").performTouchInput { longClick() }
        compose.runOnIdle {
            assertNotNull(menu.track);assertNull(menu.item)
            assertEquals("Song 1",menu.track?.title)
            assertEquals("video000001",menu.track?.ytVideoId)
            assertEquals(0,plays);menu.dismiss()
        }
        compose.onNodeWithTag("Speed dial video000001").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1,plays) }
        val playlist=BrowseParser.item(buildJsonObject { put("playlistId","PLfixture");put("title","Playlist") })
        compose.runOnIdle { menu.show(playlist);assertNotNull(menu.item);assertNull(menu.track) }
    }
    @Test fun updatedCardCallbacksAndNavigationSurviveStateChangesBetweenDownAndUp() {
        var revision by mutableIntStateOf(0)
        var selected by mutableIntStateOf(0)
        var cardResult=-1
        var tabClicks=0
        compose.setContent { JUKETheme { Column {
            val currentRevision=revision
            CompactTrackCard(songs[0],onClick={cardResult=currentRevision})
            GlassNavBar((0..2).map { index -> GlassNavItem("Tab $index",selected==index,
                {selected=index;tabClicks++},icon={}) })
        } } }
        compose.runOnIdle { revision=7 }
        compose.onNodeWithText("Song 0").performTouchInput { click() }
        compose.runOnIdle { assertEquals(7,cardResult) }
        repeat(6) { attempt ->
            val index=1+attempt%2
            compose.onNodeWithText("Tab $index").performTouchInput { down(center) }
            compose.runOnIdle { revision++ }
            compose.onNodeWithText("Tab $index").performTouchInput { up() }
            compose.runOnIdle { assertEquals(index,selected) }
        }
        compose.runOnIdle { assertEquals(6,tabClicks) }
    }

}
