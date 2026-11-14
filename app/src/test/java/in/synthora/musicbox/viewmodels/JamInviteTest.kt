package `in`.synthora.musicbox.viewmodels

import org.junit.Assert.*
import org.junit.Test

class JamInviteTest {
    private val base="https://testalexa.synthora.in"
    @Test fun invitationAcceptsOnlyConfiguredHttpsHost() {
        assertEquals("abcdefgh_123",jamInviteToken("$base/j/abcdefgh_123",base))
        assertEquals("abcdefgh_123",jamInviteToken("$base/jam/abcdefgh_123",base))
        assertEquals("abcdefgh_123",jamInviteToken("$base:443/j/abcdefgh_123",base))
        assertNull(jamInviteToken("https://other.test/j/abcdefgh_123",base))
        assertNull(jamInviteToken("http://testalexa.synthora.in/j/abcdefgh_123",base))
        assertNull(jamInviteToken("https://user@testalexa.synthora.in/j/abcdefgh_123",base))
        assertNull(jamInviteToken("https://testalexa.synthora.in:8443/j/abcdefgh_123",base))
    }
    @Test fun malformedAndUnrelatedPathsCannotBecomeJoinRequests() {
        listOf("/account", "/j/", "/j/short", "/j/abcdefgh%2F12", "/j/abcdefgh/123", "/jam-invite/abcdefgh_123").forEach {
            assertNull(it,jamInviteToken(base+it,base))
        }
        assertNull(jamInviteToken("not a URL",base))
    }
}
