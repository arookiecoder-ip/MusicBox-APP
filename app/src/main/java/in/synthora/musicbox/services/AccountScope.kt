package `in`.synthora.musicbox.services

import android.content.Context
import `in`.synthora.musicbox.database.MusicDatabase
import `in`.synthora.musicbox.utils.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * The identity last seen by this install, and the local metadata that must not
 * survive a switch to another account on the same phone.
 *
 * The server already partitions playback and provider credentials by identity.
 * This fence covers the client half: cached Home/search data, restored queues,
 * lyrics and play counts. Downloads are deliberately device-level media, not
 * account metadata; they remain available offline but are never used to infer
 * who is signed in.
 */
object AccountScope {
    private const val PREFS = "account_scope"
    private const val KEY = "identity_encrypted_v1"

    data class Identity(val id: String, val username: String, val email: String)

    suspend fun adopt(context: Context, id: String, username: String, email: String): Boolean {
        if (id.isBlank()) return false
        val previous = read(context)
        if (previous?.id == id) {
            write(context, Identity(id, username, email))
            return false
        }
        val switched = previous != null
        if (switched) clearLocal(context)
        write(context, Identity(id, username, email))
        return switched
    }

    fun current(context: Context): Identity? = read(context)

    suspend fun forget(context: Context) {
        clearLocal(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun read(context: Context): Identity? = runCatching {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        val raw = SecureStore.decrypt(encoded) ?: return null
        val json = JSONObject(raw)
        val id = json.optString("id")
        if (id.isBlank()) null else Identity(id, json.optString("username"), json.optString("email"))
    }.getOrNull()

    private fun write(context: Context, identity: Identity) {
        val raw = JSONObject()
            .put("id", identity.id)
            .put("username", identity.username)
            .put("email", identity.email)
            .toString()
        val encoded = SecureStore.encrypt(raw) ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, encoded).apply()
    }

    private suspend fun clearLocal(context: Context) = withContext(Dispatchers.IO) {
        AccountRepository.clear()
        runCatching { MusicDatabase.getDatabase(context).clearAllTables() }
        HomeFeedCache(context.getSharedPreferences("home_feed_cache", Context.MODE_PRIVATE)).clear()
        context.getSharedPreferences("search_history", Context.MODE_PRIVATE).edit().clear().apply()
        runCatching { File(context.cacheDir, "stream_cache").deleteRecursively() }
        Unit
    }
}
