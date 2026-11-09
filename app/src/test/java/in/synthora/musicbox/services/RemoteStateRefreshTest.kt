package `in`.synthora.musicbox.services
import org.junit.Assert.*
import org.junit.Test
class RemoteStateRefreshTest {
    @Test fun leaseRenewalsAreIgnoredButQueueRevisionSwitchAndPeriodicRepairRefresh() {
        val gate = RemoteStateRefresh()
        val first = SharedPlaybackOutput(mode = "phone", owner = "other", token = "one", epoch = "server", presenceRevision = 5)
        assertTrue(gate.needed(first, 100))
        gate.completed(first, 100)
        assertFalse(gate.needed(first.copy(leaseMs = 9_000), 8_100))
        assertTrue(gate.needed(first.copy(presenceRevision = 6), 8_100))
        assertTrue(gate.needed(first.copy(token = "new"), 8_100))
        assertTrue(gate.needed(first.copy(epoch = "restart"), 8_100))
        assertTrue(gate.needed(first, 30_100))
    }
    @Test fun failedReadIsNotMarkedDoneAndOldServersHaveBoundedFallback() {
        val gate = RemoteStateRefresh()
        val first = SharedPlaybackOutput(owner = "other", token = "one")
        assertTrue(gate.needed(first, 0))
        assertTrue(gate.needed(first, 2_000))
        gate.completed(first, 2_000)
        assertFalse(gate.needed(first, 10_000))
        assertTrue(gate.needed(first, 32_000))
    }
}
