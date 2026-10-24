package `in`.synthora.musicbox

import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `in`.synthora.musicbox.models.ArtistCredit
import `in`.synthora.musicbox.network.BrowseParser
import `in`.synthora.musicbox.ui.components.*
import `in`.synthora.musicbox.ui.theme.JUKETheme
import kotlinx.coroutines.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class KeyboardArtistPresentationTest {
    @get:Rule val compose = createComposeRule()
    private fun keyboardVisible(view: View): Boolean = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true

    @Test fun programmaticOpenAndRetapShowImeWhileClosingCancelsFurtherRequests() {
        var opened by mutableStateOf(false); var trigger by mutableIntStateOf(0)
        var query by mutableStateOf("Existing query")
        lateinit var view: View
        compose.setContent { JUKETheme { view = LocalView.current; Column {
            Button({ opened = true }) { Text("Open field") }
            SearchHeader("Search", query, { query = it }, opened, { opened = it }, "Find songs", selectAllTrigger = trigger)
        } } }
        compose.onNodeWithText("Open field").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.waitUntil(7000) { keyboardVisible(view) }
        compose.runOnIdle { trigger++ }
        compose.waitUntil { compose.onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange] == TextRange(0, query.length) }
        compose.onNodeWithContentDescription("Close search").performClick()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.waitUntil(7000) { !keyboardVisible(view) }
        compose.runOnIdle { opened = true; opened = false }
        compose.waitForIdle()
        assertFalse(keyboardVisible(view))
        compose.onNodeWithText("Open field").performClick()
        compose.waitUntil(7000) { keyboardVisible(view) }
        compose.onNodeWithContentDescription("Close search").performClick()
        compose.waitUntil(7000) { !keyboardVisible(view) }
    }

    @Test fun artistPhotosAreRequestedOnlyWhileOpenAndOutstandingLoadsAreCanceled() {
        var opened by mutableStateOf(false); var label by mutableStateOf("Song A")
        var started = 0; var canceled = 0
        compose.setContent { JUKETheme { Column {
            Text(label)
            Button({ opened = true }) { Text("Show artists") }
            if (opened) ArtistPickerSheet(listOf(ArtistCredit("First", "UCfirst"), ArtistCredit("Second", "UCsecond"), ArtistCredit("Third", "UCthird")),
                onDismiss = { opened = false }, loadPortrait = {
                    started++
                    try { awaitCancellation() } finally { canceled++ }
                }) { opened = false }
        } } }
        compose.runOnIdle { label = "Song B"; assertEquals(0, started) }
        compose.onNodeWithText("Show artists").performClick()
        compose.waitUntil { started == 2 }
        compose.onNodeWithTag("Artist portrait UCfirst", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { label = "Song C"; assertEquals(2, started) }
        compose.onNodeWithContentDescription("Close artists").performClick()
        compose.waitUntil { canceled == 2 }
        compose.runOnIdle { label = "Song D"; assertEquals(2, started) }
    }

    @Test fun failedOrMissingArtistImagesNeverBlockArtistSelection() {
        var selected: ArtistCredit? = null
        compose.setContent { JUKETheme { ArtistPickerSheet(listOf(ArtistCredit("Known", "UCknown"), ArtistCredit("Unknown")), {},
            loadPortrait = { if (it.id != null) error("offline") else null }) { selected = it } } }
        compose.onNodeWithText("Unknown").performClick()
        compose.runOnIdle { assertEquals("Unknown", selected?.name) }
        compose.onNodeWithText("Known").performClick()
        compose.runOnIdle { assertEquals("UCknown", selected?.id) }
    }

    @Test fun playlistTitleEllipsizesOnOneLineAndReadMoreStaysLeftWithTheExistingDialog() {
        val title = "A very long playlist name that must stay on one line ".repeat(4)
        val description = "A long playlist description that must be readable after expansion. ".repeat(20)
        val item = BrowseParser.item(buildJsonObject { put("playlistId", "PLfixture"); put("title", title) })
        compose.setContent { JUKETheme { Box(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
            CollectionDetailHero(item, title, "", "Artist", "50 tracks", description, onPlay = {}, onShuffle = {}, onQueue = {})
        } } }
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        compose.onNodeWithText("Read more").performScrollTo().assertIsDisplayed()
        val button = compose.onNodeWithText("Read more").fetchSemanticsNode().boundsInRoot
        val hero = compose.onNodeWithTag("Collection hero", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(button.left < hero.left + hero.width / 3)
        compose.onNodeWithText("Read more").performClick()
        compose.onAllNodesWithText(description).onLast().assertIsDisplayed()
        compose.onNodeWithText("Back", substring = false).performClick()
        compose.onNodeWithText("Read more").assertIsDisplayed()
    }
    @Test fun loadedPortraitReplacesThePlaceholderAndPlayButtonRemainsVisibleOnWhiteArtwork() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val image = java.io.File(context.cacheDir, "portrait-fixture.png")
        val bitmap = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        image.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        var show by mutableStateOf(true)
        var plays = 0
        compose.setContent { JUKETheme { Surface(color = Color.White) {
            Box(Modifier.size(120.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CollectionPlayButton("Play white cover", { plays++ })
            }
            if (show) ArtistPickerSheet(listOf(ArtistCredit("Portrait", "UCportrait")), { show = false },
                loadPortrait = { image.toURI().toString() }) { show = false }
        } } }
        val node = compose.onNodeWithTag("Artist portrait UCportrait", useUnmergedTree = true)
        compose.waitUntil(7000) {
            val pixels = node.captureToImage().toPixelMap()
            val center = pixels[pixels.width / 2, pixels.height / 2]
            center.red > .9f && center.green < .1f
        }
        compose.onNodeWithContentDescription("Close artists").performClick()
        val play = compose.onNodeWithContentDescription("Play white cover")
        val pixels = play.captureToImage().toPixelMap()
        assertTrue(pixels[pixels.width / 4, pixels.height / 2].red < .9f)
        play.performClick()
        compose.runOnIdle { assertEquals(1, plays) }
        image.delete()
    }

}
