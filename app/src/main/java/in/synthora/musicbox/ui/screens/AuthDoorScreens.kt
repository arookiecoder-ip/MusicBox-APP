package `in`.synthora.musicbox.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.R
import `in`.synthora.musicbox.ui.components.stableNavigationBarsPadding
import `in`.synthora.musicbox.ui.components.stableStatusBarsPadding
import `in`.synthora.musicbox.ui.theme.GlassBackdrop
import `in`.synthora.musicbox.ui.theme.glassPane
import `in`.synthora.musicbox.ui.theme.GlassLevel
import `in`.synthora.musicbox.ui.theme.isGlassDark
import `in`.synthora.musicbox.utils.rememberJukeHaptics
import `in`.synthora.musicbox.viewmodels.AccountUiState
import `in`.synthora.musicbox.viewmodels.AccountViewModel
import `in`.synthora.musicbox.viewmodels.AuthDoorState
import `in`.synthora.musicbox.viewmodels.AuthStage

/**
 * The three doors around the password: create an account, prove the mailbox,
 * recover the password. All share the same focused, single-column treatment as
 * sign-in and never put a password into ViewModel state.
 */
@Composable
fun AuthDoorScreen(state: AccountUiState, account: AccountViewModel) {
    AuthDoorFrame {
        when (state.stage) {
            AuthStage.SIGNUP -> SignupDoor(state, account)
            AuthStage.VERIFY_EMAIL -> VerifyDoor(state, account)
            AuthStage.FORGOT_PASSWORD -> ForgotDoor(state, account)
            AuthStage.RESET_PASSWORD -> ResetDoor(state, account)
            else -> Unit
        }
    }
}

@Composable
private fun AuthDoorFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(GlassBackdrop.color(isGlassDark()))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .stableStatusBarsPadding()
                .stableNavigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(72.dp).glassPane(CircleShape, GlassLevel.Thick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.music_box_pwa),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
private fun SignupDoor(state: AccountUiState, account: AccountViewModel) {
    var username by rememberSaveable { mutableStateOf(state.door.username) }
    var email by rememberSaveable { mutableStateOf(state.door.email) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val busy = state.busy
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val haptic = rememberJukeHaptics()

    DoorTitle("Create your account", "One account for your music, your Echo and every phone signed into this Music Box.")
    DoorMessage(state)
    GlassTextField(
        value = username,
        onValueChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch in "._-" }.take(32) },
        placeholder = "Username",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    )
    DoorFieldError(state, "username")
    Spacer(Modifier.height(10.dp))
    GlassTextField(
        value = email,
        onValueChange = { email = it.trimStart().take(254) },
        placeholder = "Email address",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    )
    DoorFieldError(state, "email")
    Spacer(Modifier.height(10.dp))
    GlassTextField(
        value = password,
        onValueChange = { password = it.take(200) },
        placeholder = "Password (10+ characters)",
        password = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    )
    PasswordStrength(password)
    DoorFieldError(state, "password")
    Spacer(Modifier.height(10.dp))
    GlassTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it.take(200) },
        placeholder = "Repeat the password",
        password = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            keyboard?.hide()
            account.submitSignup(username, email, password, confirmPassword)
        })
    )
    DoorFieldError(state, "confirm_password")
    Spacer(Modifier.height(20.dp))
    SubmitButton("Create account", busy) {
        haptic.click()
        keyboard?.hide()
        account.submitSignup(username, email, password, confirmPassword)
    }
    Text(
        "We send one six-digit code to this address. Nothing else is ever emailed to it.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp)
    )
    TextButton(onClick = account::backToSignIn, enabled = !busy) { Text("I already have an account") }
}

@Composable
private fun VerifyDoor(state: AccountUiState, account: AccountViewModel) {
    var username by rememberSaveable { mutableStateOf(state.door.username) }
    var code by rememberSaveable { mutableStateOf("") }
    val busy = state.busy
    val keyboard = LocalSoftwareKeyboardController.current
    val haptic = rememberJukeHaptics()

    DoorTitle("Confirm your email", "Enter the six-digit code we sent you. Codes are single-use and expire in 15 minutes.")
    DoorMessage(state)
    GlassTextField(
        value = username,
        onValueChange = { username = it.trim().take(120) },
        placeholder = "Username or email",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
    DoorFieldError(state, "username")
    Spacer(Modifier.height(10.dp))
    GlassTextField(
        value = code,
        onValueChange = { value -> code = value.filter(Char::isDigit).take(6) },
        placeholder = "000000",
        centered = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            keyboard?.hide()
            account.submitVerification(code)
        })
    )
    DoorFieldError(state, "code")
    Spacer(Modifier.height(20.dp))
    SubmitButton("Confirm email", busy) {
        haptic.click()
        keyboard?.hide()
        account.submitVerification(code)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        TextButton(onClick = account::resendVerification, enabled = !busy) { Text("Send a new code") }
        TextButton(onClick = account::backToSignIn) { Text("Back to sign in") }
    }
}

