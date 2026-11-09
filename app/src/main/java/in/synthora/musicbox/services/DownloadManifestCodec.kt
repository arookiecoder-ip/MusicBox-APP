package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.database.DownloadManifestRow
import `in`.synthora.musicbox.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable internal data class DownloadEntry(val id: Long, val track: Track)
internal data class DownloadSnapshot(
    val pending: List<DownloadEntry>, val completed: List<DownloadEntry>,
    val queued: List<Track>, val collections: List<DownloadedCollection>,
    val unavailable: List<DownloadManifestRow>
)

/** Main owns mutable state. Only immutable snapshots cross to this worker. */
internal class DownloadManifestCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private fun row(kind: String, key: String, state: String, payload: String) = DownloadManifestRow(kind, key, state, payload)
    fun collectionRows(collection: DownloadedCollection): List<DownloadManifestRow> =
        listOf(row("collection", collection.key, "complete", json.encodeToString(collection.copy(tracks = emptyList())))) +
            collection.tracks.mapIndexed { index, track -> row("member:${collection.key}", index.toString(), "member", json.encodeToString(track)) }

    suspend fun encode(snapshot: DownloadSnapshot): List<DownloadManifestRow> = withContext(Dispatchers.Default) {
        snapshot.pending.map { row("pending", it.track.ytVideoId.orEmpty(), "downloading", json.encodeToString(it)) } +
            snapshot.completed.map { row("completed", it.track.ytVideoId.orEmpty(), "complete", json.encodeToString(it)) } +
            snapshot.queued.map { row("queued", it.ytVideoId.orEmpty(), "queued", json.encodeToString(it)) } +
            snapshot.collections.flatMap(::collectionRows) + snapshot.unavailable
    }

    /** Group once rather than scanning every manifest row for every collection. IO includes file probes. */
    suspend fun decode(rows: List<DownloadManifestRow>, readable: (Track) -> Boolean): DownloadSnapshot = withContext(Dispatchers.IO) {
        val groups = rows.groupBy { it.kind }
        fun entries(kind: String) = groups[kind].orEmpty().mapNotNull {
            runCatching { json.decodeFromString<DownloadEntry>(it.payload) }.getOrNull()
        }
        val completed = mutableListOf<DownloadEntry>()
        val unavailable = groups["unavailable"].orEmpty().associateBy { it.key }.toMutableMap()
        for (entry in entries("completed")) {
            if (readable(entry.track)) completed += entry
            else unavailable[entry.track.ytVideoId.orEmpty()] = row("unavailable", entry.track.ytVideoId.orEmpty(), "missing", json.encodeToString(entry))
        }
        val retained = (entries("pending") + completed).mapNotNull { it.track.ytVideoId }.toSet()
        val queued = groups["queued"].orEmpty().mapNotNull {
            runCatching { json.decodeFromString<Track>(it.payload) }.getOrNull()
        }.distinctBy { it.ytVideoId }.filter { it.ytVideoId !in retained }
        val collections = groups["collection"].orEmpty().mapNotNull { row ->
            runCatching { json.decodeFromString<DownloadedCollection>(row.payload) }.getOrNull()?.let { collection ->
                collection.copy(tracks = groups["member:${row.key}"].orEmpty()
                    .sortedBy { it.key.toIntOrNull() ?: 0 }.mapNotNull {
                        runCatching { json.decodeFromString<Track>(it.payload) }.getOrNull()
                    })
            }
        }.distinctBy { it.key }
        DownloadSnapshot(entries("pending"), completed, queued, collections, unavailable.values.toList())
    }
}
