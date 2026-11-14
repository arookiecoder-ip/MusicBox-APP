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
import kotlinx.serialization.json.JsonElement
import java.util.UUID

// A room is independent of the signed-in account's personal library and output lease.
data class JamMember(val id: String, val name: String)
data class JamDevice(val id:String,val name:String,val kind:String,val active:Boolean=false,val online:Boolean=true)
data class JamOutput(val id: String, val name: String, val online: Boolean, val error: String,
    val username:String="Host",val volume:Int?=null,val kind:String="phone",val devices:List<JamDevice> = emptyList())
data class JamUiState(val active: Boolean = false, val host: Boolean = false, val url: String = "",
    val busy: Boolean = false, val error: String? = null, val members: List<JamMember> = emptyList(),
    val outputs: List<JamOutput> = emptyList(), val playback: JsonObject = JsonObject(emptyMap()),
    val listen: Boolean = false, val memberCount: Int = 0, val receivedAt: Long = 0)

/** Only verified, configured Music Box hosts may supply invitations. */
internal fun jamInviteToken(url: String, base: String): String? = runCatching {
    val uri = java.net.URI(url)
    val expected = java.net.URI(base)
    if (uri.scheme != "https" || !uri.host.equals(expected.host, ignoreCase=true) ||
        (if(uri.port==-1) 443 else uri.port) != (if(expected.port==-1) 443 else expected.port) || uri.userInfo != null) return null
    val parts=uri.rawPath.split('/').filter { it.isNotEmpty() }
    parts.takeIf { it.size == 2 && it[0] in setOf("j", "jam") }?.get(1)
        ?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{8,128}")) }
}.getOrNull()

internal fun mergeJamPlayback(previous: JsonObject, incoming: JsonObject): JsonObject {
    if(incoming["queue"] != null) return incoming
    check(incoming.text("queue_epoch").isNotBlank() && incoming.text("queue_epoch") == previous.text("queue_epoch")) { "Jam queue needs a refresh" }
    return JsonObject(incoming + mapOf("queue" to (previous["queue"] ?: JsonArray(emptyList()))))
}

