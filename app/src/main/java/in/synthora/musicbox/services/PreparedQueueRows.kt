package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.viewmodels.MusicUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/** A reusable snapshot: duplicate entries keep separate row keys and indices. */
data class PreparedQueueRows(
    val tracks: List<Track>,
    val keys: List<String>,
    val byKey: Map<String, Track>,
    val indices: Map<String, Int>
) {
    companion object {
        val Empty = prepare(emptyList())
        fun prepare(tracks: List<Track>): PreparedQueueRows {
            val occurrences = HashMap<String, Int>()
            val counts = tracks.groupingBy { it.ytVideoId ?: it.uuid }.eachCount()
            val keys = tracks.map { track ->
                val occurrence = occurrences.getOrDefault(track.uuid, 0)
                occurrences[track.uuid] = occurrence + 1
                "${track.uuid}:$occurrence:${counts[track.ytVideoId ?: track.uuid]}"
            }
            return PreparedQueueRows(tracks, keys, keys.zip(tracks).toMap(),
                keys.withIndex().associate { it.value to it.index })
        }
    }
}

internal fun preparedQueueRows(scope: CoroutineScope, state: Flow<MusicUiState>,
    dispatcher: CoroutineDispatcher = PresentationDispatchers.library): StateFlow<PreparedQueueRows> =
    state.map { it.queue }.distinctUntilChanged().map { PreparedQueueRows.prepare(it) }
        .flowOn(dispatcher)
        .stateIn(scope, SharingStarted.Eagerly, PreparedQueueRows.Empty)
