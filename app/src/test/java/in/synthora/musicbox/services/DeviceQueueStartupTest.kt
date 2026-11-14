package `in`.synthora.musicbox.services

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DeviceQueueStartupTest {
    @Test fun playbackStartsWhileServerSynchronizationIsStillBlocked() = runBlocking {
        val server = CompletableDeferred<Unit>()
        var playing = false
        val job = DeviceQueueStartup.start(this, { "stream" }, { playing = true }, { server.await() }, { fail("Unexpected error") })
        yield()
        assertTrue(playing)
        assertFalse(job.isCompleted)
        server.complete(Unit)
        job.join()
    }
    @Test fun synchronizationFailureDoesNotUndoPlayback() = runBlocking {
        var playing = false
        var reported = false
        val job = DeviceQueueStartup.start(this, { "stream" }, { playing = true }, { throw IOException("Offline") }, { reported = true })
        job.join()
        assertTrue(playing)
        assertTrue(reported)
    }
    @Test fun unavailableStreamDoesNotStartPlaybackOrSynchronization() = runBlocking {
        var played = false
        var synchronized = false
        try {
            DeviceQueueStartup.start(this, { throw IOException("No stream") }, { played = true }, { synchronized = true }, {})
            fail("Stream failure must be returned")
        } catch (_: IOException) {}
        assertFalse(played)
        assertFalse(synchronized)
    }
    @Test fun playlistBackfillRunsAfterFailedInitialMirror() = runBlocking {
        val events = mutableListOf<String>()
        val queue = mutableListOf(1)
        val job = DeviceQueueStartup.start(this, { "stream" }, { events += "play" },
            synchronize = { events += "publish"; throw IOException("Offline") },
            onSyncError = { events += "error" },
            afterSynchronization = {
                fillPhonePlaylist((2..1000).toList(), 200, { true }, { queue.addAll(it) }, {})
                events += "backfill"
            })
        job.join()
        assertEquals(listOf("play", "publish", "error", "backfill"), events)
        assertEquals((1..1000).toList(), queue)
    }

    @Test fun cancelledInitialMirrorNeverBackfillsAnObsoleteQueue() = runBlocking {
        val publishing = CompletableDeferred<Unit>()
        var backfilled = false
        val job = DeviceQueueStartup.start(this, { "stream" }, {},
            synchronize = { publishing.complete(Unit); awaitCancellation() },
            onSyncError = { fail("Cancellation is not a sync failure") },
            afterSynchronization = { backfilled = true })
        publishing.await()
        job.cancelAndJoin()
        assertFalse(backfilled)
    }

}
