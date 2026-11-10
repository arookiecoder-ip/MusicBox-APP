package `in`.synthora.musicbox.network

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class CookieJarTest {
    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        lateinit var editor: SharedPreferences.Editor
        editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { _, method, args ->
            when (method.name) {
                "putString" -> { values[args[0] as String] = args[1] as String; editor }
                "putStringSet" -> { @Suppress("UNCHECKED_CAST") values[args[0] as String] = (args[1] as Set<String>).toSet(); editor }
                "remove" -> { values.remove(args[0] as String); editor }
                "apply" -> null
                "commit" -> true
                else -> editor
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString" -> values[args[0]] as? String ?: args[1]
                "getStringSet" -> values[args[0]] ?: args[1]
                "edit" -> editor
                else -> null
            }
        } as SharedPreferences
    }

    // Production encrypts with the Android Keystore; this reversible test codec
    // keeps the cookie-routing assertions independent of a device key store.
    private fun jar(prefs: SharedPreferences) = PersistentCookieJar(prefs, { it }, { it })

    @Test fun stagingSessionPersistsWithoutLeakingToAnotherHostOrProductionPath() {
        val prefs = preferences()
        val login = "https://example.com/staging/login/".toHttpUrl()
        val cookie = Cookie.parse(login, "session=owner; Path=/staging; Secure; HttpOnly")!!
        jar(prefs).saveFromResponse(login, listOf(cookie))
        val restored = jar(prefs)
        assertEquals("owner", restored.loadForRequest("https://example.com/staging/api/home/".toHttpUrl()).single().value)
        assertTrue(restored.loadForRequest("https://example.com/api/home/".toHttpUrl()).isEmpty())
        assertTrue(restored.loadForRequest("https://other.com/staging/api/home/".toHttpUrl()).isEmpty())
        assertTrue(restored.loadForRequest("http://example.com/staging/api/home/".toHttpUrl()).isEmpty())
    }

    @Test fun serverCookieDeletionAndLocalSignOutPersistAcrossRestarts() {
        val prefs = preferences()
        val url = "https://example.com/login/".toHttpUrl()
        val cookies = jar(prefs)
        cookies.saveFromResponse(url, listOf(Cookie.parse(url, "session=owner; Path=/; Secure")!!))
        cookies.saveFromResponse(url, listOf(Cookie.parse(url, "session=; Path=/; Max-Age=0; Secure")!!))
        assertFalse(jar(prefs).hasCookies())
        cookies.saveFromResponse(url, listOf(Cookie.parse(url, "session=owner; Path=/; Secure")!!))
        cookies.clear()
        assertTrue(jar(prefs).loadForRequest(url).isEmpty())
    }
}
