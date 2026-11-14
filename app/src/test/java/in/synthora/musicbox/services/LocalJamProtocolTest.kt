package `in`.synthora.musicbox.services

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LocalJamProtocolTest {
    private val key="a".repeat(64)
    private fun sample()=buildJsonObject {
        put("kind","sample");put("room","room");put("intent","track:seek:revision");put("nonce","nonce")
        put("sender","leader");put("target","follower");put("position",1000)
    }
    @Test fun packetsAreAuthenticatedAndTamperingOrOtherRoomsKeysAreRejected() {
        val bytes=LocalJamProtocol.encode(key,sample())
        assertEquals(sample(),LocalJamProtocol.decode(key,bytes))
        assertNull(LocalJamProtocol.decode("b".repeat(64),bytes))
        assertNull(LocalJamProtocol.decode(key,bytes.toString(Charsets.UTF_8).replace("1000","9000").toByteArray()))
        assertNull(LocalJamProtocol.decode(key,ByteArray(4096)))
        assertNull(LocalJamProtocol.decode(key,"garbage".toByteArray()))
    }
    @Test fun replayedNonceWrongPeerTrackSeekOrTransportRevisionCannotCorrectPlayback() {
        val p=sample()
        fun matches(packet:JsonObject=p,room:String="room",intent:String="track:seek:revision",nonce:String="nonce",sender:String="leader",target:String="follower")=
            LocalJamProtocol.matches(packet,"sample",room,intent,nonce,sender,target)
        assertTrue(matches());assertFalse(matches(nonce="old"));assertFalse(matches(room="other"))
        assertFalse(matches(intent="newtrack:seek:revision"));assertFalse(matches(intent="track:newseek:revision"))
        assertFalse(matches(intent="track:seek:paused"));assertFalse(matches(sender="other"));assertFalse(matches(target="other"))
        assertFalse(matches(JsonObject(p-"position")));assertFalse(matches(JsonObject(p+mapOf("position" to JsonPrimitive(-1)))))
    }
    @Test fun driftCorrectionIsBoundedAndIgnoresJitterStallsAndTrackBoundaries() {
        assertNull(LocalJamProtocol.correction(1000,1050,20,100000))
        assertEquals(1250L,LocalJamProtocol.correction(1000,1500,20,100000))
        assertEquals(750L,LocalJamProtocol.correction(1000,500,20,100000))
        assertNull(LocalJamProtocol.correction(1000,1500,201,100000))
        assertNull(LocalJamProtocol.correction(1000,5000,20,100000))
        assertNull(LocalJamProtocol.correction(9700,10000,20,10000))
        assertNull(LocalJamProtocol.correction(-1,1000,20,10000))
    }
    @Test fun onlyAddressesOnTheCurrentWifiSubnetAreEligible() {
        val local=InetAddress.getByName("192.168.1.2")
        assertTrue(LocalJamProtocol.sameSubnet(local,InetAddress.getByName("192.168.1.99"),24))
        for(ip in listOf("192.168.2.2","8.8.8.8","127.0.0.1","224.0.0.1","0.0.0.0","::1"))
            assertFalse(ip,LocalJamProtocol.sameSubnet(local,InetAddress.getByName(ip),24))
    }
    @Test fun dualStackDiscoveryChoosesTheAddressOnTheCurrentWifiSubnet() {
        val local=InetAddress.getByName("192.168.1.2")
        val wifi=InetAddress.getByName("192.168.1.99")
        val addresses=listOf(InetAddress.getByName("2001:db8::1"),InetAddress.getByName("10.0.0.1"),wifi)
        assertEquals(wifi,LocalJamProtocol.peerAddress(local,addresses,24))
        assertNull(LocalJamProtocol.peerAddress(local,addresses.take(2),24))
    }
    @Test fun realSocketRoundTripAuthenticatesTimingAndRejectsOversizedFrames() {
        val server=ServerSocket(0,1,InetAddress.getLoopbackAddress())
        val executor=Executors.newSingleThreadExecutor()
        try {
            val response=executor.submit<Boolean> {server.accept().use{socket->
                socket.soTimeout=1000
                val request=LocalJamProtocol.read(DataInputStream(socket.getInputStream()),key)
                LocalJamProtocol.write(DataOutputStream(socket.getOutputStream()),key,sample())
                request?.get("nonce")?.jsonPrimitive?.content=="nonce"
            }}
            Socket(server.inetAddress,server.localPort).use{socket->
                socket.soTimeout=1000
                LocalJamProtocol.write(DataOutputStream(socket.getOutputStream()),key,buildJsonObject{put("nonce","nonce")})
                assertEquals(sample(),LocalJamProtocol.read(DataInputStream(socket.getInputStream()),key))
            }
            assertTrue(response.get(2,TimeUnit.SECONDS))
            val oversize=ByteArrayOutputStream().also{DataOutputStream(it).writeInt(Int.MAX_VALUE)}.toByteArray()
            assertNull(LocalJamProtocol.read(DataInputStream(ByteArrayInputStream(oversize)),key))
        } finally {server.close();executor.shutdownNow()}
    }
}
