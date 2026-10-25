package `in`.synthora.musicbox.network

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ArtistCollectionsTest {
    @Test fun optionalShelvesAreEmptyAndMalformedCardsAreIgnored() {
        val data = Json.parseToJsonElement("""{"featured":[null,{}, {"playlistId":"PLone","title":"Featured"},{"playlistId":"PLone","title":"Duplicate"}]}""") as JsonObject
        assertTrue(artistCollectionItems(data,"playlists").isEmpty())
        val cards = artistCollectionItems(data,"featured")
        assertEquals(1, cards.size)
        assertEquals("playlist", cards.single().kind)
        assertEquals("PLone", cards.single().id)
    }
    @Test fun bothPlaylistTypesOpenAsPlaylistsAndAlbumsRemainAlbums() {
        val data = Json.parseToJsonElement("""{"playlists":[{"browseId":"VLPLartist","kind":"playlist"}],"featured":[{"browseId":"VLPLfeatured"}],"albums":[{"browseId":"MPREalbum"}]}""") as JsonObject
        assertEquals("playlist",artistCollectionItems(data,"playlists").single().kind)
        assertEquals("playlist",artistCollectionItems(data,"featured").single().kind)
        assertEquals("album",artistCollectionItems(data,"albums").single().kind)
        assertEquals("Featured on",artistCollectionTitle("featured"))
        assertEquals("Playlists by artist",artistCollectionTitle("playlists"))
    }
    @Test fun extraArtistShelvesKeepPlayableTracksAndCollectionTypes() {
        val data = Json.parseToJsonElement("""{"additionalShelves":[{"title":"Live performances","items":[{"kind":"track","video_id":"abcdefghijk","title":"Live"}]},{"title":"From your library","items":[{"browseId":"MPREalbum"}]},{"title":"Empty","items":[]}]}""") as JsonObject
        val shelves = artistAdditionalShelves(data)
        assertEquals(listOf("Live performances","From your library"),shelves.map { it.title })
        assertEquals("track",shelves[0].items.single().kind)
        assertEquals("album",shelves[1].items.single().kind)
    }
}
