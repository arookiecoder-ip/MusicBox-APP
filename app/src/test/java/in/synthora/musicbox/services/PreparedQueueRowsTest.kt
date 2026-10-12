package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.viewmodels.MusicUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreparedQueueRowsTest {
    private fun track(id: String) = Track(id, id, "Artist", durationSec = 180, ytVideoId = id)

    @Test fun duplicateOccurrencesRemainDistinctAndReordersPreserveEveryEntry() {
        val a = track("a"); val b = track("b")
        val rows = PreparedQueueRows.prepare(listOf(a, a, b))
        assertEquals(3, rows.keys.toSet().size)
        assertEquals(listOf(a, a, b), rows.keys.map { rows.byKey.getValue(it) })
        assertEquals(listOf(0, 1, 2), rows.keys.map { rows.indices.getValue(it) })
        val reordered = PreparedQueueRows.prepare(listOf(b, a, a))
        assertEquals(rows.keys.toSet(), reordered.keys.toSet())
        assertEquals(0, reordered.indices.getValue(reordered.keys[0]))
        assertTrue(PreparedQueueRows.Empty.keys.isEmpty())
    }

    @Test fun queueIsPreparedWithoutAVisibleSheetAndClockSelectionChangesReuseIt() = runTest {
        val queue = (0..9999).map { track(it.toString()) }
        val source = MutableStateFlow(MusicUiState(queue = queue, currentTrack = queue[0], queueIndex = 0))
        val prepared = preparedQueueRows(backgroundScope, source, StandardTestDispatcher(testScheduler))
        runCurrent()
        val first = prepared.value
        assertSame(queue, first.tracks)
        repeat(30) {
            source.value = source.value.copy(position = it.toLong(), queueIndex = it, currentTrack = queue[it], isLoading = it % 2 == 0)
            runCurrent()
            assertSame(first, prepared.value)
        }
        val changed = queue.toMutableList().apply { removeAt(0) }
        source.value = source.value.copy(queue = changed)
        runCurrent()
        assertEquals(9999, prepared.value.tracks.size)
        assertEquals("1", prepared.value.byKey.getValue(prepared.value.keys[0]).uuid)
        source.value = source.value.copy(queue = emptyList())
        runCurrent()
        assertTrue(prepared.value.tracks.isEmpty())
    }
}
