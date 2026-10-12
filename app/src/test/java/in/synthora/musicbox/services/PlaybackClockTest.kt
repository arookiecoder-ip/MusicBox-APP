package `in`.synthora.musicbox.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackClockTest {
    private fun state(playing: Boolean = true, anchor: Long = 0) = PlaybackClockState("song", "PHONE", playing, false, anchor, 10_000)
    @Test fun twoConsumersShareSamplingAndLastConsumerStopsIt() = runTest {
        var reads = 0
        val source = MutableStateFlow(state())
        val clock = playbackClock(backgroundScope, source, { reads++; PlaybackProgress(testScheduler.currentTime, 10_000) })
        val first = backgroundScope.launch { clock.collect {} }
        val second = backgroundScope.launch { clock.collect {} }
        runCurrent()
        assertEquals(1, reads)
        advanceTimeBy(300); runCurrent()
        assertEquals(2, reads)
        first.cancel(); runCurrent()
        advanceTimeBy(300); runCurrent()
        assertEquals(3, reads)
        second.cancel(); runCurrent()
        advanceTimeBy(5_000); runCurrent()
        assertEquals(3, reads)
        val again = backgroundScope.launch { clock.collect {} }; runCurrent()
        assertEquals(4, reads)
        assertEquals(testScheduler.currentTime, clock.value.positionMs)
        again.cancel()
    }
    @Test fun pausedClockDoesNotPollButSeekAndOutputChangesRefreshImmediately() = runTest {
        var reads = 0
        val source = MutableStateFlow(state(false))
        val clock = playbackClock(backgroundScope, source, { reads++; PlaybackProgress(it.anchor, it.duration) })
        val observer = backgroundScope.launch { clock.collect {} }; runCurrent()
        advanceTimeBy(5_000); runCurrent()
        assertEquals(1, reads)
        source.value = state(false, 7_000); runCurrent()
        assertEquals(7_000L, clock.value.positionMs)
        source.value = source.value.copy(trackId = "new", output = "ALEXA", anchor = 0); runCurrent()
        assertEquals(0L, clock.value.positionMs)
        assertEquals(3, reads)
        observer.cancel()
    }
    @Test fun loadingSamplesWithoutInventingAudiblePlaybackAndEmptyQueueStops() = runTest {
        var reads = 0
        val source = MutableStateFlow(state(false).copy(loading = true))
        val clock = playbackClock(backgroundScope, source, { reads++; PlaybackProgress(it.anchor, it.duration) })
        val observer = backgroundScope.launch { clock.collect {} }; runCurrent()
        advanceTimeBy(300); runCurrent(); assertEquals(2, reads)
        source.value = source.value.copy(trackId = null); runCurrent()
        val stopped = reads
        advanceTimeBy(3_000); runCurrent(); assertEquals(stopped, reads)
        observer.cancel()
    }
    @Test fun pausedPlayerEventsRefreshRestoredCursorWithoutAnIdleTimer() = runTest {
        var position = 0L
        var reads = 0
        val source = MutableStateFlow(state(false))
        val clock = playbackClock(backgroundScope, source, { reads++; PlaybackProgress(position, it.duration) })
        val observer = backgroundScope.launch { clock.collect {} }; runCurrent()
        position = 8_000
        source.value = source.value.copy(revision = 1); runCurrent()
        assertEquals(8_000L, clock.value.positionMs)
        advanceTimeBy(3_000); runCurrent(); assertEquals(2, reads)
        observer.cancel()
    }
    @Test fun boundsDoNotFabricateDurationOrNegativePositions() {
        assertEquals(PlaybackProgress(100, 100), PlaybackProgress(200, 100).bounded())
        assertEquals(PlaybackProgress(0, 0), PlaybackProgress(-1, -1).bounded())
        assertEquals(0f, PlaybackProgress(200, 0).fraction)
    }
}