@Composable
private fun ForgotDoor(state: AccountUiState, account: AccountViewModel) {
    var identifier by rememberSaveable { mutableStateOf(state.door.username) }
    val busy = state.busy
    val keyboard = LocalSoftwareKeyboardController.current
    val haptic = rememberJukeHaptics()

    DoorTitle("Reset your password", "We send a six-digit code if the address or username matches an account. Nothing is revealed either way.")
    DoorMessage(state)
    GlassTextField(
        value = identifier,
        onValueChange = { identifier = it.trimStart().take(254) },
        placeholder = "Email address or username",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            keyboard?.hide()
            account.submitForgotPassword(identifier)
        })
    )
    DoorFieldError(state, "identifier")
    Spacer(Modifier.height(20.dp))
    SubmitButton("Send reset code", busy) {
        haptic.click()
        keyboard?.hide()
        account.submitForgotPassword(identifier)
    }
    TextButton(onClick = account::backToSignIn) { Text("Back to sign in") }
}

@Composable
private fun ResetDoor(state: AccountUiState, account: AccountViewModel) {
    var identifier by rememberSaveable { mutableStateOf(state.door.username) }
    var code by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val busy = state.busy
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val haptic = rememberJukeHaptics()

    DoorTitle("Choose a new password", "Enter the code from your email and a new password. Every other device is signed out.")
    DoorMessage(state)
    GlassTextField(
        value = identifier,
        onValueChange = { identifier = it.trimStart().take(254) },
        placeholder = "Username or email",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
    DoorFieldError(state, "identifier")
    Spacer(Modifier.height(10.dp))
    GlassTextField(
        value = code,
        onValueChange = { value -> code = value.filter(Char::isDigit).take(6) },
        placeholder = "000000",
        centered = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next)
    )
    DoorFieldError(state, "code")
    Spacer(Modifier.height(10.dp))
    GlassTextField(
        value = password,
        onValueChange = { password = it.take(200) },
        placeholder = "New password (10+ characters)",
        password = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    )
    PasswordStrength(password)
    DoorFieldError(state, "password")
    Spacer(Modifier.height(10.dp))
    GlassTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it.take(200) },
        placeholder = "Repeat the new password",
        password = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            keyboard?.hide()
            account.submitResetPassword(code, password, confirmPassword)
        })
    )
    DoorFieldError(state, "confirm_password")
    Spacer(Modifier.height(20.dp))
    SubmitButton("Change password", busy) {
        haptic.click()
        keyboard?.hide()
        account.submitResetPassword(code, password, confirmPassword)
    }
    TextButton(onClick = account::backToSignIn) { Text("Back to sign in") }
}

@Composable
private fun DoorTitle(title: String, subtitle: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun DoorMessage(state: AccountUiState) {
    val message = state.door.message
    if (message.isNotBlank()) {
        Text(
            message,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 14.dp)
        )
    }
    state.error?.takeIf { it.isNotBlank() }?.let { error ->
        Text(
            error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 14.dp)
        )
    }
}

@Composable
private fun DoorFieldError(state: AccountUiState, field: String) {
    state.door.fieldErrors[field]?.let { message ->
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, start = 4.dp)
        )
    }
}

@Composable
private fun PasswordStrength(password: String) {
    if (password.isEmpty()) return
    val classes = listOf(
        password.any(Char::isLowerCase),
        password.any(Char::isUpperCase),
        password.any(Char::isDigit),
        password.any { !it.isLetterOrDigit() }
    ).count { it }
    val strength = when {
        password.length < 10 || classes < 2 -> 0.28f
        password.length < 14 || classes < 3 -> 0.62f
        else -> 1f
    }
    val color = when {
        strength < 0.5f -> MaterialTheme.colorScheme.error
        strength < 0.9f -> Color(0xFFF4B400)
        else -> Color(0xFF66BB6A)
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(strength).height(4.dp).background(color))
        }
        Text(
            when {
                strength < 0.5f -> "Use at least 10 characters and two character types"
                strength < 0.9f -> "Good — longer is stronger"
                else -> "Strong password"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
