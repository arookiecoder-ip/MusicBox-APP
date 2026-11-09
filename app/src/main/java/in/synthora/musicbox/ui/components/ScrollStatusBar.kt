package `in`.synthora.musicbox.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalDensity

/** Draw behind system icons after scrolling; never consume gestures or add a touch target. */
internal fun Modifier.blackStatusBarOnScroll(page: Any? = Unit): Modifier = composed {
    var scrolled by remember(page) { mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(scrolled) {
        val controller = androidx.core.view.ViewCompat.getWindowInsetsController(view)
        val previous = controller?.isAppearanceLightStatusBars
        if (scrolled) controller?.isAppearanceLightStatusBars = false
        onDispose { if (scrolled && previous != null) controller?.isAppearanceLightStatusBars = previous }
    }
    val height = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    var depth by remember(page) { mutableFloatStateOf(0f) }
    val connection = remember(page) { object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (consumed.y != 0f) {
                depth=(depth-consumed.y).coerceAtLeast(0f)
                scrolled=depth>.5f
            }
            if (available.y>0f) { depth=0f;scrolled=false }
            return Offset.Zero
        }
    } }
    nestedScroll(connection).drawWithContent {
        drawContent()
        if (scrolled && height > 0f) drawRect(Color.Black, size = Size(size.width, height))
    }
}
