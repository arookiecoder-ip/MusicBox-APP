package `in`.synthora.musicbox.ui.components

import org.junit.Assert.*
import org.junit.Test

class DownloadProgressSnapshotTest {
    @Test fun delayedSemanticsReadSurvivesCompletionCancellationAndNextPlaylistSong() {
        var state: Int? = 37
        val callback = downloadProgressSnapshot(requireNotNull(state))!!
        state = null // completion/removal before Material's semantics callback
        assertEquals(.37f, callback(), .001f)
        state = -1 // next queued song starts without a known byte total
        assertNull(downloadProgressSnapshot(requireNotNull(state)))
        state = 81
        val next = downloadProgressSnapshot(requireNotNull(state))!!
        state = null // cancellation/removal while both callbacks remain scheduled
        assertNull(state)
        assertEquals(.37f, callback(), .001f)
        assertEquals(.81f, next(), .001f)
    }
    @Test fun unknownTotalsStayIndeterminateAndOverreportedPercentagesAreBounded() {
        assertNull(downloadProgressSnapshot(-1))
        assertNull(downloadProgressSnapshot(Int.MIN_VALUE))
        assertEquals(0f, downloadProgressSnapshot(0)!!(), 0f)
        assertEquals(1f, downloadProgressSnapshot(100)!!(), 0f)
        assertEquals(1f, downloadProgressSnapshot(Int.MAX_VALUE)!!(), 0f)
    }
}
