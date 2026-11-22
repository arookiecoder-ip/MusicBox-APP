package `in`.synthora.musicbox.services

import kotlinx.coroutines.delay

/** Use elapsed time so delayed coroutine dispatch cannot extend a sleep timer. */
internal suspend fun awaitSleepDeadline(durationMs: Long, nowMillis: () -> Long, onRemaining: (Long) -> Unit) {
    require(durationMs in 1..180 * 60_000L)
    val deadline = nowMillis() + durationMs
    while (true) {
        val remaining = (deadline - nowMillis()).coerceAtLeast(0)
        onRemaining(remaining)
        if (remaining == 0L) return
        delay(minOf(1000L, remaining))
    }
}
