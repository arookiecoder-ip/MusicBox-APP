package `in`.synthora.musicbox.services
import `in`.synthora.musicbox.models.Track
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class EchoLyricsOffsetTest {
    private fun snapshot(entry:String="first",position:Long=4000)=parseEchoSnapshot(buildJsonObject {
        put("video_id","abcdefghijk");put("queue_index",0);put("position_ms",position);put("duration_ms",10000);put("output_epoch","output");put("output_mode","alexa")
        put("queue",buildJsonArray{add(buildJsonObject{put("video_id","abcdefghijk");put("entry_id",entry)})})
    },0,null,false)
    @Test fun temporaryCorrectionIsKeptForOneEntryAndResetsForAnotherOrRepeatedSong() {
        val previous=snapshot().copy(lyricsControlOffsetMs=-1500)
        assertEquals(-1500L,echoLyricsOffset(previous,previous.track!!.uuid,5000,"output"))
        assertEquals(0L,echoLyricsOffset(previous,"different",5000,"output"))
        assertEquals(0L,echoLyricsOffset(previous,previous.track!!.uuid,5000,"other-output"))
        assertEquals(0L,echoLyricsOffset(previous.copy(positionMs=9800),previous.track!!.uuid,100,"output"))
        assertEquals(-1500L,echoLyricsOffset(previous.copy(positionMs=100),previous.track!!.uuid,100,"output"))
    }
}
