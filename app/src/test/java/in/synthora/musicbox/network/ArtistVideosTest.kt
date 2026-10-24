package `in`.synthora.musicbox.network

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ArtistVideosTest {
    @Test fun rawVideoShelfSupportsDuplicatesMissingIdsAndDuration() {
        val data=Json.parseToJsonElement("""{"artist":{"videos":{"results":[{"videoId":"one","title":"Video","duration":"3:10"},{"videoId":"one"},{"title":"Broken"}]}}}""").objectOrEmpty()
        val rows=artistVideoItems(data)
        assertEquals(1,rows.size);assertEquals("one",rows.single().videoId);assertEquals(190000L,rows.single().durationMs)
    }
    @Test fun normalizedAndRawShelvesMergeWithoutCrashingOnAbsentOrNullData() {
        val data=Json.parseToJsonElement("""{"videos":[{"video_id":"one"}],"artist":{"videos":{"items":[{"videoId":"two"},{"videoId":"one"}]}}}""").objectOrEmpty()
        assertEquals(listOf("one","two"),artistVideoItems(data).map { it.videoId })
        assertTrue(artistVideoItems(Json.parseToJsonElement("""{"artist":null,"videos":null}""").objectOrEmpty()).isEmpty())
    }
}
