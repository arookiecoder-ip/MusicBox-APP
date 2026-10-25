package `in`.synthora.musicbox.ui.screens

import `in`.synthora.musicbox.ui.components.stableNavigationBarsPadding

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.synthora.musicbox.network.AlexaRemotePolicy
import `in`.synthora.musicbox.network.Backend
import `in`.synthora.musicbox.ui.components.GlassTopAppBar
import `in`.synthora.musicbox.ui.theme.GlassBackdrop
import `in`.synthora.musicbox.ui.theme.isGlassDark

/**
 * Vendor sign-in only (Amazon's login proxy and the server's YouTube sign-in browser). It opens
 * with the app's web session and can't navigate away from those pages.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AccountBrowser(url: String, onClose: () -> Unit) {
    val server = Backend.baseUrl
    var browser by remember { mutableStateOf<WebView?>(null) }
    val manager = CookieManager.getInstance()
    val closeScope = rememberCoroutineScope()
    fun requestClose() {
        val web = browser
        if (web == null) { onClose(); return }
        // Let the leased page revoke its browser before destroying the WebView.
        // A failed/offline page must never trap the Android back/close action.
        closeScope.launch { delay(2_000); onClose() }
        web.evaluateJavascript("(function(){const cancel=document.getElementById('cancel');if(cancel){cancel.click();return true;}return false;})()") { handled ->
            if (handled != "true") onClose()
        }
    }
    fun clearCookies() {
        listOf(server, "$server/youtube-login/").forEach { scope ->
            manager.getCookie(scope).orEmpty().split(";").forEach { part ->
                val name = part.trim().substringBefore('=')
                if (name.isNotBlank()) {
                    manager.setCookie(scope, "$name=; Max-Age=0; Path=/; Secure")
                    manager.setCookie(scope, "$name=; Max-Age=0; Path=/youtube-login/; Secure")
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { browser?.stopLoading(); browser?.destroy(); clearCookies() } }
    Dialog(onDismissRequest = ::requestClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(GlassBackdrop.color(isGlassDark())).stableNavigationBarsPadding().imePadding()) {
            GlassTopAppBar(
                title = { Text("Account sign-in") },
                navigationIcon = {
                    IconButton(onClick = ::requestClose) { Icon(Icons.Default.Close, "Close") }
                }
            )
            AndroidView(modifier = Modifier.weight(1f), factory = { context ->
                WebView(context).apply {
                    browser = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(false)
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    manager.setAcceptCookie(true)
                    clearCookies()
                    val authCookies = Backend.cookieHeaders()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val target = request.url.toString()
                            // Amazon's own pages are proxied through the server, so every page is on it.
                            if (!AlexaRemotePolicy.trusted(server, target)) return true
                            val prefix = java.net.URI(server).path.orEmpty().trimEnd('/')
                            val path = request.url.path.orEmpty().removePrefix(prefix)
                            val allowed = path.startsWith("/alexa/proxy/") || path.startsWith("/youtube-login/") || path == "/accounts/amazon" || path == "/accounts/youtube"
                            // Leaving the sign-in pages means it finished.
                            if (!allowed && request.isForMainFrame) onClose()
                            return !allowed
                        }
                    }
                    fun installCookie(index: Int) {
                        if (index >= authCookies.size) {
                            manager.flush()
                            loadUrl(url)
                        } else manager.setCookie(server, authCookies[index]) { installCookie(index + 1) }
                    }
                    installCookie(0)
                }
            })
        }
    }
}
