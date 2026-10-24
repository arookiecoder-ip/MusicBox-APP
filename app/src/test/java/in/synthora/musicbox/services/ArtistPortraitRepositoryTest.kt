package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.models.ArtistCredit
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ArtistPortraitRepositoryTest {
    private fun portrait(url: String = "https://images.example/artist.png") = buildJsonObject {
        put("artist", buildJsonObject { put("thumbnails", buildJsonArray { add(buildJsonObject { put("url", url); put("width", 256); put("height", 256) }) })
    }
    @Test fun knownArtistLoadsOnlyItsDetailsAndReopeningUsesTheBoundedCache() = runBlocking {
        val paths = mutableListOf<String>()
        val repo = ArtistPortraitRepository(get = { path, _ -> paths += path; portrait() }, capacity = 1)
        assertNotNull(repo.load(ArtistCredit("First", "UCfirst")))
        assertNotNull(repo.load(ArtistCredit("First", "UCfirst")))
        assertEquals(listOf("/api/artist/UCfirst"), paths)
        repo.load(ArtistCredit("Second", "UCsecond")); repo.load(ArtistCredit("First", "UCfirst"))
        assertEquals(3, paths.size)
    }
    @Test fun missingIdResolvesOnlyWhenRequestedAndUnsafeIdsNeverBecomePaths() = runBlocking {
        val paths = mutableListOf<String>()
        val repo = ArtistPortraitRepository(get = { path, query ->
            paths += path
            if (path.endsWith("resolve/")) { assertEquals("Artist", query["name"]); buildJsonObject { put("channel_id", "UCsafe") } } else portrait()
        })
        repo.load(ArtistCredit(" Artist ", "../unsafe"))
        assertEquals(listOf("/api/artist/resolve/", "/api/artist/UCsafe"), paths)
    }
    @Test fun emptyNamesMissingImagesAndBadResolutionRemainRetryable() = runBlocking {
        var calls = 0
        val repo = ArtistPortraitRepository(get = { _, _ -> calls++; JsonObject(emptyMap()) })
        assertNull(repo.load(ArtistCredit(" ", "UCfirst"))); assertEquals(0, calls)
        repeat(2) { assertNull(repo.load(ArtistCredit("First", "UCfirst"))) }
        assertEquals(2, calls)
        val bad = ArtistPortraitRepository(get = { _, _ -> buildJsonObject { put("id", "../bad") } })
        assertNull(bad.load(ArtistCredit("Unknown")))
    }
    @Test fun dismissingTheSheetCanCancelTheLookup() = runBlocking {
        val repo = ArtistPortraitRepository(get = { _, _ -> throw CancellationException("closed") })
        assertTrue(runCatching { repo.load(ArtistCredit("First", "UCfirst")) }.exceptionOrNull() is CancellationException)
    }
}
