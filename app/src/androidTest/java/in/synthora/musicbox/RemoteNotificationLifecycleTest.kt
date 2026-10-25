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
        val bitmap = android.graphics.Bitmap.createBitmap(720, 720, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        val png = java.io.ByteArrayOutputStream().also { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        bitmap.recycle()
        val artwork = DelayedArtworkServer(png)
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
                Track("notification-$index", "Remote song $index", "Artist", durationSec = 180,
                    thumbnailUri = "http://127.0.0.1:${artwork.port}/$index-${System.nanoTime()}.png")
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
                // Reproduce the receiver's confirmed pause immediately before
                // changing songs, with no processing flag. 2.4.60 demoted here.
                instrumentation.runOnMainSync {
                    apply.invoke(service, EchoState(track = tracks[index - 1], queue = tracks, index = index - 1,
                        playing = false, confirmed = true, durationMs = 180_000,
                        sharedOutput = SharedPlaybackOutput(mode = if (index < 3) "phone" else "alexa",
                            owner = "fixture", token = "fixture", epoch = "fixture")))
                }
                SystemClock.sleep(150)
                send(index, if (index < 3) "phone" else "alexa")
                SystemClock.sleep(500) // Include asynchronous bitmap completion.
                assertTrue(notifications.activeNotifications.any { it.id == 1002 })
            }
            assertEquals("Track/artwork refresh restarted the background foreground service", starts,
                RemoteNotificationDiagnostics.snapshot().count { it.endsWith(":start_command") })
            assertTrue("Artwork callback was not exercised", RemoteNotificationDiagnostics.snapshot().any { it.endsWith(":artwork_update") })
            instrumentation.runOnMainSync {
                apply.invoke(service, EchoState(track = tracks.last(), queue = tracks, index = 4,
                    playing = false, confirmed = true, durationMs = 180_000,
                    sharedOutput = SharedPlaybackOutput(mode = "alexa", owner = "fixture", token = "fixture", epoch = "fixture")))
            }
            SystemClock.sleep(10_500)
            until("Sustained pause did not release foreground resources") {
                var ongoing = true
                instrumentation.runOnMainSync { ongoing = service.isPlaybackOngoing }
                !ongoing
            }
            assertTrue("Sustained pause removed media controls", notifications.activeNotifications.any { it.id == 1002 })
            val pausedStarts = RemoteNotificationDiagnostics.snapshot().count { it.endsWith(":start_command") }
            send(4, "alexa")
            SystemClock.sleep(300)
            assertEquals("External resume tried to restart a background service", pausedStarts,
                RemoteNotificationDiagnostics.snapshot().count { it.endsWith(":start_command") })
            assertTrue(RemoteNotificationDiagnostics.snapshot().any { it.endsWith(":background_regular_update") })
            val denied = RemotePlaybackService::class.java.getDeclaredMethod("onRemoteForegroundDenied").apply { isAccessible = true }
            instrumentation.runOnMainSync { denied.invoke(service) }
            SystemClock.sleep(200)
            assertTrue("Denied promotion removed existing controls", notifications.activeNotifications.any { it.id == 1002 })
            assertTrue(RemoteNotificationDiagnostics.snapshot().any { it.endsWith(":foreground_denied_retained") })
            send(4, "alexa")
            SystemClock.sleep(300)
            assertTrue("Background resume removed retained controls", notifications.activeNotifications.any { it.id == 1002 })
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            val mediaIntent = Intent(context, RemotePlaybackService::class.java).setAction(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_UNKNOWN))
            // Unknown key exercises service promotion without contacting a real receiver.
            android.app.PendingIntent.getForegroundService(context, 8091, mediaIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT).send()
            until("User media action did not regain foreground status") {
                var ongoing = false
                instrumentation.runOnMainSync { ongoing = service.isPlaybackOngoing }
                ongoing
            }
            assertTrue(RemoteNotificationDiagnostics.snapshot().any { it.endsWith(":user_media_promotion") })
        } finally {
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            context.stopService(intent)
            context.unbindService(connection)
            artwork.close()
            prefs.edit().apply {
                saved.forEach { (key, value) -> when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> remove(key)
                } }
            }.commit()
        }
    }

    /** Local HD images arrive after metadata so the real async provider path runs. */
    private class DelayedArtworkServer(private val png: ByteArray) : java.io.Closeable {
        private val server = java.net.ServerSocket(0, 8, java.net.InetAddress.getByName("127.0.0.1"))
        val port: Int get() = server.localPort
        private val worker = Thread {
            while (!server.isClosed) {
                try {
                    server.accept().use { client ->
                        client.soTimeout = 2000
                        val input = client.getInputStream().bufferedReader()
                        while (true) { val line = input.readLine() ?: break; if (line.isEmpty()) break }
                        SystemClock.sleep(250)
                        val output = client.getOutputStream()
                        output.write("HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: ${png.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        output.write(png)
                        output.flush()
                    }
                } catch (_: java.io.IOException) { if (server.isClosed) break }
            }
        }.apply { isDaemon = true; start() }
        override fun close() { server.close(); worker.join(3000) }
    }
}
