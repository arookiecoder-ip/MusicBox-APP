package `in`.synthora.musicbox.viewmodels

import `in`.synthora.musicbox.models.Track
import org.junit.Assert.*
import org.junit.Test

class QueueSelectionPresentationTest {
    private val a = Track("a", "A", "Artist", durationSec = 180)
    private val b = Track("b", "B", "Artist", durationSec = 180)
    @Test fun oldBaseAndMismatchedIndexCannotReleaseTheRequestedSong() {
        val old = MusicUiState(currentTrack = a, queue = listOf(a, b), queueIndex = 0)
        assertFalse(selectionPresented(old, "b", 1))
        assertFalse(selectionPresented(old.copy(currentTrack = b), "b", 1))
        val fresh = old.copy(currentTrack = b, queueIndex = 1)
        assertTrue(selectionPresented(fresh, "b", 1))
        assertEquals(b, withPendingPlayback(old, b, 1).currentTrack)
    }
    @Test fun duplicateSongsKeepTheirExactQueueEntryAndPosition() {
        val duplicate = b.copy(uuid = "b-second")
        val queue = listOf(a, b, duplicate)
        val first = MusicUiState(currentTrack = b, queue = queue, queueIndex = 1)
        assertFalse(selectionPresented(first, "b-second", 2))
        assertTrue(selectionPresented(first.copy(currentTrack = duplicate, queueIndex = 2), "b-second", 2))
        assertFalse(selectionPresented(first.copy(queue = emptyList()), "b", 1))
        assertTrue(selectionPresented(first.copy(queue = listOf(b, a, duplicate), queueIndex = 0), "b", 1))
    }
    @Test fun offlineEchoFallbackHandlesSavedSelectionsAndRestartedServerSerials() {
        val offline=`in`.synthora.musicbox.services.EchoDevice("offline","Echo",false)
        val online=`in`.synthora.musicbox.services.EchoDevice("online","Other Echo",true)
        assertEquals("offline",unavailableEchoSerial(listOf(offline),""))
        assertEquals("offline",unavailableEchoSerial(listOf(offline,online),"offline"))
        assertNull(unavailableEchoSerial(listOf(offline,online),""))
        assertNull(unavailableEchoSerial(listOf(online),"online"))
        assertNull(unavailableEchoSerial(emptyList(),"offline"))
    }

}
