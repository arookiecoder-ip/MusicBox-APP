package `in`.synthora.musicbox.services

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PlaybackTargetRestorationTest {
    @Test fun earlyPlayWaitsForTheAuthoritativeTargetAndSharesTheStartupLookup() = runBlocking {
        val restoration = PlaybackTargetRestoration()
        val response = CompletableDeferred<Unit>()
        var lookups = 0
        var target = ""
        var sentTo: String? = null
        val startup = launch { restoration.ensure { lookups++; response.await(); target = "other-phone" } }
        yield()
        val play = launch { if (restoration.ensure { lookups++; fail("Duplicate lookup") }) sentTo = target }
        yield()
        assertTrue(restoration.loading.value)
        assertNull(sentTo)
        response.complete(Unit)
        joinAll(startup, play)
        assertEquals(1, lookups)
        assertEquals("other-phone", sentTo)
        assertTrue(restoration.ready)
        assertFalse(restoration.loading.value)
    }

    @Test fun failedLookupStopsLoadingAndTheNextPlayRetries() = runBlocking {
        val restoration = PlaybackTargetRestoration()
        try { restoration.ensure { throw IOException("offline") }; fail("Expected failure") }
        catch (_: IOException) {}
        assertFalse(restoration.ready)
        assertFalse(restoration.loading.value)
        assertTrue(restoration.ensure { })
        assertTrue(restoration.ready)
    }

    @Test fun signoutWhileWaitingPreventsTheOldPlayFromRunning() = runBlocking {
        val restoration = PlaybackTargetRestoration()
        val response = CompletableDeferred<Unit>()
        var dispatched = false
        val play = launch { if (restoration.ensure { response.await() }) dispatched = true }
        yield()
        restoration.clear()
        response.complete(Unit)
        play.join()
        assertFalse(dispatched)
        assertFalse(restoration.loading.value)
    }

    @Test fun aNewSessionRequiresItsOwnLookupEvenIfAnOldLookupFinishes() = runBlocking {
        val restoration = PlaybackTargetRestoration()
        val response = CompletableDeferred<Unit>()
        val old = async { restoration.ensure { response.await() } }
        yield()
        restoration.reset()
        response.complete(Unit)
        assertFalse(old.await())
        assertFalse(restoration.ready)
        var resolved = false
        assertTrue(restoration.ensure { resolved = true })
        assertTrue(resolved)
    }

    @Test fun canceledPlayDoesNotMarkAnUnresolvedTargetReady() = runBlocking {
        val restoration = PlaybackTargetRestoration()
        val play = launch { restoration.ensure { awaitCancellation() } }
        yield()
        play.cancelAndJoin()
        assertFalse(restoration.ready)
        assertFalse(restoration.loading.value)
        assertTrue(restoration.ensure { })
    }

    @Test fun missingOwnerAndThisPhonesOwnerNeverBecomeRemoteCommandTargets() {
        val output = SharedPlaybackOutput(mode = "phone", token = "old-token")
        assertFalse(output.isRemotePhoneOutput("this-phone"))
        assertFalse(output.copy(owner = " ").isRemotePhoneOutput("this-phone"))
        assertFalse(output.copy(owner = "this-phone").isRemotePhoneOutput("this-phone"))
        assertTrue(output.copy(owner = "other-phone").isRemotePhoneOutput("this-phone"))
        assertFalse(output.copy(mode = "alexa", owner = "other-phone").isRemotePhoneOutput("this-phone"))
    }
}
