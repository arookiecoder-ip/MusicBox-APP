package `in`.synthora.musicbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.viewmodels.*
import `in`.synthora.musicbox.network.*
import kotlinx.serialization.json.jsonPrimitive

@Composable
internal fun JamDeviceList(state:JamUiState,onConfigure:(String,List<String>)->Unit,
                           onSelect:(String,List<String>,String,String)->Unit) {
    val multiple=state.playback.text("mode")=="multiple"
    val selected=state.playback.array("selected").map{it.jsonPrimitive.content}.filter { id -> state.outputs.any { it.id == id && it.online } }
    val targets=state.playback["targets"].objectOrEmpty()
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Play on multiple devices",Modifier.weight(1f))
            Switch(checked=multiple,onCheckedChange={onConfigure(if(it)"multiple" else "single",if(it)selected.filter { id -> state.outputs.any { output -> output.id==id && output.kind!="alexa" && !targets.text(id).startsWith("alexa:") } } else selected.take(1))},
                enabled=!state.busy,modifier=Modifier.semantics{contentDescription="Play on multiple devices"})
        }
        if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=8.dp))
        state.outputs.filter{it.online}.groupBy{it.username}.forEach{(username,outputs)->
            Text(username,style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(top=16.dp,bottom=8.dp))
            val echoOwners=outputs.flatMap{output->output.devices.filter{it.kind=="alexa"}.map{it.id to output}}.groupBy{it.first}.mapValues{(_,rows)->
                rows.firstOrNull{(_,output)->output.id in selected && (targets.text(output.id).let{it.isNotBlank() && it==rows.first().first} || output.devices.any{it.id==rows.first().first && it.active})}?.second?.id ?: rows.first().second.id
            }
            outputs.forEach{output->
                val devices=output.devices.ifEmpty{listOf(JamDevice("current",output.name,output.kind,true))}
                val target=targets.text(output.id).ifBlank{devices.firstOrNull{it.active}?.id ?: devices.first().id}
                devices.filter{it.online && (it.kind!="alexa" || echoOwners[it.id]==output.id)}.forEach{device->
                    val echoDisabled=multiple && device.kind=="alexa"
                    val checked=!echoDisabled && output.id in selected && device.id==target
                    Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(if(device.kind=="alexa")Icons.Outlined.Speaker else Icons.Outlined.PhoneAndroid,null,
                            tint=if(checked)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f).padding(end=12.dp)) {
                            Text(jamDeviceLabel(output, device),style=MaterialTheme.typography.bodyLarge)
                            Text(if(echoDisabled)"Available in single-device mode" else if(!output.online || !device.online)"Offline" else output.error.ifBlank{if(checked && output.processing)"Buffering…" else if(checked && output.playing)"Playing here" else if(checked)"Selected" else "Available"},
                                style=MaterialTheme.typography.bodySmall,color=if(output.error.isNotBlank())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(checked=checked,enabled=!state.busy && !echoDisabled && (checked || output.online && device.online),
                            onCheckedChange={enable->
                                val next=if(!enable)selected-output.id else if(multiple)(selected+output.id).distinct() else listOf(output.id)
                                if(device.id=="current")onConfigure(if(multiple)"multiple" else "single",next)
                                else onSelect(if(multiple)"multiple" else "single",next,output.id,device.id)
                            },modifier=Modifier.semantics{contentDescription="Play on ${device.name} · $username"})
                    }
                    if (multiple && checked) EchoVolumeRow(volume=output.volume, enabled=output.online,
                        onVolumeChange={JamSession.setVolume(output.id,it)})
                }
            }
        }
        if(state.outputs.none{it.online}) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=12.dp))
            Text("Finding Jam devices…",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun JamVolumeControls(state:JamUiState,onVolume:(String,Int)->Unit={device,value->JamSession.setVolume(device,value)}) {
    val selected=state.playback.array("selected").map{it.jsonPrimitive.content}.filter { id -> state.outputs.any { it.id == id && it.online } }
    state.outputs.filter{it.online && it.id in selected}.forEach{output->
        if(selected.size>1)Text("${jamOutputLabel(output)} · ${output.username}",style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=8.dp))
        EchoVolumeRow(volume=output.volume,enabled=output.online,onVolumeChange={onVolume(output.id,it)})
    }
}

internal fun jamDeviceLabel(output:JamOutput, device:JamDevice,
    ownDeviceId:String=`in`.synthora.musicbox.services.PhonePlaybackOwnership.ownerId):String =
    if(output.id==ownDeviceId && device.kind=="phone") "${device.name} · This device" else device.name

internal fun jamOutputLabel(output:JamOutput,
    ownDeviceId:String=`in`.synthora.musicbox.services.PhonePlaybackOwnership.ownerId):String =
    output.devices.firstOrNull{it.active}?.let{jamDeviceLabel(output,it,ownDeviceId)}
        ?: output.name.removeSuffix(" · This phone")
