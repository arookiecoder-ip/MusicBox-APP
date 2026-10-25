package `in`.synthora.musicbox.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.synthora.musicbox.viewmodels.MusicViewModel
import `in`.synthora.musicbox.viewmodels.PlaybackOutput

/** Keep progress readable while showing that an unlinked Echo cannot be controlled. */
@Composable
fun Modifier.alexaControlAvailability(music: MusicViewModel): Modifier {
    val output by music.output.collectAsStateWithLifecycle()
    val amazon by music.echo.amazonConnected.collectAsStateWithLifecycle()
    return alpha(if (output == PlaybackOutput.ALEXA && amazon == false) .4f else 1f)
}
