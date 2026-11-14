package `in`.synthora.musicbox.viewmodels

import `in`.synthora.musicbox.services.EchoState
import `in`.synthora.musicbox.services.PlaybackPhase

/** The room owns the queue; the selected renderer owns readiness and the audible cursor. */
internal fun presentJamPlayback(personal: MusicUiState, room: EchoState, listen: Boolean): MusicUiState {
    if (!listen) return personal.copy(isPlaying=room.playing, isLoading=false, position=room.positionMs,
        phase=if(room.playing) PlaybackPhase.PLAYING else PlaybackPhase.PAUSED)
    val matches=room.track?.ytVideoId != null && personal.currentTrack?.ytVideoId==room.track.ytVideoId
    val loading=personal.isLoading || (room.playing && !matches && personal.error==null)
    val playing=room.playing && matches && personal.isPlaying && !loading
    return personal.copy(isPlaying=playing, isLoading=loading,
        position=if(matches) personal.position else room.positionMs,
        phase=when {
            personal.phase==PlaybackPhase.SWITCHING -> PlaybackPhase.SWITCHING
            personal.error!=null -> PlaybackPhase.FAILED
            loading -> PlaybackPhase.BUFFERING
            playing -> PlaybackPhase.PLAYING
            else -> PlaybackPhase.PAUSED
        })
}
