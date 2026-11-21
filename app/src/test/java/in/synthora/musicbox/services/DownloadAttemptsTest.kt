package `in`.synthora.musicbox.services
import org.junit.Assert.*
import org.junit.Test

class DownloadAttemptsTest {
    @Test fun cancelledEnqueueCannotPublishOrRemoveImmediateRetry() {
        val attempts = DownloadAttempts()
        val old = attempts.enqueue("podcast")
        attempts.cancel("podcast")
        val retry = attempts.enqueue("podcast")
        assertFalse(attempts.owns("podcast", old))
        assertFalse(attempts.finish("podcast", old))
        assertTrue(attempts.owns("podcast", retry))
        assertTrue(attempts.finish("podcast", retry))
        assertFalse(attempts.owns("podcast", retry))
    }
    @Test fun restoredQueueHasStableAttemptAndSeparateSongsAreIndependent() {
        val attempts = DownloadAttempts()
        val first = attempts.restoring("a")
        assertEquals(first, attempts.restoring("a"))
        val second = attempts.enqueue("b")
        attempts.cancel("a")
        assertFalse(attempts.finish("a", first))
        assertTrue(attempts.finish("b", second))
        assertNotEquals(first, attempts.restoring("a"))
    }
}
