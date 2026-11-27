package `in`.synthora.musicbox.services

import android.app.Application
import java.time.Duration
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class PhoneSleepTimerTest {
    private val context get() = RuntimeEnvironment.getApplication<Application>()
    @Before fun reset() { PhoneSleepTimer.cancel(context) }
    @After fun cleanup() { PhoneSleepTimer.cancel(context) }
    @Test fun persistedDeadlineExpiresOnceWithoutTheUiCountdown() {
        PhoneSleepTimer.set(context, 60_000)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(59))
        assertFalse(PhoneSleepTimer.consumeExpired(context, true))
        ShadowSystemClock.advanceBy(Duration.ofSeconds(6))
        assertTrue(PhoneSleepTimer.consumeExpired(context, true))
        assertFalse(PhoneSleepTimer.consumeExpired(context, true))
    }
    @Test fun replacementAndCancellationInvalidateTheOldDeadline() {
        PhoneSleepTimer.set(context, 1_000)
        PhoneSleepTimer.set(context, 60_000)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
        assertFalse(PhoneSleepTimer.consumeExpired(context, true))
        PhoneSleepTimer.cancel(context)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(70))
        assertFalse(PhoneSleepTimer.consumeExpired(context, true))
    }
    @Test fun accountAndOutputChangesCannotPauseAnotherPlaybackSession() {
        assertTrue(sleepTimerApplies("alice", "lease", "alice", "lease", true))
        assertFalse(sleepTimerApplies("alice", "lease", "bob", "lease", true))
        assertFalse(sleepTimerApplies("alice", "lease", "alice", "new", true))
        assertFalse(sleepTimerApplies("alice", "lease", "alice", "lease", false))
    }
}
