package `in`.synthora.musicbox.ui.screens

import androidx.compose.foundation.layout.*
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
    val selected=state.playback.array("selected").map{it.jsonPrimitive.content}
    val targets=state.playback["targets"].objectOrEmpty()
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Play on multiple devices",Modifier.weight(1f))
            Switch(checked=multiple,onCheckedChange={onConfigure(if(it)"multiple" else "single",if(it)selected else selected.take(1))},
                enabled=!state.busy,modifier=Modifier.semantics{contentDescription="Play on multiple devices"})
        }
        state.outputs.groupBy{it.username}.forEach{(username,outputs)->
            Text(username,style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(top=16.dp,bottom=8.dp))
            outputs.forEach{output->
                val devices=output.devices.ifEmpty{listOf(JamDevice("current",output.name,output.kind,true))}
                val target=targets.text(output.id).ifBlank{devices.firstOrNull{it.active}?.id ?: devices.first().id}
                devices.forEach{device->
                    val checked=output.id in selected && device.id==target
                    Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end=12.dp)) {
                            Text(device.name,style=MaterialTheme.typography.bodyLarge)
                            Text(if(!output.online || !device.online)"Offline" else output.error.ifBlank{if(checked)"Playing here" else "Available"},
                                style=MaterialTheme.typography.bodySmall,color=if(output.error.isNotBlank())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(checked=checked,enabled=!state.busy && (checked || output.online && device.online),
                            onCheckedChange={enable->
                                val next=if(!enable)selected-output.id else if(multiple)(selected+output.id).distinct() else listOf(output.id)
                                if(device.id=="current")onConfigure(if(multiple)"multiple" else "single",next)
                                else onSelect(if(multiple)"multiple" else "single",next,output.id,device.id)
                            },modifier=Modifier.semantics{contentDescription="Play on ${device.name} · $username"})
                    }
                }
            }
        }
        if(state.outputs.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=12.dp))
            Text("Finding Jam devices…",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun JamVolumeControls(state:JamUiState) {
    val selected=state.playback.array("selected").map{it.jsonPrimitive.content}
    state.outputs.filter{it.id in selected}.forEach{output->
        if(selected.size>1)Text("${output.name} · ${output.username}",style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=8.dp))
        EchoVolumeRow(volume=output.volume,enabled=output.online,onVolumeChange={JamSession.setVolume(output.id,it)})
    }
}
