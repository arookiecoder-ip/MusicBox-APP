package `in`.synthora.musicbox.ui.components

import org.junit.Assert.*
import org.junit.Test

class AmbientPaletteTest {
    @Test fun aPaletteIsStableBetweenRecompositionsButVariesWithTimeAndVisit() {
        assertEquals(homeBlendPalette(1,100),homeBlendPalette(1,100))
        assertNotEquals(homeBlendPalette(1,100),homeBlendPalette(1,101))
        assertNotEquals(homeBlendPalette(1,100),homeBlendPalette(2,100))
    }
    @Test fun everyPaletteContainsThreeDistinctBoundedColors() {
        repeat(50) { n ->
            val colors=homeBlendPalette(n.toLong(),-n.toLong())
            assertEquals(3,colors.toSet().size)
            colors.forEach { c -> assertTrue(c.red in 0f..1f && c.green in 0f..1f && c.blue in 0f..1f && c.alpha==1f) }
        }
    }
}
