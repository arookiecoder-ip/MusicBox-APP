package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track
import org.junit.Assert.*
import org.junit.Test

class RemoteNotificationTransitionTest {
    private val first = Track("a", "Song A", "Artist", durationSec = 180)
    private val second = Track("b", "Song B", "Artist", durationSec = 200)
    private fun active(mode: String) = EchoState(track = first, playing = true, confirmed = true,
        sharedOutput = SharedPlaybackOutput(mode = mode, owner = "receiver", token = "lease", serial = "echo", epoch = "server"))

    @Test fun pendingChangesKeepForegroundWithoutShowingOldTitleForNewTrack() {
        for (mode in listOf("alexa", "phone")) {
            val bridge = RemoteNotificationTransition()
            val initial = active(mode)
            bridge.present(initial, 0)
            val pending = initial.copy(track = second, playing = false, processing = true, confirmed = false)
            val shown = bridge.present(pending, 100)
            assertEquals(second, shown.track)
            assertTrue(shown.loading)
            assertFalse(pending.playing) // Never changes authoritative receiver state.
            assertEquals(pending.copy(playing = true), shown)
        }
    }

    @Test fun missingMetadataIsRetainedOnlyDuringProcessingAndExpires() {
        val bridge = RemoteNotificationTransition()
        val initial = active("alexa")
        bridge.present(initial, 0)
        val pending = initial.copy(track = null, playing = false, processing = true, confirmed = false)
        assertEquals(first, bridge.present(pending, 100).track)
        assertEquals(30_000L, bridge.remainingMs(100))
        assertTrue(bridge.present(pending, 30_099).playing)
        assertEquals(pending, bridge.present(pending, 30_100))
        assertNull(bridge.remainingMs(30_100))
        assertEquals(pending, bridge.present(pending, 50_000))
    }

    @Test fun pauseStopDisconnectAndOwnershipChangesAreNeverOverridden() {
        val initial = active("alexa")
        val pending = initial.copy(track = second, playing = false, processing = true, confirmed = false)
        val states = listOf(initial.copy(playing = false), EchoState(),
            pending.copy(processing = false), pending.copy(confirmed = true),
            pending.copy(sharedOutput = initial.sharedOutput.copy(owner = "other")),
            pending.copy(sharedOutput = initial.sharedOutput.copy(token = "other")),
            pending.copy(sharedOutput = initial.sharedOutput.copy(serial = "other")),
            pending.copy(sharedOutput = initial.sharedOutput.copy(epoch = "other")),
            pending.copy(sharedOutput = initial.sharedOutput.copy(mode = "phone")),
            pending.copy(sharedOutput = initial.sharedOutput.copy(handoffPending = true)))
        for (state in states) {
            val bridge = RemoteNotificationTransition()
            bridge.present(initial, 0)
            assertEquals(state, bridge.present(state, 100))
        }
    }

    @Test fun alreadyPausedReceiverDoesNotInventPlayingIntent() {
        val bridge = RemoteNotificationTransition()
        val paused = active("phone").copy(playing = false)
        bridge.present(paused, 0)
        val pending = paused.copy(track = second, processing = true, confirmed = false)
        assertEquals(pending, bridge.present(pending, 100))
    }
}
