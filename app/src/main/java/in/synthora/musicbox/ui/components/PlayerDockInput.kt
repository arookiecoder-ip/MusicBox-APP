package `in`.synthora.musicbox.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/** The full dock owns its touch area, including loading/empty padding. */
internal fun Modifier.playerDockInput(): Modifier = zIndex(1f).pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            // Let mini-player buttons, taps and swipes handle the event first.
            // A non-clickable skeleton must still keep it off content below.
            awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() }
        }
    }
}
