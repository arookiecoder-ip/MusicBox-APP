package `in`.synthora.musicbox.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.network.*
import `in`.synthora.musicbox.services.PhonePlaybackOwnership
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.util.UUID

// A room is independent of the signed-in account's personal library and output lease.
data class JamMember(val id: String, val name: String)
data class JamOutput(val id: String, val name: String, val online: Boolean, val error: String)
data class JamUiState(val active: Boolean = false, val host: Boolean = false, val url: String = "",
    val busy: Boolean = false, val error: String? = null, val members: List<JamMember> = emptyList(),
    val outputs: List<JamOutput> = emptyList(), val playback: JsonObject = JsonObject(emptyMap()),
    val listen: Boolean = false, val memberCount: Int = 0)

/** Only verified, configured Music Box hosts may supply invitations. */
internal fun jamInviteToken(url: String, base: String): String? = runCatching {
    val uri = Uri.parse(url)
    val expected = Uri.parse(base)
    if (uri.scheme != "https" || uri.host != expected.host || uri.port != expected.port || uri.userInfo != null) return null
    val parts = uri.pathSegments
    parts.takeIf { it.size == 2 && it[0] in setOf("j", "jam") }?.get(1)
        ?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{8,128}")) }
}.getOrNull()

/** Application-owned polling also runs while the playback service keeps the process alive. */
object JamSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(JamUiState())
    val state = _state.asStateFlow()
    val active get() = _state.value.active
    private var polling: Job? = null
    private var applying: Job? = null
    private var lastSuccess = 0L
    var applyPlayback: (suspend (JsonObject, Boolean) -> Unit)? = null
    var outputName: () -> String = { android.os.Build.MODEL + " · audio output" }

    fun refresh() { scope.launch { refreshNow() } }
    private fun follow() {
        if (polling?.isActive == true) return
        polling = scope.launch {
            while (isActive && active) {
                refreshNow()
                delay(1_000)
            }
        }
    }
    private suspend fun refreshNow() {
        try {
            val result = Backend.get("/api/jam/state/", mapOf("device_id" to PhonePlaybackOwnership.ownerId)).objectOrEmpty()
            if (!result.flag("active")) {
                if (active || result.flag("revoked")) {
                    applying?.cancelAndJoin()
                    applyPlayback?.invoke(_state.value.playback, false)
                    if (result.flag("revoked")) Backend.post("/api/jam/leave/")
                }
                _state.value = JamUiState(error = if (result.flag("revoked")) "This Jam ended or the host removed you." else null)
                return
            }
            val playback = result["playback"].objectOrEmpty()
            val old = _state.value
            if (old.url == result.text("url") && playback.number("revision") < old.playback.number("revision")) return
            _state.value = JamUiState(active=true, host=result.flag("host"), url=result.text("url"),
                members=result.array("members").map { JamMember(it.objectOrEmpty().text("id"), it.objectOrEmpty().text("name")) },
                outputs=result.array("outputs").map { val o=it.objectOrEmpty(); JamOutput(o.text("device"), o.text("name"), o.flag("online"), o.text("error")) },
                playback=playback, listen=result.flag("listen"), memberCount=result.number("member_count").toInt())
            lastSuccess = android.os.SystemClock.elapsedRealtime()
            Backend.post("/api/jam/presence/", buildJsonObject { put("device_id", PhonePlaybackOwnership.ownerId); put("name", outputName()) })
            if (applying?.isActive != true) applying = scope.launch {
                try { applyPlayback?.invoke(playback, result.flag("listen")) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    _state.update { it.copy(error=networkErrorMessage(e) ?: e.message ?: "This output could not play.") }
                    runCatching { Backend.post("/api/jam/presence/", buildJsonObject { put("device_id", PhonePlaybackOwnership.ownerId); put("name", outputName()); put("error", "Playback unavailable on this output") }) }
                }
            }
            follow()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            _state.update { it.copy(busy=false, error=networkErrorMessage(e) ?: e.message ?: "Couldn't reconnect to Jam") }
            if (active && android.os.SystemClock.elapsedRealtime() - lastSuccess > 12_000) {
                applying?.cancelAndJoin()
                applyPlayback?.invoke(_state.value.playback, false)
            }
        }
    }
    fun command(action: String, fields: JsonObject = JsonObject(emptyMap())) {
        if (!active) return
        scope.launch { mutex.withLock {
            try {
                val result = Backend.post("/api/jam/control/", JsonObject(fields + mapOf(
                    "action" to JsonPrimitive(action), "command_id" to JsonPrimitive(UUID.randomUUID().toString()),
                    "revision" to (_state.value.playback["revision"] ?: JsonPrimitive(0))))).objectOrEmpty()
                _state.update { it.copy(playback=result["playback"].objectOrEmpty(), error=null) }
                refreshNow()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message=networkErrorMessage(e) ?: e.message ?: "Couldn't update Jam"
                refreshNow()
                _state.update { it.copy(error=message) }
                NetworkFeedback.notify(message)
            }
        } }
    }
    fun tracks(action: String, tracks: List<Track>, index: Int = 0) = command(action, buildJsonObject {
        put("tracks", JsonArray(tracks.map { track -> buildJsonObject {
            put("video_id", track.ytVideoId.orEmpty()); put("title", track.title); put("artist", track.artist)
            put("thumbnail", track.thumbnailUri.orEmpty()); put("duration_ms", track.durationSec * 1000L)
        } })); put("index", index)
    })
    fun action(path: String, body: JsonObject = JsonObject(emptyMap())) {
        if (_state.value.busy) return
        _state.update { it.copy(busy=true,error=null) }
        scope.launch { mutex.withLock {
            try {
                Backend.post(path, body)
                if (path.endsWith("leave/") || path.endsWith("stop/")) {
                    applying?.cancelAndJoin()
                    applyPlayback?.invoke(_state.value.playback, false)
                    _state.value=JamUiState()
                }
                refreshNow()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(busy=false,error=networkErrorMessage(e) ?: e.message ?: "Couldn't update Jam") } }
        } }
    }
    fun clear() {
        polling?.cancel(); applying?.cancel(); _state.value=JamUiState()
    }
}

class JamViewModel : ViewModel() {
    val state = JamSession.state
    fun refresh() = JamSession.refresh()
    fun start() = JamSession.action("/alexa/jam/start/", buildJsonObject { put("device_id", PhonePlaybackOwnership.ownerId) })
    fun stop() = JamSession.action("/alexa/jam/stop/")
    fun leave() = JamSession.action("/api/jam/leave/")
    fun join(token: String) = JamSession.action("/j/$token", buildJsonObject { put("device_id", PhonePlaybackOwnership.ownerId) })
    fun remove(id: String) = JamSession.action("/alexa/jam/remove/", buildJsonObject { put("member_id", id) })
    fun configure(mode: String, devices: List<String>) = JamSession.command("configure", buildJsonObject {
        put("mode", mode); put("selected", JsonArray(devices.map(::JsonPrimitive)))
    })
}
