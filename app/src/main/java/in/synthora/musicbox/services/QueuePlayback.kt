package `in`.synthora.musicbox.services

import androidx.media3.common.Player
import `in`.synthora.musicbox.models.Track

/** Item metadata belongs to this queue entry; aggregate player metadata can lag a seek. */
internal fun phonePlaybackSnapshot(player: Player): PhonePlaybackSnapshot = PhonePlaybackSnapshot(
    (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
    player.currentMediaItem?.mediaId, player.currentMediaItem?.let { item ->
        val metadata = item.mediaMetadata
        Track(uuid = item.mediaId, title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(), thumbnailUri = metadata.artworkUri?.toString(),
            durationSec = (player.duration.coerceAtLeast(0) / 1000).toInt(),
            localUri = item.localConfiguration?.uri?.toString())
    })

/** Choosing a row is an explicit play intent, including reselecting a paused row. */
internal fun playQueueEntry(player: Player, index: Int): Boolean {
    if (index !in 0 until player.mediaItemCount) return false
    player.seekTo(index, 0L)
    if (player.playbackState == Player.STATE_IDLE) player.prepare()
    player.play()
    return true
}
