package `in`.synthora.musicbox.services

import android.content.Context
import `in`.synthora.musicbox.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class PlaybackEventAuthenticationTest {
    @Test fun legacyServerRejectingSocketDoesNotClearValidLoginOrStopPolling() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("legacy-event-auth", Context.MODE_PRIVATE)
        preferences.edit().clear().putString("echo_serial", "speaker").commit()
        val cookies = PersistentCookieJar(preferences, { it }, { it })
        val server = MockWebServer()
        val validated = CompletableDeferred<Unit>()
        val headers = mutableListOf<String?>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(headers) { headers += request.getHeader("Cookie") }
                if (request.path?.startsWith("/api/app/events/") == true) return MockResponse().setResponseCode(401)
                if (request.getHeader("Cookie") != "session=valid-owner") return MockResponse().setResponseCode(401)
                if (request.path == "/api/account") return MockResponse().setBody("{}")
                return MockResponse().setBody("""{"video_id":"abcdefghijk","title":"Polling still works"}""")
            }
        }
        val url = server.url("/")
        cookies.saveFromResponse(url, listOf(Cookie.Builder().name("session").value("valid-owner")
            .hostOnlyDomain(url.host).path("/").build()))
        val client = OkHttpClient.Builder().cookieJar(cookies).build()
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        var signOuts = 0
        val controller = EchoController(owner, preferences, {}, { signOuts++; cookies.clear() },
            read = { path, _ ->
                val result = withContext(Dispatchers.IO) {
                    client.newCall(Request.Builder().url(server.url(path)).build()).execute().use {
                        if (it.code == 401) throw BackendAuthException("Session expired", path)
                        Json.parseToJsonElement(it.body!!.string())
                    }
                }
                if (path == "/api/account") validated.complete(Unit)
                result
            }, events = { playbackEventStream(client, server.url("/api/app/events/")) })
        try {
            controller.observeEvents(true)
            withTimeout(5_000) { validated.await() }
            controller.refresh(force = true, stateOnly = true)
            assertEquals("Polling still works", controller.state.value.track?.title)
            assertEquals(0, signOuts)
            assertTrue(cookies.hasCookies())
            assertTrue(PersistentCookieJar(preferences, { it }, { it }).hasCookies())
            assertTrue(synchronized(headers) { headers.size >= 3 && headers.all { it == "session=valid-owner" } })
        } finally {
            controller.clear(); owner.cancel()
            client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown()
        }
    }

    @Test fun genuineExpiryRequiresConfirmationFromEstablishedSessionEndpoint() = runBlocking {
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        val signedOut = CompletableDeferred<Unit>()
        var probes = 0
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("expired-event-auth", Context.MODE_PRIVATE)
        val controller = EchoController(owner, prefs, {}, { signedOut.complete(Unit) }, read = { path, _ ->
            assertEquals("/api/account", path); probes++
            throw BackendAuthException("Expired owner cookie", path)
        }, events = { flow { throw BackendAuthException("Socket rejected") } })
        try {
            controller.observeEvents(true)
            withTimeout(2_000) { signedOut.await() }
            assertEquals(1, probes)
        } finally { controller.clear(); owner.cancel() }
    }

    @Test fun offlineSessionProbePreservesLogin() = runBlocking {
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        val probed = CompletableDeferred<Unit>()
        var signedOut = false
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("offline-event-auth", Context.MODE_PRIVATE)
        val controller = EchoController(owner, prefs, {}, { signedOut = true }, read = { _, _ ->
            probed.complete(Unit); throw IOException("Network unavailable")
        }, events = { flow { throw BackendAuthException("Event proxy rejected") } })
        try {
            controller.observeEvents(true)
            withTimeout(2_000) { probed.await() }
            yield()
            assertFalse(signedOut)
        } finally { controller.clear(); owner.cancel() }
    }

    @Test fun staleProbeCannotSignOutNewlySelectedSession() = runBlocking {
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        val started = CompletableDeferred<Unit>()
        val reply = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        var signedOut = false
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("stale-event-auth", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial", "old").commit()
        val controller = EchoController(owner, prefs, {}, { signedOut = true }, read = { _, _ ->
            started.complete(Unit)
            withContext(NonCancellable) { reply.await(); finished.complete(Unit); throw BackendAuthException("Old probe expired") }
        }, events = { serial -> if (serial == "old") flow { throw BackendAuthException("Old socket") } else emptyFlow() })
        try {
            controller.observeEvents(true)
            withTimeout(2_000) { started.await() }
            controller.select("new", refreshAfter = false)
            reply.complete(Unit)
            withTimeout(2_000) { finished.await() }
            yield()
            assertFalse(signedOut)
        } finally { reply.complete(Unit); controller.clear(); owner.cancel() }
    }
}
