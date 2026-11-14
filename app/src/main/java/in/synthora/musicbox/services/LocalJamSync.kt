package `in`.synthora.musicbox.services

import android.content.Context
import android.net.*
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.net.*
import java.util.UUID
import `in`.synthora.musicbox.network.*

internal data class LocalJamContext(val room:String,val key:String,val own:String,val leader:String,
    val peers:Set<String>,val intent:String,val video:String,val entry:String,val seek:Long,val receivedAt:Long)

/** Best-effort Wi-Fi timing aid. Server state is required before every probe and correction. */
internal class LocalJamSync(context:Context,private val scope:CoroutineScope,
    private val sample:()->JsonObject,private val correct:(Long)->Unit) {
    private val connectivity=context.getSystemService(ConnectivityManager::class.java)
    private val nsd=context.getSystemService(NsdManager::class.java)
    @Volatile private var desired:LocalJamContext?=null
    @Volatile private var current:Session?=null
    @Volatile private var retryAt=0L
    private val callback=object:ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network:Network){scope.launch{reconcile()}}
        override fun onLost(network:Network){scope.launch{reconcile()}}
        override fun onCapabilitiesChanged(network:Network,capabilities:NetworkCapabilities){scope.launch{reconcile()}}
    }
    init {runCatching{connectivity.registerDefaultNetworkCallback(callback)}}
    fun update(value:LocalJamContext?) {desired=value;reconcile()}
    private fun reconcile() {
        val value=desired
        val network=connectivity.activeNetwork
        val wifi=network?.let{connectivity.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}==true
        val address=if(wifi)network?.let{connectivity.getLinkProperties(it)}?.linkAddresses?.firstOrNull {
            !it.address.isLoopbackAddress && !it.address.isLinkLocalAddress && it.address is Inet4Address
        } else null
        val old=current
        if(value==null || network==null || address==null){current=null;old?.close();return}
        if(old!=null && old.network==network && old.address==address && old.context.room==value.room &&
            old.context.key==value.key && old.context.leader==value.leader && old.context.peers==value.peers)return
        current=null;old?.close()
        if(SystemClock.elapsedRealtime()<retryAt)return
        Session(value,network,address).also{current=it;it.start()}
    }
    private inner class Session(val context:LocalJamContext,val network:Network,val address:LinkAddress) {
        @Volatile var server:ServerSocket?=null
        @Volatile var peer:InetSocketAddress?=null
        private var listener:Job?=null
        private var probing:Job?=null
        @Volatile private var registered=false
        @Volatile private var discovering=false
        private fun live()=current===this
        private fun name(id:String)="mb-${context.room.take(8)}-${id.take(8)}"
        private val registration=object:NsdManager.RegistrationListener {
            override fun onServiceRegistered(info:NsdServiceInfo){}
            override fun onRegistrationFailed(info:NsdServiceInfo,errorCode:Int){}
            override fun onServiceUnregistered(info:NsdServiceInfo){}
            override fun onUnregistrationFailed(info:NsdServiceInfo,errorCode:Int){}
        }
        private val discovery=object:NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type:String){}
            override fun onDiscoveryStopped(type:String){}
            override fun onStartDiscoveryFailed(type:String,errorCode:Int){}
            override fun onStopDiscoveryFailed(type:String,errorCode:Int){}
            override fun onServiceLost(info:NsdServiceInfo){if(info.serviceName.startsWith(name(context.leader)))peer=null}
            @Suppress("DEPRECATION")
            override fun onServiceFound(info:NsdServiceInfo) {
                if(!live() || context.own==context.leader || !info.serviceName.startsWith(name(context.leader)))return
                runCatching{nsd.resolveService(info,object:NsdManager.ResolveListener {
                    override fun onResolveFailed(service:NsdServiceInfo,errorCode:Int){}
                    override fun onServiceResolved(service:NsdServiceInfo) {
                        val id=service.attributes["peer"]?.toString(Charsets.UTF_8)
                        val room=service.attributes["room"]?.toString(Charsets.UTF_8)
                        val host=service.host ?: return
                        if(live() && id==context.leader && room==context.room && service.port in 1..65535 &&
                            LocalJamProtocol.sameSubnet(address.address,host,address.prefixLength))peer=InetSocketAddress(host,service.port)
                    }
                })}
            }
        }
        fun start() {
            listener=scope.launch(Dispatchers.IO) {
                try {
                    val socket=ServerSocket().apply{reuseAddress=true;bind(InetSocketAddress(address.address,0));soTimeout=1000}
                    server=socket
                    if(!live()){socket.close();return@launch}
                    val info=NsdServiceInfo().apply{serviceName=name(context.own);serviceType="_mbjam._tcp.";port=socket.localPort
                        setAttribute("room",context.room);setAttribute("peer",context.own)}
                    runCatching{nsd.registerService(info,NsdManager.PROTOCOL_DNS_SD,registration);registered=true}
                    runCatching{nsd.discoverServices("_mbjam._tcp.",NsdManager.PROTOCOL_DNS_SD,discovery);discovering=true}
                    if(!live()){close();return@launch}
                    probing=scope.launch(Dispatchers.IO){while(isActive && live()){runCatching{probe()};delay(1000)}}
                    while(isActive && live()) {
                        try {socket.accept().use{connection->
                            if(LocalJamProtocol.sameSubnet(address.address,connection.inetAddress,address.prefixLength)) {
                                connection.soTimeout=250
                                respond(connection)
                            }
                        }}catch(_:SocketTimeoutException){}catch(_:java.io.IOException){if(!live())break}
                    }
                } catch(_:Exception) { /* LAN permission/discovery failures never affect playback. */ }
                finally {close()}
            }
        }
        fun close() {
            if(current===this){current=null;retryAt=SystemClock.elapsedRealtime()+5000}
            runCatching{server?.close()};probing?.cancel();listener?.cancel()
            if(registered){registered=false;runCatching{nsd.unregisterService(registration)}}
            if(discovering){discovering=false;runCatching{nsd.stopServiceDiscovery(discovery)}}
        }
        private fun verified():LocalJamContext? = desired?.takeIf {
            live() && it.room==context.room && it.key==context.key && it.peers==context.peers &&
                SystemClock.elapsedRealtime()-it.receivedAt in 0..3000
        }
        private suspend fun actual(frame:LocalJamContext):JsonObject?=withContext(Dispatchers.Main.immediate) {
            sample().takeIf{verified()?.intent==frame.intent && it.text("target_id")=="phone" && it.text("video_id")==frame.video && it.text("entry_id")==frame.entry &&
                it.number("seek_revision")==frame.seek && it.flag("playing") && it.flag("confirmed") && !it.flag("processing")}
        }
        private suspend fun respond(socket:Socket) {
            val frame=verified() ?: return
            if(frame.own!=frame.leader)return
            val request=LocalJamProtocol.read(java.io.DataInputStream(socket.getInputStream()),frame.key) ?: return
            if(request.text("kind")!="probe" || request.text("room")!=frame.room || request.text("intent")!=frame.intent ||
                request.text("sender") !in frame.peers || request.text("sender")==frame.own ||
                !request.text("nonce").matches(Regex("[a-f0-9-]{36}")))return
            val status=actual(frame) ?: return
            LocalJamProtocol.write(java.io.DataOutputStream(socket.getOutputStream()),frame.key,buildJsonObject {
                put("kind","sample");put("room",frame.room);put("intent",frame.intent);put("nonce",request.text("nonce"))
                put("sender",frame.own);put("target",request.text("sender"));put("position",status.number("position_ms"))
            })
        }
        private suspend fun probe() {
            val frame=verified() ?: return
            if(frame.own==frame.leader || actual(frame)==null)return
            val destination=peer ?: return
            val nonce=UUID.randomUUID().toString()
            Socket().use{socket->
                network.bindSocket(socket);socket.soTimeout=250;socket.connect(destination,250)
                val started=SystemClock.elapsedRealtime()
                LocalJamProtocol.write(java.io.DataOutputStream(socket.getOutputStream()),frame.key,buildJsonObject {
                    put("kind","probe");put("room",frame.room);put("intent",frame.intent);put("nonce",nonce);put("sender",frame.own)
                })
                val response=LocalJamProtocol.read(java.io.DataInputStream(socket.getInputStream()),frame.key) ?: return
                val rtt=SystemClock.elapsedRealtime()-started
                if(!LocalJamProtocol.matches(response,"sample",frame.room,frame.intent,nonce,frame.leader,frame.own))return
                withContext(Dispatchers.Main.immediate) {
                    if(verified()?.intent!=frame.intent)return@withContext
                    val status=actual(frame) ?: return@withContext
                    LocalJamProtocol.correction(status.number("position_ms"),response.number("position"),rtt,status.number("duration_ms"))?.let(correct)
                }
            }
        }
    }
}
