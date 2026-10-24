package `in`.synthora.musicbox

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
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

    @Test fun intermediatePlaybackFramesNeverShowAInsideBRow() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        fun image(name:String,color:Int):String {
            val file=java.io.File(context.cacheDir,"queue-$name-${System.nanoTime()}.png")
            val bitmap=android.graphics.Bitmap.createBitmap(32,32,android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(color)
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
            return android.net.Uri.fromFile(file).toString()
        }
        val a=songs[0].copy(thumbnailUri=image("a",android.graphics.Color.RED))
        val b=songs[1].copy(thumbnailUri=image("b",android.graphics.Color.BLUE))
        val canonical=listOf(a,b)
        val known=canonical.associateBy { it.uuid }
        val presenter=TrackQueuePresentation()
        var snapshot by mutableStateOf(PhonePlaybackSnapshot(canonical.map { it.uuid },a.uuid,a))
        var remote by mutableStateOf(false)
        val emissions=mutableListOf<List<Track>>()
        compose.setContent { JUKETheme { Surface(Modifier.fillMaxWidth().height(420.dp)) {
            val resolved=if(remote) canonical else snapshot.resolveTracks(known)
            val rows=presenter.present(resolved,active=snapshot.currentMetadata)
            val index=canonical.indexOfFirst { it.uuid==snapshot.currentId }
            SideEffect { emissions += rows }
            QueueBottomSheetContent(canonical[index],rows,index,MusicUiState(),{}, {_,_->}, {_,_->}, {})
        } } }
        fun assertBlue() {
            val pixels=compose.onNodeWithTag("Queue artwork ${b.uuid}",useUnmergedTree=true).captureToImage().toPixelMap()
            val color=pixels[5.coerceAtMost(pixels.width-1),5.coerceAtMost(pixels.height-1)]
            assertTrue("B must retain its blue artwork, got $color",color.blue>color.red+.2f)
        }
        compose.waitUntil(7000) {
            val pixels=compose.onNodeWithTag("Queue artwork ${b.uuid}",useUnmergedTree=true).captureToImage().toPixelMap()
            val c=pixels[5.coerceAtMost(pixels.width-1),5.coerceAtMost(pixels.height-1)]
            c.blue>c.red+.2f
        }
        // Stop the clock so assertions inspect the intermediate frame rather than
        // letting a settled final B update hide the transient corrupt row.
        compose.mainClock.autoAdvance=false
        for (remoteMode in listOf(false,true)) {
            compose.runOnIdle { remote=remoteMode }
            repeat(4) { attempt ->
                val current=if(attempt%2==0)b else a
                val prior=if(attempt%2==0)a else b
                compose.runOnIdle { snapshot=PhonePlaybackSnapshot(canonical.map { it.uuid },current.uuid,
                    prior.copy(uuid=current.uuid,ytVideoId=current.ytVideoId)) }
                compose.mainClock.advanceTimeByFrame()
                compose.onNodeWithTag("Queue row ${b.uuid}").assert(hasAnyDescendant(hasText("Song 1",substring=false)))
                assertBlue()
                compose.runOnIdle { snapshot=PhonePlaybackSnapshot(canonical.map { it.uuid },current.uuid,current) }
                compose.mainClock.advanceTimeByFrame()
                assertBlue()
            }
        }
        compose.runOnIdle {
            assertTrue(emissions.isNotEmpty())
            assertTrue(emissions.all { rows -> rows.map { it.title }==canonical.map { it.title } &&
                rows.map { it.thumbnailUri }==canonical.map { it.thumbnailUri } })
        }
        compose.mainClock.autoAdvance=true
    }

    @Test fun queueTapSurvivesPlaybackAndEditingFlagsChangingWhileFingerIsDown() {
        var selected by mutableIntStateOf(0)
        var busy by mutableStateOf(false)
        var clicks=0
        compose.setContent { JUKETheme { Surface(Modifier.fillMaxWidth().height(480.dp)) {
            QueueBottomSheetContent(songs[selected],songs,selected,MusicUiState(isQueueOperationInProgress=busy),{}, {_,_->}, {_,_->}, {},
                onPlayEntry={_,index->selected=index;clicks++})
        } } }
        repeat(6) { attempt ->
            val row=compose.onNodeWithTag("Queue row song1")
            row.performTouchInput { down(center) }
            compose.runOnIdle { busy=!busy;selected=if(attempt%2==0) 1 else 0 }
            row.performTouchInput { moveBy(Offset(1f,1f));up() }
            compose.runOnIdle { assertEquals(attempt+1,clicks); assertEquals(1,selected) }
        }
    }

    @Test fun playlistDownloadBadgeSurvivesCompletionCancellationAndRestartBetweenFrames() {
        val progress = (0..2).map { mutableStateOf<Int?>(25) }
        val downloaded = (0..2).map { mutableStateOf(false) }
        compose.setContent { JUKETheme { Column {
            progress.forEachIndexed { index, value ->
                Box(Modifier.size(40.dp).testTag("download-$index")) {
                    DownloadStateBadge(downloaded[index].value, value.value)
                }
            }
        } } }
        compose.mainClock.autoAdvance=false
        repeat(12) { step ->
            compose.runOnIdle {
                progress.forEachIndexed { index, value ->
                    value.value=when((step+index)%4) { 0->null;1-> -1;2->100;else->70 }
                    downloaded[index].value=value.value==null
                }
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            }
            // Semantics are queried while the preceding frame can still be attached.
            (0..2).forEach { compose.onNodeWithTag("download-$it").fetchSemanticsNode() }
            compose.mainClock.advanceTimeByFrame()
        }
        compose.runOnIdle { progress.forEach { it.value=null };downloaded.forEach { it.value=true } }
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithContentDescription("Downloaded").assertCountEquals(3)
    }

}
