package `in`.synthora.musicbox.network

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/** Transport owns no retry loop; the shared controller owns reconnect policy. */
internal class PlaybackEventAccumulator {
    private var queue: kotlinx.serialization.json.JsonElement? = null
    private var queueVersion = -1L
    private var token = ""
    private var epoch = ""
    fun accept(payload: JsonObject): JsonObject {
        val state = payload["state"].objectOrEmpty()
        val incomingVersion = state.number("queue_version")
        val incomingToken = state.text("output_token")
        val incomingEpoch = state.text("event_epoch")
        val supplied = state["queue"]
        if (supplied != null) {
            queue = supplied
            queueVersion = incomingVersion
            token = incomingToken
            epoch = incomingEpoch
        } else if (state["queue_version"] != null &&
            (queue == null || incomingVersion != queueVersion || incomingToken != token || incomingEpoch != epoch)) {
            throw java.io.IOException("Missing playback queue snapshot")
        }
        // A conflated slim event must retain the full queue it superseded.
        return if (supplied == null && queue != null)
            JsonObject(payload + ("state" to JsonObject(state + ("queue" to requireNotNull(queue))))) else payload
    }
}

internal fun playbackEventStream(client: OkHttpClient, url: HttpUrl): Flow<JsonObject> = callbackFlow {
    val socket = client.newWebSocket(okhttp3.Request.Builder().url(url).build(),
        object : okhttp3.WebSocketListener() {
            private val accumulator = PlaybackEventAccumulator()
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                val payload = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull()
                if (payload == null || payload.text("type") != "playback_state" || payload["state"] !is JsonObject) {
                    close(java.io.IOException("Invalid playback event")); webSocket.cancel()
                } else {
                    try { trySend(accumulator.accept(payload)) }
                    catch (error: java.io.IOException) { close(error); webSocket.cancel() }
                }
            }
            override fun onFailure(webSocket: okhttp3.WebSocket, error: Throwable, response: okhttp3.Response?) {
                val code = response?.code
                response?.close()
                close(if (code == 401 || code == 403) BackendAuthException("Sign in to continue.") else
                    BackendHttpException(code ?: 503, "Playback events disconnected.",
                        response?.header("Retry-After")?.toLongOrNull()?.times(1_000)))
            }
            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                close(if (code == 4001) BackendAuthException("Your session has ended. Sign in again.") else
                    java.io.IOException("Playback events closed ($code)"))
            }
            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                webSocket.close(code, null)
            }
        })
    awaitClose { socket.cancel() }
}.buffer(Channel.CONFLATED)
