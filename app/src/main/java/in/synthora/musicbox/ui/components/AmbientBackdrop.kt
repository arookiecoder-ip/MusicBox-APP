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
import androidx.compose.ui.layout.onSizeChanged
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
    val colors=palette.takeIf { it.size>=3 && it.any { color -> color.luminance()>.04f } } ?: fallback
    var dimensions by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var previous by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var current by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val blend = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(colors, dimensions, baseColor, extent) {
        if (dimensions.width <= 0 || dimensions.height <= 0) return@LaunchedEffect
        val next = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            ambientRaster(colors, baseColor, dimensions.width,
                (dimensions.height * extent.coerceIn(0f, 1f)).toInt().coerceAtLeast(1))
        }
        val old = current
        blend.snapTo(if (old == null) 1f else 0f)
        previous = old; current = next
        if (old != null) blend.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        previous = null
    }
    Canvas(modifier.background(baseColor).testTag("Ambient backdrop").onSizeChanged { dimensions = it }) {
        // Sheet measurement and rasterization are asynchronous. Keep a visible
        // blend during the first frame, resize, or cancelled palette raster work.
        if(current==null) drawRect(Brush.verticalGradient(listOf(colors[0].copy(alpha=.25f),colors[1].copy(alpha=.20f),baseColor)))
        val destination = androidx.compose.ui.unit.IntSize(size.width.toInt(), (size.height * extent.coerceIn(0f, 1f)).toInt())
        if (destination.width > 0 && destination.height > 0) {
            previous?.let { drawImage(it.asImageBitmap(), dstSize = destination, filterQuality = FilterQuality.Low) }
            current?.let { drawImage(it.asImageBitmap(), dstSize = destination, alpha = blend.value, filterQuality = FilterQuality.Low) }
        }
    }
}

/** Decorative gradients have no sharp detail: rasterize at bounded resolution off Main,
 * then upload a small texture once. Scrolling never re-evaluates radial gradients per pixel. */
internal fun ambientRaster(colors: List<Color>, base: Color, width: Int, height: Int): android.graphics.Bitmap {
    val factor = minOf(1f, 512f / maxOf(width, height).coerceAtLeast(1))
    val w = (width * factor).toInt().coerceAtLeast(1)
    val h = (height * factor).toInt().coerceAtLeast(1)
    val result = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    canvas.drawColor(base.toArgb())
    val strength = if (base.luminance() < .5f) .27f else .16f
    val layers = listOf(
        android.graphics.LinearGradient(0f, 0f, w.toFloat(), 0f,
            intArrayOf(colors[0].copy(alpha = strength).toArgb(), colors[1].copy(alpha = strength).toArgb(), colors[2].copy(alpha = strength).toArgb()),
            null, android.graphics.Shader.TileMode.CLAMP),
        android.graphics.RadialGradient(w * .18f, h * .08f, maxOf(1f, w * .7f, h * .8f),
            intArrayOf(colors[1].copy(alpha = strength).toArgb(), android.graphics.Color.TRANSPARENT), null, android.graphics.Shader.TileMode.CLAMP),
        android.graphics.RadialGradient(w * .88f, h * .1f, maxOf(1f, w * .65f, h * .7f),
            intArrayOf(colors[2].copy(alpha = strength * .8f).toArgb(), android.graphics.Color.TRANSPARENT), null, android.graphics.Shader.TileMode.CLAMP),
        android.graphics.LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(android.graphics.Color.TRANSPARENT, base.toArgb()),
            null, android.graphics.Shader.TileMode.CLAMP)
    )
    val paint = android.graphics.Paint()
    for (layer in layers) { paint.shader = layer; canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint) }
    return result
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
