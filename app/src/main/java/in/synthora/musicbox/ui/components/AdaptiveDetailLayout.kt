package `in`.synthora.musicbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveDetailLayout(bottomPadding: Dp, spacing: Dp = 0.dp, onFloatingPlay: (() -> Unit)? = null,
    hero: @Composable () -> Unit, content: LazyListScope.() -> Unit) {
    val config = LocalConfiguration.current
    val landscape = config.smallestScreenWidthDp >= 600 && config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    if (landscape) Row(Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
        BoxWithConstraints(Modifier.weight(0.85f).fillMaxHeight().clipToBounds()) {
            val paneHeight = maxHeight
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CompositionLocalProvider(LocalDetailPaneHeight provides paneHeight) { hero() }
            }
        }
        LazyColumn(Modifier.weight(1.15f).fillMaxHeight().stableStatusBarsPadding(),
            contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    } else Box(Modifier.fillMaxSize()) {
      val listState = rememberLazyListState()
      val showPlay by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
        LazyColumn(Modifier.fillMaxSize().testTag("Collection tracks"), state = listState,
            contentPadding = PaddingValues(bottom = bottomPadding + if (onFloatingPlay != null) 88.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)) {
            item(key = "hero") { hero() }
            content()
        }
        if (showPlay && onFloatingPlay != null) FloatingActionButton(onClick = onFloatingPlay,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = bottomPadding + 16.dp)) {
            Text("▶ Play", Modifier.padding(horizontal = 16.dp))
        }
    }
}
