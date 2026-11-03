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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
    Box(modifier.background(baseColor).testTag("Ambient backdrop")) {
        Canvas(Modifier.fillMaxSize().drawWithCache {
            val height = size.height * extent.coerceIn(0f, 1f)
            val strength = if (baseColor.luminance() < .5f) .27f else .16f
            val horizontal = android.graphics.LinearGradient(0f, 0f, size.width.coerceAtLeast(1f), 0f,
                intArrayOf(first.copy(alpha = strength).toArgb(), second.copy(alpha = strength).toArgb(), third.copy(alpha = strength).toArgb()),
                null, android.graphics.Shader.TileMode.CLAMP)
            val left = android.graphics.RadialGradient(size.width * .18f, height * .08f,
                maxOf(1f, size.width * .7f, height * .8f),
                intArrayOf(second.copy(alpha = strength).toArgb(), android.graphics.Color.TRANSPARENT), null, android.graphics.Shader.TileMode.CLAMP)
            val right = android.graphics.RadialGradient(size.width * .88f, height * .1f,
                maxOf(1f, size.width * .65f, height * .7f),
                intArrayOf(third.copy(alpha = strength * .8f).toArgb(), android.graphics.Color.TRANSPARENT), null, android.graphics.Shader.TileMode.CLAMP)
            val fade = android.graphics.LinearGradient(0f, 0f, 0f, height.coerceAtLeast(1f),
                intArrayOf(android.graphics.Color.TRANSPARENT, baseColor.toArgb()), null, android.graphics.Shader.TileMode.CLAMP)
            fun over(bottom: android.graphics.Shader, top: android.graphics.Shader) =
                android.graphics.ComposeShader(bottom, top, android.graphics.PorterDuff.Mode.SRC_OVER)
            val paint = android.graphics.Paint().apply { shader = over(over(over(horizontal, left), right), fade) }
            // Fuse the layers into one shader draw. Avoid four full-area fills on tablet GPUs.
            onDrawBehind { if (size.width > 0 && height > 0) {
                drawIntoCanvas { canvas ->
                    if (android.os.Build.VERSION.SDK_INT >= 28) canvas.nativeCanvas.drawRect(0f, 0f, size.width, height, paint)
                    else for (layer in listOf(horizontal, left, right, fade)) {
                        paint.shader = layer
                        canvas.nativeCanvas.drawRect(0f, 0f, size.width, height, paint)
                    }
                }
            } }
        }) {}
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
