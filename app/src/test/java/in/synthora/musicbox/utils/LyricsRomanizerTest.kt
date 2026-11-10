package `in`.synthora.musicbox.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LyricsRomanizerTest {
    @Test fun preservesEverySupportedTimestampAndOffsetsWhileRomanizingOnlyWords() = runBlocking {
        val source = "\uFEFF[0:01]नमस्ते\n[00:02.5][101:03,250]नमस्ते\n[offset:-500]\n[ar:कलाकार]\n[00:04]\n[00:05.050]Already Latin!"
        val received = mutableListOf<String>()
        val result = LyricsRomanizer.romanizeSyncedLyricsUsing(source) {
            received.add(it)
            "Namaste"
        }
        assertEquals(listOf("नमस्ते", "नमस्ते"), received)
        assertEquals(parseLrc(source).map { it.timeMs }, parseLrc(result).map { it.timeMs })
        assertTrue(result.startsWith("\uFEFF[0:01]Namaste"))
        assertTrue(result.contains("[00:02.5][101:03,250]Namaste"))
        assertTrue(result.contains("[offset:-500]\n[ar:कलाकार]\n[00:04]"))
        assertTrue(result.endsWith("[00:05.050]Already Latin!"))
        assertTrue(LyricsRomanizer.isValidSyncedRomanization(source, result))
    }

    @Test fun offlineLinesKeepTimingAndCannotBeSavedAsFinishedRomanization() = runBlocking {
        val source = "[0:01]नमस्ते\n[00:02,50]Hello"
        val result = LyricsRomanizer.romanizeSyncedLyricsUsing(source) { null }
        assertEquals(source, result)
        assertFalse(LyricsRomanizer.isValidSyncedRomanization(source, result))
    }

    @Test fun repairsPreviouslyCachedPlainPartialOrWrongOffsetResults() {
        val source = "[offset:500]\n[0:01][0:02]नमस्ते"
        listOf("Namaste", "[0:01]Namaste", "[offset:0]\n[0:01][0:02]Namaste").forEach {
            assertFalse(LyricsRomanizer.isValidSyncedRomanization(source, it))
        }
        assertTrue(LyricsRomanizer.isValidSyncedRomanization(source,
            "[offset:500]\n[0:01][0:02]Namaste"))
    }

    @Test fun serviceLineBreaksCannotDetachTextFromItsTimestamp() = runBlocking {
        val result = LyricsRomanizer.romanizeSyncedLyricsUsing("[00:01]नमस्ते") { "Na\r\nmaste" }
        assertEquals("[00:01]Na maste", result)
        assertTrue(hasTimedLyrics(result))
    }
}
