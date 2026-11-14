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
    @Test fun ownedPhonePausesBeforeHttpAndRapidPlayReversesThePendingPause()=runTest {
        val pending=CompletableDeferred<JsonElement>();var localPlaying=true
        val client=JamClient(backgroundScope,{_,_->state(playback(true))},{path,body->
            if(path.contains("control") && body["action"]!!.jsonPrimitive.content=="pause") pending.await()
            else if(path.contains("control"))buildJsonObject {put("playback",playback(true,3))}
            else buildJsonObject {}
        },{"phone"},{testScheduler.currentTime},{})
        client.refresh();runCurrent()
        client.previewTransport={localPlaying=it=="play";true}
        client.command("pause")
        assertFalse(localPlaying);assertFalse(client.state.value.playback["playing"]!!.jsonPrimitive.boolean)
        runCurrent();client.command("play");assertTrue(localPlaying)
        pending.complete(buildJsonObject {put("playback",playback(false,2))});runCurrent()
        assertTrue(client.state.value.playback["playing"]!!.jsonPrimitive.boolean);client.clear()
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
    @Test fun handoffCancelsOldFollowerAndDefersNewCommandsUntilOutputCommits()=runTest {
        var desired=playback(true);var output="phone";val applied=mutableListOf<String>();var cancelled=false
        val client=JamClient(backgroundScope,{_,_->state(desired)},{_,_->buildJsonObject {}},{"phone"},{testScheduler.currentTime},{})
        client.outputName={output}
        client.applyPlayback={_,_->applied+=output;if(output=="phone")try{awaitCancellation()}finally{cancelled=true}}
        client.refresh();runCurrent();assertEquals(listOf("phone"),applied)
        client.beginOutputSwitch();assertTrue(cancelled)
        desired=playback(false,2);client.refresh();runCurrent()
        advanceTimeBy(2100);runCurrent();assertEquals(listOf("phone"),applied)
        output="alexa";client.endOutputSwitch();runCurrent()
        assertEquals(listOf("phone","alexa"),applied)
        assertFalse(client.state.value.playback["playing"]!!.jsonPrimitive.boolean)
        client.clear()
    }
    @Test fun endingJamSilencesOnceWithoutRevokedRefreshSendingAnotherPause()=runTest {
        var ended=false;var pauses=0;var readsAfterEnd=0
        val client=JamClient(backgroundScope,{_,_->
            if(ended){readsAfterEnd++;buildJsonObject{put("active",false);put("revoked",true)}}else state(playback(true))
        },{path,_->if(path.endsWith("stop/"))ended=true;buildJsonObject{}},{"phone"},{testScheduler.currentTime},{})
        client.applyPlayback={_,listen->if(!listen)pauses++}
        client.refresh();runCurrent();client.action("/alexa/jam/stop/");runCurrent()
        advanceTimeBy(10_000);runCurrent()
        assertFalse(client.active);assertEquals(1,pauses);assertEquals(0,readsAfterEnd)
        client.refresh();runCurrent();assertEquals(1,pauses);assertNull(client.state.value.error)
        client.clear()
    }

    @Test fun memberRoutesVolumeToSelectedHostAndAcknowledgesOnlyOwnCommands()=runTest {
        val writes=mutableListOf<Pair<String,JsonObject>>()
        val output=buildJsonObject{put("device","host");put("name","Echo");put("username","Host");put("online",true);put("volume",20)}
        val room=state(playback(true)).toMutableMap().apply{put("listen",JsonPrimitive(false));put("outputs",JsonArray(listOf(output)));put("playback",JsonObject(playback(true)+mapOf("selected" to JsonArray(listOf(JsonPrimitive("host"))))))}
        val client=JamClient(backgroundScope,{_,_->JsonObject(room)},{path,body->writes+=path to body;buildJsonObject{}},{"guest"},{testScheduler.currentTime},{})
        var applied=0;client.applyVolume={applied++}
        client.refresh();runCurrent();client.setVolume("host",75);advanceTimeBy(151);runCurrent()
        val sent=writes.last{it.first.contains("control")}.second
        assertEquals("host",sent["device_id"]!!.jsonPrimitive.content);assertEquals(75,sent["value"]!!.jsonPrimitive.int)
        assertEquals(0,applied)
        client.setVolume("not-in-room",10);advanceTimeBy(151);runCurrent()
        assertEquals(1,writes.count{it.first.contains("control")})
        client.clear()
    }
    @Test fun startRemainsBusyWhileUnrelatedStateRefreshIsInactive()=runTest {
        val response=CompletableDeferred<JsonElement>()
        val client=JamClient(backgroundScope,{_,_->buildJsonObject{put("active",false)}},{_,_->response.await()},{"phone"},{testScheduler.currentTime},{})
        client.action("/alexa/jam/start/");runCurrent();assertTrue(client.state.value.busy)
        client.refresh();runCurrent();assertTrue(client.state.value.busy)
        response.complete(buildJsonObject{});runCurrent();assertFalse(client.state.value.busy)
        client.clear()
    }

    @Test fun deviceLabelChangesDuringStartupDoNotCancelAudioPreparation()=runTest {
        var name="Echo";var starts=0;var cancellations=0
        val client=JamClient(backgroundScope,{_,_->state(playback(true))},{_,_->buildJsonObject{}},{"phone"},{testScheduler.currentTime},{})
        client.outputName={name}
        client.applyPlayback={_,_->starts++;try{awaitCancellation()}finally{cancellations++}}
        client.refresh();runCurrent();name="Phone";advanceTimeBy(1100);runCurrent()
        assertEquals(1,starts);assertEquals(0,cancellations);client.clear()
    }
    @Test fun songSelectionRetriesAfterBackgroundRecommendationsChangeRevision()=runTest {
        var calls=0;val revisions=mutableListOf<Long>();val ids=mutableListOf<String>()
        val client=JamClient(backgroundScope,{_,_->state(playback(true,if(calls==0)1 else 2))},{path,body->
            if(path.contains("control")) {
                calls++;revisions+=body["revision"]!!.jsonPrimitive.long;ids+=body["command_id"]!!.jsonPrimitive.content
                if(calls==1)throw `in`.synthora.musicbox.network.BackendHttpException(409,"Queue changed")
                buildJsonObject{put("playback",playback(true,3))}
            }else buildJsonObject{}
        },{"phone"},{testScheduler.currentTime},{})
        client.refresh();runCurrent();client.command("queue");runCurrent()
        assertEquals(listOf(1L,2L),revisions);assertEquals(ids[0],ids[1]);assertNull(client.state.value.error);client.clear()
    }
    @Test fun recommendationsDoNotCancelTheCurrentSongsAudioPreparation()=runTest {
        var version=1;var started=0;var cancelled=0
        val client=JamClient(backgroundScope,{_,_->state(JsonObject(playback(true)+mapOf("entry_id" to JsonPrimitive("same-song"),"queue_version" to JsonPrimitive(version))))},{_,_->buildJsonObject{}},{"phone"},{testScheduler.currentTime},{})
        client.applyPlayback={_,_->started++;try{awaitCancellation()}finally{cancelled++}}
        client.refresh();runCurrent();version=2;advanceTimeBy(1100);runCurrent()
        assertEquals(1,started);assertEquals(0,cancelled);client.clear()
    }
}
