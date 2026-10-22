package `in`.synthora.musicbox.services

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class EchoControllerLifecycleTest {
    @Test fun acceptedResponseStillEmitsErrorsAndRefreshesVolume() = runBlocking {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("accepted-lifecycle", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "device").commit()
        val errors = mutableListOf<String>()
        val reads = mutableListOf<String>()
        val controller = EchoController(this, prefs, errors::add, {}, read = { path, _ ->
            reads += path
            if (path == "/alexa/volume/") buildJsonObject { put("volume", 42) }
            else buildJsonObject {
                put("video_id", "current-song"); put("title", "Current song")
                put("playback_error", buildJsonObject { put("message", "Current error") })
            }
        }, write = { _, _ -> error("unexpected write") })
        try {
            controller.refresh(force = true)
            yield()
            assertEquals("Current song", controller.state.value.track?.title)
            assertEquals(listOf("Current error"), errors)
            assertEquals(listOf("/alexa/now_playing/", "/alexa/volume/"), reads)
            assertEquals(42, controller.state.value.volume)
        } finally { controller.clear() }
    }

    @Test fun signOutDuringDecodingDiscardsResponseSideEffects() = runBlocking {
        verifyDiscardDuringDecoding { it.clear() }
    }

    @Test fun commandDuringDecodingDiscardsResponseSideEffects() = runBlocking {
        verifyDiscardDuringDecoding { it.command("next", refreshAfter = false) }
    }

    @Test fun deviceChangeDuringDecodingDiscardsResponseSideEffects() = runBlocking {
        verifyDiscardDuringDecoding { it.select("new-device", refreshAfter = false) }
    }

    private suspend fun verifyDiscardDuringDecoding(transition: suspend (EchoController) -> Unit) = coroutineScope {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("decode-lifecycle", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "old-device").commit()
        val decoding = CompletableDeferred<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
                check(decoding.complete(block)) { "Unexpected extra snapshot decode" }
            }
        }
        val errors = mutableListOf<String>()
        var reads = 0
        val controller = EchoController(this, prefs, errors::add, {}, read = { _, _ ->
            reads++
            buildJsonObject {
                put("video_id", "old-song"); put("title", "Old song")
                put("playback_error", buildJsonObject { put("message", "stale error") })
            }
        }, write = { _, _ -> buildJsonObject {} }, snapshotDispatcher = dispatcher)
        try {
            val pending = launch { controller.refresh(force = true) }
            val decode = withTimeout(5_000) { decoding.await() }
            transition(controller)
            decode.run()
            withTimeout(5_000) { pending.join() }
            yield() // Let any incorrectly scheduled volume read run before checking.
            assertNull(controller.state.value.track)
            assertTrue("Discarded decoding must not emit stale errors", errors.isEmpty())
            assertEquals("Discarded decoding must not launch follow-up reads", 1, reads)
        } finally { controller.clear() }
    }

    @Test fun responseFromPreviousDeviceCannotChangeStateOrEmitErrors() = runBlocking {
        verifyDiscard { it.select("new-device", refreshAfter = false) }
    }

    @Test fun signOutDiscardsPendingResponseAndItsSideEffects() = runBlocking {
        verifyDiscard { it.clear() }
    }

    private suspend fun verifyDiscard(transition: (EchoController) -> Unit) = coroutineScope {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("lifecycle", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "old-device").commit()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JsonElement>()
        val errors = mutableListOf<String>()
        var reads = 0
        val controller = EchoController(this, prefs, errors::add, {},
            read = { _, _ ->
                reads++
                started.complete(Unit)
                response.await()
            }, write = { _, _ -> error("unexpected write") })
        val pending = launch { controller.refresh(force = true) }
        withTimeout(5_000) { started.await() }
        transition(controller)
        response.complete(buildJsonObject {
            put("video_id", "old-song"); put("title", "Old song")
            put("playback_error", buildJsonObject { put("message", "stale error") })
        })
        withTimeout(5_000) { pending.join() }
        assertNull(controller.state.value.track)
        assertTrue(errors.isEmpty())
        assertEquals("stale response must not launch volume or account reads", 1, reads)
        controller.clear()
    }
}
