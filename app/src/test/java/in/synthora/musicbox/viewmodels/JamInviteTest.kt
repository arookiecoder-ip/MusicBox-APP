package `in`.synthora.musicbox.viewmodels

import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class JamInviteTest {
    private val base="https://testalexa.synthora.in"
    @Test fun invitationAcceptsOnlyConfiguredHttpsHost() {
        assertEquals("abcdefgh_123",jamInviteToken("$base/j/abcdefgh_123",base))
        assertEquals("abcdefgh_123",jamInviteToken("$base/jam/abcdefgh_123",base))
        assertEquals("abcdefgh_123",jamInviteToken("$base:443/j/abcdefgh_123",base))
        assertNull(jamInviteToken("https://other.test/j/abcdefgh_123",base))
        assertNull(jamInviteToken("http://testalexa.synthora.in/j/abcdefgh_123",base))
        assertNull(jamInviteToken("https://user@testalexa.synthora.in/j/abcdefgh_123",base))
        assertNull(jamInviteToken("https://testalexa.synthora.in:8443/j/abcdefgh_123",base))
    }
    @Test fun malformedAndUnrelatedPathsCannotBecomeJoinRequests() {
        listOf("/account", "/j/", "/j/short", "/j/abcdefgh%2F12", "/j/abcdefgh/123", "/jam-invite/abcdefgh_123").forEach {
            assertNull(it,jamInviteToken(base+it,base))
        }
        assertNull(jamInviteToken("not a URL",base))
    }
}

class JamSnapshotTest {
    @org.junit.Test fun slimSnapshotKeepsQueueButUpdatesSharedPosition() {
        val previous=kotlinx.serialization.json.Json.parseToJsonElement("""{"queue_epoch":"room-A","queue":[{"video_id":"aaaaaaaaaaa"}],"position_ms":0}""").jsonObject
        val incoming=kotlinx.serialization.json.Json.parseToJsonElement("""{"queue_epoch":"room-A","position_ms":5000}""").jsonObject
        val merged=mergeJamPlayback(previous,incoming)
        org.junit.Assert.assertEquals(previous["queue"],merged["queue"])
        org.junit.Assert.assertEquals(incoming["position_ms"],merged["position_ms"])
    }
    @org.junit.Test(expected=IllegalStateException::class) fun slimSnapshotCannotReuseAnotherRoomsQueue() {
        mergeJamPlayback(kotlinx.serialization.json.Json.parseToJsonElement("""{"queue_epoch":"room-A","queue":[]}""").jsonObject,
            kotlinx.serialization.json.Json.parseToJsonElement("""{"queue_epoch":"room-B"}""").jsonObject)
    }
}
