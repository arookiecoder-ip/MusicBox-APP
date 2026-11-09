package `in`.synthora.musicbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CollectionPlayButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(onClick = onClick, modifier = modifier.size(40.dp).shadow(10.dp, CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.50f))) {
        Icon(Icons.Filled.PlayArrow, label, Modifier.size(26.dp), tint = Color.White)
    }
}

/** Decorative overlay: the artwork's existing click and long-press actions stay intact. */
@Composable
internal fun ArtworkPlayIndicator(iconSize: Dp = 28.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(iconSize + 10.dp).shadow(8.dp, CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
        .background(Color.Black.copy(alpha = .50f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PlayArrow, null, Modifier.size(iconSize), tint = Color.White)
    }
}
