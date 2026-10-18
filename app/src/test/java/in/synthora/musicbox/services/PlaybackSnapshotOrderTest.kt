package `in`.synthora.musicbox.services

import org.junit.Assert.*
import org.junit.Test

class PlaybackSnapshotOrderTest {
    private fun state(epoch: String = "server-a", output: Long = 2, playback: Long = 10, mode: String = "alexa") =
        EchoState(sharedOutput = SharedPlaybackOutput(epoch = epoch, revision = output, mode = mode), playbackRevision = playback)

    @Test fun rejectsOlderPlaybackForBothPhoneAndEcho() {
        for (mode in listOf("phone", "alexa")) {
            assertFalse(state(playback = 9, mode = mode).supersedes(state(mode = mode)))
            assertTrue(state(playback = 10, mode = mode).supersedes(state(mode = mode)))
            assertTrue(state(playback = 11, mode = mode).supersedes(state(mode = mode)))
        }
    }
    @Test fun oldOwnershipCannotOverrideANewerOutput() {
        assertFalse(state(output = 1, playback = 100).supersedes(state()))
    }
    @Test fun newOutputAndRestartMayResetPlaybackRevision() {
        assertTrue(state(output = 3, playback = 1).supersedes(state()))
        assertTrue(state(epoch = "server-b", output = 0, playback = 0).supersedes(state()))
    }
    @Test fun acceptsLegacySnapshotsWithoutOrderingFields() {
        assertTrue(EchoState().supersedes(state()))
        assertTrue(state(playback = -1).supersedes(state()))
    }
}
