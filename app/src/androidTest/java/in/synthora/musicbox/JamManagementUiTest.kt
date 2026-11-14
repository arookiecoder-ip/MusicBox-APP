package `in`.synthora.musicbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import `in`.synthora.musicbox.ui.screens.JamSettingsContent
import `in`.synthora.musicbox.ui.screens.JamDeviceList
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** No network or credentials: exercise the actual host/member management UI. */
class JamManagementUiTest {
    @get:Rule val compose=createComposeRule()
    private fun host()=JamUiState(active=true,host=true,url="https://example.test/j/abcdefghijk",memberCount=2,
        members=listOf(JamMember("alice-id","Alice")),
        outputs=listOf(JamOutput("host-phone","Host phone",true,"",username="Host"),JamOutput("alice-phone","Alice phone",true,"",username="Alice")),
        playback=buildJsonObject {put("mode","single");put("selected",JsonArray(listOf(JsonPrimitive("host-phone"))))})

    @Test fun hostCanSwitchBetweenOneAndSeveralSelectedOutputs() {
        val state=mutableStateOf(host())
        compose.setContent { JUKETheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            JamDeviceList(state.value, {mode,outputs ->
                state.value=state.value.copy(playback=buildJsonObject {put("mode",mode);put("selected",JsonArray(outputs.map(::JsonPrimitive)))})
            }, {_,_,_,_->})
        } } }
        compose.onNodeWithContentDescription("Play on multiple devices").performScrollTo().performClick()
        compose.runOnIdle {assertEquals("multiple",state.value.playback["mode"]?.jsonPrimitive?.content)}
        compose.onNodeWithContentDescription("Play on Alice phone · Alice").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(2,state.value.playback["selected"]?.jsonArray?.size)}
        compose.onNodeWithContentDescription("Play on multiple devices").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(1,state.value.playback["selected"]?.jsonArray?.size)}
    }
    @Test fun hostRemovalRequiresConfirmationAndSendsTheChosenMember() {
        var removed:String?=null
        compose.setContent { JUKETheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            JamSettingsContent(host(), {}, {}, {}, {removed=it}, {_,_->}, {})
        } } }
        compose.onNodeWithContentDescription("Remove Alice").performScrollTo().performClick()
        compose.onNodeWithText("Remove Alice?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {assertNull(removed)}
        compose.onNodeWithContentDescription("Remove Alice").performScrollTo().performClick()
        compose.onAllNodesWithText("Remove").onLast().performClick()
        compose.runOnIdle {assertEquals("alice-id",removed)}
    }
    @Test fun memberCanLeaveButDoesNotReceiveHostControls() {
        var left=false
        compose.setContent { JUKETheme {
            JamSettingsContent(host().copy(host=false,members=emptyList()), {}, {}, {left=true}, {}, {_,_->}, {})
        } }
        compose.onAllNodesWithText("Audio outputs").assertCountEquals(0)
        compose.onAllNodesWithText("Participants").assertCountEquals(0)
        compose.onAllNodesWithText("End Jam").assertCountEquals(0)
        compose.onNodeWithText("Leave Jam").performClick()
        compose.onNodeWithText("Leave this Jam?").assertIsDisplayed()
        compose.onAllNodesWithText("Leave Jam").onLast().performClick()
        compose.runOnIdle {assertTrue(left)}
    }
    @Test fun startingCannotBeSubmittedTwiceWhileBusy() {
        compose.setContent { JUKETheme {
            JamSettingsContent(JamUiState(busy=true), {}, {}, {}, {}, {_,_->}, {})
        } }
        compose.onAllNodes(isEnabled() and hasClickAction()).assertCountEquals(0)
    }
}
