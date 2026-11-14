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
    private var settlingRetries = 0
    private var otherAudioSince: Long? = null
    val isSettlingRetry get() = settlingRetries > 0 && resumePending
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

    /** Call audio/focus can linger briefly after NORMAL; a different music app wins. */
    fun waitForAudioRelease(otherAudioActive: Boolean): Boolean {
        if (!otherAudioActive) { otherAudioSince = null; return false }
        val now = nowMs()
        val since = otherAudioSince ?: now.also { otherAudioSince = it }
        if (now - since >= 2_500) explicitPause()
        return true
    }

    fun explicitPlay() {
        otherAudioSince=null
        resumePending=false
        focusLossAt=null
        settlingUntil=0L
        settlingRetries=0
        normalSince=null
        cancelResume()
        manualDuringCall=isInCall()
        updateManualFocus(manualDuringCall)
    }
    fun explicitPause() {
        otherAudioSince=null
        resumePending=false
        focusLossAt=null
        settlingUntil=0L
        settlingRetries=0
        normalSince=null
        manualDuringCall=false
        ownPauseAck=false
        cancelResume()
        // Restoring media focus during a live call can emit a new focus-loss event.
        // Keep an already disabled focus handler idle until the call ends.
        if(!isInCall()) updateManualFocus(false)
    }
    fun resumeConsumed() {
        otherAudioSince=null
        val now=nowMs()
        // Start this window when the renderer is about to resume, after any
        // asynchronous lease recovery. Retries never extend the call's window.
        if (settlingUntil == 0L) settlingUntil=now+8_000
        resumePending=false
        focusLossAt=null
        cancelResume()
    }
    private fun remember() { if (!resumePending) { resumePending=true;scheduleResume() } }
    private fun hold() {
        normalSince=null
        remember() // Notification/foreground retention sees the intent before Pause events.
        ownPauseAck=true
        try { pause() } catch(error:Throwable) { ownPauseAck=false;throw error }
    }
    fun modeChanged(wantsPlayback:Boolean) {
        if(isInCall()) {
            settlingUntil=0L
            settlingRetries=0
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
                if(blocksAutomaticPlayback) { normalSince=null;remember() }
                else if (!isInCall() && settlingUntil > 0 && nowMs() <= settlingUntil && settlingRetries < 3) {
                    // Modem/VoIP stacks can deliver several final focus losses
                    // after NORMAL. Wait for mode to settle again before each
                    // bounded retry; never fight a later music app indefinitely.
                    settlingRetries++
                    normalSince=null
                    remember()
                }
            }
            !playWhenReady && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> explicitPause()
        }
    }
}

/** A lost/renewed local lease can be recovered after a long call; a handoff cannot. */
internal fun canRecoverCallLease(expected: String, current: String,
    output: SharedPlaybackOutput, ownId: String): Boolean = current == expected ||
    (!blocksLocalResumption(output, ownId) && !output.isRemotePhoneOutput(ownId) &&
        (current.isBlank() || (output.owner == ownId && output.token == current)))
