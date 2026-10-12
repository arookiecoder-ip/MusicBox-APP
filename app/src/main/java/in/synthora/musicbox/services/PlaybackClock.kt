package `in`.synthora.musicbox.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

/** Small clock state; queues/artwork are deliberately absent from the sampling loop. */
data class PlaybackProgress(val positionMs: Long = 0, val durationMs: Long = 0) {
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    fun bounded() = copy(positionMs = if (durationMs > 0) positionMs.coerceIn(0, durationMs) else positionMs.coerceAtLeast(0),
        durationMs = durationMs.coerceAtLeast(0))
}
internal data class PlaybackClockState(val trackId: String?, val output: String, val playing: Boolean,
    val loading: Boolean, val anchor: Long, val duration: Long, val revision: Long = 0)

/** All visible progress/lyric consumers share one clock; no subscribers means no wakeups. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun playbackClock(scope: CoroutineScope, source: Flow<PlaybackClockState>,
    read: (PlaybackClockState) -> PlaybackProgress, intervalMs: Long = 300): StateFlow<PlaybackProgress> =
    source.distinctUntilChanged().flatMapLatest { state -> flow {
        emit(read(state).bounded())
        while (state.trackId != null && (state.playing || state.loading)) {
            delay(intervalMs)
            emit(read(state).bounded())
        }
    } }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(0, 0), PlaybackProgress())
