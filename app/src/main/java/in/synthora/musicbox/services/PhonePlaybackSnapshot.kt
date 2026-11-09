package `in`.synthora.musicbox.services

/** Queue identity and actual playing item delivered together by Media3.onEvents. */
data class PhonePlaybackSnapshot(val queueIds: List<String> = emptyList(), val currentId: String? = null,
    val currentMetadata: `in`.synthora.musicbox.models.Track? = null)

/** Queue descriptions are authoritative: controller fields may lag the current media ID. */
internal fun PhonePlaybackSnapshot.resolveTracks(known: Map<String, `in`.synthora.musicbox.models.Track>): List<`in`.synthora.musicbox.models.Track> =
    queueIds.mapNotNull { id ->
        val stored = known[id]
        val live = currentMetadata?.takeIf { id == currentId && it.uuid == id }
        if (live == null) stored else stored?.copy(
            title = stored.title.ifBlank { live.title },
            artist = stored.artist.ifBlank { live.artist },
            thumbnailUri = stored.thumbnailUri?.takeIf { it.isNotBlank() } ?: live.thumbnailUri,
            durationSec = live.durationSec.takeIf { it > 0 } ?: stored.durationSec) ?: live
    }
