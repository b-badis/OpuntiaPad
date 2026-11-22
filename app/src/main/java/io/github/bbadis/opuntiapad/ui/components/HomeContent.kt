package io.github.bbadis.opuntiapad.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.core.settings.RecentFile
import io.github.bbadis.opuntiapad.editor.EditorDocument

/** Empty state shown when no document is open. */
@Composable
fun HomeContent(
    recents: List<RecentFile>,
    showRecents: Boolean,
    onOpen: () -> Unit,
    onNew: () -> Unit,
    onOpenRecent: (RecentFile) -> Unit,
    onRemoveRecent: (RecentFile) -> Unit,
    onClearRecents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 560.dp).padding(24.dp),
            ) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onOpen) {
                        Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_open))
                    }
                    FilledTonalButton(onClick = onNew) {
                        Icon(Icons.AutoMirrored.Outlined.NoteAdd, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_new))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.TipsAndUpdates, null)
                        Spacer(Modifier.size(12.dp))
                        Text(stringResource(R.string.home_tip), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (showRecents) {
            recentItems(recents, onOpenRecent, onRemoveRecent, onClearRecents, Modifier.widthIn(max = 560.dp))
        }
    }
}

/** Side panel (tablets) / drawer content (phones): open tabs and recent files. */
@Composable
fun DocumentsPanel(
    tabs: List<EditorDocument>,
    activeIndex: Int,
    recents: List<RecentFile>,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (EditorDocument) -> Unit,
    onOpenRecent: (RecentFile) -> Unit,
    onRemoveRecent: (RecentFile) -> Unit,
    onClearRecents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        if (tabs.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.open_tabs)) }
            items(tabs.size, key = { tabs[it].id }) { index ->
                val doc = tabs[index]
                ListRow(
                    headline = { Text((if (doc.isDirty) "● " else "") + doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supporting = { Text(doc.language.name) },
                    leading = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null) },
                    trailing = {
                        IconButton(onClick = { onCloseTab(doc) }) { Icon(Icons.Filled.Close, stringResource(R.string.action_close_tab)) }
                    },
                    selected = index == activeIndex,
                    onClick = { onSelectTab(index) },
                )
            }
        }
        recentItems(recents, onOpenRecent, onRemoveRecent, onClearRecents, Modifier)
    }
}

private fun LazyListScope.recentItems(
    recents: List<RecentFile>,
    onOpen: (RecentFile) -> Unit,
    onRemove: (RecentFile) -> Unit,
    onClear: () -> Unit,
    itemModifier: Modifier,
) {
    item {
        Row(itemModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(stringResource(R.string.recent_files), Modifier.weight(1f))
            if (recents.isNotEmpty()) TextButton(onClick = onClear) { Text(stringResource(R.string.action_clear_recents)) }
        }
    }
    if (recents.isEmpty()) {
        item {
            Text(
                stringResource(R.string.no_recent_files),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = itemModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    items(recents, key = { it.uri.toString() }) { file ->
        ListRow(
            headline = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supporting = {
                Text(DateUtils.getRelativeTimeSpanString(file.openedAt).toString(), maxLines = 1)
            },
            leading = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null) },
            trailing = {
                IconButton(onClick = { onRemove(file) }) { Icon(Icons.Filled.Close, stringResource(R.string.action_remove)) }
            },
            modifier = itemModifier,
            onClick = { onOpen(file) },
        )
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
