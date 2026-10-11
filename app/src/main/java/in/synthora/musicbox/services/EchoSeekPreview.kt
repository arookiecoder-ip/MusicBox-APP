package `in`.synthora.musicbox.services

/** Hold a seek's cursor while older polls or an unconfirmed Alexa restart arrive. */
internal data class EchoSeekPreview(val previous: EchoState, val position: Long, val until: Long) {
    fun reconcile(snapshot: EchoState, now: Long): EchoState {
        val changed = snapshot.track?.ytVideoId != previous.track?.ytVideoId || snapshot.index != previous.index ||
            snapshot.sharedOutput.mode != previous.sharedOutput.mode ||
            snapshot.sharedOutput.token != previous.sharedOutput.token ||
            snapshot.sharedOutput.serial != previous.sharedOutput.serial
        val confirmed = snapshot.playbackRevision > previous.playbackRevision && snapshot.confirmed &&
            !snapshot.processing && kotlin.math.abs(snapshot.positionMs - position) <= 2_000
        if (changed || confirmed || now >= until) return snapshot
        return snapshot.copy(positionMs = position, anchoredAt = now,
            playing = previous.playing, processing = previous.playing, confirmed = !previous.playing)
    }
}
