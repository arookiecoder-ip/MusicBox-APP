package `in`.synthora.musicbox.services
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.viewmodels.SortOption
import org.junit.Assert.*
import org.junit.Test
class LibraryTrackPresentationTest {
    private val a = Track("a", "Beta", "First", durationSec = 60, ytVideoId = "A")
    private val b = Track("b", "alpha", "Second", durationSec = 60, ytVideoId = "B")
    @Test fun searchAndSortKeepDefaultOrderingAndReuseUnchangedRows() {
        val items = listOf(a, b)
        val default = presentLibraryTracks(items, "", SortOption.DEFAULT, emptySet())
        assertSame(a, default[0]); assertSame(b, default[1])
        assertEquals(listOf(b, a), presentLibraryTracks(items, "", SortOption.TITLE, emptySet()))
        assertEquals(listOf(b), presentLibraryTracks(items, " SECOND ", SortOption.DEFAULT, emptySet()))
        assertTrue(presentLibraryTracks(items, "Missing", SortOption.DEFAULT, emptySet()).isEmpty())
    }
    @Test fun onlyChangedLikesCreateRowsAndDuplicateSongsAreRetained() {
        val another = a.copy(uuid = "other")
        val result = presentLibraryTracks(listOf(a, another, b), "", SortOption.DEFAULT, setOf("A"))
        assertEquals(3, result.size)
        assertTrue(result[0].isFavourite); assertTrue(result[1].isFavourite)
        assertSame(b, result[2]); assertEquals("other", result[1].uuid)
        assertFalse(a.isFavourite)
    }
}
