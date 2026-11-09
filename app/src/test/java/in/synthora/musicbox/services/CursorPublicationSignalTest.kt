package `in`.synthora.musicbox.services

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CursorPublicationSignalTest {
    @Test fun pauseOrSeekWakesSleepingPublisherWithoutWaitingThreeSeconds() = runBlocking {
        val signal = CursorPublicationSignal()
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { signal.awaitChange() }
        signal.changed()
        assertTrue(withTimeout(500) { waiting.await() })
    }
    @Test fun rapidEventsCoalesceAndFallbackStillRuns() = runBlocking {
        val signal = CursorPublicationSignal()
        repeat(100) { signal.changed() }
        assertTrue(signal.awaitChange(100))
        assertFalse(signal.awaitChange(10))
    }
}
