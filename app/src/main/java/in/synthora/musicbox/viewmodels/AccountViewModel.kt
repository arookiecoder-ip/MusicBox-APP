package `in`.synthora.musicbox.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.synthora.musicbox.network.Backend
import `in`.synthora.musicbox.network.AccountWebApi
import `in`.synthora.musicbox.network.DownloadCookies
import `in`.synthora.musicbox.network.BackendAuthException
import `in`.synthora.musicbox.network.LoginStep
import `in`.synthora.musicbox.services.AccountScope
import `in`.synthora.musicbox.network.flag
import `in`.synthora.musicbox.network.objectOrEmpty
import `in`.synthora.musicbox.network.text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class AuthStage {
    CHECKING,
    SIGNED_OUT,
    SIGNUP,
    VERIFY_EMAIL,
    FORGOT_PASSWORD,
    RESET_PASSWORD,
    NEEDS_CODE,
    SIGNED_IN
}

/** Values a door needs while it is open. Passwords never enter this state. */
data class AuthDoorState(
    val csrf: String = "",
    val username: String = "",
    val email: String = "",
    val maskedEmail: String = "",
    val message: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val minPasswordLength: Int = 10,
    val signupOpen: Boolean = true,
    val signupStep: AccountWebApi.SignupStep = AccountWebApi.SignupStep.EMAIL,
    val resendAtMillis: Long = 0
)

data class TotpEnrollment(
    val secret: String,
    val account: String,
    val issuer: String,
    val message: String
) {
    val otpauthUri: String
        get() = "otpauth://totp/${java.net.URLEncoder.encode(issuer, "UTF-8")}:${java.net.URLEncoder.encode(account, "UTF-8")}" +
            "?secret=$secret&issuer=${java.net.URLEncoder.encode(issuer, "UTF-8")}&algorithm=SHA1&digits=6&period=30"
}

/** The server's view of the linked accounts (GET /api/profile_status/). */
data class AccountStatus(
    val amazonConnected: Boolean = false,
    val alexaAccessEnabled: Boolean = false,
    /** YouTube Music browser headers work: personalized Home, library, likes. */
    val youtubeLibrary: Boolean = false,
    /** An actual audio sample download passed on the configured audio server. */
    val youtubeCookies: Boolean = false,
    val youtubeCookiesPresent: Boolean = false,
    val youtubeBrowserAvailable: Boolean = false,
    val youtubeReconnectRequired: Boolean = false
) {
    val allConnected get() = (!alexaAccessEnabled || amazonConnected) && youtubeLibrary
}

data class AccountUiState(
    val stage: AuthStage = AuthStage.CHECKING,
    /** Whether this server has account pages; null while unknown or offline. */
    val doorsAvailable: Boolean? = null,
    val door: AuthDoorState = AuthDoorState(),
    val profile: AccountWebApi.AccountProfile? = null,
    val profileLoading: Boolean = false,
    val actionMessage: String? = null,
    /** Which account action produced [actionMessage], so a sheet can close itself on success. */
    val actionKey: String? = null,
    val totp: TotpEnrollment? = null,
    val busy: Boolean = false,
    val codeExpiresAt: Long = 0,
    val codeResendAt: Long = 0,
    val cookieBusy: Boolean = false,
    val cookieMessage: String? = null,
    val cookieSaved: Long = 0,
    val error: String? = null,
    /** Shown once after signing in, while the accounts are checked. */
    val showAccountCheck: Boolean = false,
    val status: AccountStatus? = null,
    val checkingStatus: Boolean = false,
    val amazonMessage: String? = null,
    val youtubeMessage: String? = null,
    /** A vendor sign-in page to open in the account browser. */
    val browserUrl: String? = null
)

const val ACTION_PASSWORD = "password"
const val ACTION_EMAIL = "email"
const val ACTION_VERIFY = "verify"
const val ACTION_TOTP_ON = "totp_on"
const val ACTION_TOTP_OFF = "totp_off"

/**
 * Web-remote sign-in (username, password, authenticator code) and the account checks the web
 * app runs after it: Amazon for the Echo, YouTube Music for personalized music and playback.
 */
