package `in`.synthora.musicbox

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import `in`.synthora.musicbox.network.Backend
import org.junit.Assert.*
import org.junit.Test

class AppLinkIntentRegressionTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun androidResolvesMusicBoxAndYouTubeMusicTargetsToTheApp() {
        for(url in listOf(Backend.baseUrl+"/",Backend.baseUrl+"/settings",Backend.baseUrl+"/account",Backend.baseUrl+"/jam-invite/abcdefghijk",
            "https://music.youtube.com/watch?v=abcdefghijk","https://music.youtube.com/playlist?list=PLexample",
            "https://music.youtube.com/browse/MPREexample","https://music.youtube.com/channel/UCexample","https://youtu.be/abcdefghijk")) {
            val intent=Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(context.packageName)
            assertEquals(url,MainActivity::class.java.name,intent.resolveActivity(context.packageManager)?.className)
        }
        val unrelated=Intent(Intent.ACTION_VIEW,Uri.parse("https://unrelated.example/account")).setPackage(context.packageName)
        assertNull(unrelated.resolveActivity(context.packageManager))
    }
    @Test fun sharingLinksAndOpeningDefaultLinkSettingsAreSupported() {
        val share=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"https://music.youtube.com/watch?v=abcdefghijk").setPackage(context.packageName)
        assertEquals(MainActivity::class.java.name,share.resolveActivity(context.packageManager)?.className)
        val settings=Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,Uri.parse("package:"+context.packageName))
        assertNotNull(settings.resolveActivity(context.packageManager))
    }
}
