package `in`.synthora.musicbox.network

import org.junit.Assert.*
import org.junit.Test

class LyricsEndpointTest {
    @Test fun officialSearchUsesDocumentedFieldNamesRatherThanTheRetiredProxy() {
        assertEquals("https://lrclib.net/api/search",LRCLIB_SEARCH_URL)
        assertEquals(mapOf("track_name" to "Song","artist_name" to "Artist","album_name" to "Album"),lrclibSearchParameters("Song","Artist","Album"))
    }
    @Test fun missingOptionalMetadataDoesNotSendEmptyArtistOrAlbumFilters() {
        assertEquals(mapOf("track_name" to "Song"),lrclibSearchParameters("Song",""))
    }
}
