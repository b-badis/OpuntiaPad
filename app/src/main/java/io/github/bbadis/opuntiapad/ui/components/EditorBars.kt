package io.github.bbadis.opuntiapad.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.editor.EditorController
import io.github.bbadis.opuntiapad.editor.EditorDocument
import io.github.rosemoe.sora.widget.SelectionMovement

private val SYMBOLS = listOf(
    "{", "}", "(", ")", "[", "]", "<", ">", "\"", "'", "`", "=", ":", ";", ",", ".", "/", "\\", "|",
    "&", "!", "?", "#", "$", "%", "*", "+", "-", "_", "@", "~", "^",
)

/** Row of keys that are awkward to reach on a phone keyboard. */
@Composable
fun SymbolBar(controller: EditorController, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        // Code is always laid out left-to-right, even in RTL locales.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item { SymbolIcon(Icons.AutoMirrored.Filled.KeyboardTab, R.string.action_indent) { controller.indent() } }
                item {
                    SymbolIcon(Icons.AutoMirrored.Filled.KeyboardTab, R.string.action_unindent, Modifier.rotate(180f)) { controller.unindent() }
                }
                item { SymbolIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, R.string.action_move_left) { controller.move(SelectionMovement.LEFT) } }
                item { SymbolIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, R.string.action_move_right) { controller.move(SelectionMovement.RIGHT) } }
                item { SymbolIcon(Icons.Filled.KeyboardArrowUp, R.string.action_move_up) { controller.move(SelectionMovement.UP) } }
                item { SymbolIcon(Icons.Filled.KeyboardArrowDown, R.string.action_move_down) { controller.move(SelectionMovement.DOWN) } }
                items(SYMBOLS) { symbol ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .widthIn(min = 40.dp)
                            .heightIn(min = 44.dp)
                            .clickable(role = Role.Button) { controller.insert(symbol) },
                    ) {
                        Text(symbol, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolIcon(icon: ImageVector, label: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = stringResource(label), modifier = modifier)
    }
}

/** Bottom status line: caret position, language, encoding, line endings. */
@Composable
fun StatusBar(
    document: EditorDocument,
    controller: EditorController,
    onLanguageClick: () -> Unit,
    onEncodingClick: () -> Unit,
    onLineEndingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val style = MaterialTheme.typography.labelMedium
            if (document.readOnly) {
                Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.status_read_only), modifier = Modifier.size(14.dp))
            }
            var position = stringResource(R.string.status_position, controller.line, controller.column)
            if (controller.selectionLength > 0) {
                position += " " + pluralStringResource(R.plurals.status_selection, controller.selectionLength, controller.selectionLength)
            }
            Text(position, style = style, modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp))
            Text("·", style = style)
            Text(pluralStringResource(R.plurals.status_lines, controller.lineCount, controller.lineCount), style = style, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.widthIn(min = 8.dp))
            StatusChip(document.language.name, onLanguageClick)
            StatusChip(document.charset.name() + if (document.hasBom) " BOM" else "", onEncodingClick)
            StatusChip(document.lineEnding.label, onLineEndingClick)
        }
    }
}

@Composable
private fun StatusChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Notepad++-style tabs with modified indicator and close button. */
@Composable
fun DocumentTabs(
    tabs: List<EditorDocument>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (EditorDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PrimaryScrollableTabRow(selectedTabIndex = activeIndex.coerceAtLeast(0), edgePadding = 0.dp) {
            tabs.forEachIndexed { index, doc ->
                Tab(
                    selected = index == activeIndex,
                    onClick = { onSelect(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (doc.isDirty) Text("● ", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
                            Text(doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_close_tab),
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(18.dp)
                                    .clickable(role = Role.Button) { onClose(doc) },
                            )
                        }
                    },
                )
            }
        }
        HorizontalDivider()
    }
}
