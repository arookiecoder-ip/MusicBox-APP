package `in`.synthora.musicbox.services

import android.os.Looper
import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE)
@androidx.media3.common.util.UnstableApi
class JamSessionPlayerTest {
    private fun player(calls:MutableList<String>):Player=Proxy.newProxyInstance(Player::class.java.classLoader,arrayOf(Player::class.java)){_,method,_->
        calls+=method.name
        when(method.name){
            "getApplicationLooper"->Looper.getMainLooper()
            "getCurrentMediaItemIndex"->0
            "getCurrentPosition"->1000L
            "getSeekBackIncrement","getSeekForwardIncrement"->5000L
            "getAvailableCommands"->Player.Commands.EMPTY
            else->when(method.returnType){java.lang.Boolean.TYPE->false;java.lang.Integer.TYPE->0;java.lang.Long.TYPE->0L;else->null}
        }
    } as Player
    @Test fun notificationCommandsUseTheirRealIntentWithoutMutatingTheLocalRenderer() {
        val calls=mutableListOf<String>();val room=mutableListOf<Pair<String,Long?>>()
        val wrapped=JamSessionPlayer(player(calls)){action,value->room+=action to value;true}
        wrapped.play();wrapped.pause();wrapped.setPlayWhenReady(true);wrapped.seekToNext();wrapped.seekToPrevious();wrapped.seekTo(42000)
        assertEquals(listOf("play","pause","play","next","previous","seek"),room.map{it.first})
        assertEquals(42000L,room.last().second)
        assertFalse(calls.any{it in setOf("play","pause","setPlayWhenReady","seekToNext","seekToPrevious","seekTo")})
    }
    @Test fun applicationRendererCommandsStillReachTheRealPlayer() {
        val calls=mutableListOf<String>();val wrapped=JamSessionPlayer(player(calls)){_,_->false}
        wrapped.play();wrapped.pause();wrapped.seekTo(10000)
        assertTrue(calls.containsAll(listOf("play","pause","seekTo")))
    }
}
