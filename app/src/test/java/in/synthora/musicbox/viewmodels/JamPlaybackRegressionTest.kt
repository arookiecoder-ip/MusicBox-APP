package `in`.synthora.musicbox.viewmodels

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JamPlaybackRegressionTest {
    private fun playback(playing:Boolean, revision:Long=1)=buildJsonObject {
        put("playing",playing);put("revision",revision);put("queue_epoch","room");put("queue_version",1)
        put("queue_index",0);put("position_ms",1000);put("queue",JsonArray(emptyList()))
    }
    private fun state(p:JsonObject)=buildJsonObject {put("active",true);put("host",true);put("listen",true);put("url","https://example/j/room");put("playback",p)}

    @Test fun startingJamDoesNotPauseHostOrOpeningHostsOwnInvite() = runTest {
        var pauses=0
        val client=JamClient(backgroundScope,{_,_->state(playback(true))},{_,_->buildJsonObject {put("host",true)}},{"phone"},{testScheduler.currentTime},{})
        client.enterPlayback={pauses++}
        client.action("/alexa/jam/start/");runCurrent()
        assertTrue(client.active);assertEquals(0,pauses)
        client.action("/j/room");runCurrent()
        assertEquals(0,pauses)
        client.clear()
    }
    @Test fun joiningAnotherRoomPausesUnrelatedPersonalPlaybackOnce() = runTest {
        var pauses=0
        val client=JamClient(backgroundScope,{_,_->state(playback(true))},{_,_->buildJsonObject {put("host",false)}},{"phone"},{testScheduler.currentTime},{})
        client.enterPlayback={pauses++}
        client.action("/j/room");runCurrent()
        assertEquals(1,pauses);client.clear()
    }
    @Test fun transportAppliesWithoutSecondGetOrPresenceRoundTrip() = runTest {
        var reads=0;var presence=0
        val applied=mutableListOf<Boolean>()
        val client=JamClient(backgroundScope,{_,_->reads++;state(playback(true))},{path,_->
            if(path.contains("presence")){presence++;buildJsonObject {}} else buildJsonObject {put("playback",playback(false,2))}
        },{"phone"},{testScheduler.currentTime},{})
        client.applyPlayback={p,_->applied+=p["playing"]!!.jsonPrimitive.boolean}
        client.refresh();runCurrent();val initialReads=reads;val initialPresence=presence
        client.command("pause");runCurrent()
        assertEquals(initialReads,reads);assertEquals(initialPresence,presence)
        assertFalse(applied.last());assertFalse(client.state.value.playback["playing"]!!.jsonPrimitive.boolean)
        client.clear()
    }
    @Test fun slowPresenceDoesNotDelayOutputReconciliation() = runTest {
        val blocked=CompletableDeferred<Unit>();var applied=false
        val client=JamClient(backgroundScope,{_,_->state(playback(true))},{_,_->blocked.await();buildJsonObject {}},{"phone"},{testScheduler.currentTime},{})
        client.applyPlayback={_,_->applied=true};client.refresh();runCurrent()
        assertTrue(applied);blocked.complete(Unit);runCurrent();client.clear()
    }
    @Test fun unchangedPollDoesNotCancelStartupButPauseCancelsObsoletePreparation() = runTest {
        var starts=0;var cancelled=0;var paused=false
        val client=JamClient(backgroundScope,{_,_->state(playback(true))},{path,_->
            if(path.contains("control"))buildJsonObject {put("playback",playback(false,2))} else buildJsonObject {}
        },{"phone"},{testScheduler.currentTime},{})
        client.applyPlayback={p,_->if(p["playing"]!!.jsonPrimitive.boolean){starts++;try {awaitCancellation()}finally{cancelled++}}else paused=true}
        client.refresh();runCurrent();advanceTimeBy(1100);runCurrent()
        assertEquals(1,starts);assertEquals(0,cancelled)
        client.command("pause");runCurrent()
        assertTrue(paused);assertEquals(1,cancelled);client.clear()
    }
    @Test fun delayedCommandCannotRestoreRoomAfterLogout() = runTest {
        val response=CompletableDeferred<JsonElement>()
        val client=JamClient(backgroundScope,{_,_->state(playback(true))},{path,_->
            if(path.contains("control"))response.await() else buildJsonObject {}
        },{"phone"},{testScheduler.currentTime},{})
        client.refresh();runCurrent();client.command("pause");runCurrent();client.clear()
        response.complete(buildJsonObject {put("playback",playback(false,2))});runCurrent()
        assertFalse(client.active);assertTrue(client.state.value.playback.isEmpty())
    }
}
