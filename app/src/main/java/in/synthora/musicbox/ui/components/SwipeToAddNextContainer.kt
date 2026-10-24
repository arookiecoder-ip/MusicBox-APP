package `in`.synthora.musicbox.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.utils.rememberJukeHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Prefer scrolling for diagonal gestures. Only clear horizontal intent claims a row. */
internal fun horizontalRowIntent(dx: Float, dy: Float, slop: Float): Boolean =
    abs(dx) > slop * 2 && abs(dx) > abs(dy) * 2

@Composable
fun SwipeToAddNextContainer(onAddNext: () -> Unit, onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    DirectionalRowSwipe(onAddNext, onAddToQueue, modifier, removing = false) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background), content = content)
    }
}

@Composable
internal fun SwipeToRemoveQueueRow(enabled: Boolean, onRemove: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    DirectionalRowSwipe(if (enabled) onRemove else null, null, modifier, removing = true, content = content)
}

@Composable
private fun DirectionalRowSwipe(onLeft: (() -> Unit)?, onRight: (() -> Unit)?,
    modifier: Modifier, removing: Boolean, content: @Composable () -> Unit) {
    val left by rememberUpdatedState(onLeft)
    val right by rememberUpdatedState(onRight)
    val haptic = rememberJukeHaptics()
    val scope = rememberCoroutineScope()
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf<Job?>(null) }
    Box(modifier.fillMaxWidth().clipToBounds().onSizeChanged { width = it.width.toFloat() }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                settling?.cancel(); offset = 0f
                var dx = 0f; var dy = 0f; var horizontal = false
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed || event.changes.count { it.pressed } > 1) break
                        val delta = change.positionChange()
                        dx += delta.x; dy += delta.y
                        if (!horizontal) {
                            if (abs(dy) > viewConfiguration.touchSlop && abs(dy) >= abs(dx) / 2) break
                            horizontal = horizontalRowIntent(dx, dy, viewConfiguration.touchSlop)
                            if (horizontal && (if (dx < 0) left else right) == null) break
                        }
                        if (horizontal) {
                            change.consume()
                            offset = dx.coerceIn(-width, width)
                        }
                        if (!change.pressed) {
                            if (horizontal && width > 0 && abs(dx) > width * .35f) {
                                haptic.confirm()
                                (if (dx < 0) left else right)?.invoke()
                            }
                            break
                        }
                    }
                } finally {
                    // One release animation; no coroutine is launched per pointer sample.
                    val start = offset
                    if (start != 0f) settling = scope.launch { animate(start, 0f, animationSpec = tween(140)) { value, _ -> offset = value } }
                }
            }
        }) {
        if (abs(offset) > 1f) {
            val positive = offset > 0
            Row(Modifier.matchParentSize().background(if (removing) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (positive) Arrangement.Start else Arrangement.End) {
                Icon(if (removing) Icons.Default.Delete else if (positive) Icons.Default.QueueMusic else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (removing) "Remove from queue" else if (positive) "Add to queue" else "Play next")
            }
        }
        Box(Modifier.fillMaxWidth().graphicsLayer { translationX = offset }) { content() }
    }
}
