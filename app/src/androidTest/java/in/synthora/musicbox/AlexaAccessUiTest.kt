package `in`.synthora.musicbox

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import `in`.synthora.musicbox.ui.screens.AccountStatusCard
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.*
import org.junit.Rule
import org.junit.Test

class AlexaAccessUiTest {
    @get:Rule val compose = createComposeRule()
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
