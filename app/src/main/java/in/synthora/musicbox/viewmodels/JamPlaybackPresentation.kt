package `in`.synthora.musicbox.viewmodels

import `in`.synthora.musicbox.services.EchoState
import `in`.synthora.musicbox.services.PlaybackPhase

/** Every member sees the room's verified renderer state, including control-only members. */
internal fun presentJamPlayback(personal: MusicUiState, room: EchoState, listen: Boolean): MusicUiState {
    val loading=room.loading
    val playing=room.playing && room.confirmed && !room.processing
    return personal.copy(isPlaying=playing,isLoading=loading,position=room.positionMs,error=null,
        phase=when {
            loading -> PlaybackPhase.BUFFERING
            playing -> PlaybackPhase.PLAYING
            else -> PlaybackPhase.PAUSED
        })
}
