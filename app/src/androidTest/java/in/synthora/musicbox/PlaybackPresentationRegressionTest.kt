package `in`.synthora.musicbox

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.models.Track
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

    @Test fun queueUsesArtworkColorsAndBlendsThemWhenTheSelectedSongChanges() {
        val songs = listOf(Track("red-song", "Red song", "Artist"), Track("blue-song", "Blue song", "Artist"))
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
        val oldRow = row("red-song")
        compose.onNodeWithTag("Queue row red-song").assertIsSelected()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state = state.copy(currentTrack = songs[1], queueIndex = 1, extractedColors = palette(Color.Blue)) }
        compose.waitUntil(5000) {
            compose.mainClock.advanceTimeByFrame()
            backdrop().let { it.blue > .025f && it.red > .05f }
        }
        compose.mainClock.advanceTimeBy(300)
        val middle = backdrop()
        assertTrue("Previous artwork remains visible during the transition", middle.red > .02f)
        assertTrue("New artwork is blended during the transition", middle.blue > .02f)
        compose.mainClock.advanceTimeBy(1800)
        val end = backdrop()
        assertTrue("Queue follows the new blue artwork", end.blue > end.red + .1f)
        val newRow = row("blue-song")
        assertTrue("Selected row follows the new palette", newRow.blue > newRow.red)
        assertTrue("Selected row used the previous palette before switching", oldRow.red > oldRow.blue)
        compose.onNodeWithTag("Queue row blue-song").assertIsSelected()
        compose.onNodeWithTag("Queue row red-song").assertIsNotSelected()
    }
}
