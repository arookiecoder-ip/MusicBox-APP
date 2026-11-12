package `in`.synthora.musicbox.viewmodels

import org.junit.Assert.*
import org.junit.Test

class AlexaAccessPolicyTest {
    @Test fun disabledAmazonDoesNotBlockConnectedYouTube() {
        assertTrue(AccountStatus(youtubeLibrary = true, youtubeCookies = true).allConnected)
    }
    @Test fun enabledAmazonRequiresConnection() {
        assertFalse(AccountStatus(alexaAccessEnabled = true, youtubeLibrary = true, youtubeCookies = true).allConnected)
        assertTrue(AccountStatus(alexaAccessEnabled = true, amazonConnected = true, youtubeLibrary = true, youtubeCookies = true).allConnected)
    }
    @Test fun disablingAmazonDoesNotBypassYouTubeChecks() {
        assertTrue(AccountStatus(youtubeLibrary = true).allConnected)
        assertFalse(AccountStatus(youtubeCookies = true).allConnected)
    }
}
