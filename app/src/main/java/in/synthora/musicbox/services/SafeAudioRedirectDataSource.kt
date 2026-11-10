package `in`.synthora.musicbox.services

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

/** The HTTP client must disable automatic redirects so credentials never cross a boundary. */
internal class SafeAudioRedirectDataSource(private val upstream: DataSource) : DataSource by upstream {
    override fun open(dataSpec: DataSpec): Long {
        var current = dataSpec
        val authenticated = dataSpec.httpRequestHeaders.keys.any {
            it.equals("X-MusicBox-Audio-Token", true) || it.equals("X-Api-Key", true) ||
                it.equals("Authorization", true) || it.equals("Cookie", true)
        }
        repeat(6) { attempt ->
            try { return upstream.open(current) }
            catch (error: HttpDataSource.InvalidResponseCodeException) {
                upstream.close()
                if (error.responseCode !in listOf(301, 302, 303, 307, 308) || attempt == 5) throw error
                val source = current.uri.toString().toHttpUrlOrNull() ?: throw IOException("Invalid audio origin", error)
                val location = error.headerFields.entries.firstOrNull { it.key.equals("Location", true) }?.value?.firstOrNull()
                val target = location?.let(source::resolve) ?: throw IOException("Invalid audio redirect", error)
                if (target.scheme != "https" && !(target.scheme == "http" && source.scheme == "http" &&
                            target.host in setOf("127.0.0.1", "localhost", "10.0.2.2") && target.host == source.host))
                    throw IOException("Insecure audio redirect rejected", error)
                if (authenticated && (source.scheme != target.scheme || source.host != target.host || source.port != target.port))
                    throw IOException("Cross-origin authenticated audio redirect rejected", error)
                current = current.withUri(Uri.parse(target.toString()))
            }
        }
        throw IOException("Too many audio redirects")
    }
}
