package `in`.synthora.musicbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.network.AccountWebApi
import `in`.synthora.musicbox.ui.components.GlassAlertDialog
import `in`.synthora.musicbox.ui.components.stableNavigationBarsPadding
import `in`.synthora.musicbox.ui.components.stableStatusBarsPadding
import `in`.synthora.musicbox.ui.theme.GlassBackdrop
import `in`.synthora.musicbox.ui.theme.GlassLevel
import `in`.synthora.musicbox.ui.theme.glassPane
import `in`.synthora.musicbox.ui.theme.isGlassDark
import `in`.synthora.musicbox.utils.rememberJukeHaptics
import `in`.synthora.musicbox.viewmodels.ACTION_EMAIL
import `in`.synthora.musicbox.viewmodels.ACTION_PASSWORD
import `in`.synthora.musicbox.viewmodels.ACTION_TOTP_OFF
import `in`.synthora.musicbox.viewmodels.ACTION_TOTP_ON
import `in`.synthora.musicbox.viewmodels.ACTION_VERIFY
import `in`.synthora.musicbox.viewmodels.AccountUiState
import `in`.synthora.musicbox.viewmodels.AccountViewModel

/**
 * Profile, mailbox proof, password, two-factor, devices and deletion.
 * Every destructive or credential-changing action asks for the current password
 * (and an emailed code where the server requires one) in a single focused sheet.
 */
