package `in`.synthora.musicbox.services

import android.content.Context
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.network.*
import `in`.synthora.musicbox.utils.SecureStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.security.MessageDigest

/** Encrypted, bounded, per-account pending history. Never sent to download credentials. */
object LocalListeningHistory {
    private val mutex = Mutex()
    private fun prefs(context: Context, id: String) = context.getSharedPreferences(
        "listening_" + MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString("") { "%02x".format(it) }, Context.MODE_PRIVATE)
    private fun read(context: Context, id: String): List<JsonElement> = runCatching {
        val encoded = prefs(context, id).getString("rows", null) ?: return emptyList()
        Json.parseToJsonElement(SecureStore.decrypt(encoded) ?: return emptyList()).jsonArray.toList()
    }.getOrDefault(emptyList())
    private fun write(context: Context, id: String, rows: List<JsonElement>) {
        val encrypted = SecureStore.encrypt(JsonArray(rows.take(100)).toString()) ?: return
        prefs(context, id).edit().putString("rows", encrypted).commit()
    }
    suspend fun deleteAccount(context: Context) = mutex.withLock {
        AccountScope.current(context)?.id?.let { prefs(context, it).edit().clear().commit() }
        Unit
    }
    suspend fun record(context: Context, track: Track) {
        val id = AccountScope.current(context)?.id ?: return
        val video = track.ytVideoId ?: return
        mutex.withLock {
            val row = buildJsonObject {
                put("video_id", video); put("videoId", video); put("title", track.title)
                put("artist", track.artist); put("thumbnail_url", track.thumbnailUri.orEmpty())
                put("played_at", System.currentTimeMillis()); put("event", java.util.UUID.randomUUID().toString())
            }
            write(context, id, listOf(row) + read(context, id).filter { it.jsonObject.text("video_id") != video })
        }
        sync(context)
    }
    suspend fun sync(context: Context) = mutex.withLock {
        val id = AccountScope.current(context)?.id ?: return@withLock
        if (!Backend.hasSession()) return@withLock
        val profile = Backend.get("/api/profile_status/", mapOf("audio_check" to "0")).objectOrEmpty()
        if (!profile.flag("youtube_auth_working") || !profile.flag("youtube_own_session")) return@withLock
        for (row in read(context, id).reversed()) {
            if (AccountScope.current(context)?.id != id) return@withLock
            val raw = row.jsonObject
            val result = Backend.post("/api/track/${raw.text("video_id")}/listen", raw).objectOrEmpty()
            if (!result.flag("ok")) return@withLock
            write(context, id, read(context, id).filter { it.jsonObject.text("event") != raw.text("event") })
        }
    }
    suspend fun history(context: Context): JsonArray {
        val id = AccountScope.current(context)?.id ?: return JsonArray(emptyList())
        try { sync(context) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { /* Show pending local history while disconnected. */ }
        val remote = try { Backend.get("/history/") as? JsonArray ?: JsonArray(emptyList()) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { JsonArray(emptyList()) }
        if (AccountScope.current(context)?.id != id) return JsonArray(emptyList())
        return mutex.withLock { JsonArray((read(context, id) + remote).distinctBy { it.jsonObject.text("video_id", "videoId") }) }
    }
}
