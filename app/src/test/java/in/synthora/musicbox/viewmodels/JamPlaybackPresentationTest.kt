package `in`.synthora.musicbox.viewmodels

import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.services.EchoState
import `in`.synthora.musicbox.services.PlaybackPhase
import org.junit.Assert.*
import org.junit.Test

class JamPlaybackPresentationTest {
    private val track=Track("entry","Song","Artist",durationSec=180,ytVideoId="aaaaaaaaaaa")
    @Test fun allMembersShowBufferingAndTheSameFrozenCursorUntilAudioConfirms() {
        val room=EchoState(track=track,playing=true,confirmed=false,processing=true,positionMs=50_000)
        val host=MusicUiState(currentTrack=track,isPlaying=false,isLoading=true,position=50_000)
        val guest=MusicUiState(currentTrack=track.copy(ytVideoId="bbbbbbbbbbb"),isPlaying=true,position=90_000)
        for(local in listOf(host,guest))for(listen in listOf(true,false)) {
            val shown=presentJamPlayback(local,room,listen)
            assertTrue(shown.isLoading);assertFalse(shown.isPlaying)
            assertEquals(PlaybackPhase.BUFFERING,shown.phase);assertEquals(50_000L,shown.position)
            val ready=presentJamPlayback(local,room.copy(positionMs=51_000,confirmed=true,processing=false),listen)
            assertFalse(ready.isLoading);assertTrue(ready.isPlaying);assertEquals(51_000L,ready.position)
        }
    }
    @Test fun multipleOutputsKeepOneControllerWhenOneMembersLocalPhoneBuffers() {
        val room=EchoState(track=track,playing=true,confirmed=true,positionMs=90_000)
        val local=MusicUiState(currentTrack=track,isLoading=true,position=12_000,error="This phone is buffering")
        for(listen in listOf(true,false)) {
            val shown=presentJamPlayback(local,room,listen)
            assertTrue(shown.isPlaying);assertFalse(shown.isLoading);assertNull(shown.error)
            assertEquals(90_000L,shown.position)
        }
    }
    @Test fun pausedRoomNeverUsesAnUnrelatedPersonalPlaybackClock() {
        val room=EchoState(track=track,playing=false,confirmed=true,positionMs=30_000)
        val shown=presentJamPlayback(MusicUiState(isPlaying=true,position=120_000),room,false)
        assertFalse(shown.isPlaying);assertFalse(shown.isLoading)
        assertEquals(PlaybackPhase.PAUSED,shown.phase);assertEquals(30_000L,shown.position)
    }
}
