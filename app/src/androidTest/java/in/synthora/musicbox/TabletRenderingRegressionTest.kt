package `in`.synthora.musicbox

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.network.BrowseParser
import `in`.synthora.musicbox.ui.components.FadedArtwork
import `in`.synthora.musicbox.ui.components.artworkBottomFade
import `in`.synthora.musicbox.ui.screens.HomeContent
import `in`.synthora.musicbox.ui.theme.JUKETheme
import `in`.synthora.musicbox.viewmodels.HomeShelf
import `in`.synthora.musicbox.viewmodels.HomeUiState
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Offline fixtures exercise production layout/renderers. Timing artifacts are diagnostic,
 * not a claim that emulator frame times equal a physical tablet's performance. */
class TabletRenderingRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun measure(name: String, action: () -> Unit) {
        val frames = Collections.synchronizedList(mutableListOf<List<Long>>())
        val thread = HandlerThread("frame-metrics").apply { start() }
        val handler = Handler(thread.looper)
        val listener = android.view.Window.OnFrameMetricsAvailableListener { _, frame, _ ->
            frames.add(listOf(frame.getMetric(FrameMetrics.TOTAL_DURATION), frame.getMetric(FrameMetrics.DRAW_DURATION),
                frame.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)))
        }
        instrumentation.runOnMainSync { compose.activity.window.addOnFrameMetricsAvailableListener(listener, handler) }
        try {
            action(); instrumentation.waitForIdleSync()
            val drained = CountDownLatch(1); handler.post { drained.countDown() }; drained.await(3, TimeUnit.SECONDS)
            val snapshot = synchronized(frames) { frames.toList() }
            val report = buildJsonObject {
                put("fixture", name); put("width_px", compose.activity.resources.displayMetrics.widthPixels)
                put("height_px", compose.activity.resources.displayMetrics.heightPixels)
                putJsonArray("frames_ns") { snapshot.forEach { row -> add(buildJsonArray { row.forEach { add(it) } }) } }
            }
            File(compose.activity.getExternalFilesDir(null), "performance/$name.json").apply { parentFile?.mkdirs(); writeText(report.toString()) }
            assertTrue("Expected rendered frames for $name", snapshot.isNotEmpty())
        } finally {
            instrumentation.runOnMainSync { compose.activity.window.removeOnFrameMetricsAvailableListener(listener) }
            thread.quitSafely()
        }
    }

    @Test fun homeScrollingWithLocalArtworkReportsFrameCosts() {
        val context = instrumentation.targetContext
        val cover = File(context.cacheDir, "performance-cover.png")
        Bitmap.createBitmap(384, 384, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(120, 65, 180))
            cover.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }; recycle()
        }
        val shelves = (0 until 20).map { shelf ->
            val tracks = (0 until 20).map { index -> Track("$shelf-$index", "Song $shelf/$index", "Artist",
                thumbnailUri = cover.toURI().toString(), durationSec = 180,
                ytVideoId = (shelf * 20 + index).toString().padStart(11, '0')) }
            val items = tracks.map { track -> BrowseParser.item(buildJsonObject {
                put("kind", "song"); put("videoId", track.ytVideoId); put("title", track.title); put("thumbnail", track.thumbnailUri)
            }) }
            HomeShelf("shelf-$shelf", "Shelf $shelf", "songs", items, tracks)
        }
        compose.setContent { JUKETheme { Surface { HomeContent(HomeUiState(shelves = shelves, isLoading = false), "") } } }
        compose.onNodeWithTag("Home feed").assertExists()
        measure("home-scroll") {
            repeat(5) { compose.onNodeWithTag("Home feed").performTouchInput { swipeUp(durationMillis = 350) }; compose.waitForIdle() }
            repeat(3) { compose.onNodeWithTag("Home feed").performTouchInput { swipeDown(durationMillis = 350) }; compose.waitForIdle() }
        }
    }

    @Test fun playerFadeKeepsTheArtworkAndRevealsTheBackdropWithoutAnOffscreenLayer() {
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        compose.setContent { Box(Modifier.fillMaxSize().background(Color.Blue)) { FadedArtwork(bitmap, Modifier.fillMaxSize().testTag("faded-artwork")) } }
        val image = compose.onNodeWithTag("faded-artwork").captureToImage().toPixelMap()
        val top = image[image.width / 2, image.height / 4]
        val bottom = image[image.width / 2, image.height - 2]
        assertTrue("Top of cover stays opaque", top.red > .9f && top.blue < .1f)
        assertTrue("Bottom reveals actual parent", bottom.blue > .9f && bottom.red < .1f)
    }

    @Test fun compareLegacyOffscreenFadeAndSinglePassArtworkAtTabletSize() {
        val bitmap = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(150, 70, 190)) }
        var legacy by mutableStateOf(true)
        var frame by mutableIntStateOf(0)
        compose.setContent { Box(Modifier.fillMaxSize().background(Color.Black)) {
            val motion = Modifier.fillMaxSize().graphicsLayer {
                translationY = (20 - frame % 20) * 2f
                if (legacy) alpha = .8f + (frame % 20) / 100f
            }
            if (legacy) Image(bitmap.asImageBitmap(), null, motion.artworkBottomFade(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            else FadedArtwork(bitmap, motion)
        } }
        for (old in listOf(true, false)) {
            compose.runOnIdle { legacy = old }
            measure(if (old) "artwork-legacy" else "artwork-single-pass") {
                repeat(30) { compose.runOnIdle { frame++ }; compose.waitForIdle() }
            }
        }
    }
}
