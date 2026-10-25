package `in`.synthora.musicbox.network

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

class SearchRankingTest {
    private fun item(id: String, title: String, type: String, videoType: String = "") = BrowseParser.item(buildJsonObject {
        put("videoId", id); put("title", title); put("artist", "Singer"); put("resultType", type); put("videoType", videoType)
    })
    @Test fun audioCounterpartReplacesVideoTopResult() {
        val video = item("video", "My Song (Official Video)", "video")
        val song = item("audio", "My Song", "song")
        assertEquals(song, audioSearchHero(listOf(video, song), listOf(song)))
    }
    @Test fun officialVideoIsNotAudioEvenWithoutResultType() {
        assertFalse(isAudioSearchItem(item("video", "Song", "track", "MUSIC_VIDEO_TYPE_OMV")))
        assertTrue(isAudioSearchItem(item("audio", "Song", "video", "MUSIC_VIDEO_TYPE_ATV")))
    }
    @Test fun unicodeTitlesMatchWithoutCollapsingDifferentSongs() {
        val video = item("video", "আমার গান (Official)", "video")
        val other = item("other", "অন্য গান", "song")
        val song = item("audio", "আমার গান", "song")
        assertEquals(song, audioSearchHero(listOf(video), listOf(other, song)))
    }
    @Test fun missingSongArrayStillShowsMixedTracksWithoutDroppingVideos() {
        val video = item("video", "Song", "video")
        val audio = item("audio", "Song", "song")
        assertEquals(listOf(video, audio), searchSongItems(listOf(video, audio), emptyList()))
        assertEquals(listOf(video), searchSongItems(listOf(video), emptyList()))
        assertEquals(listOf(audio, video), searchSongItems(listOf(video, audio), listOf(audio)))
    }
    @Test fun regionalVideoReleaseBeatsUnrelatedAudio() {
        val video = item("video", "Go (Official Video)", "video")
        val other = item("other", "Another Song", "song")
        assertEquals(video, audioSearchHero(listOf(video), listOf(other)))
    }
    @Test fun sameTitleByDifferentSingerCannotReplaceVideo() {
        val video = item("video", "Go (Official Video)", "video")
        val other = BrowseParser.item(buildJsonObject {
            put("videoId", "other"); put("title", "Go"); put("artist", "Different Artist"); put("resultType", "song")
        })
        assertEquals(video, audioSearchHero(listOf(video), listOf(other)))
    }
    @Test fun audioHeroAcceptsSharedArtistCredits() {
        val video = item("video", "Go (Official Video)", "video")
        val song = BrowseParser.item(buildJsonObject {
            put("videoId", "audio"); put("title", "Go"); put("artist", "Singer and Collaborator"); put("resultType", "song")
        })
        assertEquals(song, audioSearchHero(listOf(video), listOf(song)))
    }
    @Test fun exactSongAndArtistQueryBeatsUnrelatedProviderHit() {
        val unrelated=item("wrong","Unrelated","song")
        val song=item("right","My Song","song")
        assertEquals(song, relevantSearchHero(listOf(unrelated,song),listOf(song),"my song singer"))
        assertEquals(listOf(song,unrelated),searchSongItems(listOf(unrelated,song),emptyList(),"my song"))
    }
    @Test fun relevanceHandlesUnicodeAccentsAndBlankPunctuationWithoutLosingResults() {
        val wrong=item("wrong","Other","song");val song=item("right","Café","song")
        assertEquals(song,relevantSearchHero(listOf(wrong,song),emptyList(),"cafe"))
        assertEquals(listOf(wrong,song),searchSongItems(listOf(wrong,song),emptyList(),"!!!"))
        val native=item("native","আমার গান","song")
        assertEquals(native,relevantSearchHero(listOf(wrong,native),emptyList(),"আমার গান"))
    }
    @Test fun liveAndRemixVersionsCannotSilentlyReplaceAnOfficialVideo() {
        val video=item("video","Go (Official Video)","video")
        val remix=item("remix","Go (Remix)","song")
        assertEquals(video,audioSearchHero(listOf(video),listOf(remix)))
    }
    @Test fun mixedEntityShelvesAreRecoveredAndDeduplicated() {
        val artist=BrowseParser.item(buildJsonObject { put("browseId","UCtest");put("title","Singer") })
        assertEquals(listOf(artist),searchKindItems(listOf(artist,artist),emptyList(),"artist"))
    }

    @Test fun indicVowelMarksMustNotCollapseDifferentSongNames() {
        val wrong=item("wrong","কটা","song");val right=item("right","কাটা","song")
        assertEquals(right,relevantSearchHero(listOf(wrong,right),emptyList(),"কাটা"))
    }

    @Test fun spacedSongTitleBeatsLooselyMatchingPlaylistAndUnrelatedSongs() {
        val playlist = BrowseParser.item(buildJsonObject {
            put("browseId", "VLplaylist"); put("title", "kulo sa sped up"); put("resultType", "playlist"); put("category", "Top result")
        })
        val wrong = item("wrong", "Ra Ma Da Sa", "song")
        val song = item("actual", "KU LO SA", "song")
        assertEquals(listOf(song, wrong), searchSongItems(listOf(wrong), listOf(wrong, song), "kulo sa"))
        assertEquals(song, relevantSearchHero(listOf(playlist, wrong), listOf(song, wrong), "kulo sa"))
        val video = item("official", "KU LO SA (A COLORS SHOW)", "video")
        assertEquals(video, relevantSearchHero(listOf(playlist), listOf(video, wrong), "kulo sa"))
    }

    @Test fun explicitPlaylistAndExactArtistQueriesKeepTheirOwnTopResult() {
        val playlist = BrowseParser.item(buildJsonObject {
            put("browseId", "VLplaylist"); put("title", "Kulo Sa Playlist"); put("resultType", "playlist"); put("category", "Top result")
        })
        val song = item("song", "KU LO SA", "song")
        assertEquals(playlist, relevantSearchHero(listOf(playlist), listOf(song), "kulo sa playlist"))
        val artist = BrowseParser.item(buildJsonObject {
            put("browseId", "UCartist"); put("title", "Singer"); put("resultType", "artist"); put("category", "Top result")
        })
        assertEquals(artist, relevantSearchHero(listOf(artist), listOf(song), "Singer"))
        val unrelated = item("unrelated", "Other", "song")
        assertEquals(listOf(unrelated, item("lovely", "Lovely", "song")),
            searchSongItems(listOf(unrelated, item("lovely", "Lovely", "song")), emptyList(), "love"))
    }

}
