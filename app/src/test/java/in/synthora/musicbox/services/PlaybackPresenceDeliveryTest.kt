package `in`.synthora.musicbox.services

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackPresenceDeliveryTest {
    @Test fun stateOnlyEventDoesNotEraseQueuedCommands() = runBlocking {
        val owner = CoroutineScope(coroutineContext + Job())
        val commands = mutableListOf<String>()
        val delivered = CompletableDeferred<Unit>()
        val delivery = PlaybackPresenceDelivery<Int, String>(owner,
            onState = {}, onCommands = { _, command -> commands += command; delivered.complete(Unit) })
        try {
            delivery.offer(1, "pause")
            delivery.offerState(2)
            withTimeout(1_000) { delivered.await() }
            assertEquals(listOf("pause"), commands)
        } finally { owner.cancel() }
    }
    @Test fun switchDoesNotWaitForAnEarlierSongCommandToFinishLoading() = runBlocking {
        val owner = CoroutineScope(coroutineContext + Job())
        val loading = CompletableDeferred<Unit>()
        val finishLoad = CompletableDeferred<Unit>()
        val switched = CompletableDeferred<Unit>()
        val delivery = PlaybackPresenceDelivery<Int, String>(owner,
            onState = { if (it == 2) switched.complete(Unit) },
            onCommands = { _, command -> if (command == "slow-song") { loading.complete(Unit); finishLoad.await() } })
        try {
            delivery.offer(1, "slow-song")
            withTimeout(1000) { loading.await() }
            delivery.offer(2, "")
            withTimeout(500) { switched.await() }
            assertFalse(finishLoad.isCompleted)
        } finally { owner.cancel() }
    }
}
