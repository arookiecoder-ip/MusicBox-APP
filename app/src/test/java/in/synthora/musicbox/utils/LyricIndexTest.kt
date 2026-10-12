package `in`.synthora.musicbox.utils
import org.junit.Assert.*
import org.junit.Test
class LyricIndexTest {
    @Test fun findsLastDuplicateAndHandlesSeeksEmptyAndNegativeCursor() {
        val times = listOf(0L, 1_000, 1_000, 5_000)
        for ((position, expected) in listOf(-1L to -1, 0L to 0, 999L to 0, 1_000L to 2, 4_999L to 2, 5_000L to 3, 99_000L to 3))
            assertEquals(expected, lyricIndexAt(times.size, position) { times[it] })
        assertEquals(-1, lyricIndexAt(0, 0) { error("Empty list cannot be read") })
    }
    @Test fun longLyricsUseLogarithmicLookup() {
        var lookups = 0
        assertEquals(5_000, lyricIndexAt(10_000, 5_000) { lookups++; it.toLong() })
        assertTrue(lookups <= 15)
    }
}
