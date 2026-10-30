package `in`.synthora.musicbox.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PlaybackEventStreamTest {
    @Test fun conflatedDeltasKeepTheQueueAndTransfersRequireNewFullState() {
        val accumulator = PlaybackEventAccumulator()
        fun event(state: String) = kotlinx.serialization.json.Json.parseToJsonElement(
            "{\"state\":$state}").objectOrEmpty()
        accumulator.accept(event("""{"queue":[{"entry_id":"first"},{"entry_id":"second"}],"queue_version":1,"output_token":"one","event_epoch":"server"}"""))
        val delta = accumulator.accept(event("""{"queue_version":1,"output_token":"one","event_epoch":"server","position_ms":2000}"""))
        assertEquals(2, delta["state"].objectOrEmpty().array("queue").size)
        val missing = runCatching { accumulator.accept(event("""{"queue_version":1,"output_token":"two","event_epoch":"server"}""")) }.exceptionOrNull()
        assertTrue(missing is java.io.IOException)
    }

    @Test fun realSocketReceivesSnapshotAndCancellationClosesIt() = runBlocking {
        val server = MockWebServer()
        val disconnected = CountDownLatch(1)
        val client = OkHttpClient()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""{"type":"playback_state","state":{"title":"Actual track"}}""")
            }
            override fun onFailure(webSocket: WebSocket, error: Throwable, response: Response?) {
                disconnected.countDown()
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null); disconnected.countDown()
            }
        }))
        try {
            val result = withTimeout(5_000) { playbackEventStream(client, server.url("/api/app/events/")).first() }
            assertEquals("Actual track", result["state"].objectOrEmpty().text("title"))
            assertTrue("Cancelling collection must close its socket", disconnected.await(3, TimeUnit.SECONDS))
        } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown() }
    }

    @Test fun expiredSessionAndRetryAfterArePreserved() = runBlocking {
        for (code in listOf(401, 429)) {
            val server = MockWebServer()
            val client = OkHttpClient()
            server.enqueue(MockResponse().setResponseCode(code).setHeader("Retry-After", "30"))
            try {
                val error = runCatching {
                    withTimeout(5_000) { playbackEventStream(client, server.url("/api/app/events/")).first() }
                }.exceptionOrNull()
                if (code == 401) assertTrue(error is BackendAuthException)
                else {
                    assertTrue(error is BackendHttpException)
                    assertEquals(30_000L, (error as BackendHttpException).retryAfterMs)
                }
            } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown() }
        }
    }

    @Test fun malformedFramesFailInsteadOfReplacingPlaybackWithEmptyState() = runBlocking {
        val server = MockWebServer()
        val client = OkHttpClient()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { webSocket.send("{broken") }
        }))
        try {
            val error = runCatching {
                withTimeout(5_000) { playbackEventStream(client, server.url("/api/app/events/")).first() }
            }.exceptionOrNull()
            assertTrue(error is java.io.IOException)
            assertEquals("Invalid playback event", error?.message)
        } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown() }
    }
}
