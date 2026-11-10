package `in`.synthora.musicbox.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** Retire the old idle keepalive. Actual playback owns its normal media notification. */
class DeviceConnectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(ID)
        stopSelf(startId)
        return START_NOT_STICKY
    }
    companion object {
        private const val ID = 1003
        fun stop(context: Context) {
            context.stopService(Intent(context, DeviceConnectionService::class.java))
            context.getSystemService(NotificationManager::class.java).cancel(ID)
        }
    }
}
