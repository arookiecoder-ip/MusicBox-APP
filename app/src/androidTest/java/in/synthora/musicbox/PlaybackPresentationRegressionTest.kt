package `in`.synthora.musicbox

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.ui.components.MiniPlayerTransport
import `in`.synthora.musicbox.ui.components.MiniPlayerSkeleton
import `in`.synthora.musicbox.ui.components.player.QueueBottomSheetContent
import `in`.synthora.musicbox.ui.theme.ExtractedColors
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.MusicUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlaybackPresentationRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun miniPlayerSkeletonHasAStableFootprintAndNoPlaybackAction() {
        compose.setContent { JUKETheme { MiniPlayerSkeleton() } }
        val skeleton = compose.onNodeWithTag("Mini player loading")
        skeleton.assertIsDisplayed().assertHeightIsAtLeast(62.dp).assertHasNoClickAction()
        compose.onNodeWithContentDescription("Loading playback").assertExists()
        assertFalse(skeleton.fetchSemanticsNode().config.contains(SemanticsProperties.Selected))
    }

    @Test fun miniPlayerTransportRemainsClickableWhileLoadingAndAnimating() {
        var loading by mutableStateOf(true)
        var playing by mutableStateOf(false)
        var clicks = 0
        compose.setContent { JUKETheme {
            MiniPlayerTransport(playing, loading, { clicks++ }, Modifier.testTag("mini transport"))
        } }
        val control = compose.onNodeWithTag("mini transport")
        control.assertIsEnabled().assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        control.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, clicks); loading = false }
        compose.onNodeWithContentDescription("Play").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(2, clicks); playing = true }
        control.assertIsEnabled().assertWidthIsEqualTo(48.dp).performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(3, clicks) }
    }

    @Test fun queueUsesArtworkColorsAndBlendsThemWhenTheSelectedSongChanges() {
        val songs = listOf(Track("red-song", "Red song", "Artist", durationSec = 180), Track("blue-song", "Blue song", "Artist", durationSec = 180))
        fun palette(color: Color) = ExtractedColors(primary = color, secondary = color, tertiary = color, background = color)
        var state by mutableStateOf(MusicUiState(currentTrack = songs[0], queue = songs, queueIndex = 0,
            extractedColors = palette(Color.Red)))
        compose.setContent { JUKETheme { Surface(Modifier.fillMaxWidth().height(500.dp)) {
            QueueBottomSheetContent(requireNotNull(state.currentTrack), songs, state.queueIndex, state,
                {}, { _, _ -> }, { _, _ -> }, {})
        } } }
        fun backdrop() = compose.onNodeWithTag("Ambient backdrop").captureToImage().toPixelMap()[15, 15]
        fun row(id: String) = compose.onNodeWithTag("Queue row surface $id", useUnmergedTree = true)
            .captureToImage().toPixelMap()[15, 5]
        compose.waitUntil(5000) { backdrop().let { it.red > it.blue + .1f } }
        val oldBackdrop = backdrop()
        val oldRow = row("red-song")
        val ordinaryRow=row("blue-song")
        assertTrue("Artwork blend must remain visible beneath ordinary queue rows", ordinaryRow.red > ordinaryRow.blue + .08f)
        compose.onNodeWithTag("Queue row red-song").assertIsSelected()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state = state.copy(currentTrack = songs[1], queueIndex = 1, extractedColors = palette(Color.Blue)) }
        compose.waitUntil(5000) {
            compose.mainClock.advanceTimeByFrame()
            backdrop().let { it.blue > oldBackdrop.blue + .005f && it.red > .05f }
        }
        compose.mainClock.advanceTimeBy(300)
        val middle = backdrop()
        assertTrue("Previous artwork remains visible during the transition", middle.red > .02f)
        assertTrue("New artwork is blended during the transition", middle.blue > oldBackdrop.blue + .02f)
        compose.mainClock.advanceTimeBy(1800)
        val end = backdrop()
        assertTrue("Queue follows the new blue artwork", end.blue > end.red + .1f)
        val newRow = row("blue-song")
        val ordinaryBlueRow=row("red-song")
        assertTrue("Ordinary rows follow the new artwork too", ordinaryBlueRow.blue > ordinaryBlueRow.red + .08f)
        assertTrue("Selected row follows the new palette", newRow.blue > newRow.red)
        assertTrue("Selected row used the previous palette before switching", oldRow.red > oldRow.blue)
        compose.onNodeWithTag("Queue row blue-song").assertIsSelected()
        compose.onNodeWithTag("Queue row red-song").assertIsNotSelected()
    }
    @Test fun selectedQueueTitleRemainsReadableWithWhiteArtwork() {
        val song = Track("white-song", "White song", "Artist", durationSec = 180)
        val white = ExtractedColors(primary = Color.White, secondary = Color.White,
            tertiary = Color.White, background = Color.White)
        compose.setContent { JUKETheme { Surface(Modifier.fillMaxWidth().height(500.dp)) {
            QueueBottomSheetContent(song, listOf(song), 0, MusicUiState(extractedColors = white),
                {}, { _, _ -> }, { _, _ -> }, {})
        } } }
        compose.waitUntil(5000) {
            compose.onNodeWithTag("Ambient backdrop").captureToImage().toPixelMap()[15, 15].red > .15f
        }
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("White song").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val text = layouts.single().layoutInput.style.color.luminance()
        val background = compose.onNodeWithTag("Queue row surface white-song", useUnmergedTree = true)
            .captureToImage().toPixelMap()[15, 5].luminance()
        assertTrue("Selected title contrast must stay readable on bright artwork", (text + .05f) / (background + .05f) >= 4.5f)
    }

}
