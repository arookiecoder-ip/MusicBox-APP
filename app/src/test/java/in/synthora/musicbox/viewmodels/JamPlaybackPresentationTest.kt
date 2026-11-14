package `in`.synthora.musicbox.viewmodels

import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.services.EchoState
import `in`.synthora.musicbox.services.PlaybackPhase
import org.junit.Assert.*
import org.junit.Test

class JamPlaybackPresentationTest {
    private val track=Track("entry","Song","Artist",durationSec=180,ytVideoId="aaaaaaaaaaa")
    @Test fun bothHandoffDirectionsShowLoadingAndUseTheRendererCursor() {
        val room=EchoState(track=track,playing=true,positionMs=65_000)
        for(direction in listOf("phone to Alexa","Alexa to phone")) {
            val transferring=MusicUiState(currentTrack=track,isPlaying=false,isLoading=true,position=50_000,phase=PlaybackPhase.SWITCHING)
            val pending=presentJamPlayback(transferring,room,true)
            assertTrue(direction,pending.isLoading);assertFalse(pending.isPlaying)
            assertEquals(PlaybackPhase.SWITCHING,pending.phase);assertEquals(50_000,pending.position)
            val stillPending=presentJamPlayback(transferring,room.copy(positionMs=75_000),true)
            assertEquals("Room time must not advance an inaudible output",pending.position,stillPending.position)
            val ready=presentJamPlayback(transferring.copy(isLoading=false,isPlaying=true,position=51_000,phase=PlaybackPhase.PLAYING),room,true)
            assertFalse(ready.isLoading);assertTrue(ready.isPlaying);assertEquals(51_000,ready.position)
        }
    }
    @Test fun stoppedOrBufferingDeviceDoesNotPretendTheRoomIsAudible() {
        val room=EchoState(track=track,playing=true,positionMs=90_000)
        val stopped=MusicUiState(currentTrack=track,position=12_000)
        assertFalse(presentJamPlayback(stopped,room,true).isPlaying)
        val buffering=presentJamPlayback(stopped.copy(isLoading=true),room,true)
        assertTrue(buffering.isLoading);assertEquals(12_000,buffering.position)
        val controller=presentJamPlayback(stopped,room,false)
        assertTrue(controller.isPlaying);assertFalse(controller.isLoading);assertEquals(90_000,controller.position)
    }
    @Test fun newSongWaitsForMatchingOutputAndFailuresStayVisible() {
        val room=EchoState(track=track,playing=true,positionMs=0)
        val old=MusicUiState(currentTrack=track.copy(ytVideoId="bbbbbbbbbbb"),isPlaying=true)
        assertTrue(presentJamPlayback(old,room,true).isLoading)
        assertFalse(presentJamPlayback(old,room,true).isPlaying)
        val failed=presentJamPlayback(old.copy(error="Echo unavailable"),room,true)
        assertFalse(failed.isLoading);assertEquals(PlaybackPhase.FAILED,failed.phase)
    }
}
