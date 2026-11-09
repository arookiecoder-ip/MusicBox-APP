package `in`.synthora.musicbox.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*

/** Fade to the actual animated parent, avoiding an opaque strip at the artwork edge. */
internal fun Modifier.artworkBottomFade(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    drawRect(Brush.verticalGradient(0f to Color.White, .48f to Color.White, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
}
