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
}
