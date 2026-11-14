package `in`.synthora.musicbox.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.IOException
import android.content.Context
import android.content.SharedPreferences
import `in`.synthora.musicbox.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import `in`.synthora.musicbox.utils.SecureStore
import java.net.URLEncoder
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The web-remote session expired or was never opened: the user must sign in again. */
class BackendAuthException(
    message: String,
    val endpoint: String? = null,
    val statusCode: Int = 401
) : Exception(message)
class BackendHttpException(
    val statusCode: Int,
    message: String,
    val retryAfterMs: Long? = null,
    /** Parsed JSON error body, when the endpoint returned one. Never logged. */
    val payload: JsonElement? = null
) : IllegalStateException(message)

/** A raw HTML door response. Redirects are data here, not an authentication failure. */
data class FormResponse(val status: Int, val body: String, val location: String?)

/** Result of the first login step. */
sealed interface LoginStep {
    data object SignedIn : LoginStep
    data class NeedsCode(val token: String) : LoginStep
}

/**
 * The self-hosted server behind the web remote. Account, library, Home and Echo calls use the
 * owner's web session (username + password + authenticator code), stored in a persistent cookie
 * jar so the app stays signed in. Only the audio stream endpoints use the build's API key.
 */
object Backend {

    private const val DEFAULT_SERVER = "https://alexa.synthora.in"

    val audioBaseUrl: String get() = BuildConfig.ALEXA_BASE_URL.trim().trimEnd('/').ifBlank { DEFAULT_SERVER }
    val baseUrl: String get() = BuildConfig.ALEXA_SESSION_BASE_URL.trim().trimEnd('/').ifBlank { audioBaseUrl }
    val apiKey: String get() = BuildConfig.ALEXA_API_KEY.trim()

    private lateinit var jar: PersistentCookieJar

