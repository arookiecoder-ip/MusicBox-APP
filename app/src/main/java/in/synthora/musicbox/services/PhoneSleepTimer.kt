package `in`.synthora.musicbox.services

import android.content.Context
import android.os.SystemClock

internal fun sleepTimerApplies(account: String, token: String, currentAccount: String,
    currentToken: String, phoneOutput: Boolean): Boolean =
    phoneOutput && account == currentAccount && token == currentToken

/** The foreground playback service owns enforcement independently of the UI. */
internal object PhoneSleepTimer {
    private fun prefs(context: Context) = context.getSharedPreferences("phone_sleep_timer", Context.MODE_PRIVATE)
    fun set(context: Context, durationMs: Long) {
        val now = SystemClock.elapsedRealtime()
        prefs(context).edit().putLong("started", now).putLong("deadline", now + durationMs)
            .putString("account", AccountScope.current(context)?.id.orEmpty())
            .putString("token", PhonePlaybackOwnership.token).apply()
    }
    fun cancel(context: Context) { prefs(context).edit().clear().apply() }
    fun consumeExpired(context: Context, phoneOutput: Boolean): Boolean {
        val prefs = prefs(context)
        val deadline = prefs.getLong("deadline", 0)
        if (deadline == 0L) return false
        val now = SystemClock.elapsedRealtime()
        if (now < prefs.getLong("started", 0)) { cancel(context); return false }
        if (now < deadline) return false
        val applies = sleepTimerApplies(prefs.getString("account", "").orEmpty(), prefs.getString("token", "").orEmpty(),
            AccountScope.current(context)?.id.orEmpty(), PhonePlaybackOwnership.token, phoneOutput)
        cancel(context)
        return applies
    }
}
