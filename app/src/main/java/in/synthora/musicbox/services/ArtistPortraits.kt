package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.ArtistCredit
import `in`.synthora.musicbox.network.*
import kotlinx.serialization.json.JsonObject
import java.util.Locale

/** Only the opened artist picker calls this loader. No track observer or prefetch hook. */
internal class ArtistPortraitRepository(
    private val get: suspend (String, Map<String, String>) -> JsonObject = { path, query -> Backend.get(path, query).objectOrEmpty() },
    private val capacity: Int = 32
) {
    private val images = LinkedHashMap<String, String>()
    suspend fun load(artist: ArtistCredit): String? {
        val name = artist.name.trim()
        if (name.isEmpty()) return null
        val key = artist.id?.trim()?.takeIf { it.isNotBlank() }?.let { "id:$it" } ?: "name:${name.lowercase(Locale.ROOT)}"
        synchronized(images) { images[key]?.let { return it } }
        val safeId = Regex("[A-Za-z0-9_-]{1,128}")
        val id = artist.id?.trim()?.takeIf { safeId.matches(it) }
            ?: get("/api/artist/resolve/", mapOf("name" to name)).text("channel_id", "artist_id", "id")
                .takeIf { safeId.matches(it) } ?: return null
        val info = get("/api/artist/$id", emptyMap())["artist"].objectOrEmpty()
        val image = imageUrl(info["thumbnails"]).ifBlank { imageUrl(info["image"]) }.takeIf { it.isNotBlank() } ?: return null
        synchronized(images) {
            images.remove(key); images[key] = image
            while (images.size > capacity.coerceAtLeast(1)) images.remove(images.keys.first())
        }
        return image
    }
}

internal object ArtistPortraits {
    private val repository = ArtistPortraitRepository()
    suspend fun load(artist: ArtistCredit): String? = repository.load(artist)
}
