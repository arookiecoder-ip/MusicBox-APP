package `in`.synthora.musicbox.utils

import org.junit.Assert.*
import org.junit.Test

class LrcParserTest {
    @Test fun wholeSecondsRepeatedTimestampsAndFractionVariantsShareOneClock() {
        val lines=parseLrc("[0:01]First\n[00:02.5][01:03,250]Repeated\n[ar:metadata]\n[00:99.0]Invalid")
        assertEquals(listOf(1000L,2500L,63250L),lines.map { it.timeMs })
        assertEquals(listOf("First","Repeated","Repeated"),lines.map { it.text })
    }
    @Test fun embeddedOffsetAndUserOffsetAreAppliedOnceAndSorted() {
        val lines=parseLrc("[offset:-500]\n[00:03]Later\n[00:01.1]Earlier",200)
        assertEquals(listOf(800L,2700L),lines.map { it.timeMs })
    }
    @Test fun malformedPlainAndBlankSourcesNeverPretendToBeTimed() {
        listOf(null,"","Plain words","[00:99]Wrong seconds","[ar:Artist]","[00:01]").forEach { assertFalse(hasTimedLyrics(it)) }
        assertTrue(hasTimedLyrics("\uFEFF[0:01]Real timed line"))
    }
    @Test fun extremeOffsetsCannotOverflowIntoNegativePlaybackTimes() {
        assertEquals(0L,parseLrc("[00:01]Line",Long.MIN_VALUE).single().timeMs)
        assertEquals(Long.MAX_VALUE,parseLrc("[00:01]Line",Long.MAX_VALUE).single().timeMs)
    }
}
