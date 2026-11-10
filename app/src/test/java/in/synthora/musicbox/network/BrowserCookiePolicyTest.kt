package `in`.synthora.musicbox.network

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class BrowserCookiePolicyTest {
    @Test fun onlyMatchingLiveCookiesAreExportedWithTheirSecurityAttributes() {
        val origin = "https://owner.example/"
        fun cookie(url: String, value: String) = requireNotNull(Cookie.parse(url.toHttpUrl(), value))
        val headers = browserCookieHeaders(origin, listOf(
            cookie(origin, "session=owner; Path=/; Secure; HttpOnly; Max-Age=3600"),
            cookie("https://other.example/", "session=other; Path=/; Secure; HttpOnly"),
            cookie(origin, "narrow=secret; Path=/private; Secure; HttpOnly"),
            cookie(origin, "expired=no; Max-Age=0; Path=/")))
        assertEquals(1, headers.size)
        assertTrue(headers.single().contains("httponly", true))
        assertTrue(headers.single().contains("secure", true))
        assertTrue(headers.single().contains("path=/", true))
        assertTrue(headers.single().contains("SameSite=Lax"))
        assertFalse(headers.single().contains("other"))
    }
    @Test fun secureCookiesCannotBeExportedToCleartextOrAnotherPath() {
        val cookie = requireNotNull(Cookie.parse("https://owner.example/private/".toHttpUrl(), "session=owner; Path=/private; Secure; HttpOnly"))
        assertTrue(browserCookieHeaders("http://owner.example/private/", listOf(cookie)).isEmpty())
        assertTrue(browserCookieHeaders("https://owner.example/", listOf(cookie)).isEmpty())
        assertTrue(browserCookieHeaders("https://owner.example/private/page", listOf(cookie)).single().contains("path=/private"))
    }
}
