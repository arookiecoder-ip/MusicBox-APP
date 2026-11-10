package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.network.AlexaBackendApi.BackendTrack
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FreshRadioQueueTest {
    private fun songs(vararg ids:String)=ids.map { BackendTrack(title=it,videoId=it) }
    @Test fun repeatedIdenticalProviderQueuesStillProduceDifferentUpcomingOrder() {
        var previous=listOf("a","b","c","d")
        repeat(20) {
            val fresh=freshRadioQueue("seed",songs("seed","a","b","c","d"),emptyList(),previous,Random(7)).map { it.videoId }
            assertNotEquals(previous,fresh)
            assertNotEquals(previous.first(),fresh.first())
            assertEquals(previous.toSet(),fresh.toSet())
            previous=fresh
        }
    }
    @Test fun freshRelatedRecommendationsPrecedePreviouslyQueuedSongsWithoutDuplicates() {
        val fresh=freshRadioQueue("seed",songs("seed","a","b","a"),songs("b","new1","new2","seed",""),listOf("a","b"),Random(3)).map { it.videoId }
        assertEquals(setOf("new1","new2"),fresh.take(2).toSet())
        assertEquals(4,fresh.size)
        assertEquals(fresh.size,fresh.toSet().size)
    }
    @Test fun malformedEmptyAndSingleCandidateResultsCannotLoopOrInventSongs() {
        assertTrue(freshRadioQueue("seed",songs("seed",""),emptyList(),emptyList()).isEmpty())
        assertEquals(listOf("a"),freshRadioQueue("seed",songs("a","a"),emptyList(),listOf("a")).map { it.videoId })
        assertEquals(50,freshRadioQueue("seed",(1..200).map { BackendTrack(videoId="song$it") },emptyList(),emptyList()).size)
    }
}
