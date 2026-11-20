package `in`.synthora.musicbox
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.ui.components.player.SyncedLyricsLines
import `in`.synthora.musicbox.ui.screens.LyricLine
import `in`.synthora.musicbox.ui.theme.JUKETheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class LyricsFollowingRegressionTest {
 @get:Rule val compose=createComposeRule()
 @Test fun activeFirstWrappedAndLastLinesStayAtTheViewportCenter() {
    var position by mutableStateOf(0L)
    val lines=(0..8).map{LyricLine(it*1000L,if(it==3)"A long lyric that wraps across several lines in this narrow viewport" else "Line $it")}
    compose.setContent{JUKETheme(darkTheme=true){SyncedLyricsLines(lines,position,{},Modifier.size(220.dp,340.dp))}}
    for(index in listOf(0,3,8,2)) {
        compose.runOnIdle{position=index*1000L};compose.waitForIdle()
        val viewport=compose.onNodeWithTag("Synced lyrics").fetchSemanticsNode().boundsInRoot
        val line=compose.onNodeWithTag("Lyric line $index").assertIsSelected().fetchSemanticsNode().boundsInRoot
        assertEquals("Active line $index must be centered",viewport.center.y,line.center.y,4f)
    }
 }
}
