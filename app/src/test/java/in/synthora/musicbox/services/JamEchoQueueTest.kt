package `in`.synthora.musicbox.services

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import `in`.synthora.musicbox.models.Track
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE,sdk=[28])
@OptIn(ExperimentalCoroutinesApi::class)
class JamEchoQueueTest {
    @Test fun selectedEchoQueueNeverContainsPhoneOwnershipCredentials()=runTest {
        val writes=mutableListOf<Pair<String,JsonObject>>()
        val prefs=RuntimeEnvironment.getApplication().getSharedPreferences("jam-echo",Context.MODE_PRIVATE)
        prefs.edit().clear().putString("echo_serial","echo").commit()
        val controller=EchoController(backgroundScope,prefs,{}, {},read={path,_->
            if(path=="/api/app/output/")buildJsonObject {put("playback_output","alexa");put("output_serial","echo");put("output_token","echo-token")}
            else buildJsonObject {put("video_id","aaaaaaaaaaa");put("playing",true);put("playback_confirmed",true)}
        },write={path,body->writes+=path to body;buildJsonObject {}})
        try {
            controller.refresh(force=true,stateOnly=true)
            val track=Track("entry","Playing","Artist",durationSec=180,ytVideoId="aaaaaaaaaaa")
            controller.followJamQueue(listOf(track),0,1000,true,true)
            assertEquals(1,writes.size)
            val (path,body)=writes.single()
            assertEquals("/api/app/queue/",path);assertFalse(body.containsKey("output_owner"))
            assertEquals("echo",body["alexa_serial"]!!.jsonPrimitive.content)
            assertEquals("echo-token",body["output_token"]!!.jsonPrimitive.content)
            assertTrue(body["playing"]!!.jsonPrimitive.boolean)
        }finally{controller.clear()}
    }
}
