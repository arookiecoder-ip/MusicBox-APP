package `in`.synthora.musicbox

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.test.platform.app.InstrumentationRegistry
import `in`.synthora.musicbox.ui.screens.AccountSettingsScreen
import `in`.synthora.musicbox.network.AccountWebApi
import `in`.synthora.musicbox.ui.screens.AccountCheckScreen
import `in`.synthora.musicbox.ui.screens.SignInScreen
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.AccountCodeDelivery
import `in`.synthora.musicbox.viewmodels.AccountStatus
import `in`.synthora.musicbox.viewmodels.AccountUiState
import `in`.synthora.musicbox.viewmodels.AccountViewModel
import `in`.synthora.musicbox.viewmodels.AuthStage
import org.junit.Rule
import org.junit.Test

/** Exercise native account screens without sending credentials or changing remote accounts. */
class AccountUiRegressionTest {
    @Test fun emailCodeLoginHasNativeIdentifierAndVerificationSteps() {
        val account = account()
        var sent by mutableStateOf(false)
        compose.setContent {
            JUKETheme { SignInScreen(AccountUiState(stage = AuthStage.EMAIL_OTP,
                otpSent = sent, otpIdentifier = "listener@example.com"), account) }
        }
        compose.onNodeWithText("Send sign-in code").assertIsNotEnabled()
        compose.onNodeWithText("Email or username").performTextInput("listener@example.com")
        compose.onNodeWithText("Send sign-in code").assertIsEnabled()
        compose.runOnIdle { sent = true }
        compose.onNodeWithText("Verify and sign in").assertIsNotEnabled()
        compose.onNodeWithText("Six-digit email code").performTextInput("123456")
        compose.onNodeWithText("Verify and sign in").assertIsEnabled()
        compose.onNodeWithText("Resend code").assertIsDisplayed()
        compose.onNodeWithText("Use password instead").assertIsDisplayed()
        screenshot("email-otp-login")
    }
    @get:Rule val compose = createComposeRule()
    private fun screenshot(name:String) {
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        require(name.matches(Regex("[a-z0-9-]+")))
        val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                // Write through shell stdin instead of crossing Android's
                // app-storage boundary. This survives target-data cleanup.
                val output = "/data/local/tmp/musicbox-account-previews/account-$name.png"
                android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand("mkdir -p /data/local/tmp/musicbox-account-previews")
                ).use { it.readBytes() }
                val pipes = instrumentation.uiAutomation.executeShellCommandRw("dd of=$output")
                android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
                    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
                }
                android.os.ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
            } else {
                val directory=java.io.File(requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "jam-screenshots").apply { mkdirs() }
                java.io.File(directory, "account-$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            }
        } finally { bitmap.recycle() }
    }
    private fun account() = AccountViewModel(
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    )

    @Test fun emptySignInShowsValidationWithoutLeavingNativeLogin() {
        val account = account()
        compose.setContent {
            val state by account.state.collectAsState()
            JUKETheme { SignInScreen(state.copy(stage = AuthStage.SIGNED_OUT), account) }
        }
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Enter your username and password").assertIsDisplayed()
        compose.onNodeWithText("Username").assertIsDisplayed()
        compose.onNodeWithText("Password").assertIsDisplayed()
    }

    @Test fun authenticatorStepRejectsAnEmptyCodeLocally() {
        val account = account()
        compose.setContent {
            val state by account.state.collectAsState()
            JUKETheme { SignInScreen(state.copy(stage = AuthStage.NEEDS_CODE), account) }
        }
        compose.onNodeWithText("Verify").performClick()
        compose.onAllNodesWithText("Enter the 6-digit code from your authenticator app").assertCountEquals(2)
        compose.onNodeWithText("Use a different account").assertIsDisplayed()
    }

    @Test fun loginCannotBeSubmittedAgainWhileRequestIsRunning() {
        val account = account()
        compose.setContent {
            JUKETheme { SignInScreen(AccountUiState(stage = AuthStage.SIGNED_OUT, busy = true), account) }
        }
        compose.onNodeWithTag("account-submit").assertIsNotEnabled()
    }

    @Test fun unavailableLinkedAccountsStillAllowAnonymousRecommendations() {
        val account = account()
        compose.setContent {
            JUKETheme {
                AccountCheckScreen(AccountUiState(stage = AuthStage.SIGNED_IN, status = AccountStatus()), account)
            }
        }
        compose.onNodeWithText("Your accounts").assertIsDisplayed()
        compose.onNodeWithText("Connect what's missing, or continue").assertIsDisplayed()
        compose.onNodeWithText("Continue anyway").performScrollTo().assertIsDisplayed().performClick()
    }
    @Test fun deletionStartsWithWarningThenOffersSendCodeWithoutOtpField() {
        val account = account()
        val profile = AccountWebApi.AccountProfile(
            id = "test", username = "testuser", email = "test@example.test", pendingEmail = "",
            emailVerified = true, verificationPending = false, role = "user", roleText = "User",
            totpEnabled = false, totpPending = false, mustChangePassword = false,
            sessions = emptyList(), providers = emptyList(), minPasswordLength = 10
        )
        compose.setContent {
            JUKETheme { AccountSettingsScreen(account, AccountUiState(stage = AuthStage.SIGNED_IN, profile = profile), {}) }
        }
        compose.onNodeWithText("Delete my account").performScrollTo().performClick()
        compose.onNodeWithText("Delete this account?").assertIsDisplayed()
        compose.onAllNodesWithText("Verification code").assertCountEquals(0)
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Send code").assertIsDisplayed()
        compose.onAllNodesWithText("Verification code").assertCountEquals(0)
    }

    @Test fun resetRequestConfirmationDoesNotAutomaticallyShowOtpFields() {
        val account = account()
        val now = System.currentTimeMillis()
        val state = AccountUiState(stage=AuthStage.FORGOT_PASSWORD,
            door=`in`.synthora.musicbox.viewmodels.AuthDoorState(username="person@example.test",
                resetCodeRequested=true, resendAtMillis=now+60000, codeExpiresAtMillis=now+900000,
                message="If an account exists for this address, a reset code has been sent."))
        compose.setContent { JUKETheme {
            `in`.synthora.musicbox.ui.screens.AuthDoorScreen(state, account)
        } }
        compose.onNodeWithText("If an account exists", substring=true).assertIsDisplayed()
        compose.onNodeWithText("Enter reset code").assertIsDisplayed()
        compose.onNodeWithText("Resend in", substring=true).assertIsNotEnabled()
        compose.onNodeWithText("000000").assertDoesNotExist()
        compose.onNodeWithText("Verify your reset code").assertDoesNotExist()
    }

    @Test fun resetCodeShowsExpiryAndSeparateResendCooldown() {
        val account = account()
        val now = System.currentTimeMillis()
        var state by mutableStateOf(AccountUiState(stage=AuthStage.RESET_PASSWORD,
            door=`in`.synthora.musicbox.viewmodels.AuthDoorState(username="person@example.test",
                resendAtMillis=now+60000, codeExpiresAtMillis=now+900000)))
        compose.setContent { JUKETheme {
            `in`.synthora.musicbox.ui.screens.AuthDoorScreen(state, account)
        } }
        compose.onNodeWithText("Code expires in", substring=true).assertIsDisplayed()
        compose.onNodeWithText("Resend in", substring=true).assertIsNotEnabled()
        compose.onNodeWithText("Verify code").assertIsNotEnabled()
        compose.onNodeWithText("000000").performTextInput("123456")
        compose.onNodeWithText("Verify code").assertIsEnabled()
        compose.runOnIdle { state=state.copy(door=state.door.copy(resendAtMillis=0)) }
        compose.onNodeWithText("Resend code").assertIsEnabled()
        compose.runOnIdle { state=state.copy(door=state.door.copy(codeExpiresAtMillis=System.currentTimeMillis()-1000)) }
        compose.onNodeWithText("Code expired. Resend to continue.").assertIsDisplayed()
        compose.onNodeWithText("Verify code").assertIsNotEnabled()
    }

    @Test fun anotherActionsOtpDoesNotAppearInAuthenticatorSetup() {
        val account = account()
        val profile = AccountWebApi.AccountProfile(
            id = "stable-account-id", username = "testuser", email = "test@example.test", pendingEmail = "",
            emailVerified = true, verificationPending = false, role = "user", roleText = "User",
            totpEnabled = false, totpPending = false, mustChangePassword = false,
            sessions = emptyList(), providers = emptyList(), minPasswordLength = 10
        )
        val now = System.currentTimeMillis()
        compose.setContent {
            JUKETheme { AccountSettingsScreen(account, AccountUiState(
                stage = AuthStage.SIGNED_IN, profile = profile,
                actionMessage = "Earlier email action completed",
                codeDeliveries = mapOf("email-change" to AccountCodeDelivery(now + 600000, now + 60000))
            ), {}) }
        }
        compose.onAllNodesWithText("Earlier email action completed").assertCountEquals(0)
        compose.onNodeWithContentDescription("Copy account ID").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("stable-account-id").assertIsDisplayed()
        compose.onNodeWithText("Set up authenticator").performScrollTo().performClick()
        compose.onNodeWithText("Send code").assertIsDisplayed()
        compose.onAllNodesWithText("Verification code").assertCountEquals(0)
        compose.onAllNodesWithText("Code expires", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Resend in", substring = true).assertCountEquals(0)
    }

    @Test fun authenticatorSetupShowsSendThenOtpResendCountdownAndVerify() {
        val account = account()
        val profile = AccountWebApi.AccountProfile(id="id",username="name",email="person@example.test",pendingEmail="",emailVerified=true,
            verificationPending=false,role="user",roleText="User",totpEnabled=false,totpPending=false,mustChangePassword=false,
            sessions=emptyList(),providers=emptyList(),minPasswordLength=10)
        var state by mutableStateOf(AccountUiState(stage=AuthStage.SIGNED_IN,profile=profile))
        compose.setContent { JUKETheme { AccountSettingsScreen(account,state,{}) } }
        compose.onNodeWithText("Set up authenticator").performScrollTo().performClick()
        compose.onNodeWithText("Send code").assertIsDisplayed()
        compose.onAllNodesWithText("Verify and continue").assertCountEquals(0)
        compose.onAllNodesWithText("Verification code").assertCountEquals(0)
        compose.runOnIdle { state=state.copy(codeDeliveries=mapOf("totp-setup" to AccountCodeDelivery(System.currentTimeMillis()+600000,System.currentTimeMillis()+60000))) }
        compose.onNodeWithText("Verification code").assertIsDisplayed()
        compose.onNodeWithText("Resend in",substring=true).assertIsNotEnabled()
        compose.onNodeWithText("Code expires in",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Verify code").assertIsNotEnabled()
        compose.onNodeWithText("Verification code").performTextInput("123456")
        compose.onNodeWithText("Verify code").assertIsEnabled()
    }

    @Test fun passwordIdentityDialogShowsSendThenOnlyOtpAndVerify() {
        val account=account()
        val profile=AccountWebApi.AccountProfile(id="id",username="name",email="person@example.test",pendingEmail="",emailVerified=true,
            verificationPending=false,role="user",roleText="User",totpEnabled=false,totpPending=false,mustChangePassword=false,
            sessions=emptyList(),providers=emptyList(),minPasswordLength=10)
        var state by mutableStateOf(AccountUiState(stage=AuthStage.SIGNED_IN,profile=profile))
        compose.setContent{JUKETheme(darkTheme=true){AccountSettingsScreen(account,state,{})}}
        compose.onAllNodesWithText("User").assertCountEquals(0)
        compose.onAllNodesWithText("Copy account ID").assertCountEquals(0)
        screenshot("overview")
        compose.onAllNodesWithText("Change")[1].performScrollTo().performClick()
        compose.onNodeWithText("Send code").assertIsDisplayed()
        compose.onAllNodesWithText("Verification code").assertCountEquals(0)
        compose.onAllNodesWithText("Verify").assertCountEquals(0)
        screenshot("identity-send-code")
        compose.runOnIdle{state=state.copy(codeDeliveries=mapOf("reset" to AccountCodeDelivery(System.currentTimeMillis()+600000,0)))}
        compose.onNodeWithText("Verification code").assertIsDisplayed()
        compose.onAllNodesWithText("Send code").assertCountEquals(0)
        compose.onNodeWithText("Verify").assertIsNotEnabled()
        screenshot("identity-otp")
        compose.onNodeWithText("Verification code").performTextInput("123456")
        compose.onNodeWithText("Verify").assertIsEnabled()
    }
    @Test fun emailIdentityUsesTheSameSeparatedDeliveryAndVerificationSteps() {
        val account=account()
        val profile=AccountWebApi.AccountProfile(id="id",username="name",email="person@example.test",pendingEmail="",emailVerified=true,
            verificationPending=false,role="user",roleText="User",totpEnabled=false,totpPending=false,mustChangePassword=false,
            sessions=emptyList(),providers=emptyList(),minPasswordLength=10)
        var state by mutableStateOf(AccountUiState(stage=AuthStage.SIGNED_IN,profile=profile))
        compose.setContent{JUKETheme(darkTheme=true){AccountSettingsScreen(account,state,{})}}
        compose.onAllNodesWithText("Change")[0].performScrollTo().performClick()
        compose.onNodeWithText("Send code").assertIsDisplayed()
        compose.onAllNodesWithText("Verification code").assertCountEquals(0)
        compose.runOnIdle{state=state.copy(codeDeliveries=mapOf("email-change" to AccountCodeDelivery(System.currentTimeMillis()+600000,0)))}
        compose.onNodeWithText("Verification code").assertIsDisplayed()
        compose.onNodeWithText("Verify").assertIsDisplayed()
        compose.onAllNodesWithText("Send code").assertCountEquals(0)
    }
    @Test fun providerManagementOffersDistinctActionsAndDismissesWithoutDisconnecting() {
        val account=account()
        compose.setContent {JUKETheme(darkTheme=true) {
            `in`.synthora.musicbox.ui.screens.AccountStatusCard(
                AccountUiState(stage=AuthStage.SIGNED_IN,status=AccountStatus(amazonConnected=true,youtubeLibrary=true,alexaAccessEnabled=true)),account)
        }}
        compose.onAllNodesWithText("Manage")[0].performClick()
        compose.onNodeWithText("Amazon connection").assertIsDisplayed()
        compose.onNodeWithText("Change account").assertIsDisplayed()
        compose.onNodeWithText("Disconnect").assertIsDisplayed()
        screenshot("amazon-management")
        compose.onNodeWithContentDescription("Close connection options").performClick()
        compose.onAllNodesWithText("Manage")[1].performClick()
        compose.onNodeWithText("YouTube Music connection").assertIsDisplayed()
        screenshot("youtube-management")
        compose.onNodeWithText("Cancel").performClick()
        compose.onAllNodesWithText("Manage").assertCountEquals(2)
    }

    @Test fun closedSignupOnlyHidesCreationWhilePasswordRecoveryRemains() {
        val account=account()
        var open by mutableStateOf(false)
        compose.setContent{JUKETheme(darkTheme=true){androidx.compose.material3.Surface {
            SignInScreen(AccountUiState(stage=AuthStage.SIGNED_OUT,doorsAvailable=true,signupAvailable=open),account)
        }}}
        compose.onAllNodesWithText("Create account").assertCountEquals(0)
        compose.onNodeWithText("Forgot password?").assertIsDisplayed()
        compose.runOnIdle{open=true}
        compose.onNodeWithText("Create account").assertIsDisplayed()
    }
    @Test fun authenticatorSetupHasAnAccessibleCopyAction() {
        val account=account()
        compose.setContent{JUKETheme(darkTheme=true){androidx.compose.material3.Surface {
            AccountSettingsScreen(account,AccountUiState(stage=AuthStage.SIGNED_IN,
                profile=AccountWebApi.AccountProfile(id="id",username="name",email="person@example.test",pendingEmail="",emailVerified=true,
                verificationPending=false,role="user",roleText="User",totpEnabled=false,totpPending=false,mustChangePassword=false,
                sessions=emptyList(),providers=emptyList(),minPasswordLength=10),
                totp=`in`.synthora.musicbox.viewmodels.TotpEnrollment("ABCDEFGHIJKLMNOP","test@example.test","Music Box","")),{})
        }}}
        compose.onNodeWithContentDescription("Copy authenticator setup key").assertIsDisplayed().performClick()
        compose.onNodeWithText("Scan and confirm").assertIsDisplayed()
    }

    @Test fun deviceHierarchyShowsCurrentDeviceAndKeepsIndependentSessions() {
        val account = account()
        val profile = AccountWebApi.AccountProfile(
            id = "test-account", username = "testuser", email = "test@example.test", pendingEmail = "",
            emailVerified = true, verificationPending = false, role = "user", roleText = "User",
            totpEnabled = true, totpPending = false, mustChangePassword = false,
            sessions = listOf(
                AccountWebApi.AccountSession("browser", false, "Chrome on Windows", "Today, 10:30", "Yesterday", "192.0.2.1"),
                AccountWebApi.AccountSession("phone", true, "Music Box app", "Just now", "Today", "192.0.2.2")),
            providers = emptyList(), minPasswordLength = 10)
        compose.setContent {
            JUKETheme { AccountSettingsScreen(account, AccountUiState(stage = AuthStage.SIGNED_IN, profile = profile), {}) }
        }
        screenshot("security-overview")
        compose.onNodeWithText("Music Box app").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("THIS DEVICE").assertIsDisplayed()
        screenshot("device-hierarchy")
        compose.onNodeWithText("Chrome on Windows").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Sign out").assertCountEquals(1)
    }
}
