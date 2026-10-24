package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.network.*
import org.junit.Assert.*
import org.junit.Test

class PausedEchoHandoffTest {
    @Test fun playingSourceCarriesTheExactStoppedCursorAndOwnership() {
        val request = pausedEchoHandoff("phone", "lease", "echo", "video", "entry", 12500, true)
        assertEquals("alexa_handoff", request.text("action"))
        assertEquals("lease", request.text("output_token"))
        assertEquals("entry", request.text("current_entry_id"))
        assertEquals(12500L, request.number("position_ms"))
        assertTrue(request.flag("source_paused"))
        assertTrue(request.flag("playing"))
    }
    @Test fun pausedTrackCannotBecomeAnAudibleResume() {
        val request = pausedEchoHandoff("phone", "lease", "echo", "video", "entry", -1, false)
        assertFalse(request.flag("playing"))
        assertEquals(0L, request.number("position_ms"))
    }
}
