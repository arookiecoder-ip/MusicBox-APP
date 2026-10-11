package `in`.synthora.musicbox.services

import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test

class CallPlaybackGuardTest {
    @Test fun explicitPlayDuringCallPausesAndSchedulesResumeWithoutCancellingItself() {
        var paused = false
        var pending = false
        lateinit var guard: CallPlaybackGuard
        guard = CallPlaybackGuard({ true }, {
            paused = true
            guard.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        }, { pending = true }, { pending = false })
        guard.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        assertTrue(paused)
        assertTrue(pending)
        guard.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        assertFalse(pending) // Explicit pause prevents unwanted playback when the call ends.
    }
    @Test fun focusRestorationDuringCallIsHeldButOrdinaryPlayIsUnchanged() {
        var calling = true
        var pauses = 0
        var schedules = 0
        val guard = CallPlaybackGuard({ calling }, { pauses++ }, { schedules++ }, {})
        guard.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertEquals(1, pauses)
        assertEquals(1, schedules)
        calling = false
        guard.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        assertEquals(1, pauses)
        assertEquals(1, schedules)
    }
}
