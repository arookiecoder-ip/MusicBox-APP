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
    @Test fun focusLossImmediatelyBeforeCallModeStillResumesButUnrelatedOldFocusLossDoesNot() {
        var calling=false
        var now=1000L
        val guard=CallPlaybackGuard({calling},{},{},{},nowMs={now})
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        calling=true;now+=300;guard.modeChanged(false)
        assertTrue(guard.resumePending)
        guard.explicitPause();calling=false
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        calling=true;now+=10_001;guard.modeChanged(false)
        assertFalse(guard.resumePending)
    }

    @Test fun outgoingCallFocusSuppressionBeforeDelayedModeAndNormalCallbackRetainsIntent() {
        var calling=false;var now=1000L;var pauses=0;var schedules=0
        val guard=CallPlaybackGuard({calling},{pauses++},{schedules++},{},nowMs={now})
        guard.onPlaybackSuppressionReasonChanged(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        guard.modeChanged(false) // An OEM can still report NORMAL while dialing.
        now+=5000;calling=true;guard.modeChanged(false)
        assertTrue(guard.resumePending);assertEquals(1,schedules)
        calling=false;guard.modeChanged(false)
        assertTrue(guard.resumePending);assertEquals(2,schedules)
    }
    @Test fun repeatedCallStatesDoNotReplaceTheOriginalResumeSnapshot() {
        var schedules=0
        val guard=CallPlaybackGuard({true},{},{schedules++},{})
        guard.modeChanged(true);guard.modeChanged(false)
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertEquals(1,schedules);assertTrue(guard.resumePending)
    }
    @Test fun explicitPauseBetweenFocusLossAndCallModeCannotResurrectPlayback() {
        var calling=false
        val guard=CallPlaybackGuard({calling},{fail("Do not pause")},{fail("Do not resume")},{})
        guard.onPlaybackSuppressionReasonChanged(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        guard.explicitPause();calling=true;guard.modeChanged(false)
        assertFalse(guard.resumePending)
    }

    @Test fun focusInterruptionWakesModeFallbackBeforeTheNextIdlePoll() {
        var wakes=0
        val guard=CallPlaybackGuard({false},{},{},{},onInterruption={wakes++})
        guard.onPlaybackSuppressionReasonChanged(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        assertEquals(1,wakes);assertTrue(guard.needsModePolling)
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertEquals(2,wakes)
        guard.explicitPause();assertFalse(guard.needsModePolling)
    }

    @Test fun repeatedPostCallFocusLossRetriesAreBoundedAndWaitForSettling() {
        var calling=true;var now=1_000L;var schedules=0
        val guard=CallPlaybackGuard({calling},{},{schedules++},{},nowMs={now})
        guard.modeChanged(true);calling=false;guard.modeChanged(false)
        now+=800;assertTrue(guard.readyToResume());guard.resumeConsumed()
        now+=100;guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertTrue(guard.resumePending);assertTrue(guard.isSettlingRetry)
        assertFalse(guard.readyToResume())
        now+=750; assertTrue(guard.readyToResume())
        guard.resumeConsumed()
        repeat(2) {
            now+=100
            guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
            assertTrue(guard.resumePending)
            assertFalse(guard.readyToResume())
            now+=750; assertTrue(guard.readyToResume())
            guard.resumeConsumed()
        }
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertFalse(guard.resumePending)
    }
    @Test fun explicitPauseAndExpiredSettlingWindowNeverRestartMusic() {
        var calling=true;var now=1_000L
        val guard=CallPlaybackGuard({calling},{},{},{},nowMs={now})
        guard.modeChanged(true);calling=false;guard.modeChanged(false);guard.resumeConsumed()
        guard.explicitPause()
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertFalse(guard.resumePending)
        calling=true;guard.modeChanged(true);calling=false;guard.modeChanged(false);guard.resumeConsumed()
        now+=8_001
        guard.onPlayWhenReadyChanged(false,Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertFalse(guard.resumePending)
    }
    @Test fun normalModeMustSettleAgainAfterCallModeJitter() {
        var calling=true;var now=1_000L
        val guard=CallPlaybackGuard({calling},{},{},{},nowMs={now})
        guard.modeChanged(true);calling=false;guard.modeChanged(false)
        now+=500;assertFalse(guard.readyToResume())
        calling=true;guard.modeChanged(false);calling=false;guard.modeChanged(false)
        now+=500;assertFalse(guard.readyToResume())
        now+=250;assertTrue(guard.readyToResume())
    }

    @Test fun endingCallRestoresManualFocusOverrideOnlyOnceEvenAfterExplicitPause() {
        var calling=true;val updates=mutableListOf<Boolean>()
        val guard=CallPlaybackGuard({calling},{},{},{},{updates+=it})
        guard.explicitPlay();guard.explicitPause()
        calling=false;guard.modeChanged(false);guard.modeChanged(false)
        assertEquals(listOf(true,false),updates)
    }

    @Test fun lingeringCallAudioDefersRatherThanCancelsAndThenResumes() {
        var calling = true; var now = 1_000L
        val guard = CallPlaybackGuard({ calling }, {}, {}, {}, nowMs = { now })
        guard.modeChanged(true); calling = false; guard.modeChanged(false)
        now += 800; assertTrue(guard.readyToResume())
        assertTrue(guard.waitForAudioRelease(true)); assertTrue(guard.resumePending)
        now += 1_000; assertTrue(guard.waitForAudioRelease(true)); assertTrue(guard.resumePending)
        assertFalse(guard.waitForAudioRelease(false)); assertTrue(guard.resumePending)
        guard.resumeConsumed(); assertFalse(guard.resumePending)
    }
    @Test fun sustainedOtherMusicCancelsRecoveryAndExplicitPauseClearsItsWindow() {
        var calling = true; var now = 1_000L
        val guard = CallPlaybackGuard({ calling }, {}, {}, {}, nowMs = { now })
        guard.modeChanged(true); calling = false; guard.modeChanged(false)
        assertTrue(guard.waitForAudioRelease(true))
        now += 2_500; assertTrue(guard.waitForAudioRelease(true)); assertFalse(guard.resumePending)
        calling = true; guard.modeChanged(true); guard.explicitPause()
        now += 5_000; calling = false; guard.modeChanged(false)
        assertFalse(guard.resumePending)
    }
    @Test fun expiredAndRenewedOwnLeasesCanRecoverButRemoteHandoffsCannot() {
        val own = SharedPlaybackOutput(mode = "phone", owner = "self", token = "new", leaseMs = 12_000)
        assertTrue(canRecoverCallLease("old", "", own, "self"))
        assertTrue(canRecoverCallLease("old", "new", own, "self"))
        assertFalse(canRecoverCallLease("old", "unrelated", own, "self"))
        assertFalse(canRecoverCallLease("old", "", own.copy(owner = "other"), "self"))
        assertFalse(canRecoverCallLease("old", "", own.copy(owner = "other", leaseMs = 0), "self"))
        assertFalse(canRecoverCallLease("old", "", own.copy(mode = "alexa"), "self"))
        assertFalse(canRecoverCallLease("old", "new", own.copy(handoffPending = true), "self"))
        assertTrue(canRecoverCallLease("old", "", own.copy(owner = "", token = "", leaseMs = 0), "self"))
    }

    @Test fun slowOwnershipRecoveryDoesNotSpendThePostCallFocusWindow() {
        var calling = true; var now = 1_000L
        val guard = CallPlaybackGuard({ calling }, {}, {}, {}, nowMs = { now })
        guard.modeChanged(true); calling = false; guard.modeChanged(false)
        now += 800; assertTrue(guard.readyToResume())
        // The service can wait for a long-call lease renewal before player.play().
        now += 12_000
        assertTrue(guard.resumePending)
        guard.resumeConsumed()
        now += 300
        guard.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertTrue(guard.resumePending)
    }

    @Test fun retriesCannotExtendTheOriginalPostCallWindow() {
        var calling = true; var now = 1_000L
        val guard = CallPlaybackGuard({ calling }, {}, {}, {}, nowMs = { now })
        guard.modeChanged(true); calling = false; guard.modeChanged(false)
        guard.resumeConsumed()
        now += 7_000
        guard.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertTrue(guard.resumePending)
        now += 800; assertFalse(guard.readyToResume())
        now += 750; assertTrue(guard.readyToResume())
        guard.resumeConsumed()
        guard.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertFalse(guard.resumePending)
    }

    @Test fun aSecondCallGetsItsOwnRecoveryBudget() {
        var calling = true; var now = 1_000L
        val guard = CallPlaybackGuard({ calling }, {}, {}, {}, nowMs = { now })
        guard.modeChanged(true); calling = false; guard.modeChanged(false); guard.resumeConsumed()
        repeat(3) {
            guard.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
            guard.resumeConsumed()
        }
        calling = true; guard.modeChanged(true)
        now += 30_000; calling = false; guard.modeChanged(false); guard.resumeConsumed()
        guard.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        assertTrue(guard.resumePending)
    }

}
