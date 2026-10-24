package `in`.synthora.musicbox.services

import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test

class CallPlaybackGuardTest {
    @Test fun callEntryPausesAndRetainsIntentAcrossDelayedOwnPauseAcknowledgement() {
        var calling=true
        var pending=false
        var pauses=0
        val guard=CallPlaybackGuard({calling},{pauses++},{pending=true},{pending=false})
        guard.modeChanged(true)
        assertEquals(1,pauses);assertTrue(pending);assertTrue(guard.resumePending)
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        assertTrue(pending)
        calling=false;guard.modeChanged(false)
        assertTrue(pending)
        guard.resumeConsumed();assertFalse(pending);assertFalse(guard.resumePending)
    }
    @Test fun explicitPauseDuringCallCancelsResumeEvenWhenAlreadyPaused() {
        var pending=false
        val guard=CallPlaybackGuard({true},{},{pending=true},{pending=false})
        guard.modeChanged(true)
        guard.explicitPause()
        assertFalse(pending);assertFalse(guard.resumePending)
    }
    @Test fun explicitPlayDuringCallIsAllowedAndModeUpdatesDoNotPauseItAgain() {
        var calling=true
        var pauses=0
        var manualFocus=false
        val guard=CallPlaybackGuard({calling},{pauses++},{},{},{manualFocus=it})
        guard.explicitPlay()
        guard.onPlayWhenReadyChanged(true,Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        guard.modeChanged(true)
        assertTrue(manualFocus);assertTrue(guard.manualDuringCall);assertEquals(0,pauses)
        assertFalse(guard.resumePending)
        calling=false;guard.modeChanged(true)
        assertFalse(manualFocus);assertFalse(guard.manualDuringCall)
    }
    @Test fun transientSuppressionCapturesCallIntentEvenWithoutPlayWhenReadyChange() {
        val guard=CallPlaybackGuard({true},{},{},{})
        guard.onPlaybackSuppressionReasonChanged(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        assertTrue(guard.resumePending)
    }
    @Test fun ordinaryExternalFocusLossDoesNotCreateACallResume() {
        val guard=CallPlaybackGuard({false},{fail("Must not pause")},{fail("Must not auto-resume")},{})
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertFalse(guard.resumePending)
    }
    @Test fun explicitPauseAfterManualOverrideDoesNotReacquireFocusDuringTheCall() {
        var pending=false
        val focuses=mutableListOf<Boolean>()
        val guard=CallPlaybackGuard({true},{},{pending=true},{pending=false},{focuses+=it})
        guard.explicitPlay();guard.explicitPause()
        assertEquals(listOf(true),focuses)
        assertFalse(pending);assertFalse(guard.manualDuringCall)
    }
}
