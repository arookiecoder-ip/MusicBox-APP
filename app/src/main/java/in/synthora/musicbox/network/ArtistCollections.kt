package `in`.synthora.musicbox.network

import kotlinx.serialization.json.JsonObject

fun artistCollectionItems(data: JsonObject, category: String): List<BrowseItem> =
    data.array(category).mapNotNull { (it as? JsonObject)?.let { row ->
        BrowseParser.item(row, if (category in listOf("albums", "singles")) "albums" else "playlists")
    } }.filter { it.id.isNotBlank() }.distinctBy { it.id }

fun artistCollectionTitle(category: String): String = when (category) {
    "playlists" -> "Playlists by artist"
    "featured" -> "Featured on"
    "singles" -> "Singles"
    else -> "Albums"
}
