package `in`.synthora.musicbox.services

/** Notification-only bridge across an unconfirmed receiver track transition. */
internal class RemoteNotificationTransition {
    private var baseline: EchoState? = null
    private var startedAt: Long? = null

    fun remainingMs(now: Long): Long? = startedAt?.let { (30_000 - (now - it)).takeIf { left -> left > 0 } }

    fun present(incoming: EchoState, now: Long): EchoState {
        val previous = baseline
        val a = previous?.sharedOutput
        val b = incoming.sharedOutput
        val sameReceiver = a != null && a.mode == b.mode && a.owner == b.owner &&
            a.token == b.token && a.serial == b.serial && a.epoch == b.epoch && !b.handoffPending
        val pending = previous?.track != null && previous.playing && incoming.processing &&
            !incoming.confirmed && sameReceiver
        if (!pending) {
            startedAt = null
            baseline = incoming
            return incoming
        }
        val start = startedAt ?: now.also { startedAt = it }
        // An unreachable receiver must not leave controls claiming to buffer forever.
        if (now - start >= 30_000) return incoming
        val track = incoming.track ?: incoming.queue.getOrNull(incoming.index) ?: previous!!.track
        return incoming.copy(track = track, playing = true).also { baseline = it }
    }
}
