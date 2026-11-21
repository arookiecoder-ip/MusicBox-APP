package `in`.synthora.musicbox.services

/** A cancelled transfer can never publish into, or remove, its replacement's queue entry. */
internal class DownloadAttempts {
    private var sequence = 0L
    private val current = mutableMapOf<String, Long>()
    fun enqueue(video: String): Long = (++sequence).also { current[video] = it }
    fun restoring(video: String): Long = current[video] ?: enqueue(video)
    fun owns(video: String, attempt: Long): Boolean = current[video] == attempt
    fun cancel(video: String) { current.remove(video) }
    fun finish(video: String, attempt: Long): Boolean = owns(video, attempt).also { if (it) current.remove(video) }
}
