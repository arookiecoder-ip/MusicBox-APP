package `in`.synthora.musicbox.services

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/** Session commands retain their real intent and never mutate the local renderer first. */
@UnstableApi
internal class JamSessionPlayer(player:Player,private val route:(String,Long?)->Boolean):ForwardingPlayer(player) {
    override fun play(){if(!route("play",null))super.play()}
    override fun pause(){if(!route("pause",null))super.pause()}
    override fun stop(){if(!route("pause",null))super.stop()}
    override fun setPlayWhenReady(value:Boolean){if(value)play() else pause()}
    override fun seekToNext(){if(!route("next",null))super.seekToNext()}
    override fun seekToNextMediaItem(){if(!route("next",null))super.seekToNextMediaItem()}
    override fun seekToPrevious(){if(!route("previous",null))super.seekToPrevious()}
    override fun seekToPreviousMediaItem(){if(!route("previous",null))super.seekToPreviousMediaItem()}
    override fun seekTo(positionMs:Long){if(!route("seek",positionMs.coerceAtLeast(0)))super.seekTo(positionMs)}
    override fun seekTo(index:Int,positionMs:Long){
        if(index==currentMediaItemIndex){seekTo(positionMs);return}
        if(!route("select",index.toLong()))super.seekTo(index,positionMs)
    }
    override fun seekBack(){seekTo((currentPosition-seekBackIncrement).coerceAtLeast(0))}
    override fun seekForward(){seekTo(currentPosition+seekForwardIncrement)}
    override fun setRepeatMode(value:Int){if(!route("repeat",null))super.setRepeatMode(value)}
    override fun setShuffleModeEnabled(value:Boolean){if(!route("shuffle",null))super.setShuffleModeEnabled(value)}
}
