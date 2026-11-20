package `in`.synthora.musicbox.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import `in`.synthora.musicbox.network.rowArtworkUrl

/** Small rows never fetch/decode full hero artwork or animate each newly visible cover. */
@Composable
fun ListArtwork(url: String?, size: Dp, modifier: Modifier = Modifier, videoId:String?=null) {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val request = remember(url, pixels, context,videoId) {
        val source=url?.takeIf {runCatching{java.net.URI(it).scheme in setOf("https","http")}.getOrDefault(false)}
            ?: videoId?.takeIf{it.matches(Regex("[A-Za-z0-9_-]{11}"))}?.let{"https://i.ytimg.com/vi/$it/hqdefault.jpg"}.orEmpty()
        ImageRequest.Builder(context).data(rowArtworkUrl(source, pixels))
            .size(pixels).precision(Precision.INEXACT).crossfade(false).build()
    }
    AsyncImage(request, null, modifier, contentScale = ContentScale.Crop)
}
