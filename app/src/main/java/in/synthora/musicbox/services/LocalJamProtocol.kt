package `in`.synthora.musicbox.services

import kotlinx.serialization.json.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Local packets can report timing only; they never carry transport or queue commands. */
internal object LocalJamProtocol {
    private fun signature(key:String,body:String):ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key.toByteArray(Charsets.UTF_8),"HmacSHA256"));doFinal(body.toByteArray(Charsets.UTF_8))
    }
    fun encode(key:String,body:JsonObject):ByteArray {
        val raw=body.toString()
        return buildJsonObject {put("body",raw);put("mac",java.util.Base64.getEncoder().encodeToString(signature(key,raw)))}.toString().toByteArray(Charsets.UTF_8)
    }
    fun decode(key:String,bytes:ByteArray):JsonObject? = runCatching {
        if(bytes.size !in 1..2048)return null
        val envelope=Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
        val body=envelope["body"]!!.jsonPrimitive.content
        val mac=java.util.Base64.getDecoder().decode(envelope["mac"]!!.jsonPrimitive.content)
        if(!MessageDigest.isEqual(signature(key,body),mac))return null
        Json.parseToJsonElement(body).jsonObject
    }.getOrNull()
    fun write(output:DataOutputStream,key:String,body:JsonObject) {
        val bytes=encode(key,body);require(bytes.size<=2048)
        output.writeInt(bytes.size);output.write(bytes);output.flush()
    }
    fun read(input:DataInputStream,key:String):JsonObject? {
        val size=input.readInt();if(size !in 1..2048)return null
        return decode(key,ByteArray(size).also(input::readFully))
    }
    fun matches(packet:JsonObject,kind:String,room:String,intent:String,nonce:String,sender:String,target:String):Boolean =
        packet["kind"]?.jsonPrimitive?.contentOrNull==kind && packet["room"]?.jsonPrimitive?.contentOrNull==room &&
            packet["intent"]?.jsonPrimitive?.contentOrNull==intent && packet["nonce"]?.jsonPrimitive?.contentOrNull==nonce &&
            packet["sender"]?.jsonPrimitive?.contentOrNull==sender && packet["target"]?.jsonPrimitive?.contentOrNull==target &&
            packet["position"]?.jsonPrimitive?.longOrNull?.let{it>=0}==true

    /** Ignore jitter and large mismatches; converge gradually without a large audible seek. */
    fun correction(local:Long,peer:Long,rtt:Long,duration:Long):Long? {
        if(rtt !in 0..200 || local<0 || peer<0)return null
        val target=peer+rtt/2
        val drift=target-local
        if(kotlin.math.abs(drift) !in 120..2000)return null
        val corrected=(local+drift.coerceIn(-250,250)).coerceAtLeast(0)
        if(duration>0 && corrected>=duration-500)return null
        return corrected
    }
    fun peerAddress(local:java.net.InetAddress,addresses:List<java.net.InetAddress>,prefix:Int):java.net.InetAddress? =
        addresses.firstOrNull{sameSubnet(local,it,prefix)}

    fun sameSubnet(local:java.net.InetAddress,peer:java.net.InetAddress,prefix:Int):Boolean {
        if(peer.isLoopbackAddress || peer.isAnyLocalAddress || peer.isMulticastAddress)return false
        val a=local.address;val b=peer.address
        if(a.size!=b.size || prefix !in 1..a.size*8)return false
        for(bit in 0 until prefix) {
            val mask=1 shl (7-bit%8)
            if((a[bit/8].toInt() and mask)!=(b[bit/8].toInt() and mask))return false
        }
        return true
    }
}
