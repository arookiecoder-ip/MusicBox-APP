package `in`.synthora.musicbox.services
import org.junit.Assert.*
import org.junit.Test

class RecoveryCancelAuthorizationTest {
    @Test fun forgedDirectServiceIntentsCannotCancelPlaybackRecovery() {
        val auth = RecoveryCancelAuthorization()
        assertFalse(auth.accepts(null))
        assertFalse(auth.accepts(""))
        assertFalse(auth.accepts("forged"))
        assertTrue(auth.accepts(auth.proof))
    }
    @Test fun notificationFromADeadServiceCannotCancelANewSession() {
        val old = RecoveryCancelAuthorization()
        val current = RecoveryCancelAuthorization()
        assertFalse(current.accepts(old.proof))
        assertTrue(current.accepts(current.proof))
    }
}
