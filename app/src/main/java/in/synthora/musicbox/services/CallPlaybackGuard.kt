package `in`.synthora.musicbox.services

import androidx.media3.common.Player

/** Call interruptions retain intent; an explicit Play during the call is a user override. */
internal class CallPlaybackGuard(
    private val isInCall: () -> Boolean,
    private val pause: () -> Unit,
    private val scheduleResume: () -> Unit,
    private val cancelResume: () -> Unit,
    private val manualFocus: (Boolean) -> Unit = {},
    private val nowMs: () -> Long = { System.nanoTime()/1_000_000 }
) : Player.Listener {
    private var ownPauseAck = false
    private var focusLossAt: Long? = null
    var resumePending = false
        private set
    var manualDuringCall = false
        private set
    val blocksAutomaticPlayback get() = isInCall() && !manualDuringCall

    fun explicitPlay() {
        resumePending=false
        focusLossAt=null
        cancelResume()
        manualDuringCall=isInCall()
        manualFocus(manualDuringCall)
    }
    fun explicitPause() {
        resumePending=false
        focusLossAt=null
        manualDuringCall=false
        ownPauseAck=false
        cancelResume()
        // Restoring media focus during a live call can emit a new focus-loss event.
        // Keep an already disabled focus handler idle until the call ends.
        if(!isInCall()) manualFocus(false)
    }
    fun resumeConsumed() { resumePending=false;cancelResume() }
    private fun remember() { if (!resumePending) { resumePending=true;scheduleResume() } }
    private fun hold() {
        remember() // Notification/foreground retention sees the intent before Pause events.
        ownPauseAck=true
        try { pause() } catch(error:Throwable) { ownPauseAck=false;throw error }
    }
    fun modeChanged(wantsPlayback:Boolean) {
        if(isInCall()) {
            if(!manualDuringCall) {
                if(wantsPlayback) hold()
                else if(focusLossAt?.let { nowMs()-it in 0..10_000 } == true) remember()
            }
            focusLossAt=null
        } else {
            manualDuringCall=false
            manualFocus(false)
            if(resumePending) scheduleResume()
        }
    }
    override fun onPlaybackSuppressionReasonChanged(reason:Int) {
        if(reason==Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            // Focus suppression may precede the modem's mode change (especially outgoing calls).
            focusLossAt=nowMs()
            if (blocksAutomaticPlayback) hold()
        }
    }
    override fun onPlayWhenReadyChanged(playWhenReady:Boolean, reason:Int) {
        when {
            !playWhenReady && ownPauseAck && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> ownPauseAck=false
            playWhenReady && blocksAutomaticPlayback -> hold()
            !playWhenReady && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> {
                focusLossAt=nowMs()
                if(blocksAutomaticPlayback) remember()
            }
            !playWhenReady && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> explicitPause()
        }
    }
}
