package `in`.synthora.musicbox.services

import `in`.synthora.musicbox.network.BrowseParser
import `in`.synthora.musicbox.ui.components.MediaMenuController
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SpeedDialMenuTest {
    @Test fun trackCardsUseTheSameSongActionsAndIdentityAsOtherSongRows() {
        val song=BrowseParser.item(buildJsonObject { put("video_id","dQw4w9WgXcQ");put("title","Song");put("artist","Artist");put("duration_ms",123000) })
        val menu=MediaMenuController()
        menu.show(song)
        assertNull(menu.item)
        assertEquals("Song",menu.track?.title)
        assertEquals("Artist",menu.track?.artist)
        assertEquals(123,menu.track?.durationSec)
        assertEquals("dQw4w9WgXcQ",menu.track?.ytVideoId)
        assertFalse(menu.playerOnly)
    }
    @Test fun collectionAndTrackMenusCannotRetainActionsFromThePreviousSelection() {
        val playlist=BrowseParser.item(buildJsonObject { put("playlistId","PLfixture");put("title","Playlist") })
        val song=BrowseParser.item(buildJsonObject { put("video_id","dQw4w9WgXcQ");put("title","Song") })
        val menu=MediaMenuController()
        menu.show(playlist);assertNotNull(menu.item)
        menu.show(song);assertNull(menu.item);assertNotNull(menu.track)
        menu.dismiss();assertNull(menu.track);assertNull(menu.item)
        menu.show(playlist);assertNull(menu.track);assertNull(menu.queueActions)
    }
}