/** Application-owned polling also runs while the playback service keeps the process alive. */
open class JamClient internal constructor(
    private val scope: CoroutineScope,
    private val read: suspend (String, Map<String,String>) -> JsonElement,
    private val write: suspend (String, JsonObject) -> JsonElement,
    private val deviceId: () -> String,
    private val clock: () -> Long,
    private val feedback: (String) -> Unit
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(JamUiState())
    val state = _state.asStateFlow()
    val active get() = _state.value.active
    private var polling: Job? = null
    private var applying: Job? = null
    private var lastSuccess = 0L
    private var playbackError: String? = null
    private var generation=0L
    private var prefs: android.content.SharedPreferences? = null
    fun init(context: Context): Boolean {
        prefs=context.getSharedPreferences("jam_session",Context.MODE_PRIVATE)
        val restore=prefs?.getBoolean("active",false)==true
        if(restore) _state.value=JamUiState(active=true)
        return restore
    }
    private fun rememberActive(active: Boolean) { prefs?.edit()?.putBoolean("active",active)?.apply() }
    val positionMs: Long get() {
        val room=_state.value
        val base=room.playback.number("position_ms")
        val position=base + if(room.playback.flag("playing") && room.playback.flag("playback_confirmed") && !room.playback.flag("playback_processing")) (clock()-room.receivedAt).coerceAtLeast(0) else 0
        val duration=room.playback.number("duration_ms")
        return if(duration>0) position.coerceAtMost(duration) else position
    }
    private suspend fun silence() {
        applying?.cancelAndJoin()
        try { withTimeout(6_000) { applyPlayback?.invoke(_state.value.playback, false) } }
        catch (_: TimeoutCancellationException) { feedback("The audio output did not confirm its pause.") }
        catch(e: CancellationException) { throw e }
        catch(_: Exception) { feedback("Couldn't pause this audio output. Check its connection.") }
    }
    // Only an already-owned local phone may preview transport without waiting
    // for HTTP. The authoritative result or a rejection reconciles it afterward.
    var previewTransport: ((String) -> Boolean)? = null
    private var transportSequence=0L
    private var optimisticTransport: Long? = null
    var enterPlayback: (suspend () -> Unit)? = null
    var applyPlayback: (suspend (JsonObject, Boolean) -> Unit)? = null
    var outputStatus: () -> JsonObject = {JsonObject(emptyMap())}
    var outputDevices: () -> JsonArray = {JsonArray(emptyList())}
    var applyVolume: (suspend (Int)->Unit)? = null
    var applyOutput: (suspend (String)->Unit)? = null
    private val acknowledgements=linkedSetOf<String>()
    private val volumeJobs=mutableMapOf<String,Job>()
    fun setVolume(device:String,value:Int) {
        if(!active) return
        val target=_state.value.outputs.firstOrNull{it.id==device} ?: return
        if(!target.online || device !in _state.value.playback.array("selected").map{it.jsonPrimitive.content})return
        _state.update {it.copy(outputs=it.outputs.map{output->if(output.id==device)output.copy(volume=value.coerceIn(0,100))else output})}
        volumeJobs.remove(device)?.cancel()
        val revision=generation
        volumeJobs[device]=scope.launch {
            delay(150)
            if(revision!=generation || !active)return@launch
            try {write("/api/jam/control/",buildJsonObject {put("action","volume");put("device_id",device);put("value",value.coerceIn(0,100))})}
            catch(e:CancellationException){throw e}
            catch(e:Exception){if(revision==generation)feedback(networkErrorMessage(e) ?: "Couldn't change Jam device volume")}
        }
    }
    var outputName: () -> String = { android.os.Build.MODEL + " · audio output" }

    fun refresh() { scope.launch { refreshNow(); if(active) follow() } }
    private fun follow() {
        if (polling?.isActive == true) return
        polling = scope.launch {
            while (isActive && active) {
                refreshNow()
                delay(1_000)
            }
        }
    }
    private suspend fun refreshNow(force:Boolean=false) {
        if(_state.value.busy && !force)return
        val requestGeneration=generation
        try {
            val known=_state.value.playback
            val query=mapOf("device_id" to deviceId()) + if(known.text("queue_epoch").isNotBlank()) mapOf("queue_epoch" to known.text("queue_epoch"), "queue_version" to known.number("queue_version").toString()) else emptyMap()
            val result = read("/api/jam/state/", query).objectOrEmpty()
            if(requestGeneration != generation) return
            if (!result.flag("active")) {
                if (active) {
                    silence()

                }
                generation++;optimisticTransport=null
                rememberActive(false)
                _state.value = JamUiState(error = if (result.flag("revoked")) "This Jam ended or the host removed you." else null)
                return
            }
            val playback = mergeJamPlayback(known, result["playback"].objectOrEmpty())
            val old = _state.value
            if (old.url == result.text("url") && playback.number("revision") < old.playback.number("revision")) return
            val retainPreview=optimisticTransport!=null && result.flag("listen") &&
                old.playback.text("queue_epoch")==playback.text("queue_epoch") && old.playback.text("entry_id")==playback.text("entry_id")
            _state.value = JamUiState(active=true, host=result.flag("host"), url=result.text("url"),
                members=result.array("members").map { JamMember(it.objectOrEmpty().text("id"), it.objectOrEmpty().text("name")) },
                outputs=result.array("outputs").map { val o=it.objectOrEmpty(); JamOutput(o.text("device"), o.text("name"), o.flag("online"), o.text("error"),
                    o.text("username").ifBlank{"Host"},o["volume"]?.jsonPrimitive?.intOrNull,o.text("kind"),
                    o.array("devices").map{raw->val d=raw.objectOrEmpty();JamDevice(d.text("id"),d.text("name"),d.text("kind"),d.flag("active"),d["online"]?.jsonPrimitive?.booleanOrNull ?: true)}) },
                playback=if(retainPreview) old.playback else playback, listen=result.flag("listen"), memberCount=result.number("member_count").toInt(),
                error=playbackError, receivedAt=if(retainPreview) old.receivedAt else clock())
            rememberActive(true)
            lastSuccess = clock()
            if(!retainPreview) deliver(playback,result.flag("listen"))
            val ack=acknowledgements.toList()
            val status=outputStatus()
            val presence=write("/api/jam/presence/", buildJsonObject {
                put("device_id", deviceId());put("name", outputName());put("error",playbackError.orEmpty())
                put("status",status);put("kind",status.text("kind"));put("devices",outputDevices())
                put("ack",JsonArray(ack.map(::JsonPrimitive)))
            }).objectOrEmpty()
            acknowledgements.removeAll(ack.toSet())
            if(requestGeneration!=generation || !active)return
            for(raw in presence.array("commands")) {
                val command=raw.objectOrEmpty();val id=command.text("id")
                if(id in acknowledgements)continue
                if(command.text("action")=="volume") {
                    applyVolume?.invoke(command.number("value").toInt())
                    acknowledgements+=id
                }
            }
            follow()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            _state.update { it.copy(busy=false, error=networkErrorMessage(e) ?: e.message ?: "Couldn't reconnect to Jam") }
            if (active && clock() - lastSuccess > 12_000) {
                silence()
            }
        }
    }
    private var playbackHeld = false
    suspend fun beginOutputSwitch() {
        playbackHeld = true
        applying?.cancelAndJoin()
    }
    fun endOutputSwitch() {
        playbackHeld = false
        applyingKey = null
        if (active) refresh()
    }
    private var applyingKey: List<Any?>? = null
    private fun deliver(playback: JsonObject, listen: Boolean) {
        if (playbackHeld) return
        val key=listOf(playback.text("queue_epoch"),playback.number("queue_version"),playback.number("queue_index"),
            playback.flag("playing"),playback.number("seek_revision"),listen,outputName(),playback["targets"])
        if(applying?.isActive==true && applyingKey==key) return
        val previous=applying
        previous?.cancel()
        applyingKey=key
        applying=scope.launch {
            previous?.join()
            try {
                val target=playback["targets"].objectOrEmpty().text(deviceId())
                if(listen && target.isNotBlank())applyOutput?.invoke(target)
                applyPlayback?.invoke(playback,listen);playbackError=null
            }
            catch(e: CancellationException){throw e}
            catch(e: Exception){
                playbackError=networkErrorMessage(e) ?: e.message ?: "This output could not play."
                _state.update {it.copy(error=playbackError)}
            }
        }
    }
    fun command(action: String, fields: JsonObject = JsonObject(emptyMap())) {
        if (!active) return
        if(action=="configure" && _state.value.busy)return
        if(action=="configure")_state.update{it.copy(busy=true)}
        val commandGeneration=generation
        val sequence=++transportSequence
        val previous=_state.value
        if(action in setOf("play","pause") && previous.listen && previewTransport?.invoke(action)==true) {
            val position=positionMs
            optimisticTransport=sequence
            _state.value=previous.copy(playback=JsonObject(previous.playback+mapOf("playing" to JsonPrimitive(action=="play"),"position_ms" to JsonPrimitive(position))),receivedAt=clock())
        }
        scope.launch { mutex.withLock {
            try {
                if(commandGeneration!=generation)return@withLock
                val revision=_state.value.playback["revision"] ?: JsonPrimitive(0)
                val result = write("/api/jam/control/", JsonObject(fields + mapOf(
                    "action" to JsonPrimitive(action), "command_id" to JsonPrimitive(UUID.randomUUID().toString()),
                    "revision" to revision))).objectOrEmpty()
                if(commandGeneration!=generation) return@withLock
                if(optimisticTransport!=null && optimisticTransport!=sequence) return@withLock
                if(optimisticTransport==sequence) optimisticTransport=null
                val accepted=result["playback"].objectOrEmpty()
                if(accepted.number("revision") < _state.value.playback.number("revision")) return@withLock
                val listen=if(accepted["selected"] is JsonArray) accepted.array("selected").any {it.jsonPrimitive.content==deviceId()} else _state.value.listen
                _state.update { it.copy(playback=accepted, listen=listen, busy=false,error=null, receivedAt=clock()) }
                deliver(accepted,listen)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if(commandGeneration!=generation) return@withLock
                if(optimisticTransport==sequence) {
                    optimisticTransport=null
                    _state.value=previous
                    deliver(previous.playback,previous.listen)
                }
                val message=networkErrorMessage(e) ?: e.message ?: "Couldn't update Jam"
                refreshNow()
                _state.update { it.copy(error=message,busy=false) }
                feedback(message)
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
        val actionGeneration=generation
        scope.launch { mutex.withLock {
            try {
                val response=write(path, body).objectOrEmpty()
                if(actionGeneration!=generation) return@withLock
                generation++
                if (path.startsWith("/j/") && !response.flag("host")) {
                    applying?.cancelAndJoin()
                    try { withTimeout(6_000) { enterPlayback?.invoke() } }
                    catch (_: TimeoutCancellationException) { feedback("The audio output is reconnecting.") }
                    catch(e: CancellationException) { throw e }
                    catch(_: Exception) { feedback("Joined Jam. Your audio output needs to reconnect.") }
                }
                if (path.endsWith("leave/") || path.endsWith("stop/")) {
                    polling?.cancel()
                    silence()
                    rememberActive(false)
                    _state.value=JamUiState()
                    polling?.cancel()
                    return@withLock
                }
                refreshNow(force=true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { val message=networkErrorMessage(e) ?: e.message ?: "Couldn't update Jam"; _state.update { it.copy(busy=false,error=message) }; feedback(message) }
        } }
    }
    fun clear() {
        volumeJobs.values.forEach{it.cancel()};volumeJobs.clear();acknowledgements.clear(); generation++; playbackHeld=false; optimisticTransport=null; polling?.cancel(); applying?.cancel(); playbackError=null; rememberActive(false); _state.value=JamUiState()
    }
}

object JamSession : JamClient(
    CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate),
    {path,query->Backend.get(path,query)}, {path,body->Backend.post(path,body)},
    {PhonePlaybackOwnership.ownerId}, {android.os.SystemClock.elapsedRealtime()}, {NetworkFeedback.notify(it)}
)

class JamViewModel : ViewModel() {
    val state = JamSession.state
    fun refresh() = JamSession.refresh()
    fun start() = JamSession.action("/alexa/jam/start/", buildJsonObject { put("device_id", PhonePlaybackOwnership.ownerId) })
    fun stop() = JamSession.action("/alexa/jam/stop/")
    fun leave() = JamSession.action("/api/jam/leave/")
    fun join(token: String) = JamSession.action("/j/$token", buildJsonObject { put("device_id", PhonePlaybackOwnership.ownerId) })
    fun remove(id: String) = JamSession.action("/alexa/jam/remove/", buildJsonObject { put("member_id", id) })
    fun selectDevice(mode:String, selected:List<String>, output:String, target:String) = JamSession.command("configure",buildJsonObject {
        put("mode",mode);put("selected",JsonArray(selected.map(::JsonPrimitive)))
        put("targets",JsonObject(JamSession.state.value.playback["targets"].objectOrEmpty()+mapOf(output to JsonPrimitive(target))))
    })
    fun configure(mode: String, devices: List<String>) = JamSession.command("configure", buildJsonObject {
        put("mode", mode); put("selected", JsonArray(devices.map(::JsonPrimitive)))
        put("targets",JamSession.state.value.playback["targets"].objectOrEmpty())
    })
}