class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    /** Fires when Amazon or YouTube was (re)connected, so Home, library and Echo reload. */
    private val _accountsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val accountsChanged: SharedFlow<Unit> = _accountsChanged.asSharedFlow()

    /** Another account signed in on this phone; cached queues and metadata were fenced. */
    private val _accountSwitched = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val accountSwitched: SharedFlow<Unit> = _accountSwitched.asSharedFlow()

    private var codeToken = ""
    private var busyJob: Job? = null
    private var statusJob: Job? = null
    private var doorsJob: Job? = null
    private var accountPoll: Job? = null
    private val downloadCookies = DownloadCookies()
    private var cookieJob: Job? = null
    private var browserFlow: String? = null

    init {
        viewModelScope.launch {
            `in`.synthora.musicbox.services.AmazonAccess.enabled.collect { enabled ->
                if (!enabled && browserFlow == "amazon") {
                    accountPoll?.cancel()
                    browserFlow = null
                }
                _state.update { it.copy(
                    status = it.status?.copy(alexaAccessEnabled = enabled, amazonConnected = enabled && it.status.amazonConnected),
                    profile = it.profile?.copy(alexaAccessEnabled = enabled, providers = it.profile.providers.filter { row -> row.provider != "amazon" || enabled }),
                    browserUrl = if (!enabled && it.browserUrl?.contains("/accounts/amazon") == true) null else it.browserUrl,
                    amazonMessage = if (enabled) it.amazonMessage else null
                ) }
            }
        }
        checkSession()
    }

    private fun checkSession() {
        if (!Backend.hasSession()) {
            _state.value = AccountUiState(stage = AuthStage.SIGNED_OUT)
            checkDoors()
            return
        }
        // A saved session can open cached screens while validation runs in the background.
        _state.update { it.copy(stage = AuthStage.SIGNED_IN) }
        viewModelScope.launch {
            val stage = try {
                Backend.get("/api/account")
                AuthStage.SIGNED_IN
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendAuthException) {
                Backend.clearSession()
                AuthStage.SIGNED_OUT
            } catch (e: Exception) {
                // Offline or server unreachable: keep the saved session and let screens retry.
                AuthStage.SIGNED_IN
            }
            _state.update { it.copy(stage = stage) }
            if (stage == AuthStage.SIGNED_IN) {
                refreshStatus()
                refreshProfile()
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isEmpty()) {
            _state.update { it.copy(error = "Enter your username and password") }
            return
        }
        runBusy {
            when (val step = Backend.login(username.trim(), password)) {
                is LoginStep.NeedsCode -> {
                    codeToken = step.token
                    _state.update { it.copy(stage = AuthStage.NEEDS_CODE) }
                }
                LoginStep.SignedIn -> onSignedIn()
            }
        }
    }

    fun verifyCode(code: String) {
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) {
            _state.update { it.copy(error = "Enter the 6-digit code from your authenticator app") }
            return
        }
        runBusy {
            try {
                Backend.verifyCode(codeToken, digits)
            } catch (e: BackendAuthException) {
                // An expired login must start over; a wrong code can be retried.
                if (e.message?.contains("timed out", ignoreCase = true) == true) {
                    codeToken = ""
                    _state.update { it.copy(stage = AuthStage.SIGNED_OUT) }
                }
                throw e
            }
            codeToken = ""
            onSignedIn()
        }
    }

    /** Back from the code step to username and password. */
    fun restartLogin() {
        codeToken = ""
        _state.update { it.copy(stage = AuthStage.SIGNED_OUT, error = null) }
    }

    /**
     * Ask once whether this deployment serves the account pages. A server that
     * answers 404 for /signup has no multi-user layer, so the sign-in screen
     * says so instead of offering buttons that lead to a "page not found" page.
     */
    fun checkDoors() {
        if (doorsJob?.isActive == true) return
        doorsJob = viewModelScope.launch {
            val available = AccountWebApi.doorsAvailable() ?: return@launch
            if (_state.value.stage != AuthStage.SIGNED_IN) {
                _state.update { it.copy(doorsAvailable = available) }
            }
        }
    }

    private fun onSignedIn() {
        _state.update { it.copy(stage = AuthStage.SIGNED_IN, showAccountCheck = true, status = null, error = null, door = AuthDoorState()) }
        refreshStatus()
        refreshProfile()
    }

    /** Leave the account check and open the app. */
    fun continueToApp() {
        _state.update { it.copy(showAccountCheck = false) }
    }

    /** Check Amazon and YouTube on the server (the YouTube cookie check takes a few seconds). */
    fun refreshStatus(checkAudio: Boolean = false) {
        if (cookieJob?.isActive == true) return
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            _state.update { it.copy(checkingStatus = true) }
            try {
                val profile = Backend.get("/api/profile_status/", mapOf("audio_check" to "0")).objectOrEmpty()
                val status = AccountStatus(
                    amazonConnected = profile.flag("alexa_access_enabled") && profile.flag("amazon_connected"),
                    alexaAccessEnabled = profile.flag("alexa_access_enabled"),
                    youtubeLibrary = profile.flag("youtube_own_session"),
                    youtubeCookies = _state.value.status?.youtubeCookies ?: false,
                    youtubeCookiesPresent = profile.flag("youtube_cookies_present"),
                    youtubeBrowserAvailable = profile.flag("youtube_browser_available"),
                    youtubeReconnectRequired = profile.flag("youtube_browser_reconnect_required")
                )
                `in`.synthora.musicbox.services.AmazonAccess.update(status.alexaAccessEnabled)
                _state.update { it.copy(status = status) }
                if (profile.flag("youtube_auth_working") && profile.flag("youtube_own_session")) {
                    viewModelScope.launch {
                        try { `in`.synthora.musicbox.services.LocalListeningHistory.sync(getApplication()) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { /* Pending history stays encrypted locally for retry. */ }
                    }
                }
                if (checkAudio) {
                    val result = downloadCookies.check()
                    _state.update { it.copy(status = (it.status ?: AccountStatus()).copy(youtubeCookies = result.valid), cookieMessage = result.message) }
                }
                // Audio probes remain explicit, never on startup.
                // Everything is connected: carry on into the app without a tap.
                if (_state.value.status?.allConnected == true && _state.value.showAccountCheck) {
                    delay(1_200)
                    _state.update { it.copy(showAccountCheck = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendAuthException) {
                sessionEnded()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Couldn't check your accounts: ${`in`.synthora.musicbox.network.networkErrorMessage(e) ?: e.message}") }
            } finally {
                _state.update { it.copy(checkingStatus = false) }
            }
        }
    }

    fun testDownloadCookies() = cookieAction {
        val result = downloadCookies.check()
        _state.update { it.copy(status = (it.status ?: AccountStatus()).copy(youtubeCookies = result.valid), cookieMessage = result.message) }
    }

    fun replaceDownloadCookies(export: String) = cookieAction {
        val result = downloadCookies.replace(export)
        _state.update { it.copy(status = (it.status ?: AccountStatus()).copy(youtubeCookies = result.valid, youtubeCookiesPresent = true),
            cookieMessage = "Audio download passed. New cookies saved.", cookieSaved = it.cookieSaved + 1) }
    }

    private fun cookieAction(block: suspend () -> Unit) {
        if (cookieJob?.isActive == true) return
        // An explicit cookie action takes precedence over the background account check.
        statusJob?.cancel()
        _state.update { it.copy(checkingStatus = false) }
        cookieJob = viewModelScope.launch {
            _state.update { it.copy(cookieBusy = true, cookieMessage = "Downloading an audio sample…") }
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(cookieMessage = e.message ?: "Cookie test failed. Existing cookies were kept.") } }
            finally { _state.update { it.copy(cookieBusy = false) } }
        }
    }

    // ---------- Amazon (Echo control) ----------

    fun connectAmazon(email: String = "", password: String = "") {
        if (_state.value.status?.alexaAccessEnabled != true) return
        accountPoll?.cancel()
        browserFlow = "amazon"
        _state.update { it.copy(browserUrl = absolute("/accounts/amazon"), amazonMessage = null) }
    }

    fun disconnectAmazon() {
        if (_state.value.status?.alexaAccessEnabled != true) return
        runBusy {
            Backend.post("/alexa/amazon_signout/")
            _accountsChanged.tryEmit(Unit)
            refreshStatus()
        }
    }

    // ---------- YouTube Music ----------

    fun connectYouTube(retry: Boolean = false) {
        accountPoll?.cancel()
        browserFlow = "youtube"
        _state.update { it.copy(browserUrl = absolute("/accounts/youtube"), youtubeMessage = null) }
    }

    fun browserClosed() {
        val flow = browserFlow
        browserFlow = null
        accountPoll?.cancel()
        _state.update { it.copy(browserUrl = null) }
        viewModelScope.launch {
            if (flow == "youtube") runCatching { Backend.post("/api/youtube/browser-session/stop") }
            _accountsChanged.tryEmit(Unit)
            refreshStatus()
        }
    }

    /** Poll the sign-in like the web app: Amazon every 2.5 s, YouTube every 1.5 s, up to 120 times. */
    private fun pollAccount(flow: String) {
        accountPoll?.cancel()
        accountPoll = viewModelScope.launch {
            repeat(120) {
                delay(if (flow == "amazon") 2_500 else 1_500)
                if (!isActive) return@launch
                try {
                    if (flow == "amazon") {
                        val result = Backend.get("/alexa/proxy_check/").objectOrEmpty()
                        if (result.flag("logged_in")) {
                            finishAccountFlow(amazon = "Amazon connected")
                            return@launch
                        }
                        val error = result.text("error")
                        if (error.isNotBlank()) {
                            browserFlow = null
                            _state.update { it.copy(amazonMessage = error, browserUrl = null) }
                            return@launch
                        }
                    } else {
                        val result = Backend.get("/api/youtube/browser-session/status").objectOrEmpty()
                        val state = result.text("state")
                        _state.update { it.copy(youtubeMessage = result.text("message").ifBlank { youtubeLabel(state) }) }
                        when (state) {
                            "connected" -> {
                                finishAccountFlow(youtube = "YouTube Music connected")
                                return@launch
                            }
                            "reconnect_required", "unavailable", "idle" -> {
                                browserFlow = null
                                return@launch
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: BackendAuthException) {
                    sessionEnded()
                    return@launch
                } catch (_: Exception) {
                    // Keep polling through a dropped request.
                }
            }
            browserFlow = null
            _state.update { it.copy(browserUrl = null, amazonMessage = null, youtubeMessage = "Sign-in timed out. Try again.") }
        }
    }

    private fun finishAccountFlow(amazon: String? = null, youtube: String? = null) {
        browserFlow = null
        _state.update {
            it.copy(
                browserUrl = null,
                amazonMessage = amazon ?: it.amazonMessage,
                youtubeMessage = youtube ?: it.youtubeMessage
            )
        }
        _accountsChanged.tryEmit(Unit)
        refreshStatus()
    }

    private fun youtubeLabel(state: String): String = when (state) {
        "connecting" -> "Starting the secure browser…"
        "waiting_for_login" -> "Sign in to YouTube Music in the browser…"
        "captured", "capture_requested" -> "Signed in. Saving…"
        "settling_session" -> "Saving the signed-in browser profile…"
        "validating", "validating_login" -> "Checking your YouTube Music account…"
        "refreshing" -> "Refreshing the saved session…"
        "connected" -> "Connected"
        "reconnect_required" -> "Google needs you to sign in again or finish a challenge."
        "unavailable" -> "The sign-in browser is unavailable on the server."
        else -> ""
    }

    fun signOut() {
        `in`.synthora.musicbox.services.AmazonAccess.update(false)
        busyJob?.cancel()
        statusJob?.cancel()
        cookieJob?.cancel()
        accountPoll?.cancel()
        codeToken = ""
        browserFlow = null
        AccountWebApi.clearSecrets()
        _state.value = AccountUiState(stage = AuthStage.SIGNED_OUT, busy = true)
        checkDoors()
        busyJob = viewModelScope.launch {
            try { Backend.logout() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Local session is cleared even if the server is offline. */ }
            finally {
                AccountScope.forget(getApplication())
                _state.update { it.copy(busy = false, profile = null) }
            }
        }
    }

    /** The server no longer accepts the session (expired, revoked or signed out elsewhere). */
    fun sessionEnded() {
        `in`.synthora.musicbox.services.AmazonAccess.update(false)
        if (_state.value.stage == AuthStage.SIGNED_OUT) return
        Backend.clearSession()
        AccountWebApi.clearSecrets()
        cookieJob?.cancel()
        busyJob?.cancel()
        codeToken = ""
        browserFlow = null
        accountPoll?.cancel()
        statusJob?.cancel()
        _state.value = AccountUiState(stage = AuthStage.SIGNED_OUT, error = "Your session ended. Sign in again.")
        checkDoors()
        viewModelScope.launch { AccountScope.forget(getApplication()) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun dismissActionMessage() {
        _state.update { it.copy(actionMessage = null, actionKey = null) }
    }

    // ------------------------------------------------------------ sign-up / recovery

    private var signupOpenedAt = 0L

    fun openSignup() = runBusy {
        val gate = AccountWebApi.openSignup()
        if (!gate.open) {
            _state.update { it.copy(error = gate.message) }
            return@runBusy
        }
        signupOpenedAt = android.os.SystemClock.elapsedRealtime()
        _state.update {
            it.copy(
                stage = AuthStage.SIGNUP,
                door = AuthDoorState(csrf = gate.csrf, signupOpen = true, signupStep = gate.step, maskedEmail = gate.maskedEmail),
                error = null
            )
        }
    }

    private fun applySignupReply(reply: AccountWebApi.SignupReply, email: String? = null, username: String? = null, sentCode: Boolean = false) {
        val gate = reply.gate
        if (gate == null && reply.result.ok) {
            onSignedIn()
            return
        }
        if (gate?.step == AccountWebApi.SignupStep.EMAIL) signupOpenedAt = android.os.SystemClock.elapsedRealtime()
        _state.update { current ->
            val expired = gate?.step == AccountWebApi.SignupStep.EMAIL && current.door.signupStep != gate.step
            current.copy(door = current.door.copy(
                csrf = gate?.csrf ?: current.door.csrf,
                signupStep = gate?.step ?: current.door.signupStep,
                maskedEmail = gate?.maskedEmail.orEmpty(),
                email = email ?: current.door.email,
                username = username ?: current.door.username,
                resendAtMillis = if (sentCode && reply.result.ok) System.currentTimeMillis() + 60_000 else current.door.resendAtMillis,
                message = if (expired) "Your verification expired. Request a new code to continue." else if (reply.result.ok) "" else reply.result.message,
                fieldErrors = reply.result.fieldErrors
            ), error = null)
        }
    }

    fun submitSignupEmail(email: String) {
        val clean = email.trim()
        if (!clean.contains('@') || clean.length < 5 || clean.length > 254) {
            _state.update { it.copy(door = it.door.copy(fieldErrors = mapOf("email" to "Enter a valid email address."))) }
            return
        }
        runBusy {
            val elapsed = android.os.SystemClock.elapsedRealtime() - signupOpenedAt
            if (elapsed in 0 until 3_200) delay(3_200 - elapsed)
            applySignupReply(AccountWebApi.beginSignup(_state.value.door.csrf, clean), email = clean, sentCode = true)
        }
    }

    fun submitSignupCode(code: String) {
        val digits = code.filter(Char::isDigit)
        if (digits.length != 6) {
            _state.update { it.copy(door = it.door.copy(fieldErrors = mapOf("code" to "Enter the 6-digit code from your email."))) }
            return
        }
        runBusy { applySignupReply(AccountWebApi.verifySignup(_state.value.door.csrf, digits)) }
    }

    fun resendSignupCode() = runBusy {
        if (System.currentTimeMillis() < _state.value.door.resendAtMillis) return@runBusy
        applySignupReply(AccountWebApi.resendSignup(_state.value.door.csrf), sentCode = true)
    }

    fun restartSignup() = runBusy {
        applySignupReply(AccountWebApi.restartSignup(_state.value.door.csrf), email = "", username = "")
    }

    fun submitSignup(username: String, email: String, password: String, confirmPassword: String) {
        if (_state.value.door.signupStep != AccountWebApi.SignupStep.DETAILS) {
            _state.update { it.copy(door = it.door.copy(message = "Verify your email before creating the account.")) }
            return
        }
        val cleanUser = username.trim()
        val errors = buildMap {
            if (!cleanUser.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{2,31}"))) {
                put("username", "Use 3–32 letters, numbers, dots, dashes or underscores.")
            }
            if (password.length < 10) put("password", "Use at least 10 characters.")
            if (password != confirmPassword) put("confirm_password", "The two passwords do not match.")
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(door = it.door.copy(fieldErrors = errors), error = null) }
            return
        }
        runBusy {
            applySignupReply(AccountWebApi.signup(_state.value.door.csrf, cleanUser, email, password, confirmPassword), username = cleanUser)
        }
    }

    fun submitVerification(code: String) {
        val digits = code.filter(Char::isDigit)
        if (digits.length != 6) {
            _state.update { it.copy(door = it.door.copy(fieldErrors = mapOf("code" to "Enter the 6-digit code from your email."))) }
            return
        }
        runBusy {
            val csrf = _state.value.door.csrf.ifBlank { AccountWebApi.openVerification() }
            val result = AccountWebApi.verifyEmail(csrf, _state.value.door.username, digits)
            if (!result.ok) {
                _state.update { it.copy(door = it.door.copy(csrf = csrf, message = result.message, fieldErrors = result.fieldErrors)) }
                return@runBusy
            }
            _state.update {
                it.copy(
                    stage = AuthStage.SIGNED_OUT,
                    door = AuthDoorState(message = result.notice.ifBlank { "Email confirmed. Sign in." }),
                    error = null
                )
            }
        }
    }

    fun resendVerification() = runBusy {
        val identifier = _state.value.door.username
        if (identifier.isBlank()) {
            _state.update { it.copy(error = "Enter the username you signed up with.") }
            return@runBusy
        }
        val result = AccountWebApi.resendVerification(identifier)
        _state.update { it.copy(door = it.door.copy(message = result.notice.ifBlank { result.message })) }
    }

    fun openForgotPassword() = runBusy {
        val csrf = AccountWebApi.openRecovery()
        _state.update {
            it.copy(stage = AuthStage.FORGOT_PASSWORD, door = AuthDoorState(csrf = csrf), error = null)
        }
    }

    fun submitForgotPassword(identifier: String) = runBusy {
        val clean = identifier.trim()
        if (clean.isBlank()) {
            _state.update { it.copy(door = it.door.copy(fieldErrors = mapOf("identifier" to "Enter your email or username."))) }
            return@runBusy
        }
        val result = AccountWebApi.requestPasswordReset(_state.value.door.csrf, clean)
        if (!result.ok) {
            _state.update { it.copy(door = it.door.copy(message = result.message, fieldErrors = result.fieldErrors)) }
            return@runBusy
        }
        val resetCsrf = runCatching { AccountWebApi.openReset() }.getOrDefault("")
        _state.update {
            it.copy(
                stage = AuthStage.RESET_PASSWORD,
                door = AuthDoorState(csrf = resetCsrf, username = clean, message = result.notice.ifBlank { result.message }),
                error = null
            )
        }
    }

    fun openResetPassword() = runBusy {
        val csrf = AccountWebApi.openReset()
        _state.update { it.copy(stage = AuthStage.RESET_PASSWORD, door = AuthDoorState(csrf = csrf), error = null) }
    }

    fun verifyResetCode(identifier: String, code: String, onVerified: () -> Unit) = runBusy {
        AccountWebApi.checkResetCode(_state.value.door.csrf, identifier.trim(), code)
        _state.update { it.copy(door = it.door.copy(username = identifier.trim(), message = "", fieldErrors = emptyMap())) }
        onVerified()
    }

    fun submitResetPassword(code: String, password: String, confirmPassword: String) = runBusy {
        val identifier = _state.value.door.username
        val errors = buildMap {
            if (identifier.isBlank()) put("identifier", "Enter the account you requested a code for.")
            if (code.filter(Char::isDigit).length != 6) put("code", "Enter the 6-digit code from your email.")
            if (password.length < 10) put("password", "Use at least 10 characters.")
            if (password != confirmPassword) put("confirm_password", "The two passwords do not match.")
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(door = it.door.copy(fieldErrors = errors)) }
            return@runBusy
        }
        val result = AccountWebApi.resetPassword(
            _state.value.door.csrf, identifier, code.filter(Char::isDigit), password, confirmPassword
        )
        if (!result.ok) {
            _state.update { it.copy(door = it.door.copy(message = result.message, fieldErrors = result.fieldErrors)) }
            return@runBusy
        }
        _state.update {
            it.copy(
                stage = AuthStage.SIGNED_OUT,
                door = AuthDoorState(message = result.notice.ifBlank { "Password changed. Sign in with the new one." }),
                error = null
            )
        }
    }

    fun backToSignIn() {
        _state.update { it.copy(stage = AuthStage.SIGNED_OUT, door = AuthDoorState(), error = null) }
    }

    // ------------------------------------------------------------- account control

    fun refreshProfile() {
        if (_state.value.stage != AuthStage.SIGNED_IN) return
        viewModelScope.launch {
            _state.update { it.copy(profileLoading = true) }
            try {
                val profile = AccountWebApi.profile()
                val switched = AccountScope.adopt(getApplication(), profile.id, profile.username, profile.email)
                `in`.synthora.musicbox.services.AmazonAccess.update(profile.alexaAccessEnabled)
                _state.update { it.copy(profile = profile) }
                if (switched) _accountSwitched.tryEmit(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendAuthException) {
                sessionEnded()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Couldn't load your account details.") }
            } finally {
                _state.update { it.copy(profileLoading = false) }
            }
        }
    }

    private fun accountAction(success: String = "Saved.", key: String? = null, block: suspend () -> JsonObject) {
        if (busyJob?.isActive == true) return
        busyJob = viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, actionMessage = null, actionKey = null) }
            try {
                val result = block()
                _state.update {
                    it.copy(actionMessage = result.text("message").ifBlank { success }, actionKey = key)
                }
                refreshProfile()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendAuthException) {
                sessionEnded()
            } catch (e: AccountWebApi.AccountActionException) {
                _state.update { it.copy(error = e.message,
                    codeResendAt = if (e.retryAfterSeconds > 0) System.currentTimeMillis() + e.retryAfterSeconds * 1000 else it.codeResendAt) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Couldn't update your account.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) =
        accountAction("Password changed.", key = ACTION_PASSWORD) {
            AccountWebApi.changePassword(currentPassword, newPassword, confirmPassword)
        }

    private fun recordCodeTiming(result: JsonObject): JsonObject {
        _state.update { it.copy(
            codeExpiresAt = ((result.text("expires_at").toDoubleOrNull() ?: 0.0) * 1000).toLong(),
            codeResendAt = ((result.text("resend_at").toDoubleOrNull() ?: 0.0) * 1000).toLong()
        ) }
        return result
    }

    fun verifySecurityCode(purpose: String, code: String, onVerified: () -> Unit) = accountAction("Code verified.") {
        AccountWebApi.checkAccountCode(purpose, code).also { onVerified() }
    }

    fun verifyIdentity(password: String, onVerified: () -> Unit) = accountAction("Identity verified.") {
        AccountWebApi.checkIdentity(password).also { onVerified() }
    }

    fun sendPasswordResetCode() = accountAction("Reset code sent.") {
        recordCodeTiming(AccountWebApi.sendPasswordResetCode())
    }

    fun resetPasswordWithCode(code: String, newPassword: String, confirmPassword: String, signOutOthers: Boolean = true) =
        accountAction("Password changed.", key = ACTION_PASSWORD) {
            AccountWebApi.resetPasswordSignedIn(code, newPassword, confirmPassword, signOutOthers)
        }

    fun sendEmailCode() = accountAction("Verification code sent.") {
        recordCodeTiming(AccountWebApi.sendEmailCode())
    }

    fun changeEmail(email: String, currentPassword: String, emailCode: String) =
        accountAction("Check your new address for a confirmation code.", key = ACTION_EMAIL) {
            recordCodeTiming(AccountWebApi.updateEmail(email, currentPassword, emailCode))
        }

    fun resendEmailVerification() = accountAction("Verification code sent.") {
        recordCodeTiming(AccountWebApi.resendVerificationCode())
    }

    fun confirmEmail(code: String) = accountAction("Email confirmed.", key = ACTION_VERIFY) {
        AccountWebApi.confirmVerificationCode(code).also { result ->
            if (result.text("sign_in_required") == "true") {
                Backend.clearSession()
                AccountWebApi.clearSecrets()
                accountPoll?.cancel()
                statusJob?.cancel()
                `in`.synthora.musicbox.services.AmazonAccess.update(false)
                AccountScope.forget(getApplication())
                _state.update { it.copy(stage = AuthStage.SIGNED_OUT, profile = null, error = null,
                    door = AuthDoorState(message = "Your email was updated. Sign in with your new address.")) }
            }
        }
    }

    fun revokeSession(sessionId: String) = accountAction("Device signed out.") {
        AccountWebApi.revokeSessions(sessionId = sessionId)
    }

    fun signOutOtherSessions() = accountAction("Other devices signed out.") {
        AccountWebApi.revokeSessions(others = true)
    }

    fun beginTotp(currentPassword: String, emailCode: String = "") = accountAction("Two-factor setup started.") {
        val result = AccountWebApi.beginTotp(currentPassword, emailCode)
        _state.update {
            it.copy(
                totp = TotpEnrollment(
                    secret = result.text("secret"),
                    account = result.text("account"),
                    issuer = result.text("issuer").ifBlank { "Music Box" },
                    message = result.text("message")
                )
            )
        }
        result
    }

    fun cancelTotp() = accountAction("Two-factor setup cancelled.") {
        _state.update { it.copy(totp = null) }
        AccountWebApi.cancelTotp()
    }

    fun confirmTotp(code: String) = accountAction("Two-factor is on.", key = ACTION_TOTP_ON) {
        val result = AccountWebApi.confirmTotp(code)
        _state.update { it.copy(totp = null) }
        result
    }

    fun disableTotp(currentPassword: String, emailCode: String = "") =
        accountAction("Two-factor is off.", key = ACTION_TOTP_OFF) {
            _state.update { it.copy(totp = null) }
            AccountWebApi.disableTotp(currentPassword, emailCode)
        }

    fun disconnectProvider(provider: String) = accountAction("Disconnected.") {
        val result = AccountWebApi.disconnectProvider(provider)
        _accountsChanged.tryEmit(Unit)
        result
    }

    fun deleteAccount(currentPassword: String, username: String, emailCode: String) {
        if (busyJob?.isActive == true) return
        busyJob = viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                AccountWebApi.deleteAccount(currentPassword, username, emailCode)
                Backend.clearSession()
                AccountWebApi.clearSecrets()
                AccountScope.forget(getApplication())
                _state.value = AccountUiState(
                    stage = AuthStage.SIGNED_OUT,
                    door = AuthDoorState(message = "Your account and its data were deleted.")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendAuthException) {
                sessionEnded()
            } catch (e: AccountWebApi.AccountActionException) {
                _state.update { it.copy(error = e.message,
                    codeResendAt = if (e.retryAfterSeconds > 0) System.currentTimeMillis() + e.retryAfterSeconds * 1000 else it.codeResendAt) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Couldn't delete the account.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun absolute(url: String) = if (url.startsWith("/")) Backend.baseUrl + url else url

    private fun runBusy(block: suspend () -> Unit) {
        if (busyJob?.isActive == true) return
        busyJob = viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendAuthException) {
                val stage = _state.value.stage
                if (stage == AuthStage.SIGNED_IN) sessionEnded()
                else _state.update { it.copy(error = `in`.synthora.musicbox.network.networkErrorMessage(e) ?: e.message ?: "Sign-in failed") }
            } catch (e: Exception) {
                _state.update { it.copy(error = `in`.synthora.musicbox.network.networkErrorMessage(e) ?: e.message ?: "Something went wrong") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
