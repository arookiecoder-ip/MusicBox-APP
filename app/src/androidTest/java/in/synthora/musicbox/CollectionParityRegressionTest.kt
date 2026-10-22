package `in`.synthora.musicbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.network.BrowseParser
import `in`.synthora.musicbox.services.DownloadedCollection
import `in`.synthora.musicbox.ui.components.*
import `in`.synthora.musicbox.ui.screens.DownloadSelection
import `in`.synthora.musicbox.ui.screens.DownloadTrackRow
import `in`.synthora.musicbox.ui.theme.JUKETheme
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CollectionParityRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun playlistPlayFloatsOnlyAfterDetailsScrollAway() {
        var played = 0
        compose.setContent { JUKETheme {
            val portrait = android.content.res.Configuration(LocalConfiguration.current).apply {
                orientation = android.content.res.Configuration.ORIENTATION_PORTRAIT
                smallestScreenWidthDp = 390
            }
            CompositionLocalProvider(LocalConfiguration provides portrait) {
                AdaptiveDetailLayout(bottomPadding = 0.dp, onFloatingPlay = { played++ },
                    hero = { Text("Playlist details", Modifier.height(260.dp)) }) {
                    items(50) { Text("Track $it", Modifier.fillMaxWidth().height(56.dp)) }
                }
            }
        } }
        compose.onNodeWithText("▶ Play").assertDoesNotExist()
        compose.onNodeWithTag("Collection tracks").performScrollToIndex(10)
        compose.onNodeWithText("▶ Play").assertIsDisplayed().performClick()
        assertEquals(1, played)
        compose.onNodeWithTag("Collection tracks").performScrollToIndex(0)
        compose.onNodeWithText("▶ Play").assertDoesNotExist()
    }

    @Test fun longPlaylistDescriptionCanExpandAndCollapse() {
        val item = BrowseParser.item(buildJsonObject { put("playlistId", "PLfixture"); put("title", "Playlist") })
        compose.setContent { JUKETheme {
            CompositionLocalProvider(LocalMediaMenu provides MediaMenuController()) {
                Box(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    CollectionDetailHero(item, "Playlist", "", "Artist", "50 tracks",
                        description = "A long description with links and useful playlist information. ".repeat(20),
                        onPlay = {}, onShuffle = {}, onQueue = {})
                }
            }
        } }
        compose.onNodeWithText("Read more").assertIsDisplayed().performClick()
        compose.onNodeWithText("Show less").performScrollTo().performClick()
        compose.onNodeWithText("Read more").performScrollTo().assertIsDisplayed()
    }
    @Test fun downloadedAndAccountCollectionsKeepSameHeroAndActions() {
        val menu = MediaMenuController()
        val online = BrowseParser.item(buildJsonObject { put("kind", "album"); put("browseId", "MPREfixture"); put("title", "Album") })
        val offline = DownloadedCollection(online.id, "album", "Album", "", "Artist", emptyList()).browseItem()
        var downloaded by mutableStateOf(false)
        var played = 0; var shuffled = 0; var queued = 0
        compose.setContent { JUKETheme { CompositionLocalProvider(LocalMediaMenu provides menu) {
            Surface { CollectionDetailHero(if (downloaded) offline else online, "Album", "", "Artist", "Album · 12 tracks",
                onPlay = { played++ }, onShuffle = { shuffled++ }, onQueue = { queued++ },
                options = if (downloaded) listOf(ExtraSongOption(Icons.Default.CheckBoxOutlineBlank, "Select songs") {}) else emptyList()) }
        } } }
        val originalBounds = compose.onNodeWithTag("Collection hero").fetchSemanticsNode().boundsInRoot
        // The hero tag covers padded content, not the parent's available width.
        // Preserve the chosen adaptive size and verify square artwork/parity.
        val artworkBounds = compose.onNodeWithTag("Collection artwork", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val artworkSize = artworkBounds.right - artworkBounds.left
        assertTrue(kotlin.math.abs(artworkSize.value - 200f) <= 0.5f || kotlin.math.abs(artworkSize.value - 220f) <= 0.5f)
        for (local in listOf(false, true)) {
            compose.runOnIdle { downloaded = local }
            assertEquals(originalBounds, compose.onNodeWithTag("Collection hero").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("Collection artwork", useUnmergedTree = true).assertWidthIsEqualTo(artworkSize).assertHeightIsEqualTo(artworkSize)
            compose.onNodeWithContentDescription("Play collection").performClick()
            compose.onNodeWithContentDescription("Shuffle collection").performClick()
            compose.onNodeWithContentDescription("Add collection to queue").performClick()
            compose.onNodeWithContentDescription("Share collection").assertIsDisplayed()
            compose.onNodeWithContentDescription("Collection options").performClick()
            assertEquals(if (local) offline else online, menu.item)
            assertEquals(if (local) listOf("Select songs") else emptyList<String>(), menu.extraOptions.map { it.label })
        }
        assertEquals(2, played); assertEquals(2, shuffled); assertEquals(2, queued)
    }
    @Test fun downloadedAlbumRowsKeepNumberingOptionsAndSelection() {
        val menu = MediaMenuController()
        val track = Track("song", "Song", "Artist", ytVideoId = "abcdefghijk", durationSec = 180)
        val selection = DownloadSelection()
        var played = false
        compose.setContent { JUKETheme { CompositionLocalProvider(LocalMediaMenu provides menu) {
            Surface { DownloadTrackRow(track, selection, { played = true }, number = 1, sharpArtwork = false) }
        } } }
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("3:00").assertIsDisplayed()
        compose.onNodeWithText("Song").performClick(); assertTrue(played)
        compose.onNodeWithContentDescription("More options").performClick()
        assertEquals(track, menu.track)
        compose.runOnIdle { menu.queueActions!!.select!!() }
        assertEquals(setOf(track.ytVideoId), selection.selected)
        compose.onNode(isToggleable()).assertIsDisplayed()
    }
}
