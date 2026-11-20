package `in`.synthora.musicbox.network

import java.net.URI
import java.net.URLDecoder

internal data class AppLink(val route:String,val id:String="")
internal fun appLink(input:String,base:String):AppLink? = runCatching {
    val uri=URI(input.trim());val server=URI(base)
    if(uri.scheme !in setOf("https","http") || uri.userInfo!=null)return null
    val host=uri.host?.lowercase() ?: return null
    val own=uri.scheme=="https" && host==server.host?.lowercase() && (if(uri.port==-1)443 else uri.port)==(if(server.port==-1)443 else server.port)
    if(!own && host !in setOf("music.youtube.com","youtube.com","www.youtube.com","m.youtube.com","youtu.be","www.youtu.be"))return null
    val parts=uri.path.orEmpty().trim('/').split('/').filter(String::isNotEmpty)
    fun valid(id:String?)=id?.takeIf{it.matches(Regex("[A-Za-z0-9_-]{2,200}"))}
    if(own && parts.firstOrNull() in setOf("j","jam","jam-invite") && parts.size==2)return valid(parts[1])?.let{AppLink("jam",it)}
    youtubeLink(if(own)"https://music.youtube.com"+uri.rawPath+(uri.rawQuery?.let{"?$it"} ?: "") else input)?.let {
        return if(it.videoId!=null)AppLink("song",it.videoId) else AppLink("playlist",it.playlistId.orEmpty())
    }
    if(parts.size==2) {
        val id=valid(parts[1]) ?: return null
        return when(parts[0]) {
            "channel","artist" -> AppLink("artist",id)
            "browse" -> AppLink(if(id.startsWith("UC"))"artist" else if(id.startsWith("VL"))"playlist" else "album",id.removePrefix("VL"))
            "album","playlist" -> AppLink(parts[0],id)
            else -> if(own)AppLink("web",input) else null
        }
    }
    if(own)return when(parts.firstOrNull()) {
        null,"home","remote" -> AppLink("home")
        "search" -> AppLink("search",uri.rawQuery.orEmpty().split('&').firstOrNull{it.substringBefore('=')=="q"}?.substringAfter('=',"")?.let{URLDecoder.decode(it,"UTF-8")}.orEmpty())
        "account" -> AppLink("account")
        "settings" -> AppLink("settings")
        "login" -> AppLink("home")
        "signup" -> AppLink("signup")
        "forgot-password" -> AppLink("forgot")
        "jam-room" -> AppLink("jam-room")
        else -> AppLink("web",input)
    }
    null
}.getOrNull()
