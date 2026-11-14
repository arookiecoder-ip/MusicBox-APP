package `in`.synthora.musicbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import `in`.synthora.musicbox.ui.components.MiniPlayerTransport
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import `in`.synthora.musicbox.ui.components.SwipeToRemoveQueueRow
import `in`.synthora.musicbox.ui.components.playerDockInput
import `in`.synthora.musicbox.ui.components.player.QueueBottomSheetContent
import `in`.synthora.musicbox.viewmodels.MusicUiState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.network.BrowseParser
import `in`.synthora.musicbox.ui.components.DetailHero
import `in`.synthora.musicbox.ui.screens.*
import `in`.synthora.musicbox.ui.theme.JUKETheme
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FeatureInteractionRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun librarySwipesInBothDirectionsThroughAllFilters() {
        val names = listOf("All", "Playlists", "Albums", "Artists", "Downloads")
        compose.setContent { JUKETheme { Surface(Modifier.fillMaxSize()) {
            val pager = rememberPagerState { names.size }
            LibraryPages(pager) { page -> Box(Modifier.fillMaxSize()) { Text(names[page]) } }
        } } }
        for (name in names.drop(1)) {
            compose.onNodeWithTag("Library pages").performTouchInput { swipeLeft() }
            compose.waitForIdle()
            compose.onNodeWithText(name).assertIsDisplayed()
        }
        for (name in names.dropLast(1).reversed()) {
            compose.onNodeWithTag("Library pages").performTouchInput { swipeRight() }
            compose.waitForIdle()
            compose.onNodeWithText(name).assertIsDisplayed()
        }
    }
    @Test fun artistBannerDoesNotStretchWhenDescriptionExpands() {
        var expanded by mutableStateOf(false)
        compose.setContent { JUKETheme { DetailHero(null, artist = true) {
            Spacer(Modifier.height(170.dp))
            Button(onClick = { expanded = !expanded }) { Text("More") }
            if (expanded) Spacer(Modifier.height(300.dp))
        } } }
        compose.onNodeWithTag("Artist banner", useUnmergedTree = true).assertHeightIsEqualTo(380.dp)
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithTag("Artist banner", useUnmergedTree = true).assertHeightIsEqualTo(380.dp)
    }
    @Test fun searchArtworkSizeIsConsistentAndArtistShowsThreeSongs() {
        var kind by mutableStateOf("song")
        val songs = (1..3).map { Track("song$it", "Top song $it", "Artist", durationSec = 180, ytVideoId = "aaaaaaaaaaa") }
        compose.setContent { JUKETheme { Surface {
            val item = BrowseParser.item(buildJsonObject {
                put("kind", kind); put("title", "Top result");
                if (kind == "song") put("videoId", "aaaaaaaaaaa") else put("browseId", when(kind) { "artist" -> "UCfixture"; "album" -> "MPREfixture"; else -> "PLfixture" })
            })
            SearchHeroCard(item, songs, {}, {}, {}, {})
        } } }
        for (type in listOf("song", "album", "playlist", "artist")) {
            compose.runOnIdle { kind = type }
            compose.onNodeWithTag("Search result artwork", useUnmergedTree = true).assertWidthIsEqualTo(84.dp).assertHeightIsEqualTo(84.dp)
            if (type != "artist") compose.onNodeWithTag("Search top result").assertHeightIsEqualTo(120.dp)
        }
        compose.onAllNodesWithText("Artist", substring = false).assertCountEquals(4)
        for (song in songs) compose.onNodeWithText(song.title).assertIsDisplayed()
        compose.onNodeWithText("Open Artist").assertDoesNotExist()
    }
    @Test fun selectedDownloadsDeleteOnlyAfterConfirmation() {
        val tracks = (1..3).map { Track("s$it", "Song $it", "Artist", durationSec = 180, ytVideoId = "video00000$it") }
        val selection = DownloadSelection().apply { select(tracks[0]); toggle(tracks[2]) }
        var removed = emptyList<Track>()
        compose.setContent { JUKETheme { Surface { DownloadSelectionBar(selection, tracks) { removed = it } } } }
        compose.onNodeWithText("2 selected").assertExists()
        compose.onNodeWithContentDescription("Delete selected downloads").performClick()
        assertTrue(removed.isEmpty())
        compose.onNodeWithText("Cancel", substring = false).performClick()
        assertTrue(removed.isEmpty())
        compose.onNodeWithContentDescription("Delete selected downloads").performClick()
        compose.onNodeWithText("Delete", substring = false).performClick()
        assertEquals(listOf(tracks[0], tracks[2]), removed)
        assertFalse(selection.active)
    }
    @Test fun downloadNotificationsAreProgressNotificationsThatOpenDownloads() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val destination = `in`.synthora.musicbox.services.downloadDestinationIntent(context)
        assertEquals(MainActivity::class.java.name, destination.component?.className)
        assertTrue(destination.getBooleanExtra(`in`.synthora.musicbox.services.DownloadService.OPEN_DOWNLOADS, false))
        assertNull(destination.data)
        val state = `in`.synthora.musicbox.services.DownloadStatus(active = 3, total = 10, completed = 2, title = "Song", percent = 25)
        val active = `in`.synthora.musicbox.services.downloadNotification(context, state, true)
        assertEquals(android.app.Notification.CATEGORY_PROGRESS, active.category)
        assertTrue(active.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(25, active.extras.getInt(android.app.Notification.EXTRA_PROGRESS))
        assertNotNull(active.contentIntent)
        assertEquals("Cancel", active.actions.single().title.toString())
        assertFalse(active.extras.getString(android.app.Notification.EXTRA_TEMPLATE).orEmpty().contains("MediaStyle"))
        val finished = `in`.synthora.musicbox.services.downloadNotification(context, state.copy(active = 0, completed = 10), false)
        assertEquals("Downloads complete", finished.extras.getString(android.app.Notification.EXTRA_TITLE))
        assertTrue(finished.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test fun queueRemovalKeepsItsSurfaceAndRequiresADeliberateSwipe() {
        var removed = 0
        compose.setContent { JUKETheme { Surface {
            SwipeToRemoveQueueRow(true, { removed++ }, Modifier.width(300.dp).testTag("swipe-row")) {
                Box(Modifier.fillMaxWidth().height(64.dp))
            }
        } } }
        val row = compose.onNodeWithTag("swipe-row")
        val original = row.captureToImage().toPixelMap().let { it[it.width / 2, it.height / 2] }
        row.performTouchInput { down(center); moveBy(Offset(-width * .25f, 0f)) }
        val dragging = row.captureToImage().toPixelMap().let { it[it.width / 2, it.height / 2] }
        assertEquals("The removal background must stay behind the displaced edge", original, dragging)
        row.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(0, removed)
        row.performTouchInput { swipe(Offset(width * .85f, height * .5f), Offset(width * .15f, height * .5f), 400) }
        compose.waitForIdle()
        assertEquals(1, removed)
    }

    @Test fun dockTouchesNeverReachContentBehindLoadingOrLiveControls() {
        var behind = 0; var expanded = 0; var loading by mutableStateOf(true)
        compose.setContent { JUKETheme {
            Box(Modifier.size(300.dp, 100.dp)) {
                Button(onClick = { behind++ }, modifier = Modifier.fillMaxSize()) { Text("Behind dock") }
                Box(Modifier.fillMaxSize().playerDockInput().testTag("dock"), contentAlignment = Alignment.Center) {
                    if (loading) Box(Modifier.fillMaxSize().background(Color.Gray))
                    else TextButton(onClick = { expanded++ }) { Text("Open mini player") }
                }
            }
        } }
        compose.onNodeWithTag("dock").performTouchInput { click(center) }
        assertEquals(0, behind)
        compose.runOnIdle { loading = false }
        compose.onNodeWithText("Open mini player").performTouchInput { click(center) }
        assertEquals(1, expanded); assertEquals(0, behind)
        compose.onNodeWithTag("dock").performTouchInput { click(Offset(2f, 2f)) }
        assertEquals(0, behind)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test fun dockPreservesPhysicalTransportTapsLongPressesAndSwipes() {
        var behind = 0; var expanded = 0; var held = 0; var swipes = 0; var transport = 0
        compose.setContent { JUKETheme {
            Box(Modifier.size(320.dp, 80.dp)) {
                Button(onClick = { behind++ }, modifier = Modifier.fillMaxSize()) { Text("Behind") }
                Row(Modifier.fillMaxSize().playerDockInput()) {
                    Box(Modifier.weight(1f).fillMaxHeight().testTag("mini gestures")
                        .combinedClickable(onClick = { expanded++ }, onLongClick = { held++ })
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(onDragEnd = { swipes++ }) { change, _ -> change.consume() }
                        }) { Text("Current song") }
                    MiniPlayerTransport(false, false, { transport++ }, Modifier.testTag("mini transport"))
                }
            }
        } }
        // Physical input is essential: semantics performClick bypasses pointer cancellation.
        compose.onNodeWithTag("mini transport").performTouchInput {
            down(center); moveBy(Offset(1f, 0f)); advanceEventTime(40); up()
        }
        compose.runOnIdle { assertEquals(1, transport); assertEquals(0, expanded) }
        val region = compose.onNodeWithTag("mini gestures")
        region.performTouchInput {
            down(center); advanceEventTime(80); moveBy(Offset(1f, 0f))
            advanceEventTime(700); moveBy(Offset(1f, 0f)); up()
        }
        compose.runOnIdle { assertEquals(1, held); assertEquals(0, expanded) }
        region.performTouchInput { swipeLeft(durationMillis = 400) }
        compose.runOnIdle { assertEquals(1, swipes); assertEquals(0, expanded) }
        region.performTouchInput { swipeRight(durationMillis = 400) }
        compose.runOnIdle { assertEquals(2, swipes); assertEquals(0, expanded) }
        region.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, expanded); assertEquals(0, behind) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun scrollingQueueToAnEdgeDoesNotDismissItsModalSheet() {
        var open by mutableStateOf(true)
        var dismissals = 0
        val songs = (1..60).map { Track("queue-$it", "Queue song $it", "Artist", durationSec = 180) }
        compose.setContent { JUKETheme {
            if (open) {
                val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(onDismissRequest = { dismissals++; open = false }, sheetState = sheet, dragHandle = null) {
                    Box(Modifier.fillMaxWidth().height(520.dp)) {
                        QueueBottomSheetContent(songs[0], songs, 0, MusicUiState(),
                            onClose = { dismissals++; open = false }, onMoveTrack = { _, _ -> },
                            onRemoveTrack = { _, done -> done(false) }, onPlayTrack = {})
                    }
                }
            }
        } }
        compose.onNodeWithTag("Queue rows").performScrollToNode(hasText("Queue song 12"))
        compose.onNodeWithTag("Queue rows").performTouchInput { swipeDown(durationMillis = 180) }
        compose.onNodeWithContentDescription("Close queue").assertIsDisplayed()
        compose.onNodeWithText("Queue song 1", substring = false).assertIsDisplayed()
        compose.onNodeWithTag("Queue rows").performScrollToNode(hasText("Queue song 60"))
        repeat(3) {
            compose.onNodeWithTag("Queue rows").performTouchInput { swipeUp(durationMillis = 180) }
            compose.onNodeWithContentDescription("Close queue").assertIsDisplayed()
        }
        assertEquals(0, dismissals)
        compose.onNodeWithContentDescription("Close queue").performClick()
        compose.waitForIdle()
        assertEquals(1, dismissals)
        compose.onNodeWithTag("Queue rows").assertDoesNotExist()
    }

}
