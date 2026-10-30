package `in`.synthora.musicbox.services

/** Stream and HTTP snapshots share a server epoch/revision. A retired server
 * epoch cannot win after reconnect, even if its HTTP response arrives late. */
internal class PlaybackEventOrder {
    private var epoch = ""
    private var revision = -1L
    private val retired = linkedSetOf<String>()
    fun accepts(incomingEpoch: String, incomingRevision: Long): Boolean =
        incomingEpoch.isBlank() || (incomingEpoch !in retired &&
            (incomingEpoch != epoch || incomingRevision >= revision))
    fun accepted(incomingEpoch: String, incomingRevision: Long) {
        if (incomingEpoch.isBlank()) return
        if (epoch.isNotBlank() && epoch != incomingEpoch) {
            retired += epoch
        }
        epoch = incomingEpoch
        revision = incomingRevision
    }
    fun reset() { epoch = ""; revision = -1; retired.clear() }
}

internal fun eventReconnectDelay(failures: Int, unsupported: Boolean, retryAfter: Long = 0): Long =
    maxOf(if (unsupported) 60_000L else (1_000L shl failures.coerceIn(0, 6)).coerceAtMost(60_000),
        retryAfter.coerceAtLeast(0))
