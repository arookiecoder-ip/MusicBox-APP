package `in`.synthora.musicbox.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coalesces startup and early Play requests; a failed lookup remains retryable. */
internal class PlaybackTargetRestoration {
    private val mutex = Mutex()
    private var generation = 0L
    private var restoredGeneration = -1L
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()
    val ready get() = restoredGeneration == generation

    fun reset() { generation++; _loading.value = true }
    fun clear() { generation++; restoredGeneration = generation; _loading.value = false }

    suspend fun ensure(resolve: suspend () -> Unit): Boolean {
        val expected = generation
        return mutex.withLock {
            if (expected != generation) return@withLock false
            if (ready) return@withLock true
            _loading.value = true
            try {
                resolve()
                if (expected != generation) return@withLock false
                restoredGeneration = expected
                true
            } finally {
                if (expected == generation) _loading.value = false
            }
        }
    }
}

internal fun SharedPlaybackOutput.isRemotePhoneOutput(ownId: String): Boolean =
    mode == "phone" && owner.isNotBlank() && owner != ownId
