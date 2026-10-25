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
import androidx.compose.ui.graphics.toPixelMap
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

    @Test fun localSessionUpdatesCannotRemoveRemoteTrackNotifications() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")).use { it.readBytes() }
        }
        val prefs = context.getSharedPreferences("music_settings_prefs", android.content.Context.MODE_PRIVATE)
        val savedOutput = prefs.getString("playback_output", null)
        val notifications = context.getSystemService(android.app.NotificationManager::class.java)
        val channel = "remote-notification-regression"
        notifications.createNotificationChannel(android.app.NotificationChannel(channel, "Remote playback test", android.app.NotificationManager.IMPORTANCE_LOW))
        compose.setContent { Text("Remote notification fixture") }
        lateinit var future: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>
        instrumentation.runOnMainSync {
            future = androidx.media3.session.MediaController.Builder(context, androidx.media3.session.SessionToken(context,
                android.content.ComponentName(context, `in`.synthora.musicbox.services.PlaybackService::class.java))).buildAsync()
        }
        val controller = future.get(20, java.util.concurrent.TimeUnit.SECONDS)
        try {
            val serviceClass = `in`.synthora.musicbox.services.PlaybackService::class.java
            val service = (serviceClass.getDeclaredField("activeService").apply { isAccessible = true }.get(null)
                as java.lang.ref.WeakReference<*>).get() as `in`.synthora.musicbox.services.PlaybackService
            val session = serviceClass.getDeclaredField("mediaSession").apply { isAccessible = true }.get(service)
                as androidx.media3.session.MediaSession
            for (output in listOf("ALEXA", "REMOTE_PHONE")) {
                prefs.edit().putString("playback_output", output).commit()
                for (title in listOf("Song A", "Song B")) {
                    val notification = androidx.core.app.NotificationCompat.Builder(context, channel)
                        .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setOngoing(true).build()
                    instrumentation.runOnMainSync {
                        `in`.synthora.musicbox.services.PlaybackService.joinRemoteForeground(notification)
                    }
                    compose.waitUntil(5000) { notifications.activeNotifications.any { it.id == 1002 } }
                    instrumentation.runOnMainSync { service.onUpdateNotification(session, false) }
                    compose.waitForIdle()
                    android.os.SystemClock.sleep(300)
                    val retained = notifications.activeNotifications.singleOrNull { it.id == 1002 }
                    assertNotNull("Local session removed $output notification for $title", retained)
                    assertEquals(title, retained!!.notification.extras.getString(android.app.Notification.EXTRA_TITLE))
                }
                instrumentation.runOnMainSync {
                    `in`.synthora.musicbox.services.PlaybackService.leaveRemoteForeground(force = true)
                }
                notifications.cancel(1002)
            }
        } finally {
            instrumentation.runOnMainSync {
                `in`.synthora.musicbox.services.PlaybackService.leaveRemoteForeground(force = true)
                controller.release()
            }
            notifications.cancel(1002)
            notifications.deleteNotificationChannel(channel)
            prefs.edit().apply { if (savedOutput == null) remove("playback_output") else putString("playback_output", savedOutput) }.commit()
        }
    }

    @Test fun loggedOutEchoRemainsVisibleButCannotBeSelectedAndPhoneStillWorks() {
        var echoSelections = 0
        var phoneSelections = 0
        compose.setContent { JUKETheme { OutputPickerContent(
            isAlexa = true,
            devices = listOf(EchoDevice("echo-one", "Bedroom", true, false)),
            mobileDevices = emptyList(), ownDeviceId = "phone", selectedMobileId = null,
            onSelectMobile = {}, selectedSerial = "echo-one", busy = false,
            onSelectPhone = { phoneSelections++ }, onSelectEcho = { echoSelections++ }, onRefresh = {}
        ) } }
        compose.onNodeWithText("Bedroom (logged out)").performClick()
        compose.runOnIdle { assertEquals(0, echoSelections) }
        compose.onNodeWithText("This phone").performClick()
        compose.runOnIdle { assertEquals(1, phoneSelections) }
    }

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

    @Test fun queueGripScrollsNormallyAndSlightFingerMotionStillSelectsOnce() {
        var plays = 0
        var moves = 0
        var removals = 0
        compose.setContent { JUKETheme { QueueBottomSheetContent(songs[0], songs, 0, MusicUiState(), {},
            { _, _ -> moves++ }, { _, done -> removals++; done(true) }, { plays++ }) } }
        compose.onNodeWithTag("Queue rows").performTouchInput {
            swipe(Offset(24.dp.toPx(), height * .8f), Offset(27.dp.toPx(), height * .2f), 400)
        }
        compose.onNodeWithTag("Queue row s1").assertDoesNotExist()
        compose.onNodeWithText("Song 12").performScrollTo().performTouchInput {
            down(center); moveBy(Offset(1.dp.toPx(), 1.dp.toPx())); up()
        }
        compose.runOnIdle { assertEquals(1, plays); assertEquals(0, moves); assertEquals(0, removals) }
    }

    @Test fun queueGripRequiresHoldToReorderWithoutPlayingTheSong() {
        var queue by mutableStateOf(songs)
        var moves = 0
        var plays = 0
        compose.setContent { JUKETheme { QueueBottomSheetContent(songs[0], queue, 0, MusicUiState(), {},
            { from, to -> moves++; queue = queue.toMutableList().apply { add(to, removeAt(from)) } },
            { _, done -> done(true) }, { plays++ }) } }
        compose.onNodeWithContentDescription("Drag to reorder Song 2", useUnmergedTree = true).performTouchInput {
            down(center); advanceEventTime(650)
            moveBy(Offset(0f, 70.dp.toPx())); advanceEventTime(32)
            moveBy(Offset(0f, 70.dp.toPx())); up()
        }
        compose.runOnIdle { assertEquals(1, moves); assertEquals(0, plays); assertTrue(queue.indexOf(songs[1]) > 1) }
    }

    @Test fun queuePlayingBarsAnimateAndFreezeAfterPause() {
        var playing by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent { JUKETheme { QueueBottomSheetContent(songs[0], songs, 0,
            MusicUiState(isPlaying = playing), {}, { _, _ -> }, { _, _ -> }, {}) } }
        fun pixels(): List<androidx.compose.ui.graphics.Color> {
            val image = compose.onNodeWithTag("Queue playing bars", useUnmergedTree = true).captureToImage().toPixelMap()
            return List(image.width * image.height) { image[it % image.width, it / image.width] }
        }
        compose.mainClock.advanceTimeBy(64)
        val start = pixels()
        compose.mainClock.advanceTimeBy(450)
        assertNotEquals(start, pixels())
        compose.runOnIdle { playing = false }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithContentDescription("Paused", useUnmergedTree = true).assertExists()
        val paused = pixels()
        compose.mainClock.advanceTimeBy(500)
        assertEquals(paused, pixels())
    }

    @Test fun queueDeleteFinishesSlidingOffscreenBeforeCallingRemovalAndCanRecover() {
        var removals = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { JUKETheme {
            `in`.synthora.musicbox.ui.components.SwipeToRemoveQueueRow(true, { removals++ }, Modifier.testTag("Delete fixture")) {
                Box(Modifier.fillMaxWidth().height(64.dp).testTag("Delete foreground")) { Text("Delete song") }
            }
        } }
        val original = compose.onNodeWithTag("Delete foreground").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("Delete fixture").performTouchInput {
            swipe(Offset(width * .9f, centerY), Offset(width * .4f, centerY), 180)
        }
        compose.runOnIdle { assertEquals(0, removals) }
        compose.mainClock.advanceTimeBy(208)
        compose.runOnIdle { assertEquals(1, removals) }
        val displaced = compose.onNodeWithTag("Delete foreground").fetchSemanticsNode().boundsInRoot
        assertTrue("Committed row did not slide fully away", displaced.right <= original.left + 2f)
        // A retained/rejected row returns to its original position and remains tappable.
        compose.mainClock.advanceTimeBy(500)
        val restored = compose.onNodeWithTag("Delete foreground").fetchSemanticsNode().boundsInRoot
        assertEquals(original.left, restored.left, 2f)
        compose.runOnIdle { assertEquals(1, removals) }
    }

    @Test fun cookieControlsStayUsableDuringUnrelatedAccountRefreshAndRecoverAfterFailure() {
        var state by mutableStateOf(`in`.synthora.musicbox.viewmodels.AccountUiState(checkingStatus = true))
        var checks = 0
        var replacements = 0
        compose.setContent { JUKETheme { Column {
            `in`.synthora.musicbox.ui.screens.DownloadCookieControls(state, { checks++; state = state.copy(cookieBusy = true) }, { replacements++ })
        } } }
        compose.onNodeWithText("Replace cookies").assertIsEnabled().performClick()
        compose.onNodeWithText("Check again").assertIsEnabled().performClick()
        compose.onNodeWithText("Checking…").assertIsNotEnabled()
        compose.onNodeWithText("Replace cookies").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(cookieBusy = false, cookieMessage = "Audio check timed out. Try again.") }
        compose.onNodeWithText("Audio check timed out. Try again.").assertIsDisplayed()
        compose.onNodeWithText("Check again").assertIsEnabled()
        compose.runOnIdle { assertEquals(1, checks); assertEquals(1, replacements) }
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

    @Test fun playlistCreationTrimsNameRejectsBlankAndSubmitsOnce() {
        val created = mutableListOf<String>()
        var open by mutableStateOf(true)
        compose.setContent { JUKETheme { if (open) CreatePlaylistDialog(onDismiss = { open=false }, onCreate = { created.add(it);open=false }) } }
        compose.onNodeWithText("Create").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("   ")
        compose.onNodeWithText("Create").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("  Late night  ")
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Create").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("Late night"), created) }
    }

    @Test fun playlistRenameUsesExistingNameAndImeDone() {
        val renamed = mutableListOf<String>()
        var open by mutableStateOf(true)
        compose.setContent { JUKETheme { if (open) PlaylistNameDialog("Rename playlist", "Old name", "Save", {open=false}, { renamed.add(it);open=false }) } }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("New name")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(listOf("New name"), renamed) }
    }

    @Test fun deletingPlaylistRequiresExplicitConfirmationAndCancelDoesNotDelete() {
        var deletes = 0; var dismissed = 0
        var open by mutableStateOf(true)
        compose.setContent { JUKETheme { if (open) PlaylistDeleteDialog("My playlist", { dismissed++;open=false }, { deletes++;open=false }) } }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(1, dismissed); assertEquals(0, deletes);open=true }
        compose.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(1, deletes) }
    }

    @Test fun closingPlaylistDialogCannotHideNewSearchKeyboard() {
        var dialog by mutableStateOf(true)
        var search by mutableStateOf(false)
        var query by mutableStateOf("")
        lateinit var view: android.view.View
        compose.setContent { JUKETheme {
            view=androidx.compose.ui.platform.LocalView.current
            Column {
                if (!dialog) Button({search=true}) { Text("Search after playlist") }
                SearchHeader("Search",query,{query=it},search,{search=it},"Find songs")
            }
            if (dialog) CreatePlaylistDialog({dialog=false},{dialog=false})
        } }
        compose.onNode(hasSetTextAction()).performTextInput("Playlist")
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Search after playlist").performClick()
        compose.waitUntil(7000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(view)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true
        }
    }
}
