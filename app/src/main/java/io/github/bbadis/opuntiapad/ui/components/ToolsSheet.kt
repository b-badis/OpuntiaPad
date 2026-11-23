package io.github.bbadis.opuntiapad.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.core.text.DocumentFormatter
import io.github.bbadis.opuntiapad.core.text.TextTools
import io.github.bbadis.opuntiapad.editor.EditorDocument
import io.github.bbadis.opuntiapad.ui.tools.Checksums
import io.github.bbadis.opuntiapad.ui.tools.DocumentTools

private data class ToolItem(val icon: ImageVector, val title: Int, val subtitle: String? = null, val action: () -> Unit)

private data class ToolSection(val title: Int, val items: List<ToolItem>)

/** Bottom sheet grouping every document tool. Tools that don't apply to the language are hidden. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsSheet(
    document: EditorDocument,
    tools: DocumentTools,
    hasSelection: Boolean,
    onShowChecksums: () -> Unit,
    onDismiss: () -> Unit,
) {
    val lang = document.language.id
    val editable = !document.readOnly
    val languageName = document.language.name
    val newTab = stringResource(R.string.tools_new_tab_summary)
    val formatSummary = stringResource(R.string.tools_format_summary, languageName)

    val sections = buildList {
        add(
            ToolSection(
                R.string.tools_section_format,
                buildList {
                    if (editable && DocumentFormatter.canFormat(lang)) add(ToolItem(Icons.Outlined.AutoFixHigh, R.string.tools_format, formatSummary) { tools.format(document) })
                    if (editable && DocumentFormatter.canMinify(lang)) add(ToolItem(Icons.Outlined.Compress, R.string.tools_minify) { tools.minify(document) })
                    if (DocumentFormatter.canConvertToYaml(lang)) add(ToolItem(Icons.Outlined.SwapHoriz, R.string.tools_json_to_yaml, newTab) { tools.convertToYaml(document) })
                    if (DocumentFormatter.canConvertToJson(lang)) add(ToolItem(Icons.Outlined.SwapHoriz, R.string.tools_yaml_to_json, newTab) { tools.convertToJson(document) })
                },
            ),
        )
        if (editable) {
            add(
                ToolSection(
                    R.string.tools_section_lines,
                    listOf(
                        ToolItem(Icons.AutoMirrored.Outlined.Comment, R.string.tools_toggle_comment) { tools.toggleComment(document) },
                        ToolItem(Icons.Outlined.ContentCopy, R.string.tools_duplicate_line) { tools.duplicateLines(document) },
                        ToolItem(Icons.Outlined.ArrowUpward, R.string.tools_move_up) { tools.moveLines(document, -1) },
                        ToolItem(Icons.Outlined.ArrowDownward, R.string.tools_move_down) { tools.moveLines(document, 1) },
                        ToolItem(Icons.Outlined.SortByAlpha, R.string.tools_sort_asc) { tools.sortLines(descending = false) },
                        ToolItem(Icons.Outlined.SortByAlpha, R.string.tools_sort_desc) { tools.sortLines(descending = true) },
                        ToolItem(Icons.Outlined.FilterAlt, R.string.tools_dedupe) { tools.removeDuplicateLines() },
                        ToolItem(Icons.Outlined.CleaningServices, R.string.tools_trim) { tools.trimTrailingWhitespace() },
                    ),
                ),
            )
            add(
                ToolSection(
                    R.string.tools_section_text,
                    listOf(
                        ToolItem(Icons.Outlined.TextFields, R.string.tools_upper) { tools.changeCase(TextTools.Case.UPPER) },
                        ToolItem(Icons.Outlined.TextFields, R.string.tools_lower) { tools.changeCase(TextTools.Case.LOWER) },
                        ToolItem(Icons.Outlined.TextFields, R.string.tools_title_case) { tools.changeCase(TextTools.Case.TITLE) },
                        ToolItem(Icons.AutoMirrored.Outlined.FormatIndentIncrease, R.string.tools_tabs_to_spaces) { tools.tabsToSpaces() },
                        ToolItem(Icons.AutoMirrored.Outlined.FormatIndentIncrease, R.string.tools_spaces_to_tabs) { tools.spacesToTabs() },
                    ),
                ),
            )
        }
        add(
            ToolSection(
                R.string.tools_section_encode,
                buildList {
                    if (editable) {
                        add(ToolItem(Icons.Outlined.Code, R.string.tools_base64_encode) { tools.base64Encode() })
                        add(ToolItem(Icons.Outlined.Code, R.string.tools_base64_decode) { tools.base64Decode() })
                        add(ToolItem(Icons.Outlined.Link, R.string.tools_url_encode) { tools.urlEncode() })
                        add(ToolItem(Icons.Outlined.Link, R.string.tools_url_decode) { tools.urlDecode() })
                    }
                    add(ToolItem(Icons.Outlined.Fingerprint, R.string.tools_checksums) { onShowChecksums() })
                },
            ),
        )
    }.filter { it.items.isNotEmpty() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.navigationBarsPadding()) {
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Text(stringResource(R.string.tools_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(if (hasSelection) R.string.tools_scope_selection else R.string.tools_scope_document),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            sections.forEachIndexed { index, section ->
                if (index > 0) item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                item {
                    Text(
                        stringResource(section.title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                section.items.forEach { tool ->
                    item {
                        ListRow(
                            headline = { Text(stringResource(tool.title)) },
                            supporting = tool.subtitle?.let { { Text(it) } },
                            leading = { Icon(tool.icon, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = {
                                tool.action()
                                onDismiss()
                            },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** Shows SHA-256, SHA-1 and MD5 of the selection or document, each copyable. */
@Composable
fun ChecksumsDialog(checksums: Checksums, onCopied: () -> Unit, onDismiss: () -> Unit) {
    @Suppress("DEPRECATION") // LocalClipboard (suspend API) is overkill for copying a short string.
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tools_checksums)) },
        text = {
            Column {
                listOf("SHA-256" to checksums.sha256, "SHA-1" to checksums.sha1, "MD5" to checksums.md5).forEach { (name, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            SelectionContainer {
                                Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(value))
                            onCopied()
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.action_copy), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}
