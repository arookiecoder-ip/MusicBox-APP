package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.database.DownloadManifestRow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DownloadManifestCodecTest {
    private fun song(index: Int) = Track("uuid-$index", "Song $index", "Artist", durationSec = 180,
        ytVideoId = index.toString().padStart(11, '0'), localUri = "/fixture/$index.audio")
    @Test fun largeManifestRestoresCollectionOrderAndMissingMembersOffCallerThread() = runBlocking {
        val codec = DownloadManifestCodec()
        val tracks = (0 until 2000).map(::song)
        val collections = tracks.chunked(20).mapIndexed { index, items ->
            DownloadedCollection("id$index", "album", "Album $index", "", "Artist", items)
        }
        val snapshot = DownloadSnapshot(emptyList(), tracks.mapIndexed { i, track -> DownloadEntry(i.toLong(), track) }, emptyList(), collections, emptyList())
        val rows = codec.encode(snapshot)
        val caller = Thread.currentThread()
        val restored = codec.decode(rows) {
            assertNotSame("File probes must never run on the caller/UI thread", caller, Thread.currentThread())
            it.ytVideoId != song(10).ytVideoId
        }
        assertEquals(1999, restored.completed.size)
        assertEquals(collections, restored.collections)
        assertEquals("missing", restored.unavailable.single().state)
    }
    @Test fun duplicatePendingItemsAreNotRequeuedAndMalformedRowsAreIgnored() = runBlocking {
        val codec = DownloadManifestCodec()
        val track = song(1)
        val rows = codec.encode(DownloadSnapshot(listOf(DownloadEntry(1, track)), emptyList(), listOf(track, song(2)), emptyList(), emptyList())) +
            DownloadManifestRow("completed", "bad", "complete", "{invalid")
        val restored = codec.decode(rows) { true }
        assertEquals(listOf(song(2)), restored.queued)
        assertEquals(listOf(track), restored.pending.map { it.track })
        assertTrue(restored.completed.isEmpty())
    }
    @Test fun newerSnapshotCanRemoveItemsWithoutLosingCollectionMembership() = runBlocking {
        val codec = DownloadManifestCodec()
        val track = song(1)
        val collection = DownloadedCollection("id", "playlist", "Playlist", "", "", listOf(track, track))
        val removed = DownloadSnapshot(emptyList(), emptyList(), emptyList(), listOf(collection), emptyList())
        assertEquals(listOf(collection), codec.decode(codec.encode(removed)) { false }.collections)
    }
}
