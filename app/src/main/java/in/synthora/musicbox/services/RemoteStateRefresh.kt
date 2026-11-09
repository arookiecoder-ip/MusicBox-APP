package `in`.synthora.musicbox.services

/** Presence revisions carry queue/state changes; lease countdowns do not require a queue read. */
internal class RemoteStateRefresh {
    private var identity = ""
    private var revision = Long.MIN_VALUE
    private var refreshedAt = Long.MIN_VALUE
    fun needed(output: SharedPlaybackOutput, now: Long): Boolean {
        val key = listOf(output.mode, output.owner, output.token, output.epoch).joinToString("|")
        return key != identity || (output.presenceRevision >= 0 && output.presenceRevision != revision) ||
            refreshedAt == Long.MIN_VALUE || now - refreshedAt >= 30_000
    }
    fun completed(output: SharedPlaybackOutput, now: Long) {
        identity = listOf(output.mode, output.owner, output.token, output.epoch).joinToString("|")
        revision = output.presenceRevision
        refreshedAt = now
    }
}
