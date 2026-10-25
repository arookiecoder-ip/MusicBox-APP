package `in`.synthora.musicbox.ui.components

import androidx.compose.runtime.Composable
import `in`.synthora.musicbox.utils.rememberJukeHaptics

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    val haptic = rememberJukeHaptics()
    PlaylistNameDialog(title = "New playlist", confirmLabel = "Create", onDismiss = onDismiss,
        onConfirm = { name -> haptic.heavyClick(); onCreate(name) })
}
