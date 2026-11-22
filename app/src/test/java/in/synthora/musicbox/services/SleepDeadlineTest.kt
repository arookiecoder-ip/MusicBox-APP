package `in`.synthora.musicbox.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepDeadlineTest {
    @Test fun deadlineDoesNotDriftWhenDispatchIsLate() = runTest {
        var elapsed = 0L
        var remaining = -1L
        var expired = false
        val timer = launch { awaitSleepDeadline(60_000, { elapsed }) { remaining = it }; expired = true }
        runCurrent()
        assertEquals(60_000L, remaining)
        elapsed = 59_900
        advanceTimeBy(1000); runCurrent()
        assertEquals(100L, remaining)
        elapsed = 65_000
        advanceTimeBy(100); runCurrent()
        assertTrue(expired)
        assertEquals(0L, remaining)
        timer.join()
    }
    @Test fun cancellingOldDeadlineCannotExpireReplacement() = runTest {
        var expirations = 0
        val first = launch { awaitSleepDeadline(1000, { testScheduler.currentTime }) {}; expirations++ }
        runCurrent()
        first.cancel()
        val replacement = launch { awaitSleepDeadline(3000, { testScheduler.currentTime }) {}; expirations++ }
        runCurrent()
        advanceTimeBy(1000); runCurrent()
        assertEquals(0, expirations)
        advanceTimeBy(2000); runCurrent()
        assertEquals(1, expirations)
        replacement.join()
    }
}
