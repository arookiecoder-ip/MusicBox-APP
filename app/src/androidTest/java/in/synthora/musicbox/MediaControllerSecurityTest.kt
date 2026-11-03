package `in`.synthora.musicbox

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MediaControllerSecurityTest {
    @Test fun anotherInstalledUidCannotConnectToTheOwnerMediaSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.context
        val target = instrumentation.targetContext
        @Suppress("DEPRECATION")
        target.packageManager.getServiceInfo(ComponentName(target, `in`.synthora.musicbox.services.PlaybackService::class.java), 0)
        val completed = CountDownLatch(1)
        var result: Bundle? = null
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, data: Bundle?) { result = data; completed.countDown() }
        }
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
            override fun onServiceDisconnected(name: ComponentName?) {}
        }
        val intent = Intent().setComponent(ComponentName(context.packageName, UntrustedControllerProbeService::class.java.name))
            .putExtra("result", receiver)
        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Separate controller did not report its result", completed.await(15, TimeUnit.SECONDS))
            val report = requireNotNull(result)
            assertNotEquals("Probe must run under another installed UID", target.applicationInfo.uid, report.getInt("uid"))
            assertTrue("Expected an explicit security rejection: ${report.getString("errorType")}", report.getBoolean("rejected"))
        } finally { context.unbindService(connection) }
    }
}
