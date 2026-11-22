package io.github.bbadis.opuntiapad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.editor.EditorController
import io.github.bbadis.opuntiapad.editor.SearchQuery
import kotlinx.coroutines.delay

/** Find / replace panel with case, whole-word and regex options. */
@Composable
fun SearchPanel(controller: EditorController, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var matchCase by rememberSaveable { mutableStateOf(false) }
    var wholeWord by rememberSaveable { mutableStateOf(false) }
    var regex by rememberSaveable { mutableStateOf(false) }
    var showReplace by rememberSaveable { mutableStateOf(false) }
    val focus = androidx.compose.runtime.remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(query, matchCase, wholeWord, regex) {
        delay(150) // debounce typing
        controller.search(SearchQuery(query, matchCase, wholeWord, regex))
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    isError = controller.searchError,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    supportingText = null,
                    trailingIcon = {
                        val label = when {
                            controller.searchError -> stringResource(R.string.search_invalid_regex)
                            query.isEmpty() -> ""
                            controller.matchCount == 0 -> stringResource(R.string.search_no_results)
                            else -> stringResource(R.string.search_count, controller.matchIndex + 1, controller.matchCount)
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { controller.findNext() }),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                )
                IconButton(onClick = { controller.findPrevious() }) {
                    Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.search_previous))
                }
                IconButton(onClick = { controller.findNext() }) {
                    Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.search_next))
                }
                IconToggleButton(checked = showReplace, onCheckedChange = { showReplace = it }) {
                    Icon(Icons.Filled.FindReplace, stringResource(R.string.search_toggle_replace))
                }
                IconButton(onClick = {
                    controller.stopSearch()
                    onClose()
                }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.search_close))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = matchCase, onClick = { matchCase = !matchCase }, label = { Text(stringResource(R.string.search_match_case)) })
                FilterChip(selected = wholeWord, onClick = { wholeWord = !wholeWord }, label = { Text(stringResource(R.string.search_whole_word)) })
                FilterChip(selected = regex, onClick = { regex = !regex }, label = { Text(".*") })
            }
            if (showReplace) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = { replacement = it },
                        placeholder = { Text(stringResource(R.string.replace_hint)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { controller.replaceCurrent(replacement) }, enabled = controller.matchCount > 0) {
                        Text(stringResource(R.string.replace_one))
                    }
                    TextButton(onClick = { controller.replaceAll(replacement) }, enabled = controller.matchCount > 0) {
                        Text(stringResource(R.string.replace_all))
                    }
                }
            }
        }
    }
    HorizontalDivider()
}
