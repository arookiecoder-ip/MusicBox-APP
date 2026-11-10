package `in`.synthora.musicbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import `in`.synthora.musicbox.services.ArtistPortraits
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.models.ArtistCredit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPickerSheet(artists: List<ArtistCredit>, onDismiss: () -> Unit,
    loadPortrait: suspend (ArtistCredit) -> String? = ArtistPortraits::load, onArtist: (ArtistCredit) -> Unit) {
    val credits = remember(artists) { artists.filter { it.name.isNotBlank() }.distinctBy { it.id?.takeIf(String::isNotBlank) ?: it.name.trim().lowercase(java.util.Locale.ROOT) } }
    val images = remember(credits) { mutableStateMapOf<ArtistCredit, String>() }
    val loader by rememberUpdatedState(loadPortrait)
    // This effect exists only while the sheet is composed. Dismissal or a new
    // credit list cancels outstanding requests and prevents stale portraits.
    LaunchedEffect(credits) {
        credits.chunked(2).forEach { batch -> coroutineScope {
            batch.forEach { artist -> launch {
                try {
                    withTimeoutOrNull(5_000) { loader(artist) }?.takeIf(String::isNotBlank)?.let { images[artist] = it }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* Keep a usable placeholder and artist navigation. */ }
            } }
        } }
    }
    ModalBottomSheet(containerColor = `in`.synthora.musicbox.ui.theme.glassSheetColor(), onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Artists", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close artists") }
            }
            credits.forEach { artist ->
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onArtist(artist) }
                    .padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("Artist portrait ${artist.id ?: artist.name}"), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        images[artist]?.let { ListArtwork(it, 44.dp, Modifier.fillMaxSize()) }
                    }
                    Text(artist.name, Modifier.weight(1f).padding(start = 16.dp), style = MaterialTheme.typography.titleMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
