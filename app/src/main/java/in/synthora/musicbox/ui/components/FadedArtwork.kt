package `in`.synthora.musicbox.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/** One shader pass replaces a full-size offscreen artwork layer plus its alpha mask. */
@Composable
internal fun FadedArtwork(bitmap: Bitmap, modifier: Modifier = Modifier) {
    Canvas(modifier.drawWithCache {
        val paint = fadedArtworkPaint(bitmap, size.width, size.height)
        onDrawBehind {
            drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) }
        }
    }) {}
}

internal fun fadedArtworkPaint(bitmap: Bitmap, width: Float, height: Float): Paint {
    val scale = maxOf(width / bitmap.width, height / bitmap.height)
    val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate((width - bitmap.width * scale) / 2f, (height - bitmap.height * scale) / 2f)
    }
    val cover = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
    val mask = LinearGradient(0f, 0f, 0f, height.coerceAtLeast(1f),
        intArrayOf(android.graphics.Color.WHITE, android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT),
        floatArrayOf(0f, .48f, 1f), Shader.TileMode.CLAMP)
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        shader = ComposeShader(cover, mask, PorterDuff.Mode.DST_IN)
    }
}
