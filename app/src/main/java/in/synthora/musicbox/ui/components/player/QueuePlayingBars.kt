package `in`.synthora.musicbox.ui.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

/** The web player's five staggered bars; only the active artwork redraws per frame. */
@Composable
internal fun QueuePlayingBars(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val phase: State<Float> = if (playing) {
        rememberInfiniteTransition(label = "Queue equalizer").animateFloat(0f, (2 * PI).toFloat(),
            infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "Queue bar phase")
    } else remember { mutableFloatStateOf(0f) }
    Canvas(modifier.semantics { contentDescription = if (playing) "Playing" else "Paused" }) {
        val heights = floatArrayOf(8f, 13f, 17f, 11f, 7f)
        val delays = floatArrayOf(0f, .25f, .45f, .15f, .35f)
        val barWidth = 2.dp.toPx()
        val gap = 2.dp.toPx()
        val left = (size.width - (barWidth * 5 + gap * 4)) / 2
        for (i in heights.indices) {
            val wave = (.5f - .5f * cos(phase.value + delays[i] * (2 * PI).toFloat() / 1.8f))
            val scale = .45f + .55f * wave
            val height = heights[i].dp.toPx() * scale
            drawRoundRect(color.copy(alpha = .65f + .35f * wave), Offset(left + i * (barWidth + gap), (size.height - height) / 2),
                Size(barWidth, height), CornerRadius(barWidth / 2))
        }
    }
}
