package `in`.synthora.musicbox.performance

import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "in.synthora.musicbox"

/** Real app journeys; use the same account/cache/playback state for comparisons. */
private fun browse(device: UiDevice) {
    for (tab in listOf("Home", "Search", "Library")) {
        val selector = By.desc(tab)
        assumeTrue("Sign in on the performance APK before measuring browsing", device.wait(Until.hasObject(selector), 5_000))
        device.findObject(selector).click()
        device.waitForIdle()
        val x = device.displayWidth / 2
        device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 3, 20)
        device.waitForIdle()
    }
}

@RunWith(AndroidJUnit4::class)
class PlaybackPerformanceTest {
    @get:Rule val benchmark = MacrobenchmarkRule()
    @Test fun coldStartup() = benchmark.measureRepeated(PACKAGE,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(), startupMode = StartupMode.COLD, iterations = 5,
        setupBlock = { pressHome() }) { startActivityAndWait() }

    @Test fun homeSearchLibraryScrolling() = benchmark.measureRepeated(PACKAGE,
        metrics = listOf(FrameTimingMetric()), compilationMode = CompilationMode.Partial(), iterations = 5,
        setupBlock = { startActivityAndWait() }) { browse(device) }

    @Test fun playerAndQueueOpening() = benchmark.measureRepeated(PACKAGE,
        metrics = listOf(FrameTimingMetric()), compilationMode = CompilationMode.Partial(), iterations = 5,
        setupBlock = { startActivityAndWait() }) {
        val mini = By.res("mini_player")
        assumeTrue("Prepare a selected song before measuring player/queue opening", device.wait(Until.hasObject(mini), 5_000))
        device.findObject(mini).click()
        device.waitForIdle()
        val queue = By.desc("Queue")
        assumeTrue("Use portrait mode for the queue-opening measurement", device.wait(Until.hasObject(queue), 5_000))
        device.findObject(queue).click()
        device.waitForIdle()
        device.pressBack()
        device.pressBack()
    }
}

@RunWith(AndroidJUnit4::class)
class PlaybackBaselineProfileTest {
    @get:Rule val profile = BaselineProfileRule()
    @Test fun startupAndBrowsing() = profile.collect(PACKAGE) {
        startActivityAndWait()
        browse(device)
    }
}
