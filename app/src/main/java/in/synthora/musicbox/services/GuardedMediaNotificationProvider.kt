package `in`.synthora.musicbox.services

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

/** Deliver delayed artwork only for the live output, on Media3's main application looper. */
@UnstableApi
internal class GuardedMediaNotificationProvider(
    private val delegate: MediaNotification.Provider,
    private val active: () -> Boolean,
    private val onDenied: () -> Unit,
    private val onRecoveryUpdate: () -> Boolean = { false },
    private val onUpdated: () -> Unit = {},
    private val publishForegroundUpdate: (MediaNotification) -> Boolean = { false }
) : MediaNotification.Provider {
    private val main = Handler(Looper.getMainLooper())
    private var lastActions: MediaNotification.ActionFactory? = null
    private var generation = 0L

    /** Rebuild controls using the existing service action factory, without asking
     * Media3 to promote/demote the service during a remote receiver transition. */
    fun refresh(session: MediaSession, publish: (MediaNotification) -> Unit): Boolean {
        val actions = lastActions ?: return false
        val buttons = session.mediaButtonPreferences.ifEmpty { session.customLayout }
        val notification = createNotification(session, ImmutableList.copyOf(buttons), actions) { publish(it) }
        publish(notification)
        onUpdated()
        return true
    }
    override fun createNotification(session: MediaSession, buttons: ImmutableList<CommandButton>,
        actions: MediaNotification.ActionFactory, callback: MediaNotification.Provider.Callback): MediaNotification {
        lastActions = actions
        val expected = ++generation
        return delegate.createNotification(session, buttons, actions) { notification ->
            main.post {
                if (expected != generation) return@post
                deliverNotificationUpdate({ if (!onRecoveryUpdate()) {
                        if (!publishForegroundUpdate(notification)) callback.onNotificationChanged(notification)
                        onUpdated()
                    } },
                    { Build.VERSION.SDK_INT >= 31 && it is android.app.ForegroundServiceStartNotAllowedException }, onDenied, active)
            }
        }
    }
    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        delegate.handleCustomCommand(session, action, extras)
}
