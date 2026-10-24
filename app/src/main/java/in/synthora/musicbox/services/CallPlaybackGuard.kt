package `in`.synthora.musicbox.services

import androidx.media3.common.Player

/** Explicit play and audio-focus restoration both stay inaudible during calls. */
internal class CallPlaybackGuard(
    private val isInCall: () -> Boolean,
    private val pause: () -> Unit,
    private val scheduleResume: () -> Unit,
    private val cancelResume: () -> Unit
) : Player.Listener {
    private var holding = false
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady && isInCall()) {
            holding = true
            try { pause() } catch (error: Throwable) { holding = false; throw error }
            scheduleResume()
        } else if (!playWhenReady && holding && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            // Media3 may dispatch the pause acknowledgement after pause() returns.
            holding = false
        } else if (!playWhenReady && !holding && isInCall() && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) {
            scheduleResume()
        } else if (!playWhenReady && !holding && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            cancelResume()
        }
    }
}
