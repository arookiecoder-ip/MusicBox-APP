package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track

/** Progress ticks reuse row objects; queue edits and account changes invalidate them. */
internal class TrackQueuePresentation {
    private var source: List<Track>? = null
    private var likes: Set<String>? = null
    private var current: Track? = null
    private var result = emptyList<Track>()
    fun present(queue: List<Track>, liked: Set<String>? = null, active: Track? = null): List<Track> {
        if (queue === source && liked == likes && active == current) return result
        source = queue; likes = liked; current = active
        result = queue.map { original ->
            // Keep current lyrics/flags without allowing a transient current-track
            // description to rewrite the selected queue row.
            val track = if (active != null && original.uuid == active.uuid &&
                (original.ytVideoId.isNullOrBlank() || active.ytVideoId.isNullOrBlank() || original.ytVideoId == active.ytVideoId)) active.copy(
                title = original.title.ifBlank { active.title },
                artist = original.artist.ifBlank { active.artist },
                thumbnailUri = original.thumbnailUri?.takeIf { it.isNotBlank() } ?: active.thumbnailUri,
                ytVideoId = original.ytVideoId ?: active.ytVideoId,
                artists = original.artists.ifEmpty { active.artists },
                artistId = original.artistId ?: active.artistId,
                albumId = original.albumId ?: active.albumId
            ) else original
            if (liked == null || track.isFavourite == (track.ytVideoId in liked)) track
            else track.copy(isFavourite = track.ytVideoId in liked)
        }
        return result
    }
}
