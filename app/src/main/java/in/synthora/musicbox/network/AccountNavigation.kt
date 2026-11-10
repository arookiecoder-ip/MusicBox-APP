package `in`.synthora.musicbox.network

import java.net.URI

/** Only Amazon verification subframes may leave the proxied account origin. */
internal fun amazonVerificationFrame(url: String): Boolean = runCatching {
    val uri=URI(url)
    val host=uri.host?.lowercase() ?: return@runCatching false
    val regions=setOf("amazon.com","amazon.in","amazon.co.uk","amazon.de","amazon.fr","amazon.it","amazon.es","amazon.ca","amazon.co.jp","amazon.com.au","amazon.com.br","amazon.com.mx","amazon.nl","amazon.pl","amazon.se","amazon.sg","amazon.ae","amazon.sa","amazon.com.tr","amazon.eg")
    uri.scheme.equals("https",ignoreCase=true) && uri.userInfo == null && uri.port in listOf(-1,443) &&
        regions.any { host==it || host.endsWith(".$it") }
}.getOrDefault(false)
