package io.github.bbadis.opuntiapad.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.ui.components.ListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A third-party component bundled in the APK (assets/licenses/components.json). */
private data class Component(
    val name: String,
    val detail: String,
    val copyright: String,
    val license: String,
    val url: String,
    val file: String,
)

private const val LICENSES_DIR = "licenses"

private fun loadComponents(context: Context): List<Component> {
    val json = context.assets.open("$LICENSES_DIR/components.json").bufferedReader().use { it.readText() }
    val array = JSONObject(json).getJSONArray("components")
    return (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Component(o.getString("name"), o.getString("detail"), o.getString("copyright"), o.getString("license"), o.getString("url"), o.getString("file"))
    }
}

/** Lists the open-source components shipped in the app and shows their full license texts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val components by produceState(emptyList<Component>()) {
        value = withContext(Dispatchers.IO) { loadComponents(context) }
    }
    var openFile by rememberSaveable { mutableStateOf<String?>(null) }
    val open = components.firstOrNull { it.file == openFile }

    BackHandler(enabled = open != null) { openFile = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(open?.name ?: stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (open != null) openFile = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (open != null) {
            LicenseText(open, Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Text(
                        stringResource(R.string.licenses_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(16.dp),
                    )
                }
                items(components, key = { it.name }) { c ->
                    ListRow(
                        headline = { Text(c.name) },
                        supporting = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(c.detail)
                                Text(c.copyright, style = MaterialTheme.typography.bodySmall)
                                Text(c.license, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                        },
                        modifier = Modifier.widthIn(max = 720.dp),
                        onClick = { openFile = c.file },
                    )
                    HorizontalDivider(Modifier.widthIn(max = 720.dp))
                }
            }
        }
    }
}

@Composable
private fun LicenseText(component: Component, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Split into paragraphs so long notices (100+ KB) stay smooth in a lazy list.
    val paragraphs by produceState(emptyList<String>(), component.file) {
        value = withContext(Dispatchers.IO) {
            context.assets.open("$LICENSES_DIR/${component.file}").bufferedReader().use { it.readText() }.split("\n\n")
        }
    }
    SelectionContainer(modifier) {
        LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Text("${component.copyright}\n${component.url}\n", style = MaterialTheme.typography.bodyMedium)
            }
            items(paragraphs) { p ->
                Text(
                    p,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}
