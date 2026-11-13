package `in`.synthora.musicbox.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Native account doors on top of the server's existing web session.
 *
 * Sign-up, verification and recovery are deliberately HTML forms on the server:
 * they already carry CSRF protection, a sign-up nonce, enumeration-safe replies
 * and the provider's rate limits. This client reads the live CSRF field, posts
 * the same form a browser would, and turns the rendered result into typed state.
 * Account reads/writes use the JSON account endpoints with that same CSRF token.
 */
object AccountWebApi {
    private const val CSRF_HEADER = "X-CSRF-Token"

    @Volatile
    private var csrfToken: String? = null

    enum class SignupStep(val value: String) { EMAIL("email"), CODE("code"), DETAILS("details") }
    data class SignupGate(
        val open: Boolean, val message: String, val csrf: String,
        val step: SignupStep = SignupStep.EMAIL, val maskedEmail: String = ""
    )
    data class SignupReply(val result: DoorResult, val gate: SignupGate? = null)
    data class DoorResult(
        val ok: Boolean,
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap(),
        val notice: String = ""
    )

    data class AccountSession(
        val id: String,
        val current: Boolean,
        val device: String,
        val seen: String,
        val signedIn: String,
        val address: String
    )

    data class LinkedProvider(
        val provider: String,
        val connected: Boolean,
        val detail: String
    )

    data class AccountProfile(
        val id: String,
        val username: String,
        val email: String,
        val pendingEmail: String,
        val emailVerified: Boolean,
        val verificationPending: Boolean,
        val role: String,
        val roleText: String,
        val totpEnabled: Boolean,
        val totpPending: Boolean,
        val mustChangePassword: Boolean,
        val sessions: List<AccountSession>,
        val providers: List<LinkedProvider>,
        val minPasswordLength: Int,
        val alexaAccessEnabled: Boolean = false
    )

    internal const val DOORS_MISSING =
        "This server doesn't have multi-user accounts yet, so sign-up, verification and password recovery are unavailable. " +
            "Sign in with the owner account, or ask the administrator to enable MULTI_USER_ENABLED and redeploy."

    class AccountActionException(
        message: String,
        val fields: Map<String, String> = emptyMap(),
        val retryAfterSeconds: Long = 0
    ) : Exception(message)

    // ---------------------------------------------------------------- public doors

