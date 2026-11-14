package `in`.synthora.musicbox.services

import android.app.Application
import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class SafeAudioRedirectTest {
    @Test fun sameOriginRedirectPreservesHeadersAndCrossOriginRedirectNeverReceivesThem() {
        val first = MockWebServer(); val second = MockWebServer()
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
        fun source() = SafeAudioRedirectDataSource(OkHttpDataSource.Factory(client).createDataSource())
        try {
            first.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/actual"))
            first.enqueue(MockResponse().setBody("audio"))
            val spec = DataSpec.Builder().setUri(Uri.parse(first.url("/audio").toString()))
                .setHttpRequestHeaders(mapOf("X-MusicBox-Audio-Token" to "dummy-token")).build()
            source().let { data -> try { assertEquals(5L, data.open(spec)) } finally { data.close() } }
            assertEquals("dummy-token", first.takeRequest().getHeader("X-MusicBox-Audio-Token"))
            assertEquals("dummy-token", first.takeRequest().getHeader("X-MusicBox-Audio-Token"))
            first.enqueue(MockResponse().setResponseCode(302).setHeader("Location", second.url("/steal")))
            source().let { data -> try { assertTrue(runCatching { data.open(spec) }.isFailure) } finally { data.close() } }
            assertEquals(0, second.requestCount)
        } finally { first.shutdown(); second.shutdown(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
    }
    @Test fun redirectedAudioRetainsRepresentationAndRangeHeaders() {
        val server = MockWebServer()
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/actual"))
            server.enqueue(MockResponse().setResponseCode(206).setBody("audio")
                .setHeader("Content-Type", "audio/mp4").setHeader("ETag", "\"version-a\"")
                .setHeader("Content-Range", "bytes 5-9/10"))
            val source = SafeAudioRedirectDataSource(OkHttpDataSource.Factory(client).createDataSource())
            try {
                source.open(DataSpec.Builder().setUri(server.url("/audio").toString()).setPosition(5).build())
                fun header(name: String) = source.responseHeaders.entries.firstOrNull { it.key.equals(name, true) }?.value?.firstOrNull()
                assertEquals("audio/mp4", header("Content-Type"))
                assertEquals("\"version-a\"", header("ETag"))
                assertEquals("bytes 5-9/10", header("Content-Range"))
                assertEquals("5", header("Content-Length"))
            } finally { source.close() }
        } finally { server.shutdown(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
    }

    @androidx.media3.common.util.UnstableApi
    @Test fun representationGuardStillRejectsChangedAudioThroughTheRedirectWrapper() {
        val server = MockWebServer()
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
        val directory = java.io.File(org.robolectric.RuntimeEnvironment.getApplication().cacheDir, "redirect-audio-${System.nanoTime()}")
        val cache = androidx.media3.datasource.cache.SimpleCache(directory, androidx.media3.datasource.cache.NoOpCacheEvictor())
        fun source() = ValidatingAudioDataSource(SafeAudioRedirectDataSource(OkHttpDataSource.Factory(client).createDataSource()), cache)
        try {
            val spec = DataSpec.Builder().setUri(server.url("/audio").toString()).build()
            server.enqueue(MockResponse().setBody("audio").setHeader("Content-Type", "audio/mp4").setHeader("ETag", "\"version-a\""))
            source().let { data -> try { data.open(spec) } finally { data.close() } }
            assertEquals("\"version-a\"", audioRepresentationHeader(cache, spec.buildUpon().setPosition(1).build()))
            server.enqueue(MockResponse().setBody("other").setHeader("Content-Type", "audio/mp4").setHeader("ETag", "\"version-b\""))
            source().let { data ->
                try { data.open(spec); fail("A different representation must trigger a safe reload") }
                catch (_: AudioRepresentationChangedException) { }
                finally { data.close() }
            }
            cache.removeResource(spec.uri.toString())
            clearAudioRepresentation(cache, spec.uri.toString())
            server.enqueue(MockResponse().setBody("other").setHeader("Content-Type", "audio/mp4").setHeader("ETag", "\"version-b\""))
            source().let { data -> try { assertEquals(5L, data.open(spec)) } finally { data.close() } }
            assertEquals("\"version-b\"", audioRepresentationHeader(cache, spec.buildUpon().setPosition(1).build()))
        } finally {
            cache.release(); directory.deleteRecursively(); server.shutdown()
            client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown()
        }
    }

}
