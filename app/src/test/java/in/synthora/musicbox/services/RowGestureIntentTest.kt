package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.ui.components.horizontalRowIntent
import org.junit.Assert.*
import org.junit.Test

class RowGestureIntentTest {
    @Test fun tiltedVerticalAndDiagonalScrollingRemainAvailable() {
        for ((dx, dy) in listOf(20f to 100f, 60f to 100f, 100f to 100f, -40f to -100f))
            assertFalse(horizontalRowIntent(dx, dy, 8f))
    }
    @Test fun deliberateSwipesWorkInBothDirectionsAndIgnoreTouchJitter() {
        assertTrue(horizontalRowIntent(100f, 20f, 8f))
        assertTrue(horizontalRowIntent(-100f, -20f, 8f))
        assertFalse(horizontalRowIntent(8f, 0f, 8f))
    }
}
