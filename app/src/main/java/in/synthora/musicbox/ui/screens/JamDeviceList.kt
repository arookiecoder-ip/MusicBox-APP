package `in`.synthora.musicbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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
            Text(username,style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(top=12.dp,bottom=4.dp))
            val echoOwners=outputs.flatMap{output->output.devices.filter{it.kind=="alexa"}.map{it.id to output}}.groupBy{it.first}.mapValues{(_,rows)->
                rows.firstOrNull{(_,output)->output.id in selected && (targets.text(output.id).let{it.isNotBlank() && it==rows.first().first} || output.devices.any{it.id==rows.first().first && it.active})}?.second?.id ?: rows.first().second.id
            }
            outputs.forEach{output->
                val devices=output.devices.ifEmpty{listOf(JamDevice("current",output.name,output.kind,true))}
                val target=targets.text(output.id).ifBlank{devices.firstOrNull{it.active}?.id ?: devices.first().id}
                devices.filter{it.online && (it.kind!="alexa" || echoOwners[it.id]==output.id)}.forEach{device->
                    val echoDisabled=multiple && device.kind=="alexa"
                    val checked=!echoDisabled && output.id in selected && device.id==target
                    val available=!state.busy && !echoDisabled && output.online && device.online
                    val background by animateColorAsState(if(checked)MaterialTheme.colorScheme.primary.copy(alpha=.06f) else Color.Transparent,label="Selected device")
                    fun select(enable:Boolean) {
                        val next=if(!enable)selected-output.id else if(multiple)(selected+output.id).distinct() else listOf(output.id)
                        if(device.id=="current")onConfigure(if(multiple)"multiple" else "single",next)
                        else onSelect(if(multiple)"multiple" else "single",next,output.id,device.id)
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min=60.dp).clip(RoundedCornerShape(8.dp)).background(background)
                        .toggleable(value=checked,enabled=available,role=Role.Checkbox,onValueChange=::select)
                        .semantics{contentDescription="Play on ${device.name} · $username"}.padding(start=8.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Icon(if(device.kind=="alexa")Icons.Outlined.Speaker else Icons.Outlined.PhoneAndroid,null,
                            tint=if(checked)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).padding(end=12.dp)) {
                            Text(jamDeviceLabel(output, device),style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis,
                                color=if(checked && output.playing)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Text(if(echoDisabled)"Available in single-device mode" else if(!output.online || !device.online)"Offline" else output.error.ifBlank{if(checked && output.processing)"Buffering…" else if(checked && output.playing)"Playing here" else if(checked)"Selected" else "Available"},
                                maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=if(output.error.isNotBlank())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(checked=checked,enabled=available,onCheckedChange=null,modifier=Modifier.size(48.dp))
                    }
                    if (multiple && checked) Box(Modifier.padding(start=36.dp,end=8.dp)) {
                        EchoVolumeRow(volume=output.volume, enabled=output.online,onVolumeChange={JamSession.setVolume(output.id,it)})
                    }
                }
            }
        }
        if(state.outputs.none{it.online}) {
            Text("No connected devices. Refresh to check again.",Modifier.padding(vertical=16.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun JamVolumeControls(state:JamUiState,
    ownDeviceId:String=`in`.synthora.musicbox.services.PhonePlaybackOwnership.ownerId,
    onVolume:(String,Int)->Unit={device,value->JamSession.setVolume(device,value)}) {
    val selected=state.playback.array("selected").map{it.jsonPrimitive.content}.filter { id -> state.outputs.any { it.id == id && it.online } }
    state.outputs.filter{it.online && it.id in selected && (state.playback.text("mode")!="multiple" || it.id==ownDeviceId)}.forEach{output->
        if(state.playback.text("mode")=="multiple")Text("${jamOutputLabel(output)} · ${output.username}",style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=8.dp))
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
