package `in`.synthora.musicbox.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Support normalized and raw artist shelves; ignore invalid/unplayable rows. */
internal fun artistVideoItems(data: JsonObject): List<BrowseItem> {
    val raw = data["artist"].objectOrEmpty()
    fun rows(root: JsonObject): List<BrowseItem> {
        val section = root["videos"]
        val items = section as? JsonArray ?: section.objectOrEmpty().array("results").ifEmpty { section.objectOrEmpty().array("items") }
        return items.mapNotNull { it as? JsonObject }.map { BrowseParser.item(it) }
    }
    return (rows(data) + rows(raw)).filter { it.videoId.isNotBlank() }.map { it.copy(id = it.videoId, kind = "track") }.distinctBy { it.videoId }
}
