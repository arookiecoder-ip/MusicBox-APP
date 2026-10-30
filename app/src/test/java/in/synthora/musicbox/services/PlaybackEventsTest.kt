package `in`.synthora.musicbox.services

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class PlaybackEventsTest {
    @Test fun newerEventsWinOverDelayedHttpAndPreviousServerEpoch() = runBlocking {
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        val stream = MutableSharedFlow<JsonObject>(extraBufferCapacity = 10)
        val httpStarted = CompletableDeferred<Unit>()
        val httpReply = CompletableDeferred<JsonElement>()
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("realtime-order", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "speaker").commit()
        val controller = EchoController(owner, prefs, {}, {}, read = { _, _ ->
            httpStarted.complete(Unit); httpReply.await()
        }, write = { _, _ -> error("unexpected command") },
            snapshotDispatcher = Dispatchers.Unconfined, events = { stream })
        try {
            controller.observeEvents(true)
            withTimeout(2_000) { stream.subscriptionCount.first { it == 1 } }
            val pending = owner.launch { controller.refresh(force = true, stateOnly = true) }
            withTimeout(2_000) { httpStarted.await() }
            stream.emit(envelope("server-a", 2, "Song B"))
            awaitTitle(controller, "Song B") // Must not wait behind the HTTP request.
            httpReply.complete(snapshot("server-a", 1, "Song A"))
            pending.join()
            assertEquals("Song B", controller.state.value.track?.title)
            stream.emit(envelope("server-b", 0, "Restarted server"))
            awaitTitle(controller, "Restarted server")
            stream.emit(envelope("server-a", 999, "Retired server"))
            delay(20)
            assertEquals("Restarted server", controller.state.value.track?.title)
        } finally { controller.clear(); owner.cancel() }
    }

    @Test fun deviceChangeAndSignOutCancelOldEventSubscriptions() = runBlocking {
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        val streams = mutableMapOf<String, MutableSharedFlow<JsonObject>>()
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("realtime-lifecycle", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "first").commit()
        val controller = EchoController(owner, prefs, {}, {}, read = { _, _ -> error("unexpected read") },
            write = { _, _ -> error("unexpected write") }, snapshotDispatcher = Dispatchers.Unconfined,
            events = { streams.getOrPut(it) { MutableSharedFlow(extraBufferCapacity = 4) } })
        try {
            controller.observeEvents(true)
            yield()
            val first = streams.getValue("first")
            withTimeout(2_000) { first.subscriptionCount.first { it == 1 } }
            controller.select("second", refreshAfter = false)
            yield()
            withTimeout(2_000) { first.subscriptionCount.first { it == 0 } }
            val second = streams.getValue("second")
            withTimeout(2_000) { second.subscriptionCount.first { it == 1 } }
            first.emit(envelope("server", 99, "Old device"))
            second.emit(envelope("server", 1, "New device"))
            awaitTitle(controller, "New device")
            controller.clear()
            withTimeout(2_000) { second.subscriptionCount.first { it == 0 } }
            assertNull(controller.state.value.track)
        } finally { controller.clear(); owner.cancel() }
    }

    @Test fun eventErrorsAreDeliveredOnceAndQueueEntriesSurviveSlimUpdates() = runBlocking {
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        val stream = MutableSharedFlow<JsonObject>(extraBufferCapacity = 4)
        val errors = mutableListOf<String>()
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("realtime-delta", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "speaker").commit()
        val controller = EchoController(owner, prefs, errors::add, {},
            snapshotDispatcher = Dispatchers.Unconfined, events = { stream })
        try {
            controller.observeEvents(true)
            withTimeout(2_000) { stream.subscriptionCount.first { it == 1 } }
            val initial = JsonObject(snapshot("server", 1, "Initial") + mapOf(
                "queue" to JsonArray(listOf(buildJsonObject {
                    put("video_id", "abcdefghijk"); put("title", "Queued"); put("entry_id", "unique-entry")
                })), "queue_version" to JsonPrimitive(8)))
            stream.emit(buildJsonObject { put("state", initial) })
            awaitTitle(controller, "Initial")
            assertEquals(1, controller.state.value.queue.size)
            val delta = JsonObject(snapshot("server", 2, "Updated") + mapOf(
                "playback_error_id" to JsonPrimitive("one-error"),
                "playback_error" to buildJsonObject { put("message", "Device unavailable") }))
            repeat(2) { stream.emit(buildJsonObject { put("state", delta) }) }
            awaitTitle(controller, "Updated")
            delay(20)
            assertEquals(1, controller.state.value.queue.size)
            assertEquals("unique-entry", controller.state.value.queue.single().uuid)
            assertEquals(listOf("Device unavailable"), errors)
        } finally { controller.clear(); owner.cancel() }
    }

    @Test fun eventBackoffHonoursServerRetryAndUnsupportedServers() {
        assertEquals(60_000L, eventReconnectDelay(1, true))
        assertEquals(120_000L, eventReconnectDelay(1, false, 120_000))
        assertEquals(60_000L, eventReconnectDelay(100, false))
        val order = PlaybackEventOrder()
        order.accepted("first", 10)
        assertFalse(order.accepts("first", 9))
        assertTrue(order.accepts("first", 10))
        order.accepted("restart", 0)
        assertFalse(order.accepts("first", 100))
        order.reset()
        assertTrue(order.accepts("first", 1))
    }

    private suspend fun awaitTitle(controller: EchoController, title: String) {
        withTimeout(2_000) { controller.state.first { it.track?.title == title } }
    }
    private fun envelope(epoch: String, revision: Long, title: String) = buildJsonObject {
        put("state", snapshot(epoch, revision, title))
    }
    private fun snapshot(epoch: String, revision: Long, title: String) = buildJsonObject {
        put("event_epoch", epoch); put("event_revision", revision)
        put("output_epoch", epoch); put("output_revision", 1)
        put("playback_revision", revision); put("playback_output", "alexa")
        put("video_id", "abcdefghijk"); put("title", title); put("playing", true)
        put("playback_confirmed", true)
    }
}
