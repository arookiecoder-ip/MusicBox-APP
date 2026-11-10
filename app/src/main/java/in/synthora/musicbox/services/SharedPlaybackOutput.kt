package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.network.number
import `in`.synthora.musicbox.network.text
import kotlinx.serialization.json.JsonObject

data class SharedPlaybackOutput(val mode: String = "", val owner: String = "", val token: String = "",
    val leaseMs: Long = 0, val serial: String = "", val handoffPending: Boolean = false, val controller: String = "", val revision: Long = 0, val epoch: String = "", val nowPlaying: JsonObject? = null, val presenceRevision: Long = -1) {
    fun olderThan(other: SharedPlaybackOutput): Boolean = epoch.isNotBlank() && epoch == other.epoch && revision < other.revision

    fun belongsToPhone(ownerId: String, claimedToken: String): Boolean =
        mode == "phone" && owner == ownerId && token.isNotBlank() && token == claimedToken && !handoffPending
}

fun sharedPlaybackOutput(json: JsonObject) = SharedPlaybackOutput(json.text("playback_output"),
    json.text("output_owner"), json.text("output_token"), json.number("phone_lease_ms"), json.text("output_serial"), json["handoff_pending"]?.toString() == "true", json.text("output_controller"), json.number("output_revision"), json.text("output_epoch"), json["now_playing"] as? JsonObject, (json["revision"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() ?: -1)

/** Reject prepared audio from an older play intent, including a relinquished online lease. */
internal fun canStartPhonePlayback(expectedToken: String, currentToken: String, relinquishing: Boolean,
    leaseUntil: Long, now: Long): Boolean = !relinquishing && expectedToken == currentToken &&
    (currentToken.isBlank() || now < leaseUntil)

internal fun canContinueDownloadedOffline(online: Boolean, scheme: String?): Boolean =
    !online && scheme in setOf("file", "content")

internal fun canRetryPhoneCommand(previous: SharedPlaybackOutput, current: SharedPlaybackOutput,
    action: String, hasExplicitTracks: Boolean = false): Boolean = current.mode == "phone" &&
    current.owner.isNotBlank() && current.owner == previous.owner && !current.handoffPending &&
    (action in setOf("play", "pause", "volume", "song") || (action == "queue" && hasExplicitTracks))

/** Closing a task must not interpret its still-live server token as a new transfer. */
internal fun dismissedPhoneOutput(output: SharedPlaybackOutput, ownerId: String, dismissedToken: String): Boolean =
    output.mode == "phone" && output.owner == ownerId && output.token.isNotBlank() && output.token == dismissedToken

/** Follow an externally selected Echo even when the app was already in Alexa mode. */
internal fun shouldFollowAlexa(output: SharedPlaybackOutput, currentSerial: String, alreadyAlexa: Boolean): Boolean {
    if (output.mode != "alexa") return false
    if (!alreadyAlexa) return output.serial.isNotBlank() || currentSerial.isNotBlank()
    return output.serial.isNotBlank() && output.serial != currentSerial
}