@Composable
fun AccountSettingsScreen(
    account: AccountViewModel,
    state: AccountUiState,
    onNavigateBack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    BackHandler(onBack = onNavigateBack)
    val profile = state.profile
    val haptic = rememberJukeHaptics()
    var dialog by remember { mutableStateOf<AccountDialog?>(null) }
    LaunchedEffect(state.actionKey, state.actionMessage) {
        if (state.actionKey == ACTION_EMAIL && state.actionMessage != null) dialog = AccountDialog.VERIFY_EMAIL
    }
    LaunchedEffect(state.totp) {
        if (state.totp != null) dialog = AccountDialog.TOTP_CONFIRM
    }
    // Opening a sheet starts a fresh action: drop the note left by the previous one.
    LaunchedEffect(dialog) {
        if (dialog != null) account.dismissActionMessage()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(GlassBackdrop.color(isGlassDark()))
            .stableStatusBarsPadding()
            .stableNavigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.profileLoading) CircularProgressIndicator()
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Account details are unavailable.", textAlign = TextAlign.Center)
                    TextButton(onClick = account::refreshProfile) { Text("Try again") }
                }
            }
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomPadding + 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(56.dp).glassPane(RoundedCornerShape(8.dp), GlassLevel.Thick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profile.username.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(profile.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (profile.emailVerified) profile.email else "Email not confirmed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.roleText.isNotBlank()) {
                        Text(profile.roleText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            state.actionMessage?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
            state.error?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            if (!profile.emailVerified || profile.verificationPending || profile.pendingEmail.isNotBlank()) {
                AccountSection("Confirm your email") {
                    Text(
                        if (profile.pendingEmail.isNotBlank()) "Waiting to confirm ${profile.pendingEmail}."
                        else "We need to prove this address before password recovery can work.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        TextButton(onClick = { dialog = AccountDialog.VERIFY_EMAIL }) { Text("Enter code") }
                        TextButton(
                            onClick = { haptic.click(); account.resendEmailVerification() },
                            enabled = !state.busy
                        ) { Text("Send a new code") }
                    }
                }
            }

            AccountSection("Email and password") {
                AccountActionRow("Email", profile.email.ifBlank { "Not set" }, "Change") {
                    dialog = AccountDialog.CHANGE_EMAIL
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                AccountActionRow("Password", "Verify your identity to choose a new password", "Change") {
                    dialog = AccountDialog.CHANGE_PASSWORD
                }
                Text(
                    "Security changes can sign out other devices. This device stays signed in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            AccountSection("Two-factor authentication") {
                Text(
                    if (profile.totpEnabled) "On — this account asks for a 6-digit authenticator code at sign-in."
                    else "Add an authenticator app so a stolen password is not enough.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (profile.totpPending) {
                    Text("A new authenticator is waiting to be confirmed.", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
                if (profile.totpEnabled) {
                    TextButton(onClick = { dialog = AccountDialog.TOTP_BEGIN }, enabled = !state.busy) { Text("Change authenticator") }
                }
                TextButton(
                    onClick = {
                        if (state.totp != null) dialog = AccountDialog.TOTP_CONFIRM
                        else if (profile.totpEnabled) dialog = AccountDialog.TOTP_DISABLE
                        else dialog = AccountDialog.TOTP_BEGIN
                    },
                    enabled = !state.busy,
                    modifier = Modifier.padding(top = 4.dp)
                ) { Text(if (profile.totpEnabled) "Turn off two-factor" else "Set up two-factor") }
            }

            AccountSection("Signed-in devices") {
                if (profile.sessions.isEmpty()) {
                    Text("No other sessions.", style = MaterialTheme.typography.bodyMedium)
                }
                profile.sessions.forEach { session ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                session.device.ifBlank { "Unknown device" } + if (session.current) " · this phone" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (session.current) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                listOf(session.address, session.seen).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!session.current) {
                            TextButton(onClick = { haptic.click(); account.revokeSession(session.id) }, enabled = !state.busy) {
                                Text("Sign out")
                            }
                        }
                    }
                }
                if (profile.sessions.count { !it.current } > 0) {
                    TextButton(onClick = { haptic.click(); account.signOutOtherSessions() }, enabled = !state.busy) {
                        Text("Sign out all other devices")
                    }
                }
            }


            AccountSection("Delete account", danger = true) {
                Text(
                    "This erases the account, its playlists, likes and connected services. It cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { haptic.click(); account.sendEmailCode(); dialog = AccountDialog.DELETE },
                    enabled = !state.busy,
                    modifier = Modifier.padding(top = 4.dp)
                ) { Text("Delete my account", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    when (dialog) {
        AccountDialog.CHANGE_EMAIL -> ChangeEmailDialog(state, account) { dialog = null }
        AccountDialog.CHANGE_PASSWORD -> ChangePasswordDialog(state, account) { dialog = null }
        AccountDialog.VERIFY_EMAIL -> VerifyEmailDialog(state, account) { dialog = null }
        AccountDialog.TOTP_BEGIN -> BeginTotpDialog(state, account) { dialog = null }
        AccountDialog.TOTP_CONFIRM -> state.totp?.let { TotpConfirmDialog(it, state, account) { dialog = null } }
        AccountDialog.TOTP_DISABLE -> DisableTotpDialog(state, account) { dialog = null }
        AccountDialog.DELETE -> state.profile?.let { DeleteAccountDialog(it, state, account) { dialog = null } }
        null -> Unit
    }
}

private enum class AccountDialog {
    CHANGE_EMAIL, CHANGE_PASSWORD, VERIFY_EMAIL, TOTP_BEGIN, TOTP_CONFIRM, TOTP_DISABLE, DELETE
}

@Composable
private fun AccountSection(
    title: String,
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .glassPane(RoundedCornerShape(8.dp), GlassLevel.Regular)
            .border(
                1.dp,
                if (danger) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(20.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AccountActionRow(title: String, detail: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun DialogError(state: AccountUiState) {
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/** The matching action succeeded: the note is shown on the screen behind the sheet. */
@Composable
private fun DismissOnAction(state: AccountUiState, key: String, onDismiss: () -> Unit) {
    LaunchedEffect(state.actionKey, state.actionMessage) {
        if (state.actionMessage != null && state.actionKey == key) onDismiss()
    }
}

@Composable
private fun SecurityCodeFields(state: AccountUiState, account: AccountViewModel, purpose: String, code: String, onCode: (String) -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.codeExpiresAt, state.codeResendAt) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val wait = ((state.codeResendAt - now + 999) / 1000).coerceAtLeast(0)
    val remaining = ((state.codeExpiresAt - now + 999) / 1000).coerceAtLeast(0)
    val destination = if (purpose == "verify") state.profile?.pendingEmail?.ifBlank { state.profile?.email.orEmpty() }.orEmpty() else state.profile?.email.orEmpty()
    Text("Code delivery: $destination", style = MaterialTheme.typography.bodyMedium)
    TextButton(onClick = { when (purpose) { "reset" -> account.sendPasswordResetCode(); "verify" -> account.resendEmailVerification(); else -> account.sendEmailCode() } }, enabled = !state.busy && wait == 0L) {
        Text(if (wait > 0) "Resend in ${wait}s" else "Send code")
    }
    GlassTextField(code, { onCode(it.filter(Char::isDigit).take(6)) }, "Verification code", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
    if (state.codeExpiresAt > 0) Text(if (remaining > 0) "Code expires in ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}" else "Code expired. Send a new code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChangePasswordDialog(state: AccountUiState, account: AccountViewModel, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }
    val mailbox = !state.profile?.email.isNullOrBlank()
    DismissOnAction(state, ACTION_PASSWORD, onDismiss)
    GlassAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text(if (verified) "Choose your password" else "Verify your identity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (verified) "Step 2 of 2" else "Step 1 of 2", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!verified) {
                    if (mailbox) SecurityCodeFields(state, account, "reset", code, { code = it })
                    else GlassTextField(current, { current = it }, "Current password", password = true)
                } else {
                    Text("Use at least ${state.profile?.minPasswordLength ?: 10} characters.")
                    GlassTextField(password, { password = it }, "New password", password = true)
                    GlassTextField(confirm, { confirm = it }, "Confirm new password", password = true)
                }
                DialogError(state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!verified) {
                    if (mailbox) account.verifySecurityCode("reset", code) { verified = true }
                    else account.verifyIdentity(current) { verified = true }
                } else if (mailbox) account.resetPasswordWithCode(code, password, confirm)
                else account.changePassword(current, password, confirm)
            }, enabled = !state.busy) { Text(if (verified) "Save password" else "Verify and continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancel") } }
    )
}

@Composable
private fun ChangeEmailDialog(state: AccountUiState, account: AccountViewModel, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(state.profile?.email.isNullOrBlank()) }
    GlassAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text(if (verified) "New email address" else "Verify your current email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (verified) "Step 2 of 3" else "Step 1 of 3", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!verified) SecurityCodeFields(state, account, "reauth", code, { code = it })
                else {
                    GlassTextField(email, { email = it.trimStart() }, "New email address", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email))
                    GlassTextField(password, { password = it }, "Current password", password = true)
                    Text("A code will be sent to your new address to finish the change.")
                }
                DialogError(state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!verified) account.verifySecurityCode("reauth", code) { verified = true }
                else account.changeEmail(email, password, code)
            }, enabled = !state.busy) { Text(if (verified) "Send confirmation code" else "Verify and continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancel") } }
    )
}

@Composable
private fun VerifyEmailDialog(state: AccountUiState, account: AccountViewModel, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    DismissOnAction(state, ACTION_VERIFY, onDismiss)
    GlassAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text("Confirm email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecurityCodeFields(state, account, "verify", code, { code = it })
                DialogError(state)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { keyboard?.hide(); account.confirmEmail(code) },
                enabled = !state.busy && code.length == 6
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancel") } }
    )
}

@Composable
private fun BeginTotpDialog(state: AccountUiState, account: AccountViewModel, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(state.profile?.email.isNullOrBlank()) }
    GlassAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() }, title = { Text("Set up authenticator") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (verified) "Confirm your password to create a setup key." else "First, verify your email.")
            if (!verified) SecurityCodeFields(state, account, "reauth", emailCode, { emailCode = it })
            else GlassTextField(password, { password = it }, "Current password", password = true)
            DialogError(state)
        } },
        confirmButton = { TextButton(onClick = {
            if (!verified) account.verifySecurityCode("reauth", emailCode) { verified = true }
            else account.beginTotp(password, emailCode)
        }, enabled = !state.busy) { Text(if (verified) "Create setup key" else "Verify and continue") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancel") } }
    )
}

@Composable
private fun TotpConfirmDialog(
    enrollment: `in`.synthora.musicbox.viewmodels.TotpEnrollment,
    state: AccountUiState,
    account: AccountViewModel,
    onDismiss: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    DismissOnAction(state, ACTION_TOTP_ON, onDismiss)
    val bitmap = remember(enrollment.otpauthUri) {
        runCatching {
            val matrix = com.google.zxing.MultiFormatWriter()
                .encode(enrollment.otpauthUri, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888).apply {
                setPixels(
                    IntArray(512 * 512) { i ->
                        if (matrix[i % 512, i / 512]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    }, 0, 512, 0, 0, 512, 512
                )
            }
        }.getOrNull()
    }
    GlassAlertDialog(
        onDismissRequest = { if (!state.busy) { account.cancelTotp(); onDismiss() } },
        title = { Text("Scan and confirm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Scan this in Google Authenticator, Aegis, 1Password or any TOTP app, then type the code it shows.")
                if (bitmap != null) {
                    Image(
                        bitmap.asImageBitmap(),
                        contentDescription = "Two-factor setup QR code",
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
                Text(
                    "Can't scan? Enter this key manually:\n${enrollment.secret}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlassTextField(
                    code, { value -> code = value.filter(Char::isDigit).take(6) }, "000000", centered = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done
                    )
                )
                DialogError(state)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { keyboard?.hide(); account.confirmTotp(code) },
                enabled = !state.busy && code.length == 6
            ) { Text("Turn on") }
        },
        dismissButton = { TextButton(onClick = { account.cancelTotp(); onDismiss() }, enabled = !state.busy) { Text("Cancel") } }
    )
}

@Composable
private fun DisableTotpDialog(state: AccountUiState, account: AccountViewModel, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }
    DismissOnAction(state, ACTION_TOTP_OFF, onDismiss)
    GlassAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() }, title = { Text("Remove authenticator") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!verified) SecurityCodeFields(state, account, "reauth", emailCode, { emailCode = it })
            else {
                Text("Confirm your password to turn off two-factor sign-in.")
                GlassTextField(password, { password = it }, "Current password", password = true)
            }
            DialogError(state)
        } },
        confirmButton = { TextButton(onClick = {
            if (!verified) account.verifySecurityCode("reauth", emailCode) { verified = true }
            else account.disableTotp(password, emailCode)
        }, enabled = !state.busy) { Text(if (verified) "Remove authenticator" else "Verify and continue") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteAccountDialog(
    profile: AccountWebApi.AccountProfile,
    state: AccountUiState,
    account: AccountViewModel,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var emailCode by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    GlassAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text("Delete @${profile.username}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This permanently erases the account and everything attached to it. There is no undo and no recovery.",
                    color = MaterialTheme.colorScheme.error
                )
                Text("Type ${profile.username} exactly.")
                GlassTextField(username, { username = it }, "Username")
                GlassTextField(password, { password = it }, "Current password", password = true)
                if (profile.email.isNotBlank()) {
                    Text("A code was sent to ${profile.email}. Enter it to finish.")
                    GlassTextField(
                        emailCode, { value -> emailCode = value.filter(Char::isDigit).take(6) }, "6-digit email code",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    TextButton(onClick = account::sendEmailCode, enabled = !state.busy) { Text("Send a new code") }
                }
                DialogError(state)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { keyboard?.hide(); account.deleteAccount(password, username, emailCode) },
                enabled = !state.busy && username.equals(profile.username, ignoreCase = true) &&
                    password.isNotBlank() && (profile.email.isBlank() || emailCode.length == 6)
            ) { Text("Delete forever", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Keep my account") } }
    )
}
