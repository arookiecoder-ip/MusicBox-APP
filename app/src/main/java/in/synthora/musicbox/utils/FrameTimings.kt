package `in`.synthora.musicbox.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Constant memory and work per frame; no track titles, URLs, files or network traffic. */
internal class FrameTimingSummary {
    private val bins = LongArray(8)
    private var frames = 0L
    private var overBudget = 0L
    private var maxNs = 0L
    private var layoutNs = 0L
    private var drawNs = 0L
    private var budgetsNs = 0L
    @Synchronized fun record(total: Long, layout: Long, draw: Long, budget: Long) {
        if (total <= 0 || budget <= 0) return
        frames++; if (total > budget) overBudget++
        maxNs = maxOf(maxNs, total); layoutNs += layout.coerceAtLeast(0); drawNs += draw.coerceAtLeast(0); budgetsNs += budget
        val ms = total / 1_000_000
        val bin = when { ms < 8 -> 0; ms < 16 -> 1; ms < 24 -> 2; ms < 33 -> 3; ms < 50 -> 4; ms < 100 -> 5; ms < 250 -> 6; else -> 7 }
        bins[bin]++
    }
    @Synchronized fun report(): String = "frames=$frames, over_budget=$overBudget, max_ms=${maxNs / 1_000_000}, " +
        "mean_budget_us=${if (frames == 0L) 0 else budgetsNs / frames / 1000}, " +
        "layout_ms=${layoutNs / 1_000_000}, draw_ms=${drawNs / 1_000_000}, " +
        "buckets_ms=[<8,<16,<24,<33,<50,<100,<250,>=250]:${bins.joinToString(",")}"
}

internal object FrameTimings {
    private val summaries = mapOf("main" to FrameTimingSummary(), "player" to FrameTimingSummary())
    fun report(): String = summaries.entries.joinToString("\n") { "${it.key}: ${it.value.report()}" }
    fun observe(window: Window, screen: String): AutoCloseable {
        val summary = summaries.getValue(screen)
        val interval = (1_000_000_000 / (window.decorView.display?.refreshRate ?: 60f).coerceAtLeast(1f)).toLong()
        val thread = HandlerThread("musicbox-frame-timings").apply { start() }
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            val deadline = if (Build.VERSION.SDK_INT >= 31) metrics.getMetric(FrameMetrics.DEADLINE).takeIf { it > 0 } ?: interval else interval
            summary.record(metrics.getMetric(FrameMetrics.TOTAL_DURATION), metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION),
                metrics.getMetric(FrameMetrics.DRAW_DURATION), deadline)
        }
        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        return AutoCloseable { window.removeOnFrameMetricsAvailableListener(listener); thread.quitSafely() }
    }
}

private fun Context.activityWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> if (baseContext !== this) baseContext.activityWindow() else null
    else -> null
}

/** Both the activity and the player's dialog stop observation when no longer visible. */
@Composable
internal fun ObserveWindowFrameTimings(screen: String) {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(view, lifecycle, screen) {
        val window = (view.parent as? DialogWindowProvider)?.window ?: view.context.activityWindow()
        var observer: AutoCloseable? = null
        fun update() {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (observer == null && window != null) observer = FrameTimings.observe(window, screen)
            } else { observer?.close(); observer = null }
        }
        val listener = LifecycleEventObserver { _, _ -> update() }
        lifecycle.addObserver(listener); update()
        onDispose { lifecycle.removeObserver(listener); observer?.close() }
    }
}
