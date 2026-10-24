package `in`.synthora.musicbox.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.random.Random

internal fun homeBlendPalette(seed: Long, minute: Long): List<Color> {
    val random=Random(seed xor (minute*31))
    val hue=random.nextFloat()*360
    return listOf(0f,95f,225f).map { shift -> Color.hsv((hue+shift)%360,.6f,.95f) }
}

/** Gradients are drawn behind content and never participate in pointer input. */
@Composable
internal fun AnimatedAmbientBackdrop(palette: List<Color>, modifier: Modifier = Modifier,
    extent: Float = 1f, baseColor: Color = MaterialTheme.colorScheme.background) {
    val fallback=listOf(Color(0xFF9276DF),Color(0xFF457FB8),Color(0xFFB45986))
    val colors=palette.takeIf { it.size>=3 } ?: fallback
    val first by animateColorAsState(colors[0],tween(1100,easing=FastOutSlowInEasing),label="ambient-first")
    val second by animateColorAsState(colors[1],tween(1100,easing=FastOutSlowInEasing),label="ambient-second")
    val third by animateColorAsState(colors[2],tween(1100,easing=FastOutSlowInEasing),label="ambient-third")
    BoxWithConstraints(modifier.background(baseColor).testTag("Ambient backdrop")) {
        Canvas(Modifier.fillMaxWidth().height(maxHeight*extent.coerceIn(0f,1f))
            .graphicsLayer { compositingStrategy=CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(listOf(Color.White,Color.Transparent)),blendMode=BlendMode.DstIn)
            }) {
            if(size.width<=0f || size.height<=0f) return@Canvas
            val strength=if(baseColor.luminance()<.5f) .27f else .16f
            drawRect(Brush.horizontalGradient(listOf(first.copy(alpha=strength),second.copy(alpha=strength),third.copy(alpha=strength))))
            drawRect(Brush.radialGradient(listOf(second.copy(alpha=strength),Color.Transparent),
                center=Offset(size.width*.18f,size.height*.08f),radius=maxOf(size.width*.7f,size.height*.8f)))
            drawRect(Brush.radialGradient(listOf(third.copy(alpha=strength*.8f),Color.Transparent),
                center=Offset(size.width*.88f,size.height*.1f),radius=maxOf(size.width*.65f,size.height*.7f)))
        }
    }
}

@Composable
internal fun HomeAmbientBackdrop(modifier: Modifier = Modifier) {
    val seed=rememberSaveable { Random.nextLong() }
    var minute by remember { mutableLongStateOf(System.currentTimeMillis()/60_000) }
    val lifecycle=LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(true) {
                minute=System.currentTimeMillis()/60_000
                delay(60_000)
            }
        }
    }
    val palette=remember(seed,minute) { homeBlendPalette(seed,minute) }
    AnimatedAmbientBackdrop(palette,modifier,extent=.30f)
}
