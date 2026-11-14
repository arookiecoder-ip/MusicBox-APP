package `in`.synthora.musicbox.services

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE,sdk=[28])
class JamPhoneAuthorizationTest {
    @Test fun selectedJamPhoneDoesNotNeedToTakeThePersonalEchoLease() {
        PhonePlaybackOwnership.forget()
        assertFalse(PhonePlaybackOwnership.permitsPlayback())
        PhonePlaybackOwnership.jamPlayback=true
        assertTrue(PhonePlaybackOwnership.permitsPlayback())
        assertTrue(PhonePlaybackOwnership.permitsLocalContinuation())
        assertEquals("",PhonePlaybackOwnership.token)
        PhonePlaybackOwnership.forget()
        assertFalse(PhonePlaybackOwnership.jamPlayback)
        assertFalse(PhonePlaybackOwnership.permitsPlayback())
    }
}
