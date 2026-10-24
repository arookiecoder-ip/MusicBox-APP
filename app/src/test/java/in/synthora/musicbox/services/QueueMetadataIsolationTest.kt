package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track
import org.junit.Assert.*
import org.junit.Test

class QueueMetadataIsolationTest {
    private val a=Track("a", "Song A", "Artist A", thumbnailUri="a.jpg", durationSec=180, ytVideoId="videoaaaaaa")
    private val b=Track("b", "Song B", "Artist B", thumbnailUri="b.jpg", durationSec=240, ytVideoId="videobbbbbb")

    @Test fun laggingTransportMetadataCannotPutAInsideTheKnownBQueueRow() {
        // A controller may already expose B's media ID while its descriptive fields still describe A.
        val lagging=a.copy(uuid=b.uuid,ytVideoId=null)
        val resolved=PhonePlaybackSnapshot(listOf(a.uuid,b.uuid),b.uuid,lagging).resolveTracks(mapOf(a.uuid to a,b.uuid to b))
        assertEquals("Song B",resolved[1].title)
        assertEquals("Artist B",resolved[1].artist)
        assertEquals("b.jpg",resolved[1].thumbnailUri)
        assertEquals(b.ytVideoId,resolved[1].ytVideoId)
    }
    @Test fun remoteActiveMetadataCannotReplaceAnExistingRowsTitleAndArtwork() {
        val projected=TrackQueuePresentation().present(listOf(a,b),active=a.copy(uuid=b.uuid,ytVideoId=b.ytVideoId))
        assertEquals("Song B",projected[1].title)
        assertEquals("b.jpg",projected[1].thumbnailUri)
        assertEquals(a,projected[0])
    }
    @Test fun alternatingStaleAndFreshFramesCannotCorruptDuplicateSongOccurrences() {
        val duplicate=b.copy(uuid="b-second")
        val queue=listOf(a,b,duplicate)
        val known=queue.associateBy { it.uuid }
        val presenter=TrackQueuePresentation()
        repeat(60) { frame ->
            val selected=if(frame%2==0)b else duplicate
            val live=if(frame%3==0)a.copy(uuid=selected.uuid,ytVideoId=null) else selected
            val resolved=PhonePlaybackSnapshot(queue.map { it.uuid },selected.uuid,live).resolveTracks(known)
            val presented=presenter.present(resolved,active=live)
            assertEquals(queue.map { it.title },presented.map { it.title })
            assertEquals(queue.map { it.thumbnailUri },presented.map { it.thumbnailUri })
            assertEquals(queue.map { it.uuid },presented.map { it.uuid })
        }
    }
}
