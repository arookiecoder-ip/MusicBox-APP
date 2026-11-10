package `in`.synthora.musicbox.network

/** Match the mobile website: a song/audio upload takes precedence over a music video hero. */
fun isAudioSearchItem(item: BrowseItem): Boolean {
    if (item.kind != "track") return false
    val videoType = item.raw.text("videoType", "video_type")
    return videoType.contains("ATV", true) ||
        (!videoType.contains("OMV", true) && !videoType.contains("UGC", true) &&
            !item.raw.text("resultType", "result_type", "type").equals("video", true))
}

fun audioSearchHero(all: List<BrowseItem>, songs: List<BrowseItem>): BrowseItem? {
    val top = all.firstOrNull { it.raw.text("category").equals("Top result", true) && it.id.isNotBlank() }
        ?: all.firstOrNull { it.id.isNotBlank() } ?: songs.firstOrNull()
    if (top == null || top.kind != "track" || isAudioSearchItem(top)) return top
    fun normalize(value: String) = value.lowercase().replace(Regex("[\\(\\[]\\s*(?:official(?: music)?(?: video| audio)?|lyrics?|hd|4k)\\s*[\\)\\]]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
    val audio = (songs + all).filter(::isAudioSearchItem).distinctBy { it.videoId }
    return audio.firstOrNull { normalize(it.title) == normalize(top.title) &&
        (top.subtitle.isBlank() || it.subtitle.isBlank() || normalize(it.subtitle) == normalize(top.subtitle) ||
            (normalize(it.subtitle).isNotBlank() && (normalize(top.subtitle).contains(normalize(it.subtitle)) || normalize(it.subtitle).contains(normalize(top.subtitle))))) }
        ?: top
}

/** Song shelves must survive servers that only return tracks in the mixed result array. */
fun searchSongItems(all: List<BrowseItem>, songs: List<BrowseItem>, query: String = ""): List<BrowseItem> {
    val playable = (songs + all).filter { it.kind == "track" && it.videoId.isNotBlank() }.distinctBy { it.videoId }
    // The server's relevance order can include video-only regional releases.
    // Prefer an audio counterpart in the hero, not every unrelated audio hit.
    if (query.isBlank()) return playable
    return playable.sortedByDescending { searchMatch(it, query) }
}

/** Locale-independent, Unicode-safe comparison; retain remix/live/version qualifiers. */
private fun searchText(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKD)
    .lowercase(java.util.Locale.ROOT).replace(Regex("(?<=\\p{IsLatin})\\p{M}+"), "")
    .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

private fun searchMatch(item: BrowseItem, query: String): Int {
    val q=searchText(query)
    if(q.isBlank()) return 0
    val title=searchText(item.title)
    val artist=searchText(item.subtitle)
    val words=("$title $artist").split(" ").toSet()
    val compactQuery = q.replace(" ", "")
    val titlePrefixes = title.split(" ").runningFold("") { prefix, word -> prefix + word }
    return when {
        title == q || (compactQuery.length >= 4 && title.replace(" ", "") == compactQuery) -> 5
        artist == q && item.kind == "artist" -> 5
        item.kind == "track" && compactQuery.length >= 4 && compactQuery in titlePrefixes -> 4
        q.split(" ").all { it in words } -> 3
        title.startsWith("$q ") -> 2
        else -> 0
    }
}

/** Keep provider order for ties, but let an exact matching result lead unrelated hits. */
internal fun relevantSearchHero(all: List<BrowseItem>, songs: List<BrowseItem>, query: String): BrowseItem? {
    val provider = audioSearchHero(all, songs)
    val best = (all + songs).filter { it.id.isNotBlank() }.maxByOrNull { searchMatch(it,query) } ?: return provider
    return if(searchMatch(best,query) >= 3 && searchMatch(best,query) > (provider?.let { searchMatch(it,query) } ?: 0))
        audioSearchHero(listOf(best),songs) else provider
}

internal fun searchKindItems(all: List<BrowseItem>, specific: List<BrowseItem>, kind: String): List<BrowseItem> =
    (specific + all).filter { it.kind == kind && it.id.isNotBlank() }.distinctBy { it.id }
