package io.github.bbadis.opuntiapad.ui.screens

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.ui.components.ListRow
import io.github.bbadis.opuntiapad.core.language.EditorTheme
import io.github.bbadis.opuntiapad.core.settings.EditorSettings
import io.github.bbadis.opuntiapad.core.settings.ThemeMode
import io.github.bbadis.opuntiapad.ui.components.ChoiceDialog
import java.util.Locale

/** Languages the UI is translated into (BCP-47 tag → native name). */
private val APP_LANGUAGES = listOf(
    "" to null,
    "en" to "English",
    "fr" to "Français",
    "ar" to "العربية",
    "es" to "Español",
    "de" to "Deutsch",
)

private enum class SettingsDialog { NONE, APP_LANGUAGE, LIGHT_THEME, DARK_THEME, TAB_SIZE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: EditorSettings,
    themes: List<EditorTheme>,
    onUpdate: ((EditorSettings) -> EditorSettings) -> Unit,
    onBack: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf(SettingsDialog.NONE) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Section(R.string.settings_section_general)
                val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').substringBefore('-')
                val currentLanguage = APP_LANGUAGES.firstOrNull { it.first == currentTag }?.second
                ClickableSetting(
                    title = stringResource(R.string.settings_app_language),
                    summary = currentLanguage ?: stringResource(R.string.settings_language_system),
                ) { dialog = SettingsDialog.APP_LANGUAGE }

