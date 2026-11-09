package `in`.synthora.musicbox.utils

import org.junit.Assert.*
import org.junit.Test

class FrameTimingSummaryTest {
    @Test fun aTenMillisecondFrameMisses120HzBudgetButNot60HzBudget() {
        val highRefresh = FrameTimingSummary(); val standard = FrameTimingSummary()
        highRefresh.record(10_000_000,1_000_000,2_000_000,8_333_333)
        standard.record(10_000_000,1_000_000,2_000_000,16_666_667)
        assertTrue(highRefresh.report().contains("over_budget=1"))
        assertTrue(standard.report().contains("over_budget=0"))
    }
    @Test fun zeroAndInvalidSamplesDoNotPolluteTheSummaryAndLargeSamplesUseBoundedBuckets() {
        val summary = FrameTimingSummary()
        summary.record(0,0,0,16_666_667)
        summary.record(-1,0,0,16_666_667)
        repeat(10000) { summary.record(300_000_000,1_000_000,2_000_000,16_666_667) }
        val report = summary.report()
        assertTrue(report.contains("frames=10000"))
        assertTrue(report.contains("max_ms=300"))
        assertTrue(report.length < 300)
    }
}
