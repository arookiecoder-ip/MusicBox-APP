package `in`.synthora.musicbox.services
import org.junit.Assert.*
import org.junit.Test
class QueueContinuationTest {
    @Test fun emptyRadioAndNetworkFailuresRetryTheSameSeedAfterBackoff() {
        val retry = QueueContinuationRetry()
        assertTrue(retry.ready("lease:last-song", 0))
        retry.deferred(100)
        assertFalse(retry.ready("lease:last-song", 30_099))
        assertTrue(retry.ready("lease:last-song", 30_100))
        retry.deferred(30_100)
        assertTrue(retry.ready("lease:last-song", 60_100))
        retry.completed("lease:last-song")
        assertFalse(retry.ready("lease:last-song", 90_100))
        assertTrue(retry.ready("lease:next-song", 90_100))
        assertTrue(retry.ready("new-lease:last-song", 90_100))
    }

    @Test fun sourceRecoveryRetainsIntentButDoesNotFightCallsOrAnExplicitPause() {
        assertTrue(shouldRecoverStream(false, true, false, false))
        assertTrue(shouldRecoverStream(false, false, true, false))
        assertTrue(shouldRecoverStream(true, false, false, false))
        assertFalse(shouldRecoverStream(false, false, false, false))
        assertFalse(shouldRecoverStream(false, true, true, true))
        assertFalse(shouldRecoverStream(true, false, true, true))
    }
    @Test fun radioDoesNotRepeatPlayedSongsOrDuplicateCandidates() {
        assertEquals(listOf("c", "d"), unheardQueueIds(listOf("a", "b"), listOf("a", "c", "c", "b", "d")))
    }
    @Test fun fullQueueIsNeverExtendedBeyondItsActualCapacity() {
        assertEquals(listOf("new"), unheardQueueIds(List(4999) { "$it" }, listOf("new", "another")))
        assertTrue(unheardQueueIds(List(5000) { "$it" }, listOf("new")).isEmpty())
    }
    @Test fun shuffleEditsNewPlayAndOutputSwitchInvalidatePendingContinuation() {
        assertTrue(canApplyQueueContinuation(listOf("a", "b"), listOf("a", "b"), "epoch", "epoch", true))
        assertFalse(canApplyQueueContinuation(listOf("a", "b"), listOf("b", "a"), "epoch", "epoch", true))
        assertFalse(canApplyQueueContinuation(listOf("a"), listOf("a", "new"), "epoch", "epoch", true))
        assertFalse(canApplyQueueContinuation(listOf("a"), listOf("a"), "old", "new", true))
        assertFalse(canApplyQueueContinuation(listOf("a"), listOf("a"), "epoch", "epoch", false))
    }
    @Test fun exhaustedQueueContinuesWithoutSkippingNativeAutoAdvanceOrOverridingPause() {
        assertTrue(shouldAdvanceExhaustedQueue(true, 9, 9, true, true))
        assertFalse(shouldAdvanceExhaustedQueue(true, 9, 10, true, true))
        assertFalse(shouldAdvanceExhaustedQueue(true, 9, 9, false, true))
        assertFalse(shouldAdvanceExhaustedQueue(false, 9, 9, true, true))
        assertFalse(shouldAdvanceExhaustedQueue(true, 9, 9, true, false))
    }
}
