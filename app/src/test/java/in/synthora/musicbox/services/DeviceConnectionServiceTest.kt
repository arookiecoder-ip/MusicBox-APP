package `in`.synthora.musicbox.services

import android.app.Application
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], manifest=Config.NONE, application=Application::class)
class DeviceConnectionServiceTest {
    @Test fun idleConnectionNeverPromotesAndStickyRestartStopsImmediately() {
        val app=RuntimeEnvironment.getApplication()
        val controller=Robolectric.buildService(DeviceConnectionService::class.java).create()
        val service=controller.get()
        try {
            assertEquals(Service.START_NOT_STICKY, service.onStartCommand(Intent(app,DeviceConnectionService::class.java),0,1))
            assertNull(shadowOf(service).lastForegroundNotification)
            assertEquals(Service.START_NOT_STICKY, service.onStartCommand(null,0,2))
            assertNull(shadowOf(service).lastForegroundNotification)
            DeviceConnectionService.stop(app)
            assertFalse(app.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id==1003 })
        } finally { controller.destroy() }
    }
}
