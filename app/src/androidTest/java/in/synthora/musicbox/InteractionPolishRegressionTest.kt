package `in`.synthora.musicbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.ui.components.*
import `in`.synthora.musicbox.ui.components.player.QueueBottomSheetContent
import `in`.synthora.musicbox.ui.screens.HomeContent
import `in`.synthora.musicbox.ui.screens.OutputPickerContent
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.HomeUiState
import `in`.synthora.musicbox.viewmodels.MusicUiState
import `in`.synthora.musicbox.services.EchoDevice
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class InteractionPolishRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val songs = (1..30).map { Track("s$it", "Song $it", "Artist", durationSec = 180, ytVideoId = "video0000$it") }

    @Test fun speedDialHoldOpensSongOptionsWithoutPlayingOrNavigating() {
        val menu = MediaMenuController()
        var plays = 0; var opens = 0
        compose.setContent { JUKETheme { CompositionLocalProvider(LocalMediaMenu provides menu) {
            HomeContent(HomeUiState(recentTracks = songs.take(3), isLoading = false), "", onOpenItem = { opens++ }, onPlayTracks = { _, _ -> plays++ })
        } } }
        compose.onNodeWithText("Song 1").performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals("video00001", menu.item?.videoId ?: menu.track?.ytVideoId)
            assertEquals(0, plays); assertEquals(0, opens)
        }
    }

    @Test fun speedDialTitleIsInsideTheBottomOfItsArtwork() {
        compose.setContent { JUKETheme { HomeContent(HomeUiState(recentTracks = songs.take(3), isLoading = false), "") } }
        val title = compose.onNodeWithText("Song 1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("Speed dial video00001", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(title.top >= card.top && title.bottom <= card.bottom)
        assertTrue(title.center.y > card.center.y)
    }

    @Test fun diagonalVerticalScrollDoesNotTriggerRowActions() {
        var actions = 0
        compose.setContent { JUKETheme { LazyColumn(Modifier.fillMaxSize().testTag("Tracks")) {
            items(songs) { song -> SwipeToAddNextContainer({ actions++ }, { actions++ }) {
                Text(song.title, Modifier.fillMaxWidth().height(64.dp).padding(16.dp))
            } }
        } } }
        compose.onNodeWithTag("Tracks").performTouchInput {
            swipe(Offset(width * .65f, height * .8f), Offset(width * .40f, height * .2f), 400)
        }
        compose.onNodeWithText("Song 1").assertIsNotDisplayed()
        compose.runOnIdle { assertEquals(0, actions) }
    }

    @Test fun diagonalQueueScrollDoesNotDeleteAndSelectingBDoesNotHighlightA() {
        var selected by mutableIntStateOf(0)
        var removals = 0
        compose.setContent { JUKETheme { QueueBottomSheetContent(songs[selected], songs, selected, MusicUiState(), {}, { _, _ -> },
            { _, done -> removals++; done(true) }, { selected = songs.indexOf(it) }) } }
        compose.onNodeWithTag("Queue row s2").performTouchInput { click() }
        compose.onNodeWithTag("Queue row s2").assertIsSelected()
        compose.onNodeWithTag("Queue row s1").assertIsNotSelected()
        compose.onNodeWithTag("Queue rows").performTouchInput {
            swipe(Offset(width * .75f, height * .8f), Offset(width * .45f, height * .2f), 400)
        }
        compose.runOnIdle { assertEquals(0, removals) }
        compose.onNodeWithTag("Queue row s1").assertDoesNotExist()
    }

    @Test fun configuredOfflineEchoDoesNotShowConnectionInstructions() {
        compose.setContent { JUKETheme { OutputPickerContent(false, listOf(EchoDevice("echo", "Echo Dot", false)),
            emptyList(), "this-phone", null, {}, "echo", false, {}, {}, {}) } }
        compose.onNodeWithText("This phone").assertIsDisplayed()
        compose.onNodeWithText("Echo Dot").assertDoesNotExist()
        compose.onNodeWithText("No Echo found. Connect Amazon in Settings to play on an Echo.").assertDoesNotExist()
    }

    @Test fun playlistTitleAndBackArrowShareTheSameCenterAndTitleFadesBothWays() {
        compose.mainClock.autoAdvance = false
        compose.setContent { JUKETheme { Box(Modifier.fillMaxSize()) {
            AdaptiveDetailLayout(0.dp, title = "Playlist title", hero = { Spacer(Modifier.height(240.dp)) }) {
                items((1..50).toList()) { Text("Track $it", Modifier.fillMaxWidth().height(64.dp)) }
            }
            DetailBackButton({}, Modifier.align(Alignment.TopStart), transparent = true)
        } } }
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("Collection tracks").performScrollToIndex(4)
        compose.mainClock.advanceTimeBy(250)
        val title = compose.onNodeWithText("Playlist title").fetchSemanticsNode().boundsInRoot
        val back = compose.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(title.center.y - back.center.y) < 3f)
        compose.onNodeWithTag("Collection tracks").performScrollToIndex(0)
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithText("Playlist title").assertDoesNotExist()
    }
}
