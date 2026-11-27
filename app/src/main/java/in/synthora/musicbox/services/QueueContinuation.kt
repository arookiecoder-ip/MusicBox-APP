package `in`.synthora.musicbox.services

/** Explicit Pause and a live call must win over source retry callbacks. */
internal fun shouldRecoverStream(manual: Boolean, wantsPlayback: Boolean,
    recoveryPending: Boolean, callBlocksPlayback: Boolean): Boolean =
    !callBlocksPlayback && (manual || wantsPlayback || recoveryPending)

internal class QueueContinuationRetry {
    private var completedSeed = ""
    private var retryAt = 0L
    fun ready(seed: String, now: Long): Boolean = seed != completedSeed && now >= retryAt
    fun completed(seed: String) { completedSeed = seed; retryAt = 0 }
    fun deferred(now: Long) { retryAt = now + 30_000 }
}

internal fun unheardQueueIds(existing: List<String>, radio: List<String>): List<String> {
    val played = existing.toSet()
    return radio.distinct().filter { it !in played }.take((5000 - existing.size).coerceAtLeast(0))
}

internal fun canApplyQueueContinuation(before: List<String>, now: List<String>, expectedToken: String,
    currentToken: String, phoneOwnsPlayback: Boolean): Boolean = phoneOwnsPlayback &&
    expectedToken.isNotBlank() && expectedToken == currentToken && before == now

internal fun shouldAdvanceExhaustedQueue(ended: Boolean, beforeIndex: Int, afterIndex: Int,
    playWhenReady: Boolean, hasNext: Boolean): Boolean = ended && beforeIndex == afterIndex && playWhenReady && hasNext
