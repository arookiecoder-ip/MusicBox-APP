package `in`.synthora.musicbox.network
import org.junit.Assert.*
import org.junit.Test

class AudioLanguageTest {
    @Test fun languageSelectionSurvivesRecoveryWithoutBecomingADefaultForOtherTracks() {
        assertEquals("en-US.10", selectedAudioLanguage("https://example.test/audio/?video_id=M8ICGx4p0lA&audio_track=en-US.10"))
        assertEquals("hi.4", selectedAudioLanguage("https://example.test/audio/?audio_track=hi.4&video_id=M8ICGx4p0lA"))
        assertNull(selectedAudioLanguage("https://example.test/audio/?video_id=abcdefghijk"))
    }
    @Test fun localOpaqueMalformedAndDuplicateSelectionsCannotBecomeRecoveryParameters() {
        for (source in listOf(null, "file:///music/track.m4a", "data:audio_track=en.4", "https://example.test/?audio_track=../bad", "https://example.test/?audio_track=en.4&audio_track=hi.4", "https://example.test/?audio_track="))
            assertNull(source, selectedAudioLanguage(source))
    }
}
