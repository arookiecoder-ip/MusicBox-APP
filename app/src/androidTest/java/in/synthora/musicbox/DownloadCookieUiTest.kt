package `in`.synthora.musicbox

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import `in`.synthora.musicbox.ui.screens.AccountStatusCard
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.AccountStatus
import `in`.synthora.musicbox.viewmodels.AccountUiState
import `in`.synthora.musicbox.viewmodels.AccountViewModel
import org.junit.Rule
import org.junit.Test

class DownloadCookieUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cookieControlsAreAbsentRegardlessOfCookieState() {
        val state = mutableStateOf(AccountUiState(status = AccountStatus(youtubeCookies = true), checkingStatus = true))
        val account = AccountViewModel(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application)
        compose.setContent { JUKETheme { Column(Modifier.verticalScroll(rememberScrollState())) { AccountStatusCard(state.value, account) } } }
        compose.onNodeWithText("Check again").assertDoesNotExist()
        compose.onNodeWithText("Replace cookies").assertDoesNotExist()
        compose.onNodeWithText("Download cookies").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(status = AccountStatus(youtubeCookies = false)) }
        compose.onNodeWithText("Replace cookies").assertDoesNotExist()
        compose.onNodeWithText("YouTube Music").assertExists()
    }

    @Test fun connectedServiceOffersManagementWithoutCookieExports() {
        val state = AccountUiState(status = AccountStatus(youtubeLibrary = true))
        val account = AccountViewModel(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application)
        compose.setContent { JUKETheme { Column(Modifier.verticalScroll(rememberScrollState())) { AccountStatusCard(state, account) } } }
        compose.onNodeWithText("Manage").performScrollTo().performClick()
        compose.onNodeWithText("YouTube Music connection").assertIsDisplayed()
        compose.onNodeWithText("Change account").assertIsEnabled()
        compose.onNodeWithText("Disconnect").assertIsEnabled()
        compose.onNodeWithTag("download-cookie-export").assertDoesNotExist()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("YouTube Music connection").assertDoesNotExist()
    }

    @Test fun accessChangesRemoveAndRestoreAmazonRow() {
        val state = mutableStateOf(AccountUiState(status = AccountStatus()))
        val account = AccountViewModel(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application)
        compose.setContent { JUKETheme { AccountStatusCard(state.value, account) } }
        compose.onNodeWithText("Echo (Amazon)").assertDoesNotExist()
        compose.onNodeWithText("YouTube Music").assertExists()
        compose.runOnIdle { state.value = state.value.copy(status = AccountStatus(alexaAccessEnabled = true)) }
        compose.onNodeWithText("Echo (Amazon)").assertExists()
        compose.runOnIdle { state.value = state.value.copy(status = AccountStatus(alexaAccessEnabled = false, amazonConnected = true)) }
        compose.onNodeWithText("Echo (Amazon)").assertDoesNotExist()
        compose.onNodeWithText("YouTube Music").assertExists()
    }
}
