package `in`.synthora.musicbox.services
import org.junit.Assert.*
import org.junit.Test
class PreloadPowerPolicyTest {
    private fun policy(power: PreloadPowerState) = preloadPolicy(true, false, true, false, power)
    @Test fun constraintsOnlySuspendSpeculationAndChargingDoesNotBypassThermalLimits() {
        assertEquals(0, policy(PreloadPowerState(batteryPercent = 15)).tracks)
        assertEquals(5, policy(PreloadPowerState(batteryPercent = 15, charging = true)).tracks)
        assertEquals(0, policy(PreloadPowerState(batterySaver = true, charging = true)).tracks)
        assertEquals(1, policy(PreloadPowerState(thermalStatus = 2)).tracks)
        for (status in 3..6) assertEquals(0, policy(PreloadPowerState(thermalStatus = status, charging = true)).tracks)
        assertEquals(5, policy(PreloadPowerState()).tracks)
        assertEquals(-1L, policy(PreloadPowerState()).bytesPerTrack)
    }
    @Test fun unknownBatteryDoesNotDisableWarmupAndNetworkRestrictionsStillWin() {
        assertEquals(5, policy(PreloadPowerState(batteryPercent = -1)).tracks)
        assertEquals(0, preloadPolicy(false, false, true, false).tracks)
        assertEquals(0, preloadPolicy(true, true, false, false, PreloadPowerState(charging = true)).tracks)
        assertEquals(0, preloadPolicy(true, false, true, true).tracks)
    }
}