                Section(R.string.settings_section_appearance)
                ListRow(
                    headline = { Text(stringResource(R.string.settings_theme)) },
                    supporting = {
                        val modes = listOf(
                            ThemeMode.SYSTEM to R.string.settings_theme_system,
                            ThemeMode.LIGHT to R.string.settings_theme_light,
                            ThemeMode.DARK to R.string.settings_theme_dark,
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            modes.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { onUpdate { it.copy(themeMode = mode) } },
                                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                ) { Text(stringResource(label), maxLines = 1) }
                            }
                        }
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchSetting(R.string.settings_dynamic_color, R.string.settings_dynamic_color_summary, settings.dynamicColor) { v ->
                        onUpdate { it.copy(dynamicColor = v) }
                    }
                }
                ClickableSetting(
                    stringResource(R.string.settings_light_editor_theme),
                    themes.firstOrNull { it.id == settings.lightEditorTheme }?.name.orEmpty(),
                ) { dialog = SettingsDialog.LIGHT_THEME }
                ClickableSetting(
                    stringResource(R.string.settings_dark_editor_theme),
                    themes.firstOrNull { it.id == settings.darkEditorTheme }?.name.orEmpty(),
                ) { dialog = SettingsDialog.DARK_THEME }

                Section(R.string.settings_section_editor)
                FontSizeSetting(settings.fontSize) { v -> onUpdate { it.copy(fontSize = v) } }
                SwitchSetting(R.string.settings_word_wrap, R.string.settings_word_wrap_summary, settings.wordWrap) { v -> onUpdate { it.copy(wordWrap = v) } }
                SwitchSetting(R.string.settings_line_numbers, null, settings.lineNumbers) { v -> onUpdate { it.copy(lineNumbers = v) } }
                SwitchSetting(R.string.settings_pin_line_numbers, R.string.settings_pin_line_numbers_summary, settings.pinLineNumbers) { v ->
                    onUpdate { it.copy(pinLineNumbers = v) }
                }
                SwitchSetting(R.string.settings_highlight_line, null, settings.highlightCurrentLine) { v -> onUpdate { it.copy(highlightCurrentLine = v) } }
                SwitchSetting(R.string.settings_whitespace, null, settings.showWhitespace) { v -> onUpdate { it.copy(showWhitespace = v) } }
                SwitchSetting(R.string.settings_sticky_scroll, R.string.settings_sticky_scroll_summary, settings.stickyScroll) { v ->
                    onUpdate { it.copy(stickyScroll = v) }
                }
                SwitchSetting(R.string.settings_keep_screen_on, null, settings.keepScreenOn) { v -> onUpdate { it.copy(keepScreenOn = v) } }

                Section(R.string.settings_section_typing)
                ClickableSetting(stringResource(R.string.settings_tab_size), settings.tabSize.toString()) { dialog = SettingsDialog.TAB_SIZE }
                SwitchSetting(R.string.settings_use_spaces, null, settings.useSpaces) { v -> onUpdate { it.copy(useSpaces = v) } }
                SwitchSetting(R.string.settings_auto_close, null, settings.autoCloseBrackets) { v -> onUpdate { it.copy(autoCloseBrackets = v) } }
                SwitchSetting(R.string.settings_auto_complete, R.string.settings_auto_complete_summary, settings.autoComplete) { v ->
                    onUpdate { it.copy(autoComplete = v) }
                }
                SwitchSetting(R.string.settings_symbol_bar, R.string.settings_symbol_bar_summary, settings.symbolBar) { v -> onUpdate { it.copy(symbolBar = v) } }
            }
        }
    }

    val dismiss = { dialog = SettingsDialog.NONE }
    when (dialog) {
        SettingsDialog.APP_LANGUAGE -> {
            val systemLabel = stringResource(R.string.settings_language_system)
            ChoiceDialog(
                title = stringResource(R.string.settings_app_language),
                options = APP_LANGUAGES,
                selected = APP_LANGUAGES.firstOrNull {
                    it.first == AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').substringBefore('-')
                },
                label = { it.second ?: systemLabel },
                onSelect = { (tag, _) ->
                    dismiss()
                    // Per-app language (Android 13+ native, AppCompat back-port below). Recreates the activity.
                    AppCompatDelegate.setApplicationLocales(
                        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                    )
                },
                onDismiss = dismiss,
            )
        }
        SettingsDialog.LIGHT_THEME, SettingsDialog.DARK_THEME -> {
            val dark = dialog == SettingsDialog.DARK_THEME
            val options = themes.filter { it.dark == dark }
            ChoiceDialog(
                title = stringResource(if (dark) R.string.settings_dark_editor_theme else R.string.settings_light_editor_theme),
                options = options,
                selected = options.firstOrNull { it.id == if (dark) settings.darkEditorTheme else settings.lightEditorTheme },
                label = EditorTheme::name,
                onSelect = { theme ->
                    dismiss()
                    onUpdate { if (dark) it.copy(darkEditorTheme = theme.id) else it.copy(lightEditorTheme = theme.id) }
                },
                onDismiss = dismiss,
            )
        }
        SettingsDialog.TAB_SIZE -> ChoiceDialog(
            title = stringResource(R.string.settings_tab_size),
            options = listOf(2, 3, 4, 8),
            selected = settings.tabSize,
            label = Int::toString,
            onSelect = { size ->
                dismiss()
                onUpdate { it.copy(tabSize = size) }
            },
            onDismiss = dismiss,
        )
        SettingsDialog.NONE -> Unit
    }
}

@Composable
private fun Section(title: Int) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ClickableSetting(title: String, summary: String, onClick: () -> Unit) {
    ListRow(
        headline = { Text(title) },
        supporting = { Text(summary) },
        onClick = onClick,
    )
}

@Composable
private fun SwitchSetting(title: Int, summary: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListRow(
        headline = { Text(stringResource(title)) },
        supporting = summary?.let { { Text(stringResource(it)) } },
        trailing = { Switch(checked = checked, onCheckedChange = null) },
        role = Role.Switch,
        onClick = { onChange(!checked) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSizeSetting(value: Float, onChange: (Float) -> Unit) {
    val state = remember {
        SliderState(
            value,
            (EditorSettings.MAX_FONT - EditorSettings.MIN_FONT).toInt() - 1,
            EditorSettings.MIN_FONT..EditorSettings.MAX_FONT,
        )
    }
    val currentOnChange by rememberUpdatedState(onChange)
    LaunchedEffect(value) { state.value = value }
    ListRow(
        headline = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_font_size), Modifier.weight(1f))
                Text(stringResource(R.string.settings_font_size_value, String.format(Locale.ROOT, "%.0f", state.value)))
            }
        },
        supporting = {
            Slider(
                state = state,
                onValueChange = { state.value = it },
                onValueChangeFinished = { currentOnChange(state.value) },
            )
        },
    )
}
