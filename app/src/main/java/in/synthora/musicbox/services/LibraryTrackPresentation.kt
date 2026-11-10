package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.viewmodels.SortOption

/** CPU work prepared off the UI thread; unchanged rows retain their identity. */
internal fun presentLibraryTracks(tracks: List<Track>, search: String, sort: SortOption, liked: Set<String>): List<Track> {
    val query = search.trim()
    val filtered = if (query.isEmpty()) tracks else tracks.filter {
        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
    }
    val ordered = when (sort) {
        SortOption.DEFAULT -> filtered
        SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
        SortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
    }
    return ordered.map { if (it.isFavourite == (it.ytVideoId in liked)) it else it.copy(isFavourite = it.ytVideoId in liked) }
}
