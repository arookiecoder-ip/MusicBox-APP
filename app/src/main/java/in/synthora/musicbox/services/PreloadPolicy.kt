package `in`.synthora.musicbox.services

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

data class PreloadPolicy(val tracks: Int, val bytesPerTrack: Long)
data class PreloadPowerState(val batteryPercent: Int? = null, val charging: Boolean = false,
    val batterySaver: Boolean = false, val thermalStatus: Int = 0)

/** Constraints apply to speculative work, never the selected song's demand writer. */
fun preloadPolicy(online: Boolean, metered: Boolean, allowMetered: Boolean, buffering: Boolean,
    power: PreloadPowerState = PreloadPowerState()): PreloadPolicy = when {
    !online || buffering || (metered && !allowMetered) -> PreloadPolicy(0, 0)
    power.batterySaver || power.thermalStatus >= 3 ||
        (!power.charging && power.batteryPercent?.let { it in 0..15 } == true) -> PreloadPolicy(0, 0)
    power.thermalStatus == 2 -> PreloadPolicy(1, -1L)
    else -> PreloadPolicy(5, -1L)
}

internal fun preloadPowerState(context: Context?): PreloadPowerState {
    if (context == null) return PreloadPowerState()
    return runCatching {
        val battery = context.getSystemService(BatteryManager::class.java)
        val power = context.getSystemService(PowerManager::class.java)
        PreloadPowerState(battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 },
            battery.isCharging, power.isPowerSaveMode,
            if (Build.VERSION.SDK_INT >= 29) power.currentThermalStatus else 0)
    }.getOrDefault(PreloadPowerState())
}
