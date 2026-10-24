package `in`.synthora.musicbox.ui.components

import `in`.synthora.musicbox.ui.components.stableStatusBarsPadding

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import `in`.synthora.musicbox.ui.theme.GlassLevel
import `in`.synthora.musicbox.ui.theme.GlassShapes
import `in`.synthora.musicbox.ui.theme.glassFloat
import `in`.synthora.musicbox.utils.rememberJukeHaptics

/** A queued hide from an old field must never close a newer field's IME. Main-thread only. */
private object SearchImeOwner {
    private val owners = java.util.WeakHashMap<android.view.View, Any>()
    fun claim(view: android.view.View, token: Any) { owners[view] = token }
    fun owns(view: android.view.View, token: Any) = owners[view] === token
    fun finish(view: android.view.View, token: Any) { if (owns(view, token)) owners.remove(view) }
}

/**
 * One compact header shared by Library and Search: title + actions at rest, and the same
 * pill turns into the search field when [open] (tap the search icon). Saves a whole row.
 */
@Composable
fun SearchHeader(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    /** Bump to focus the field, select everything in it and show the keyboard (Search tab re-tap). */
    selectAllTrigger: Int = 0,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val haptic = rememberJukeHaptics()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val window = LocalWindowInfo.current
    val view = LocalView.current

    // The field keeps its own selection so a re-tap can select the whole query for replacing.
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query) {
        if (field.text != query) field = TextFieldValue(query, TextRange(query.length))
    }
    var handledSelectAll by remember { mutableIntStateOf(selectAllTrigger) }
    Row(
        modifier = modifier
            .stableStatusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .glassFloat(GlassShapes.Pill, GlassLevel.Regular)
            .then(if (!open) Modifier.clickable { onOpenChange(true) } else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        run {
            if (open) {
                var fieldFocused by remember { mutableStateOf(false) }
                val sessionToken = remember(selectAllTrigger) { Any() }
                var imeWanted by remember(selectAllTrigger) { mutableStateOf(true) }
                var imeJob by remember { mutableStateOf<Job?>(null) }
                fun hideIme() {
                    keyboard?.hide()
                    ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
                }
                DisposableEffect(sessionToken) {
                    SearchImeOwner.claim(view, sessionToken)
                    onDispose {
                        imeJob?.cancel()
                        if (SearchImeOwner.owns(view, sessionToken)) {
                            hideIme()
                            view.post {
                                if (SearchImeOwner.owns(view, sessionToken)) {
                                    ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
                                    SearchImeOwner.finish(view, sessionToken)
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(sessionToken, window.isWindowFocused, imeWanted) {
                    if (!imeWanted) return@LaunchedEffect
                    imeJob=currentCoroutineContext()[Job]
                    // Compose focus can be established before Android grants window focus.
                    // A window-focus change restarts this request; never wait indefinitely.
                    androidx.compose.runtime.withFrameNanos { }
                    if (!view.hasFocus()) view.requestFocus()
                    focus.requestFocus()
                    if (selectAllTrigger != handledSelectAll) {
                        handledSelectAll = selectAllTrigger
                        field = field.copy(selection = TextRange(0, field.text.length))
                    }
                    if (!window.isWindowFocused) return@LaunchedEffect
                    var visibleChecks=0
                    repeat(20) {
                        androidx.compose.runtime.withFrameNanos { }
                        if (!fieldFocused) focus.requestFocus()
                        keyboard?.show()
                        ViewCompat.getWindowInsetsController(view)?.show(WindowInsetsCompat.Type.ime())
                        delay(100)
                        val visible=fieldFocused && ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                        visibleChecks=if(visible) visibleChecks+1 else 0
                        // Do not mistake an old input connection's visible IME for this field.
                        if(visibleChecks>=3) return@LaunchedEffect
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        haptic.click()
                        focusManager.clearFocus(force = true)
                        imeWanted=false
                        imeJob?.cancel()
                        keyboard?.hide()
                        ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
                        onQueryChange("")
                        onOpenChange(false)
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close search") }
                    BasicTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            if (it.text != query) onQueryChange(it.text)
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            imeWanted=false
                            imeJob?.cancel()
                            keyboard?.hide()
                            ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
                            view.post { if(SearchImeOwner.owns(view, sessionToken) && !imeWanted) ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime()) }
                            onSearch()
                        }),
                        modifier = Modifier.weight(1f).focusRequester(focus).onFocusChanged { fieldFocused = it.isFocused },
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        placeholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { haptic.click(); onQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 14.dp)
                    )
                    IconButton(onClick = { haptic.click(); onOpenChange(true) }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    actions()
                }
            }
        }
    }
}