    fun init(context: Context) {
        if (!::jar.isInitialized) {
            jar = PersistentCookieJar(context.getSharedPreferences("backend_session", Context.MODE_PRIVATE))
        }
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            followRedirects = false
            engine {
                config {
                    cookieJar(jar)
                    followRedirects(false)
                    followSslRedirects(false)
                    retryOnConnectionFailure(true)
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 45_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    private val eventClient by lazy {
        okhttp3.OkHttpClient.Builder().cookieJar(jar)
            .followRedirects(false).followSslRedirects(false)
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .pingInterval(20, java.util.concurrent.TimeUnit.SECONDS).build()
    }

    /** One owner-authenticated read-only event stream; cancellation closes its socket. */
    fun playbackEvents(serial: String): kotlinx.coroutines.flow.Flow<JsonObject> {
        val url = (baseUrl + "/api/app/events/").toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("serial", serial.ifBlank { "phone" }).build()
        return playbackEventStream(eventClient, url)
    }

    /** True while a web-session cookie is stored (it may still have been revoked server-side). */
    fun hasSession(): Boolean = jar.hasCookies()

    fun clearSession() { AudioCredentials.clear(); jar.clear() }

    suspend fun get(path: String, query: Map<String, String> = emptyMap()): JsonElement =
        call(HttpMethod.Get, path, query, null)

    suspend fun post(
        path: String,
        body: JsonObject = JsonObject(emptyMap()),
        headers: Map<String, String> = emptyMap()
    ): JsonElement = call(HttpMethod.Post, path, emptyMap(), body, headers)

    /** Download cookies belong to the audio server, which may differ from account sign-in. */
    suspend fun downloadCookieRequest(method: HttpMethod, body: JsonObject? = null, force: Boolean = false): JsonElement =
        performCall(method, if (method == HttpMethod.Get) "/api/youtube/download-cookies/status" else "/api/youtube/download-cookies",
            if (force) mapOf("refresh" to "1") else emptyMap(), body, audio = true)

    suspend fun patch(
        path: String,
        body: JsonObject,
        headers: Map<String, String> = emptyMap()
    ): JsonElement = call(HttpMethod.Patch, path, emptyMap(), body, headers)

    // Session writes must be JSON (the server's CSRF guard), so DELETE always carries a body.
    suspend fun delete(
        path: String,
        body: JsonObject = JsonObject(emptyMap()),
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): JsonElement = call(HttpMethod.Delete, path, query, body, headers)

    /**
     * Step 1 of the web login. Returns [LoginStep.NeedsCode] when the owner account has an
     * authenticator, otherwise the session is open.
     */
    suspend fun login(username: String, password: String): LoginStep {
        val result = post("/login/", JsonObject(mapOf(
            "username" to JsonPrimitive(username),
            "password" to JsonPrimitive(password)
        ))).objectOrEmpty()
        return if (result.flag("totp_required")) LoginStep.NeedsCode(result.text("token"))
        else LoginStep.SignedIn
    }

    /** Step 2 of the web login: the 6-digit authenticator code. */
    suspend fun verifyCode(token: String, code: String) {
        post("/login/", JsonObject(mapOf(
            "step" to JsonPrimitive("totp"),
            "token" to JsonPrimitive(token),
            "code" to JsonPrimitive(code.trim())
        )))
    }

    suspend fun logout() {
        try { post("/logout/") } finally { clearSession() }
    }

    private suspend fun call(
        method: HttpMethod,
        path: String,
        query: Map<String, String>,
        body: JsonObject?,
        headers: Map<String, String> = emptyMap()
    ): JsonElement = coroutineScope {
        // Background presence/queue polls share this client with browsing. A slow
        // request is not a user-visible failure; feature loaders show pending work.
        try {
            try { performCall(method, path, query, body, headers = headers) }
            catch (error: BackendAuthException) {
                // A YouTube/Alexa/optional endpoint can reject access while the
                // Music Box account cookie remains valid. Only the account door
                // establishes expiry; outages must retain protected cookies.
                throw verifiedAccountFailure(path,error) {
                    performCall(HttpMethod.Get,"/api/account",emptyMap(),null)
                }
            }
            catch (error: BackendHttpException) {
                if (method != HttpMethod.Get || error.statusCode != 429 || error.retryAfterMs == null) throw error
                delay(error.retryAfterMs)
                performCall(method, path, query, body, headers = headers) // Only read-only requests are retried, once.
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val message = networkErrorMessage(error)
            if (message != null) {
                NetworkFeedback.notify(message)
                throw IOException(message, error)
            }
            throw error
        }
    }

    private suspend fun performCall(
        method: HttpMethod,
        path: String,
        query: Map<String, String>,
        body: JsonObject?,
        audio: Boolean = false,
        headers: Map<String, String> = emptyMap()
    ): JsonElement {
        require(path.startsWith("/") && !path.startsWith("//"))
        val requestStarted = System.nanoTime()
        val serializedBody = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { body?.toString() }
        val response = client.request((if (audio) audioBaseUrl else baseUrl) + path) {
            this.method = method
            if (audio && apiKey.isNotBlank()) header("X-Api-Key", apiKey)
            if (audio || path == "/api/profile_status/") timeout {
                requestTimeoutMillis = 90_000
                socketTimeoutMillis = 90_000
            }
            header(HttpHeaders.Accept, "application/json")
            headers.forEach { (name, value) -> header(name, value) }
            query.forEach { (name, value) -> parameter(name, value) }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(serializedBody.orEmpty())
            }
        }
        val text = response.bodyAsText()
        val parsed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { Json.parseToJsonElement(text) }.getOrNull() }
        val code = response.status.value
        `in`.synthora.musicbox.services.PlaybackDiagnostics.record(`in`.synthora.musicbox.services.PlaybackDiagnostics.Stage.SERVER_REQUEST,
            (System.nanoTime() - requestStarted) / 1_000_000, code !in 200..299)
        if (code == 401) {
            if (audio) throw BackendHttpException(code, "The audio server needs its owner login or a valid API key.")
            throw BackendAuthException(errorMessage(parsed) ?: "Your session has ended. Sign in again.", path, code)
        }
        // Without a session the server redirects browser-style requests to its login page.
        if (code in 300..399) {
            if (path == "/login/") {
                throw IllegalStateException("Account login is unavailable on this server. Update the app or contact the server administrator.")
            }
            throw BackendAuthException("Sign in to continue.", path, code)
        }
        if (code !in 200..299) {
            val message = when (code) {
                408, 504 -> "The server took too long to respond. Please try again."
                429 -> "Too many requests. Please wait a moment and try again."
                in 500..599 -> "The server is temporarily unavailable. Please try again."
                else -> errorMessage(parsed) ?: "Request failed ($code). Please try again."
            }
            if (code >= 500 || code == 408 || code == 429) NetworkFeedback.notify(message)
            throw BackendHttpException(code, message, if (code == 429) rateLimitReadRetryDelay(response.headers[HttpHeaders.RetryAfter]) else null, parsed)
        }
        return parsed ?: throw IllegalStateException("The server returned an unreadable response. Please try again.")
    }

    private fun errorMessage(parsed: JsonElement?): String? {
        val error = parsed.objectOrEmpty()["error"]
        return ((error as? JsonPrimitive)?.contentOrNull ?: error.objectOrEmpty().text("message"))
            .takeIf { it.isNotBlank() }
    }

    /**
     * Native account doors (sign-up, verification, recovery) are HTML forms on the
     * same origin as the session. Reading their CSRF token from the rendered form is
     * the only credential the server exposes without adding a second, weaker API.
     * They never follow redirects: a 302 is the server's success signal.
     */
    suspend fun formGet(path: String): FormResponse = formRequest(HttpMethod.Get, path, null)

    suspend fun formPost(path: String, fields: Map<String, String>): FormResponse =
        formRequest(HttpMethod.Post, path, fields)

    private suspend fun formRequest(method: HttpMethod, path: String, fields: Map<String, String>?): FormResponse {
        require(path.startsWith("/") && !path.startsWith("//"))
        val encoded = fields?.entries?.joinToString("&") { (name, value) ->
            "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        val response = client.request(baseUrl + path) {
            this.method = method
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            timeout { requestTimeoutMillis = 30_000; connectTimeoutMillis = 20_000; socketTimeoutMillis = 30_000 }
            if (encoded != null) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(encoded)
            }
        }
        return FormResponse(
            status = response.status.value,
            body = response.bodyAsText(),
            location = response.headers[HttpHeaders.Location]
        )
    }

    /** Only membership-authorized Jam streams may receive the account cookie. */
    fun jamAudioCookie(requestUrl: String): String? {
        if (!isJamAudioRequest(baseUrl, requestUrl)) return null
        val url = requestUrl.toHttpUrlOrNull() ?: return null
        return jar.loadForRequest(url).joinToString("; ") { "${it.name}=${it.value}" }.takeIf { it.isNotBlank() }
    }

    /** Cookie headers for the account sign-in browser, so it opens with the same session. */
    fun cookieHeaders(): List<String> = browserCookieHeaders(baseUrl, jar.all())
}

/** Preserve HttpOnly and URL scope, using the server session's Lax policy. */
internal fun browserCookieHeaders(destination: String, cookies: List<Cookie>): List<String> {
    val url = destination.toHttpUrlOrNull() ?: return emptyList()
    return cookies.filter { it.matches(url) && it.expiresAt > System.currentTimeMillis() }
        .map { cookie -> cookie.toString() + "; SameSite=Lax" }
}

/** Keeps the web session across app restarts, encrypted at rest. */
internal class PersistentCookieJar(
    private val prefs: SharedPreferences,
    private val encryptCookie: (String) -> String? = { SecureStore.encrypt(it) },
    private val decryptCookie: (String) -> String? = { SecureStore.decrypt(it) }
) : CookieJar {
    private val cookies = linkedMapOf<String, Cookie>()

    init {
        val restored = prefs.getString(ENCRYPTED_KEY, null)?.let { encoded ->
            decryptCookie(encoded)?.let { raw ->
                runCatching {
                    raw.lineSequence().mapNotNull { encodedCookie ->
                        parse(String(Base64.getDecoder().decode(encodedCookie), Charsets.UTF_8))
                    }.toList()
                }.getOrNull()
            }
        }
        if (restored != null) {
            restored.forEach { cookies[key(it)] = it }
        } else {
            // One-way migration from the plaintext cookie set used by older builds.
            prefs.getStringSet(LEGACY_KEY, emptySet()).orEmpty().forEach { stored ->
                parse(stored)?.let { cookies[key(it)] = it }
            }
            if (cookies.isNotEmpty()) persist()
            prefs.edit().remove(LEGACY_KEY).apply()
        }
    }

    private fun parse(stored: String): Cookie? {
        val (url, header) = stored.split('\n', limit = 2).takeIf { it.size == 2 } ?: return null
        val parsed = url.toHttpUrlOrNull()?.let { Cookie.parse(it, header) } ?: return null
        return parsed.takeIf { it.expiresAt >= System.currentTimeMillis() }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        cookies.forEach { cookie ->
            if (cookie.expiresAt < now) this.cookies.remove(key(cookie)) else this.cookies[key(cookie)] = cookie
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        if (cookies.values.removeAll { it.expiresAt < now }) persist()
        return cookies.values.filter { it.matches(url) }
    }

    @Synchronized
    fun hasCookies(): Boolean = cookies.values.any { it.expiresAt >= System.currentTimeMillis() }

    @Synchronized
    fun all(): List<Cookie> = cookies.values.toList()

    @Synchronized
    fun clear() {
        cookies.clear()
        prefs.edit().remove(ENCRYPTED_KEY).remove(LEGACY_KEY).apply()
    }

    private fun persist() {
        // Each entry is base64("origin + cookie header") on its own line, so a
        // cookie value can never forge a second entry.
        val raw = cookies.values.joinToString("\n") { cookie ->
            Base64.getEncoder().encodeToString("https://${cookie.domain}${cookie.path}\n$cookie".toByteArray(Charsets.UTF_8))
        }
        val encoded = encryptCookie(raw)
        if (encoded == null) {
            // Never fall back to plaintext: an unwritable session is a re-login, not a leak.
            prefs.edit().remove(ENCRYPTED_KEY).apply()
            return
        }
        prefs.edit().putString(ENCRYPTED_KEY, encoded).remove(LEGACY_KEY).apply()
    }

    private fun key(cookie: Cookie) = "${cookie.name}|${cookie.domain}|${cookie.path}"

    companion object {
        private const val ENCRYPTED_KEY = "cookies_encrypted_v1"
        private const val LEGACY_KEY = "cookies"
    }
}
