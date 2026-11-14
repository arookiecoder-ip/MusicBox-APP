package `in`.synthora.musicbox.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/** The full dock owns its touch area, including loading/empty padding. */
internal fun Modifier.playerDockInput(): Modifier = zIndex(1f).pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            // Register the dock as a hit target so empty/loading areas shield
            // siblings underneath. Do not consume descendant events: consuming
            // in Final cancels button presses, long presses and drag detectors.
            awaitPointerEvent()
        }
    }
}
