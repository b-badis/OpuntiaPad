package io.github.bbadis.opuntiapad.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bbadis.opuntiapad.ui.screens.AboutScreen
import io.github.bbadis.opuntiapad.ui.screens.EditorScreen
import io.github.bbadis.opuntiapad.ui.screens.SettingsScreen
import io.github.bbadis.opuntiapad.ui.theme.OpuntiaPadTheme
import io.github.bbadis.opuntiapad.ui.theme.isDarkTheme

private enum class Screen { EDITOR, SETTINGS, ABOUT }

@Composable
fun OpuntiaPadRoot(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dark = isDarkTheme(settings.themeMode)
    val editorTheme = viewModel.catalog.theme(if (dark) settings.darkEditorTheme else settings.lightEditorTheme, dark)
    var screen by rememberSaveable { mutableStateOf(Screen.EDITOR) }

    OpuntiaPadTheme(dark = dark, dynamicColor = settings.dynamicColor) {
        // The editor stays composed underneath so its state (scroll, selection) survives visits to settings.
        EditorScreen(
            viewModel = viewModel,
            settings = settings,
            editorTheme = editorTheme,
            onOpenSettings = { screen = Screen.SETTINGS },
            onOpenAbout = { screen = Screen.ABOUT },
        )
        AnimatedContent(targetState = screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { target ->
            when (target) {
                Screen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    themes = viewModel.catalog.themes,
                    onUpdate = viewModel::updateSettings,
                    onBack = { screen = Screen.EDITOR },
                )
                Screen.ABOUT -> AboutScreen(viewModel.catalog.languages.size, onBack = { screen = Screen.EDITOR })
                Screen.EDITOR -> Unit
            }
        }
        // Registered last so it takes precedence over the editor's own back handling.
        BackHandler(enabled = screen != Screen.EDITOR) { screen = Screen.EDITOR }
    }
}