    /**
     * Whether this server mounts the account pages at all. A 404 on the sign-up
     * form means the multi-user layer is off, not that the form is temporarily
     * unhappy, so the sign-in screen can stop offering doors that cannot open.
     * Null means the server could not be asked (offline, timeouts).
     */
    suspend fun doorsAvailable(): Boolean? = try {
        when (Backend.formGet("/signup").status) {
            404 -> false
            503 -> true // Sign-ups are closed, but the recovery doors do exist.
            in 200..399 -> true
            else -> null
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun openSignup(): SignupGate {
        val response = Backend.formGet("/signup")
        val message = DoorHtml.resultText(response.body)
        if (response.status == 503) return SignupGate(false, message.ifBlank { "Sign-ups are closed on this server." }, "")
        requireOk(response, message)
        return DoorHtml.signupGate(response.body)
            ?: throw AccountActionException("The signup step could not be read. Reload signup and try again.")
    }

    suspend fun beginSignup(csrf: String, email: String): SignupReply =
        signupStep(csrf, "email", mapOf("email" to email, "website" to ""))

    suspend fun verifySignup(csrf: String, code: String): SignupReply =
        signupStep(csrf, "code", mapOf("code" to code))

    suspend fun resendSignup(csrf: String): SignupReply = signupStep(csrf, "resend")

    suspend fun restartSignup(csrf: String): SignupReply = signupStep(csrf, "restart")

    suspend fun signup(
        csrf: String, username: String, email: String, password: String, confirmPassword: String
    ): SignupReply = signupStep(csrf, "details", mapOf(
        "username" to username, "password" to password, "confirm_password" to confirmPassword
    ))

    private suspend fun signupStep(
        csrf: String, step: String, fields: Map<String, String> = emptyMap()
    ): SignupReply {
        val response = Backend.formPost("/signup", fields + mapOf("_csrf" to csrf, "step" to step))
        // A redirect advances the same server-side challenge. Read its next form,
        // including the fresh CSRF token, instead of treating it as account creation.
        if (response.status == 303 && response.location == "/signup") {
            return SignupReply(DoorResult(true, ""), openSignup())
        }
        if (step == "details" && response.status == 303 && response.location == "/account") {
            return SignupReply(DoorResult(true, "Account created."), null)
        }
        val result = DoorHtml.doorResult(response, listOf("account created"))
        val gate = DoorHtml.signupGate(response.body)
        return SignupReply(result.copy(ok = result.ok && gate == null && response.status in 200..299), gate)
    }

    suspend fun openVerification(): String {
        val response = Backend.formGet("/verify")
        requireOk(response, DoorHtml.resultText(response.body))
        return DoorHtml.csrf(response.body)
            ?: throw AccountActionException("The verification page could not be read. Try again.")
    }

    suspend fun verifyEmail(csrf: String, username: String, code: String): DoorResult {
        val response = Backend.formPost(
            "/verify",
            mapOf("_csrf" to csrf, "username" to username, "code" to code)
        )
        return DoorHtml.doorResult(response, listOf("email confirmed"))
    }

    suspend fun resendVerification(identifier: String): DoorResult {
        val page = Backend.formGet("/resend-code")
        requireOk(page, DoorHtml.resultText(page.body))
        val csrf = DoorHtml.csrf(page.body)
            ?: throw AccountActionException("The verification page could not be read. Try again.")
        val response = Backend.formPost(
            "/resend-code",
            mapOf("_csrf" to csrf, "identifier" to identifier, "website" to "")
        )
        return DoorHtml.doorResult(response, listOf("fresh six-digit code", "still needs confirming"))
    }

    suspend fun openRecovery(): String {
        val response = Backend.formGet("/forgot-password")
        requireOk(response, DoorHtml.resultText(response.body))
        return DoorHtml.csrf(response.body)
            ?: throw AccountActionException("The password reset page could not be read. Try again.")
    }

    suspend fun requestPasswordReset(csrf: String, identifier: String): DoorResult {
        val response = Backend.formPost(
            "/forgot-password",
            mapOf("_csrf" to csrf, "identifier" to identifier, "website" to "")
        )
        return DoorHtml.doorResult(response, listOf("if an account matches"))
    }

    suspend fun openReset(): String {
        val response = Backend.formGet("/reset-password")
        requireOk(response, DoorHtml.resultText(response.body))
        return DoorHtml.csrf(response.body)
            ?: throw AccountActionException("The password reset page could not be read. Try again.")
    }

    suspend fun checkResetCode(csrf: String, identifier: String, code: String) {
        val response = Backend.formPost("/reset-password", mapOf("_csrf" to csrf, "identifier" to identifier, "code" to code, "verify_only" to "1"))
        val payload = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(response.body) as? JsonObject }.getOrNull()
        if (response.status !in 200..299 || payload?.get("ok")?.toString() != "true") {
            val detail = payload?.get("error") as? JsonObject
            throw AccountActionException(detail?.text("message") ?: "This code could not be verified. Check the account and code, or send a new one.")
        }
    }

    suspend fun resetPassword(
        csrf: String,
        identifier: String,
        code: String,
        password: String,
        confirmPassword: String
    ): DoorResult {
        val response = Backend.formPost(
            "/reset-password",
            mapOf(
                "_csrf" to csrf,
                "identifier" to identifier,
                "code" to code,
                "password" to password,
                "confirm_password" to confirmPassword
            )
        )
        return DoorHtml.doorResult(response, listOf("password changed", "account activated"))
    }

    // ----------------------------------------------------------- account (JSON)

    suspend fun profile(): AccountProfile {
        refreshCsrf()
        val root = Backend.get("/api/account").objectOrEmpty()
        return parseProfile(root)
    }

    suspend fun refreshCsrf(): String {
        val response = Backend.formGet("/account")
        if (response.status in 300..399) throw BackendAuthException("Sign in to continue.", "/account", response.status)
        requireOk(response, DoorHtml.resultText(response.body))
        val token = DoorHtml.csrf(response.body)
            ?: throw AccountActionException("Your account page token could not be read. Sign in again.")
        csrfToken = token
        return token
    }

    suspend fun updateEmail(email: String, currentPassword: String, emailCode: String): JsonObject =
        mutate("/api/account/email/change", buildJsonObject {
            put("email", email)
            put("current_password", currentPassword)
            put("email_code", emailCode)
        })

    suspend fun changePassword(currentPassword: String, password: String, confirmPassword: String): JsonObject =
        mutate("/api/account/password", buildJsonObject {
            put("current_password", currentPassword)
            put("password", password)
            put("confirm_password", confirmPassword)
        })

    suspend fun sendPasswordResetCode(): JsonObject = mutate("/api/account/password/reset/send", JsonObject(emptyMap()))

    suspend fun resetPasswordSignedIn(
        code: String,
        password: String,
        confirmPassword: String,
        signOutOthers: Boolean
    ): JsonObject = mutate("/api/account/password/reset", buildJsonObject {
        put("code", code)
        put("password", password)
        put("confirm_password", confirmPassword)
        put("sign_out_others", signOutOthers)
    })

    suspend fun checkAccountCode(purpose: String, code: String): JsonObject =
        mutate("/api/account/code/check", buildJsonObject { put("purpose", purpose); put("code", code) })

    suspend fun checkIdentity(password: String): JsonObject =
        mutate("/api/account/identity/check", buildJsonObject { put("current_password", password) })

    suspend fun sendEmailCode(): JsonObject = mutate("/api/account/email/reauth", JsonObject(emptyMap()))

    suspend fun resendVerificationCode(): JsonObject =
        mutate("/api/account/email/verification", JsonObject(emptyMap()))

    suspend fun confirmVerificationCode(code: String): JsonObject =
        mutate("/api/account/email/verification/confirm", buildJsonObject { put("code", code) })

    suspend fun revokeSessions(sessionId: String = "", all: Boolean = false, others: Boolean = false): JsonObject =
        mutate("/api/account/sessions/revoke", buildJsonObject {
            if (sessionId.isNotBlank()) put("id", sessionId)
            put("all", all)
            put("others", others)
        })

    suspend fun beginTotp(currentPassword: String, emailCode: String = ""): JsonObject =
        mutate("/api/account/totp/begin", buildJsonObject {
            put("current_password", currentPassword)
            put("email_code", emailCode)
        })

    suspend fun confirmTotp(code: String): JsonObject =
        mutate("/api/account/totp/confirm", buildJsonObject { put("code", code) })

    suspend fun cancelTotp(): JsonObject = mutate("/api/account/totp/cancel", JsonObject(emptyMap()))

    suspend fun disableTotp(currentPassword: String, emailCode: String = ""): JsonObject =
        mutate("/api/account/totp/disable", buildJsonObject {
            put("current_password", currentPassword)
            put("email_code", emailCode)
        })

    suspend fun deleteAccount(
        currentPassword: String,
        confirmUsername: String,
        emailCode: String = "",
        verifyOnly: Boolean = false
    ): JsonObject = mutate("/api/account/delete", buildJsonObject {
        put("current_password", currentPassword)
        put("confirm_username", confirmUsername)
        put("email_code", emailCode)
        put("verify_only", verifyOnly)
    })

    suspend fun disconnectProvider(provider: String): JsonObject =
        mutate("/api/account/providers/$provider/disconnect", JsonObject(emptyMap()))

    fun clearSecrets() {
        csrfToken = null
    }

    // ---------------------------------------------------------------- internals

    private suspend fun mutate(path: String, body: JsonObject): JsonObject {
        var token = csrfToken ?: refreshCsrf()
        return try {
            Backend.post(path, body, mapOf(CSRF_HEADER to token)).objectOrEmpty()
        } catch (error: BackendHttpException) {
            if (error.statusCode == 400 && error.isStaleCsrf()) {
                token = refreshCsrf()
                Backend.post(path, body, mapOf(CSRF_HEADER to token)).objectOrEmpty()
            } else {
                throw error.toAccountAction()
            }
        }
    }

    private fun BackendHttpException.isStaleCsrf(): Boolean {
        val root = payload.objectOrEmpty()
        val detail = root["error"]
        val text = when (detail) {
            is JsonObject -> detail.text("code") + " " + detail.text("message")
            is JsonPrimitive -> detail.contentOrNull.orEmpty()
            else -> ""
        }
        return text.contains("csrf", ignoreCase = true) || text.contains("page token is stale", ignoreCase = true)
    }

    private fun BackendHttpException.toAccountAction(): AccountActionException {
        val root = payload.objectOrEmpty()
        val detail = root["error"]
        val fallback = message.orEmpty()
        val message = when (detail) {
            is JsonObject -> detail.text("message").ifBlank { fallback }
            is JsonPrimitive -> detail.contentOrNull.orEmpty().ifBlank { fallback }
            else -> fallback
        }
        val fields = (detail as? JsonObject)?.get("fields").objectOrEmpty().mapNotNull { (key, value) ->
            ((value as? JsonPrimitive)?.contentOrNull ?: (value as? JsonObject)?.text("message"))
                ?.takeIf(String::isNotBlank)?.let { key to it }
        }.toMap()
        val retry = ((detail as? JsonObject)?.number("retry_after") ?: 0L)
        return AccountActionException(message.ifBlank { "Request failed. Please try again." }, fields, retry)
    }

    private fun requireOk(response: FormResponse, message: String) {
        if (response.status !in 200..299) {
            // A 404 here is the server's own "page not found" page, which is not
            // about this app's request: the deployment simply has no multi-user
            // routes. Saying so beats echoing "We couldn't find that page".
            val text = if (response.status == 404) DOORS_MISSING
                else message.ifBlank { "The account server rejected the request (${response.status})." }
            throw BackendAuthException(text, statusCode = response.status)
        }
    }

    private fun parseProfile(root: JsonObject): AccountProfile {
        val user = root["user"].objectOrEmpty()
        val sessions = root.array("sessions").mapNotNull { row ->
            val obj = row as? JsonObject ?: return@mapNotNull null
            AccountSession(
                id = obj.text("id"),
                current = obj.flag("current"),
                device = obj.text("device", "user_agent", "name").ifBlank { "Unknown device" },
                seen = obj.text("seen_text", "last_seen_text"),
                signedIn = obj.text("signed_in_text", "created_text"),
                address = obj.text("ip", "ip_label")
            )
        }
        val providers = root.array("providers").mapNotNull { row ->
            val obj = row as? JsonObject ?: return@mapNotNull null
            val status = obj.text("status")
            LinkedProvider(
                provider = obj.text("provider"),
                connected = obj.flag("connected") || obj.flag("linked") || status == "connected",
                detail = obj.text("detail", "account").ifBlank {
                    if (status == "connected") "" else obj.text("last_error", "status")
                }
            )
        }
        return AccountProfile(
            id = user.text("id"),
            username = user.text("username").ifBlank { root.text("username") },
            email = root.text("email").ifBlank { user.text("email") },
            pendingEmail = root.text("pending_email"),
            emailVerified = root.flag("email_verified") || user.flag("email_verified"),
            verificationPending = root.flag("verification_pending"),
            role = root.text("role").ifBlank { user.text("role") },
            roleText = root.text("role_text"),
            totpEnabled = root.flag("totp_enabled") || user.flag("totp_enabled"),
            totpPending = root.flag("totp_pending") || user.flag("totp_pending"),
            mustChangePassword = root.flag("must_change_password") || user.flag("must_change_password"),
            sessions = sessions,
            providers = providers.filter { it.provider != "amazon" || root.flag("alexa_access_enabled") },
            alexaAccessEnabled = root.flag("alexa_access_enabled"),
            minPasswordLength = root.number("password_min_length").toInt().takeIf { it >= 8 } ?: 10
        )
    }
}

/** Minimal, strict parser for the server-rendered account doors. */
internal object DoorHtml {
    private val csrf = Regex("""(?:name="_csrf"\s+value|data-csrf)="([^"]+)"""")
    private val fieldError = Regex("""<p class="field-error">(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val generalError = Regex("""<p class="form-note error">(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val note = Regex("""<p class="form-note">(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val heading = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val label = Regex("""<label for="([^"]+)"""")
    private val tag = Regex("""<[^>]+>""")
    private val spaces = Regex("""\s+""")

    fun csrf(html: String): String? = csrf.find(html)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)

    fun signupGate(html: String): AccountWebApi.SignupGate? {
        val raw = Regex("""data-signup-step="(email|code|details)"""").find(html)?.groupValues?.get(1)
            ?: Regex("""name="step"\s+value="(email|code|details)"""").find(html)?.groupValues?.get(1)
            ?: return null
        val token = csrf(html) ?: return null
        val step = AccountWebApi.SignupStep.entries.firstOrNull { it.value == raw } ?: return null
        val masked = Regex("""data-masked-email="([^"<]*)"""").find(html)?.groupValues?.get(1)?.let(::clean).orEmpty()
        return AccountWebApi.SignupGate(true, "", token, step, masked)
    }

    fun resultText(html: String): String {
        val h1 = heading.find(html)?.groupValues?.getOrNull(1)?.let(::clean).orEmpty()
        val body = note.findAll(html).map { clean(it.groupValues[1]) }.firstOrNull { it.isNotBlank() }.orEmpty()
        return listOf(h1, body).filter(String::isNotBlank).joinToString(". ")
    }

    fun doorResult(response: FormResponse, successMarkers: List<String>): AccountWebApi.DoorResult {
        val redirected = response.status in 300..399
        val errors = fieldErrors(response.body)
        val general = generalError.find(response.body)?.groupValues?.getOrNull(1)?.let(::clean).orEmpty()
        val text = resultText(response.body)
        val marked = successMarkers.any { response.body.contains(it, ignoreCase = true) }
        val ok = redirected || (response.status in 200..299 && errors.isEmpty() && general.isBlank() && marked)
        val message = general.ifBlank { errors.values.firstOrNull().orEmpty() }
            .ifBlank { if (ok) "" else text }
            .ifBlank { if (response.status in 200..299) "Done." else "Request failed (${response.status})." }
        return AccountWebApi.DoorResult(ok, message, errors, if (ok) text else "")
    }

    private fun fieldErrors(html: String): Map<String, String> {
        val labels = label.findAll(html).toList()
        if (labels.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, String>()
        labels.forEachIndexed { index, match ->
            val start = match.range.last + 1
            val end = labels.getOrNull(index + 1)?.range?.first ?: html.length
            val block = html.substring(start.coerceAtMost(html.length), end.coerceAtMost(html.length))
            val error = fieldError.find(block)?.groupValues?.getOrNull(1)?.let(::clean).orEmpty()
            if (error.isNotBlank()) out[match.groupValues[1]] = error
        }
        return out
    }

    private fun clean(raw: String): String = raw
        .replace(tag, " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace(spaces, " ")
        .trim()
}
