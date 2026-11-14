package `in`.synthora.musicbox.ui.screens

import androidx.compose.ui.graphics.asImageBitmap
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.synthora.musicbox.ui.components.GlassAlertDialog
import `in`.synthora.musicbox.ui.components.rememberShareAction
import `in`.synthora.musicbox.viewmodels.JamViewModel

@Composable
fun JamSettings(jam: JamViewModel = viewModel()) {
    val state by jam.state.collectAsStateWithLifecycle()
    LaunchedEffect(jam) { jam.refresh() }
    JamSettingsContent(state, jam::start, jam::stop, jam::leave, jam::remove, jam::configure, jam::refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JamSettingsContent(
    state: `in`.synthora.musicbox.viewmodels.JamUiState,
    onStart: () -> Unit, onStop: () -> Unit, onLeave: () -> Unit,
    onRemove: (String) -> Unit, onConfigure: (String, List<String>) -> Unit, onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val share = rememberShareAction()
    var showParticipants by remember { mutableStateOf(false) }
    LaunchedEffect(state.active) {if(!state.active)showParticipants=false}
    var confirmEnd by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var removeMember by remember { mutableStateOf<`in`.synthora.musicbox.viewmodels.JamMember?>(null) }
    Column(Modifier.padding(horizontal=24.dp,vertical=12.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable(enabled=state.active,role=Role.Button,onClickLabel="Open Jam participants"){showParticipants=true}
            .semantics {if(state.active)contentDescription="Jam participants, ${state.memberCount} people"}
            .padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Jam",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                Text(if(state.active) "${state.memberCount} ${if(state.memberCount==1)"person" else "people"} · one shared queue"
                    else "Listen together. Your likes and playlists stay yours.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(state.active) {
                Icon(Icons.Outlined.Groups,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=12.dp).size(22.dp))
                Icon(Icons.AutoMirrored.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(24.dp))
            }
        }
        state.error?.takeIf { state.active }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        if (state.active) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(modifier = Modifier.weight(1f).height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), enabled = state.url.isNotBlank() && !state.busy, onClick = { showQr = true }) { Text("Invite") }
                Button(modifier = Modifier.weight(1f).height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error), enabled = !state.busy, onClick = { confirmEnd = true }) { if (state.busy && state.pendingAction.endsWith("stop/")) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error) else Text(if(state.host) "End Jam" else "Leave Jam") }
            }
        } else Button(enabled = !state.busy, onClick = onStart, modifier = Modifier.fillMaxWidth().height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) { if (state.busy) {CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary);Spacer(Modifier.width(8.dp));Text("Starting Jam…")} else Text("Start Jam") }
        }
    }
    if(showParticipants && state.active) {
        ModalBottomSheet(onDismissRequest={showParticipants=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
            containerColor=`in`.synthora.musicbox.ui.theme.glassSheetColor(),tonalElevation=0.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max=560.dp).navigationBarsPadding().padding(horizontal=24.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Jam participants",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        Text("${state.memberCount} ${if(state.memberCount==1)"person" else "people"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick={showParticipants=false}){Icon(Icons.Outlined.Close,"Close Jam participants")}
                }
                HorizontalDivider(Modifier.padding(vertical=12.dp),color=MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(Modifier.fillMaxWidth().weight(1f,fill=false),contentPadding=PaddingValues(bottom=16.dp)) {
                    item(key="host") {JamParticipantRow(state.hostName,"Host",null)}
                    items(state.members, key={it.id.ifBlank { "participant:${it.name}" }}) { member ->
                        JamParticipantRow(member.name,"Participant",if(state.host && member.id.isNotBlank()) ({removeMember=member}) else null,!state.busy)
                    }
                    if(state.members.isEmpty())item {Text("No other participants yet. Invite someone to listen together.",Modifier.padding(vertical=16.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
            }
        }
    }
    removeMember?.let { member ->
        GlassAlertDialog(onDismissRequest={removeMember=null}, title={Text("Remove ${member.name}?")},
            text={Text("Their playback and controls will disconnect. They cannot rejoin this Jam with the same account.")},
            confirmButton={TextButton(onClick={removeMember=null; onRemove(member.id)}){Text("Remove")}},
            dismissButton={TextButton(onClick={removeMember=null}){Text("Cancel")}})
    }
    if (showQr && state.url.isNotBlank()) {
        val bitmap = remember(state.url) {
            val matrix = com.google.zxing.MultiFormatWriter().encode(state.url, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888).apply {
                setPixels(IntArray(512 * 512) { i -> if (matrix[i % 512, i / 512]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }, 0, 512, 0, 0, 512, 512)
            }
        }
        GlassAlertDialog(onDismissRequest = { showQr = false }, title = { Text("Join this Jam") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.Image(bitmap.asImageBitmap(), "Scan to join this Jam", Modifier.fillMaxWidth().aspectRatio(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button( modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), onClick = { share("Join my Music Box Jam", state.url) }) { Text("Share link") }
                    FilledTonalButton(modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), onClick = {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Music Box Jam", state.url))
                        `in`.synthora.musicbox.network.NetworkFeedback.notify("Jam link copied")
                    }) { Text("Copy") }
                }
            } },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text("Done") } })
    }
    if (confirmEnd) GlassAlertDialog(onDismissRequest = { confirmEnd = false }, title = { Text(if(state.host) "End this Jam?" else "Leave this Jam?") },
        text = { Text(if(state.host) "Playback stops for the group and the invite expires." else "Your output disconnects. Everyone else can keep listening.") },
        confirmButton = { TextButton(onClick = { confirmEnd = false; if(state.host) onStop() else onLeave() }) { Text(if(state.host) "End Jam" else "Leave Jam") } },
        dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancel") } })
}

@Composable
private fun JamParticipantRow(name:String,role:String,onRemove:(()->Unit)?,enabled:Boolean=true) {
    Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(Icons.Outlined.Person,null,tint=if(role=="Host")MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(horizontal=12.dp,vertical=8.dp)) {
            Text(name,style=MaterialTheme.typography.bodyMedium,fontWeight=if(role=="Host")FontWeight.SemiBold else FontWeight.Normal,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(role,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(onRemove!=null)TextButton(onClick=onRemove,enabled=enabled,modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription="Remove $name"}) {Text("Remove",color=MaterialTheme.colorScheme.error)}
    }
}
