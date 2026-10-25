package `in`.synthora.musicbox.services

import androidx.media3.common.Player

/** Call interruptions retain intent; an explicit Play during the call is a user override. */
internal class CallPlaybackGuard(
    private val isInCall: () -> Boolean,
    private val pause: () -> Unit,
    private val scheduleResume: () -> Unit,
    private val cancelResume: () -> Unit,
    private val manualFocus: (Boolean) -> Unit = {},
    private val nowMs: () -> Long = { System.nanoTime()/1_000_000 },
    private val onInterruption: () -> Unit = {}
) : Player.Listener {
    private var ownPauseAck = false
    private var focusOverrideActive = false
    private fun updateManualFocus(override: Boolean) {
        if (focusOverrideActive == override) return
        focusOverrideActive = override
        manualFocus(override)
    }
    private var focusLossAt: Long? = null
    private var normalSince: Long? = null
    private var settlingUntil = 0L
    private var settlingRetryUsed = false
    val isSettlingRetry get() = settlingRetryUsed && resumePending
    fun readyToResume(): Boolean {
        if (!resumePending || isInCall()) { normalSince = null; return false }
        val now = nowMs()
        val since = normalSince ?: now.also { normalSince = it }
        return now - since >= 750
    }
    var resumePending = false
        private set
    var manualDuringCall = false
        private set
    val needsModePolling get() = resumePending || manualDuringCall || focusLossAt?.let { nowMs()-it in 0..10_000 } == true
    val blocksAutomaticPlayback get() = isInCall() && !manualDuringCall

    fun explicitPlay() {
        resumePending=false
        focusLossAt=null
        settlingUntil=0L
        settlingRetryUsed=false
        normalSince=null
        cancelResume()
        manualDuringCall=isInCall()
        updateManualFocus(manualDuringCall)
    }
    fun explicitPause() {
        resumePending=false
        focusLossAt=null
        settlingUntil=0L
        settlingRetryUsed=false
        normalSince=null
        manualDuringCall=false
        ownPauseAck=false
        cancelResume()
        // Restoring media focus during a live call can emit a new focus-loss event.
        // Keep an already disabled focus handler idle until the call ends.
        if(!isInCall()) updateManualFocus(false)
    }
    fun resumeConsumed() {
        val now=nowMs()
        if (now > settlingUntil || settlingUntil == 0L) settlingRetryUsed=false
        settlingUntil=now+2_000
        resumePending=false
        focusLossAt=null
        cancelResume()
    }
    private fun remember() { if (!resumePending) { resumePending=true;scheduleResume() } }
    private fun hold() {
        remember() // Notification/foreground retention sees the intent before Pause events.
        ownPauseAck=true
        try { pause() } catch(error:Throwable) { ownPauseAck=false;throw error }
    }
    fun modeChanged(wantsPlayback:Boolean) {
        if(isInCall()) {
            normalSince=null
            if(!manualDuringCall) {
                if(wantsPlayback) hold()
                else if(focusLossAt?.let { nowMs()-it in 0..10_000 } == true) remember()
            }
            focusLossAt=null
        } else {
            if(normalSince == null) normalSince=nowMs()
            updateManualFocus(false)
            manualDuringCall=false
            if(resumePending) scheduleResume()
        }
    }
    override fun onPlaybackSuppressionReasonChanged(reason:Int) {
        if(reason==Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            // Focus suppression may precede the modem's mode change (especially outgoing calls).
            focusLossAt=nowMs()
            onInterruption()
            if (blocksAutomaticPlayback) hold()
        }
    }
    override fun onPlayWhenReadyChanged(playWhenReady:Boolean, reason:Int) {
        when {
            !playWhenReady && ownPauseAck && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> ownPauseAck=false
            playWhenReady && blocksAutomaticPlayback -> hold()
            !playWhenReady && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> {
                focusLossAt=nowMs()
                onInterruption()
                if(blocksAutomaticPlayback) remember()
                else if (!isInCall() && settlingUntil > 0 && nowMs() <= settlingUntil && !settlingRetryUsed) {
                    // Some modem/VoIP stacks deliver the call's final focus loss
                    // after NORMAL and the first resume. Repair once, bounded
                    // by this call, without retrying ordinary later focus loss.
                    settlingRetryUsed=true
                    remember()
                }
            }
            !playWhenReady && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> explicitPause()
        }
    }
}
