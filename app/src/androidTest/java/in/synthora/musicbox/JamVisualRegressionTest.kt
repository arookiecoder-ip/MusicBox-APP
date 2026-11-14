package `in`.synthora.musicbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.ui.components.AnimatedAmbientBackdrop
import `in`.synthora.musicbox.ui.screens.PlayerAction
import `in`.synthora.musicbox.ui.theme.JUKETheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JamVisualRegressionTest {
    @get:Rule val compose=createComposeRule()
    @Test fun jamDeviceIconIsGreenEvenWhenThePhoneIsOnlyAController() {
        val active=mutableStateOf(true)
        compose.setContent {JUKETheme {PlayerAction(rememberVectorPainter(Icons.Outlined.PhoneAndroid),"Audio device",24.dp,active=false,jamActive=active.value){}}}
        val icon=compose.onNodeWithContentDescription("Audio device · Jam active")
        val pixels=icon.captureToImage().toPixelMap()
        var green=0
        for(y in 0 until pixels.height)for(x in 0 until pixels.width)if(pixels[x,y].toArgb()==Color(0xFF66BB6A).toArgb())green++
        assertTrue("Jam must visibly tint the glyph green",green>5)
        compose.runOnIdle{active.value=false}
        compose.onNodeWithContentDescription("Audio device").assertIsDisplayed()
    }
    @Test fun blackOrMissingArtworkStillHasAVisibleQueueBlend() {
        val palette=mutableStateOf<List<Color>>(emptyList())
        compose.setContent {JUKETheme {Box(Modifier.size(240.dp)){AnimatedAmbientBackdrop(palette.value,Modifier.matchParentSize(),baseColor=Color(0xFF101015))}}}
        fun assertBlend(){
            val pixels=compose.onNodeWithTag("Ambient backdrop").captureToImage().toPixelMap()
            val colors=mutableSetOf<Int>()
            for(y in 0 until pixels.height step 8)for(x in 0 until pixels.width step 8)colors+=pixels[x,y].toArgb()
            assertTrue("Queue must not render as one flat dark color",colors.size>16)
        }
        assertBlend()
        compose.runOnIdle{palette.value=listOf(Color.Black,Color.Black,Color.Black)}
        compose.waitForIdle();assertBlend()
    }
}
