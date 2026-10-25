package `in`.synthora.musicbox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import `in`.synthora.musicbox.ui.theme.dockColor

@Composable
private fun PlaylistDialogSurface(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RectangleShape, color = dockColor(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))) {
            Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
        }
    }
}

@Composable
fun PlaylistNameDialog(title: String, initialName: String = "", confirmLabel: String,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(TextFieldValue(initialName, TextRange(0, initialName.length))) }
    var submitted by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val trimmed = name.text.trim()
    val enabled = trimmed.isNotEmpty() && !submitted && (initialName.isEmpty() || trimmed != initialName.trim())
    val submit: () -> Unit = { if (enabled && !submitted) { submitted = true; keyboard?.hide(); onConfirm(trimmed) } }
    PlaylistDialogSurface(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        TextField(value = name, onValueChange = { name = it }, singleLine = true,
            label = { Text("Playlist name") }, shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            colors = TextFieldDefaults.colors(focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }))
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(onClick = submit, enabled = enabled, shape = RectangleShape) { Text(confirmLabel) }
        }
        LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus(); keyboard?.show() }
    }
}

@Composable
fun PlaylistDeleteDialog(name: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var submitted by remember(name) { mutableStateOf(false) }
    PlaylistDialogSurface(onDismiss) {
        Text("Delete playlist?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text("This removes the playlist from your YouTube Music account. This cannot be undone.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(enabled = !submitted, onClick = { if (!submitted) { submitted = true; onDelete() } }, shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError)) { Text("Delete") }
        }
    }
}
