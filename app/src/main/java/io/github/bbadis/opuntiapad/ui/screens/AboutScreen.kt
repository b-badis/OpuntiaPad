package io.github.bbadis.opuntiapad.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.bbadis.opuntiapad.BuildConfig
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.ui.components.ListRow

const val REPOSITORY_URL = "https://github.com/b-badis/OpuntiaPad"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(languageCount: Int, onOpenLicenses: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val openUrl = { url: String ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        Unit
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(96.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    pluralStringResource(R.plurals.about_languages_count, languageCount, languageCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(R.string.about_description),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
                ListRow(
                    headline = { Text(stringResource(R.string.about_source_code)) },
                    supporting = { Text(REPOSITORY_URL.removePrefix("https://")) },
                    leading = { Icon(Icons.Outlined.Code, null) },
                    onClick = { openUrl(REPOSITORY_URL) },
                )
                ListRow(
                    headline = { Text(stringResource(R.string.about_report_issue)) },
                    leading = { Icon(Icons.Outlined.BugReport, null) },
                    onClick = { openUrl("$REPOSITORY_URL/issues") },
                )
                ListRow(
                    headline = { Text(stringResource(R.string.about_license)) },
                    supporting = { Text(stringResource(R.string.about_license_value)) },
                    leading = { Icon(Icons.Outlined.Description, null) },
                    onClick = { openUrl("$REPOSITORY_URL/blob/main/LICENSE") },
                )
                ListRow(
                    headline = { Text(stringResource(R.string.licenses_title)) },
                    supporting = { Text(stringResource(R.string.about_third_party_value)) },
                    leading = { Icon(Icons.Outlined.Extension, null) },
                    onClick = onOpenLicenses,
                )
                Text(
                    stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
