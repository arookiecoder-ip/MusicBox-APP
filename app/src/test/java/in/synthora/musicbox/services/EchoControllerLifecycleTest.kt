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
    @Test fun seekPreviewIsImmediateAndQueuedScrubsOnlySendTheLatestIntent() = runBlocking {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("coalesced-seek", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "device").commit()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val writes = mutableListOf<JsonObject>()
        val controller = EchoController(this, prefs, { fail(it) }, {},
            read = { _, _ -> buildJsonObject {
                put("video_id", "AAAAAAAAAAA"); put("title", "Current song"); put("duration_ms", 200_000)
                put("position_ms", 5_000); put("playing", true); put("playback_confirmed", true)
                put("playback_output", "alexa"); put("playback_revision", 1)
            } }, write = { path, body ->
                assertEquals("/alexa/seek/", path)
                writes += body
                if (writes.size == 1) { started.complete(Unit); release.await() }
                buildJsonObject { put("ok", true) }
            })
        try {
            controller.refresh(force = true, stateOnly = true)
            val first = launch { controller.seek(20_000, refreshAfter = false) }
            withTimeout(5_000) { started.await() }
            assertEquals(20_000L, controller.state.value.positionMs)
            val second = launch { controller.seek(40_000, refreshAfter = false) }; yield()
            val third = launch { controller.seek(60_000, refreshAfter = false) }; yield()
            assertEquals("The preview cannot wait for the outstanding HTTP request", 60_000L, controller.state.value.positionMs)
            assertEquals(1, writes.size)
            release.complete(Unit)
            withTimeout(5_000) { joinAll(first, second, third) }
            assertEquals(listOf(20_000L, 60_000L), writes.map { it.getValue("position_ms").jsonPrimitive.long })
            assertTrue(writes.all { it.getValue("async").jsonPrimitive.boolean })
        } finally { release.complete(Unit); controller.clear() }
    }

    @Test fun uncertainAvailabilityPreservesOnlyAnExplicitlySavedDevice() = runBlocking {
        for (saved in listOf("", "uncertain")) {
            val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("uncertain-default-$saved", Context.MODE_PRIVATE)
            prefs.edit().clear().putString("echo_serial", saved).commit()
            val controller = EchoController(this, prefs, { fail(it) }, {}, read = { path, _ ->
                assertEquals("/alexa/init/", path)
                buildJsonObject {
                    put("alexa_access_enabled", true)
                    put("status", buildJsonObject { put("logged_in", true) })
                    put("serial", "uncertain")
                    put("devices", buildJsonArray { add(buildJsonObject {
                        put("serial", "uncertain"); put("name", "Echo")
                        put("online", false); put("availability_unknown", true)
                    }) })
                }
            }, write = { _, _ -> error("unexpected write") })
            try {
                assertEquals(saved.isNotBlank(), controller.loadDevices())
                assertEquals(saved, controller.serial.value)
                assertTrue(controller.devices.value.single().canConnect)
                assertFalse(controller.devices.value.single().online)
            } finally { controller.clear() }
        }
    }

}
