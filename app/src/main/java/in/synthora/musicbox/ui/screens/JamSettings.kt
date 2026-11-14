package `in`.synthora.musicbox.ui.screens

import androidx.compose.ui.graphics.asImageBitmap
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val context = LocalContext.current
    val share = rememberShareAction()
    var confirmEnd by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var removeMember by remember { mutableStateOf<`in`.synthora.musicbox.viewmodels.JamMember?>(null) }
    LaunchedEffect(Unit) { jam.refresh() }
    Column(Modifier.padding(16.dp)) {
        Text("Jam", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(if (state.active) "${state.memberCount} people · one shared queue" else "Listen together. Your likes and playlists stay yours.", style = MaterialTheme.typography.bodyMedium)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = jam::refresh) { Text("Retry") } }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        if (state.active) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(modifier = Modifier.weight(1f).height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), enabled = state.url.isNotBlank(), onClick = { showQr = true }) { Text("Manage") }
                Button(modifier = Modifier.weight(1f).height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error), enabled = !state.busy, onClick = { confirmEnd = true }) { if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error) else Text(if(state.host) "End Jam" else "Leave Jam") }
            }
        } else Button(enabled = !state.busy, onClick = jam::start, modifier = Modifier.fillMaxWidth().height(48.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) { if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Start Jam") }
        }
        if (state.active && state.host) {
            HorizontalDivider(Modifier.padding(vertical=16.dp))
            Text("Audio outputs", style=MaterialTheme.typography.titleSmall)
            Text("Choose one output, or select several to play together.", style=MaterialTheme.typography.bodySmall)
            val multiple=state.playback["mode"]?.toString()?.trim('"') == "multiple"
            val selected=(state.playback["selected"] as? kotlinx.serialization.json.JsonArray)?.map { it.toString().trim('"') }.orEmpty()
            Row(Modifier.fillMaxWidth(), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text("Play on multiple outputs", Modifier.weight(1f))
                Switch(multiple, { enabled -> jam.configure(if(enabled) "multiple" else "single", if(enabled) selected else selected.take(1)) }, enabled=!state.busy)
            }
            state.outputs.forEach { output ->
                Row(Modifier.fillMaxWidth(), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(output.name, style=MaterialTheme.typography.bodyMedium)
                        Text(if(!output.online) "Offline" else output.error.ifBlank { "Connected" }, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Checkbox(output.id in selected, { checked ->
                        jam.configure(if(multiple) "multiple" else "single", if(!multiple) if(checked) listOf(output.id) else emptyList() else if(checked) selected+output.id else selected-output.id)
                    }, enabled=!state.busy && (output.online || output.id in selected))
                }
            }
            if(state.outputs.isEmpty()) Text("Connected outputs will appear here.", style=MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.padding(vertical=16.dp))
            Text("Participants", style=MaterialTheme.typography.titleSmall)
            Text("You · Host", Modifier.padding(vertical=8.dp))
            state.members.forEach { member ->
                Row(Modifier.fillMaxWidth(), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Text(member.name, Modifier.weight(1f))
                    TextButton(onClick={removeMember=member}, enabled=!state.busy) { Text("Remove", color=MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    removeMember?.let { member ->
        GlassAlertDialog(onDismissRequest={removeMember=null}, title={Text("Remove ${member.name}?")},
            text={Text("Their playback and controls will disconnect. They cannot rejoin this Jam with the same account.")},
            confirmButton={TextButton(onClick={removeMember=null; jam.remove(member.id)}){Text("Remove")}},
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
                    Button( modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), onClick = { share("Join my Music Box Jam", state.url) }) { Text("Share link") }
                    FilledTonalButton(modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), onClick = {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Music Box Jam", state.url))
                        `in`.synthora.musicbox.network.NetworkFeedback.notify("Jam link copied")
                    }) { Text("Copy") }
                }
            } },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text("Done") } })
    }
    if (confirmEnd) GlassAlertDialog(onDismissRequest = { confirmEnd = false }, title = { Text(if(state.host) "End this Jam?" else "Leave this Jam?") },
        text = { Text(if(state.host) "Playback stops for the group and the invite expires." else "Your output disconnects. Everyone else can keep listening.") },
        confirmButton = { TextButton(onClick = { confirmEnd = false; if(state.host) jam.stop() else jam.leave() }) { Text(if(state.host) "End Jam" else "Leave Jam") } },
        dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancel") } })
}
