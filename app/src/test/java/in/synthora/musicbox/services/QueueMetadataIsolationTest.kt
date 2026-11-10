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
    @Test fun missingFieldsStillFillAndLyricsAndLikesArePreserved() {
        val incomplete=b.copy(title="",artist="",thumbnailUri="")
        val live=b.copy(plainLyrics="Lyrics",isFavourite=true)
        val resolved=PhonePlaybackSnapshot(listOf(b.uuid),b.uuid,live).resolveTracks(mapOf(b.uuid to incomplete)).single()
        assertEquals(b.title,resolved.title);assertEquals(b.thumbnailUri,resolved.thumbnailUri)
        val projected=TrackQueuePresentation().present(listOf(b),liked=setOf(b.ytVideoId!!),active=live).single()
        assertEquals("Lyrics",projected.plainLyrics);assertTrue(projected.isFavourite)
    }
    @Test fun metadataForAnotherItemDoesNotModifyANoncurrentRow() {
        val wrong=a.copy(title="Unexpected",thumbnailUri="wrong.jpg")
        val resolved=PhonePlaybackSnapshot(listOf(a.uuid,b.uuid),b.uuid,wrong).resolveTracks(mapOf(a.uuid to a,b.uuid to b))
        assertEquals(listOf(a,b),resolved)
    }

    @Test fun actualQueueMetadataEditsStillUpdateTheRow() {
        val presenter=TrackQueuePresentation()
        presenter.present(listOf(a,b),active=b)
        val corrected=b.copy(title="Corrected B",thumbnailUri="corrected-b.jpg")
        val row=presenter.present(listOf(a,corrected),active=b)[1]
        assertEquals(corrected.title,row.title)
        assertEquals(corrected.thumbnailUri,row.thumbnailUri)
    }

}
