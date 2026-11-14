package `in`.synthora.musicbox.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive

/** Stream preparation is required; server queue synchronization must not gate playback. */
object DeviceQueueStartup {
    suspend fun <T> start(scope: CoroutineScope, prepare: suspend () -> T, play: (T) -> Unit,
        synchronize: suspend (T) -> Unit, onSyncError: (Exception) -> Unit, afterSynchronization: suspend () -> Unit = {}): Job {
        val prepared = prepare()
        play(prepared)
        return scope.launch {
            try { synchronize(prepared) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { onSyncError(e) }
            // Local playlist completion must survive an unavailable server mirror.
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            afterSynchronization()
        }
    }
}
