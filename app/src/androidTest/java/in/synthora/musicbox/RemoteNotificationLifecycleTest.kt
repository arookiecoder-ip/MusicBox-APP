package `in`.synthora.musicbox

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSessionService
import androidx.test.platform.app.InstrumentationRegistry
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.services.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@UnstableApi
class RemoteNotificationLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun backgroundSongAndArtworkChangesUpdateExistingForegroundService() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("music_settings_prefs", Context.MODE_PRIVATE)
        val saved = listOf("playback_output", "echo_serial", "remote_controls_dismissed").associateWith { prefs.all[it] }
        prefs.edit().putString("playback_output", "REMOTE_PHONE").putString("echo_serial", "")
            .putBoolean("remote_controls_dismissed", false).commit()
        compose.setContent { Text("Remote lifecycle fixture") }
        val connected = CountDownLatch(1)
        lateinit var service: RemotePlaybackService
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = (binder.javaClass.getDeclaredField("serviceReference").apply { isAccessible = true }.get(binder)
                    as java.lang.ref.WeakReference<*>).get() as RemotePlaybackService
                connected.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName) {}
        }
        val intent = Intent(context, RemotePlaybackService::class.java).setAction(MediaSessionService.SERVICE_INTERFACE)
        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        val covers = mutableListOf<java.io.File>()
        fun until(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 8000
            while (SystemClock.elapsedRealtime() < deadline) {
                if (condition()) return
                SystemClock.sleep(30)
            }
            fail(message)
        }
        try {
            assertTrue(connected.await(10, TimeUnit.SECONDS))
            val apply = RemotePlaybackService::class.java.getDeclaredMethod("applyRemoteSnapshot", EchoState::class.java).apply { isAccessible = true }
            val notifications = context.getSystemService(NotificationManager::class.java)
            val tracks = (0..4).map { index ->
                val cover = java.io.File(context.cacheDir, "notification-cover-$index.png").also { covers.add(it) }
                val bitmap = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.rgb(index * 40, 80, 100))
                cover.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                Track("notification-$index", "Remote song $index", "Artist", durationSec = 180, thumbnailUri = cover.toURI().toString())
            }
            fun send(index: Int, mode: String) {
                instrumentation.runOnMainSync {
                    apply.invoke(service, EchoState(track = tracks[index], queue = tracks, index = index,
                        playing = true, confirmed = true, durationMs = 180_000, anchoredAt = SystemClock.elapsedRealtime(),
                        sharedOutput = SharedPlaybackOutput(mode = mode, owner = "fixture", token = "fixture", epoch = "fixture")))
                }
                until("Notification did not show ${tracks[index].title}") {
                    notifications.activeNotifications.any { it.id == 1002 &&
                        it.notification.extras.getString(android.app.Notification.EXTRA_TITLE) == tracks[index].title }
                }
            }
            send(0, "phone")
            until("Remote session did not enter foreground") {
                var ongoing = false
                instrumentation.runOnMainSync { ongoing = service.isPlaybackOngoing }
                ongoing
            }
            SystemClock.sleep(800) // Let the initial Media3 self-start command settle.
            val starts = RemoteNotificationDiagnostics.snapshot().count { it.endsWith(":start_command") }
            assertTrue("Initial foreground start was not observed", starts > 0)
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            until("App did not enter background") {
                !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
            for (index in 1..4) {
                val output = if (index < 3) "REMOTE_PHONE" else "ALEXA"
                prefs.edit().putString("playback_output", output).commit()
                send(index, if (index < 3) "phone" else "alexa")
                SystemClock.sleep(500) // Include asynchronous bitmap completion.
                assertTrue(notifications.activeNotifications.any { it.id == 1002 })
            }
            assertEquals("Track/artwork refresh restarted the background foreground service", starts,
                RemoteNotificationDiagnostics.snapshot().count { it.endsWith(":start_command") })
            assertTrue("Artwork callback was not exercised", RemoteNotificationDiagnostics.snapshot().any { it.endsWith(":artwork_update") })
        } finally {
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            context.stopService(intent)
            context.unbindService(connection)
            covers.forEach { it.delete() }
            prefs.edit().apply {
                saved.forEach { (key, value) -> when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> remove(key)
                } }
            }.commit()
        }
    }
}
