package `in`.synthora.musicbox.network
import org.junit.Assert.*
import org.junit.Test
class AppLinkTest {
    private val base="https://alexa.synthora.in"
    @Test fun nativeTargetsPreserveTheirIdentifiers() {
        assertEquals(AppLink("song","abcdefghijk"),appLink("$base/watch?v=abcdefghijk",base))
        assertEquals(AppLink("song","abcdefghijk"),appLink("https://music.youtube.com/watch?v=abcdefghijk&si=test",base))
        assertEquals(AppLink("playlist","PLexample"),appLink("https://music.youtube.com/playlist?list=PLexample",base))
        assertEquals(AppLink("artist","UCexample"),appLink("https://music.youtube.com/channel/UCexample",base))
        assertEquals(AppLink("album","MPREexample"),appLink("https://music.youtube.com/browse/MPREexample",base))
        assertEquals(AppLink("jam","abcdefghijk"),appLink("$base/jam-invite/abcdefghijk",base))
        assertEquals(AppLink("account"),appLink("$base/account",base))
    }
    @Test fun externalOriginsCredentialsAndEncodedRouteInjectionAreRejected() {
        for(url in listOf("https://other.test/account","https://alexa.synthora.in.evil.test/account","https://person@alexa.synthora.in/account","http://alexa.synthora.in/account","javascript:alert(1)","https://music.youtube.com/browse/bad%2Fpath"))assertNull(url,appLink(url,base))
    }
    @Test fun signupClosureDoesNotHideRecoveryAndUnknownAvailabilityDoesNotOfferSignup() {
        assertEquals(AccountWebApi.DoorAvailability(true,false),AccountWebApi.doorAvailability(503))
        assertEquals(AccountWebApi.DoorAvailability(false,false),AccountWebApi.doorAvailability(404))
        assertEquals(AccountWebApi.DoorAvailability(true,true),AccountWebApi.doorAvailability(200))
        assertNull(AccountWebApi.doorAvailability(500))
    }
}
