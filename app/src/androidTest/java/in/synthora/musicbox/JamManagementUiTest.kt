package `in`.synthora.musicbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import `in`.synthora.musicbox.ui.screens.JamSettingsContent
import `in`.synthora.musicbox.ui.screens.JamDeviceList
import `in`.synthora.musicbox.ui.screens.JamVolumeControls
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** No network or credentials: exercise the actual host/member management UI. */
class JamManagementUiTest {
    @get:Rule val compose=createComposeRule()
    private fun screenshot(name:String) {
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val directory=java.io.File(context.getExternalFilesDir(null),"jam-screenshots").apply{mkdirs()}
        java.io.File(directory,name+".png").outputStream().use {output->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,output)
        }
    }
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
        compose.onNodeWithText("Starting Jam…").assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo)).assertExists()
    }
    @Test fun ownPhoneAndEchoCanBothBeSelectedInTheMainGroupedList() {
        val state=mutableStateOf(host().copy(outputs=listOf(
            JamOutput("host-phone","Phone",true,"",username="Host",devices=listOf(JamDevice("phone","Phone","phone",true))),
            JamOutput("host-echo","Echo",true,"",username="Host",kind="alexa",devices=listOf(JamDevice("alexa:serial","Echo","alexa",true))),
            JamOutput("alice-phone","Tablet",true,"",username="Alice",devices=listOf(JamDevice("phone","Tablet","phone",true)))
        )))
        fun configure(mode:String,selected:List<String>,target:Pair<String,String>?=null) {
            val targets=state.value.playback["targets"]?.jsonObject.orEmpty().toMutableMap()
            target?.let{targets[it.first]=JsonPrimitive(it.second)}
            state.value=state.value.copy(playback=buildJsonObject {put("mode",mode);put("selected",JsonArray(selected.map(::JsonPrimitive)));put("targets",JsonObject(targets))})
        }
        compose.setContent{JUKETheme{Column(Modifier.verticalScroll(rememberScrollState())){
            JamDeviceList(state.value,{mode,selected->configure(mode,selected)},{mode,selected,owner,target->configure(mode,selected,owner to target)})
        }}}
        compose.onNodeWithText("Host").assertIsDisplayed()
        compose.onNodeWithText("Alice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Play on multiple devices").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Play on Echo · Host").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(setOf("host-phone","host-echo"),state.value.playback["selected"]!!.jsonArray.map{it.jsonPrimitive.content}.toSet())}
        compose.onNodeWithContentDescription("Play on Tablet · Alice").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(3,state.value.playback["selected"]!!.jsonArray.size)}
        compose.onAllNodesWithText("Audio outputs").assertCountEquals(0)
        screenshot("jam-play-on-multiple")
    }
    @Test fun memberControlsHostVolumeInSingleModeAndSeparateDeviceVolumesInMultipleMode() {
        val state=mutableStateOf(host().copy(host=false,outputs=listOf(
            JamOutput("echo","Echo",true,"",username="Host",volume=20,kind="alexa"),
            JamOutput("phone","Tablet",true,"",username="Alice",volume=40)),
            playback=buildJsonObject{put("mode","single");put("selected",JsonArray(listOf(JsonPrimitive("echo"))))}))
        val changes=mutableListOf<Pair<String,Int>>()
        compose.setContent{JUKETheme{Column{JamVolumeControls(state.value){device,value->changes+=device to value}}}}
        compose.onAllNodesWithContentDescription("Volume").assertCountEquals(1)
        compose.onNodeWithContentDescription("Volume").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(.7f)}
        compose.runOnIdle{assertEquals("echo",changes.last().first);assertEquals(70,changes.last().second)
            state.value=state.value.copy(playback=buildJsonObject{put("mode","multiple");put("selected",JsonArray(listOf(JsonPrimitive("echo"),JsonPrimitive("phone"))))})}
        compose.onAllNodesWithContentDescription("Volume").assertCountEquals(2)
        screenshot("jam-shared-volume")
        compose.onAllNodesWithContentDescription("Volume").onLast().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(.3f)}
        compose.runOnIdle{assertEquals("phone",changes.last().first);assertEquals(30,changes.last().second)}
    }

}
