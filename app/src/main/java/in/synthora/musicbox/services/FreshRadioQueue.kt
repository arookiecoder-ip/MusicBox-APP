package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.network.AlexaBackendApi.BackendTrack
import kotlin.random.Random

/** YouTube can return an identical watch playlist for repeated requests of one seed. */
internal fun freshRadioQueue(seed: String, primary: List<BackendTrack>, related: List<BackendTrack>,
    previous: List<String>, random: Random = Random.Default): List<BackendTrack> {
    val candidates = (primary + related).filter { it.videoId.isNotBlank() && it.videoId != seed }.distinctBy { it.videoId }
    val old = previous.toSet()
    val result = (candidates.filter { it.videoId !in old }.shuffled(random) +
        candidates.filter { it.videoId in old }.shuffled(random)).take(50).toMutableList()
    // Even a deterministic provider/random source must not repeat the last first song.
    if (result.size > 1 && result.first().videoId == previous.firstOrNull()) {
        val first = result.removeAt(0)
        result.add(first)
    }
    return result
}
