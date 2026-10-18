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
