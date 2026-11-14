package `in`.synthora.musicbox.network

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AccountSessionErrorsTest {
    @Test fun providerAndPlaybackRejectionsCannotLogOutAValidAccount()=runTest {
        for(path in listOf("/alexa/command/","/api/app/queue/","/api/library/","/alexa/now_playing/")) {
            var checked=0
            val result=verifiedAccountFailure(path,BackendAuthException("Provider denied",path)){checked++}
            assertTrue(result is BackendHttpException);assertEquals(1,checked)
        }
    }
    @Test fun onlyCanonicalExpiryOrConfirmedRejectionCanEndSession()=runTest {
        val original=BackendAuthException("Revoked")
        assertSame(original,verifiedAccountFailure("/api/account/",original){error("No recursive probe")})
        assertSame(original,verifiedAccountFailure("/alexa/command/",original){throw BackendAuthException("Revoked account")})
    }
    @Test fun validationOutageAndCancellationNeverBecomeExpiry()=runTest {
        assertTrue(verifiedAccountFailure("/alexa/command/",BackendAuthException("Denied")){throw BackendHttpException(503,"Database unavailable")} is BackendHttpException)
        try {verifiedAccountFailure("/alexa/command/",BackendAuthException("Denied")){throw CancellationException("New login")};fail("Cancellation lost")}
        catch(_:CancellationException){}
    }
}
