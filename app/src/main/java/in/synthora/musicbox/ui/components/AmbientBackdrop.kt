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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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

/** One small texture is shared by the sheet and the foreground of its swiping rows. */
@Stable
internal class AmbientBackdropState {
    var current by mutableStateOf<android.graphics.Bitmap?>(null)
    var previous by mutableStateOf<android.graphics.Bitmap?>(null)
    val blend=androidx.compose.animation.core.Animatable(1f)
    var dimensions by mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
    var base by mutableStateOf(Color(0xFF101015))
    var colors by mutableStateOf(listOf(Color(0xFF9276DF),Color(0xFF457FB8),Color(0xFFB45986)))
}

/** Gradients are drawn behind content and never participate in pointer input. */
@Composable
internal fun AnimatedAmbientBackdrop(palette: List<Color>, modifier: Modifier = Modifier,
    extent: Float = 1f, baseColor: Color = MaterialTheme.colorScheme.background,
    state:AmbientBackdropState=remember{AmbientBackdropState()}) {
    val fallback=listOf(Color(0xFF9276DF),Color(0xFF457FB8),Color(0xFFB45986))
    val colors=palette.takeIf{it.size>=3 && it.any{color->color.luminance()>.04f}} ?: fallback
    SideEffect{state.colors=colors;state.base=baseColor}
    val dimensions=state.dimensions
    LaunchedEffect(colors,dimensions,baseColor,extent) {
        if(dimensions.width<=0 || dimensions.height<=0)return@LaunchedEffect
        val next=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            ambientRaster(colors,baseColor,dimensions.width,(dimensions.height*extent.coerceIn(0f,1f)).toInt().coerceAtLeast(1))
        }
        val old=state.current
        state.blend.snapTo(if(old==null)1f else 0f)
        state.previous=old;state.current=next
        if(old!=null)state.blend.animateTo(1f,tween(1100,easing=FastOutSlowInEasing))
        state.previous=null
    }
    Canvas(modifier.background(baseColor).testTag("Ambient backdrop").onSizeChanged{state.dimensions=it}) {
        drawAmbientTexture(state,size.width.toInt(),(size.height*extent.coerceIn(0f,1f)).toInt(),0)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientTexture(state:AmbientBackdropState,width:Int,height:Int,y:Int) {
    drawRect(state.base)
    if(state.current==null)drawRect(Brush.verticalGradient(listOf(state.colors[0].copy(alpha=.25f),state.colors[1].copy(alpha=.20f),state.base)))
    if(width<=0 || height<=0)return
    val destination=androidx.compose.ui.unit.IntSize(width,height)
    val offset=androidx.compose.ui.unit.IntOffset(0,-y)
    state.previous?.let{drawImage(it.asImageBitmap(),dstOffset=offset,dstSize=destination,filterQuality=FilterQuality.Low)}
    state.current?.let{drawImage(it.asImageBitmap(),dstOffset=offset,dstSize=destination,alpha=state.blend.value,filterQuality=FilterQuality.Low)}
}

/** Keep the original artwork patch attached to the row; only its uncovered strip is red. */
@Composable
internal fun AmbientRowBackground(state:AmbientBackdropState,sheetTop:Float,modifier:Modifier=Modifier) {
    var top by remember{mutableFloatStateOf(0f)}
    Canvas(modifier.then(Modifier.fillMaxSize()).clipToBounds().onGloballyPositioned{top=it.positionInRoot().y}) {
        drawAmbientTexture(state,state.dimensions.width,state.dimensions.height,(top-sheetTop).toInt())
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
