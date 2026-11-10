package `in`.synthora.musicbox.network

import org.junit.Assert.*
import org.junit.Test

class AccountNavigationTest {
    @Test fun amazonVerificationSubframesAreAllowedOnSupportedRegions() {
        assertTrue(amazonVerificationFrame("https://www.amazon.in/ap/cvf"))
        assertTrue(amazonVerificationFrame("https://captcha.amazon.com/verify"))
        assertTrue(amazonVerificationFrame("https://www.amazon.co.uk/ap/signin"))
    }
    @Test fun unrelatedHostsAndSuffixSpoofingAreRejected() {
        assertFalse(amazonVerificationFrame("https://amazon.com.evil.test/login"))
        assertFalse(amazonVerificationFrame("https://notamazon.com/login"))
        assertFalse(amazonVerificationFrame("https://example.test/amazon.com"))
    }
    @Test fun unsafeSchemesAndCredentialsCannotBypassTheBoundary() {
        assertFalse(amazonVerificationFrame("http://www.amazon.com/ap/cvf"))
        assertFalse(amazonVerificationFrame("https://user@www.amazon.com/ap/cvf"))
        assertFalse(amazonVerificationFrame("https://www.amazon.com:444/ap/cvf"))
        assertFalse(amazonVerificationFrame("javascript:alert(1)"))
    }
}
