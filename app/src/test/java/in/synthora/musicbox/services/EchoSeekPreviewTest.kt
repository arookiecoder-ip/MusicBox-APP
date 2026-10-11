package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track
import org.junit.Assert.*
import org.junit.Test

class EchoSeekPreviewTest {
    private val before = EchoState(track = Track(uuid = "one", title = "One", artist = "Artist", durationSec = 200, ytVideoId = "AAAAAAAAAAA"),
        playing = true, confirmed = true, positionMs = 10_000, playbackRevision = 4)
    @Test fun olderPollCannotUndoPendingSeek() {
        val preview = EchoSeekPreview(before, 50_000, 9_000)
        val state = preview.reconcile(before, 2_000)
        assertEquals(50_000L, state.positionMs)
        assertTrue(state.loading)
        val staged = before.copy(positionMs = 50_000, playbackRevision = 5, confirmed = false, processing = true)
        assertTrue(preview.reconcile(staged, 3_000).loading)
        val confirmed = staged.copy(confirmed = true, processing = false, positionMs = 51_000)
        assertSame(confirmed, preview.reconcile(confirmed, 4_000))
    }
    @Test fun pauseSeekDoesNotPretendToPlayAndTimeoutUsesServerCursor() {
        val paused = before.copy(playing = false)
        val preview = EchoSeekPreview(paused, 50_000, 9_000)
        val state = preview.reconcile(paused, 2_000)
        assertEquals(50_000L, state.positionMs)
        assertFalse(state.playing)
        assertFalse(state.loading)
        assertSame(paused, preview.reconcile(paused, 9_000))
    }
    @Test fun changedTrackOrOutputWinsImmediatelyButLeaseRenewalDoesNot() {
        val preview = EchoSeekPreview(before, 50_000, 9_000)
        val other = before.copy(track = before.track!!.copy(ytVideoId = "BBBBBBBBBBB"))
        assertSame(other, preview.reconcile(other, 2_000))
        val duplicate = before.copy(index = 2)
        assertSame(duplicate, preview.reconcile(duplicate, 2_000))
        val output = before.copy(sharedOutput = SharedPlaybackOutput(mode = "phone", token = "new"))
        assertSame(output, preview.reconcile(output, 2_000))
        assertEquals(50_000L, preview.reconcile(before.copy(sharedOutput = before.sharedOutput.copy(leaseMs = 100)), 2_000).positionMs)
    }
}
