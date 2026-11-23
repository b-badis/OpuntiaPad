package io.github.bbadis.opuntiapad.core.settings

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class EditorSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val lightEditorTheme: String = "github-light",
    val darkEditorTheme: String = "one-dark-pro",
    val fontSize: Float = 14f,
    val tabSize: Int = 4,
    val useSpaces: Boolean = true,
    val wordWrap: Boolean = false,
    val lineNumbers: Boolean = true,
    val pinLineNumbers: Boolean = false,
    val highlightCurrentLine: Boolean = true,
    val showWhitespace: Boolean = false,
    val autoCloseBrackets: Boolean = true,
    val autoComplete: Boolean = false,
    val symbolBar: Boolean = true,
    val stickyScroll: Boolean = false,
    val keepScreenOn: Boolean = false,
    val restoreSession: Boolean = true,
) {
    companion object {
        const val MIN_FONT = 8f
        const val MAX_FONT = 40f
    }
}

data class RecentFile(val uri: Uri, val name: String, val openedAt: Long)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val lightTheme = stringPreferencesKey("light_editor_theme")
        val darkTheme = stringPreferencesKey("dark_editor_theme")
        val fontSize = floatPreferencesKey("font_size")
        val tabSize = intPreferencesKey("tab_size")
        val useSpaces = booleanPreferencesKey("use_spaces")
        val wordWrap = booleanPreferencesKey("word_wrap")
        val lineNumbers = booleanPreferencesKey("line_numbers")
        val pinLineNumbers = booleanPreferencesKey("pin_line_numbers")
        val highlightLine = booleanPreferencesKey("highlight_current_line")
        val whitespace = booleanPreferencesKey("show_whitespace")
        val autoClose = booleanPreferencesKey("auto_close_brackets")
        val autoComplete = booleanPreferencesKey("auto_complete")
        val symbolBar = booleanPreferencesKey("symbol_bar")
        val stickyScroll = booleanPreferencesKey("sticky_scroll")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val restoreSession = booleanPreferencesKey("restore_session")
        val recents = stringPreferencesKey("recent_files")
    }

    val settings: Flow<EditorSettings> = store.data.map { it.toSettings() }

    suspend fun update(transform: (EditorSettings) -> EditorSettings) {
        store.edit { p ->
            val s = transform(p.toSettings())
            p[Keys.themeMode] = s.themeMode.name
            p[Keys.dynamicColor] = s.dynamicColor
            p[Keys.lightTheme] = s.lightEditorTheme
            p[Keys.darkTheme] = s.darkEditorTheme
            p[Keys.fontSize] = s.fontSize.coerceIn(EditorSettings.MIN_FONT, EditorSettings.MAX_FONT)
            p[Keys.tabSize] = s.tabSize
            p[Keys.useSpaces] = s.useSpaces
            p[Keys.wordWrap] = s.wordWrap
            p[Keys.lineNumbers] = s.lineNumbers
            p[Keys.pinLineNumbers] = s.pinLineNumbers
            p[Keys.highlightLine] = s.highlightCurrentLine
            p[Keys.whitespace] = s.showWhitespace
            p[Keys.autoClose] = s.autoCloseBrackets
            p[Keys.autoComplete] = s.autoComplete
            p[Keys.symbolBar] = s.symbolBar
            p[Keys.stickyScroll] = s.stickyScroll
            p[Keys.keepScreenOn] = s.keepScreenOn
            p[Keys.restoreSession] = s.restoreSession
        }
    }

    private fun Preferences.toSettings(): EditorSettings {
        val d = EditorSettings()
        return EditorSettings(
            themeMode = this[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            dynamicColor = this[Keys.dynamicColor] ?: d.dynamicColor,
            lightEditorTheme = this[Keys.lightTheme] ?: d.lightEditorTheme,
            darkEditorTheme = this[Keys.darkTheme] ?: d.darkEditorTheme,
            fontSize = this[Keys.fontSize] ?: d.fontSize,
            tabSize = this[Keys.tabSize] ?: d.tabSize,
            useSpaces = this[Keys.useSpaces] ?: d.useSpaces,
            wordWrap = this[Keys.wordWrap] ?: d.wordWrap,
            lineNumbers = this[Keys.lineNumbers] ?: d.lineNumbers,
            pinLineNumbers = this[Keys.pinLineNumbers] ?: d.pinLineNumbers,
            highlightCurrentLine = this[Keys.highlightLine] ?: d.highlightCurrentLine,
            showWhitespace = this[Keys.whitespace] ?: d.showWhitespace,
            autoCloseBrackets = this[Keys.autoClose] ?: d.autoCloseBrackets,
            autoComplete = this[Keys.autoComplete] ?: d.autoComplete,
            symbolBar = this[Keys.symbolBar] ?: d.symbolBar,
            stickyScroll = this[Keys.stickyScroll] ?: d.stickyScroll,
            keepScreenOn = this[Keys.keepScreenOn] ?: d.keepScreenOn,
            restoreSession = this[Keys.restoreSession] ?: d.restoreSession,
        )
    }

    // --- Recent files ------------------------------------------------------------------------

    val recentFiles: Flow<List<RecentFile>> = store.data.map { p -> decodeRecents(p[Keys.recents]) }

    suspend fun addRecent(uri: Uri, name: String) {
        store.edit { p ->
            val list = decodeRecents(p[Keys.recents]).filterNot { it.uri == uri }
            p[Keys.recents] = encodeRecents((listOf(RecentFile(uri, name, System.currentTimeMillis())) + list).take(MAX_RECENTS))
        }
    }

    suspend fun removeRecent(uri: Uri) {
        store.edit { p -> p[Keys.recents] = encodeRecents(decodeRecents(p[Keys.recents]).filterNot { it.uri == uri }) }
    }

    suspend fun clearRecents() {
        store.edit { it.remove(Keys.recents) }
    }

    private fun encodeRecents(list: List<RecentFile>): String =
        list.joinToString("\n") { "${it.openedAt}\t${it.name.replace('\t', ' ').replace('\n', ' ')}\t${it.uri}" }

    private fun decodeRecents(raw: String?): List<RecentFile> = raw.orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.split('\t', limit = 3)
        if (parts.size != 3) return@mapNotNull null
        RecentFile(parts[2].toUri(), parts[1], parts[0].toLongOrNull() ?: 0L)
    }.toList()

    private companion object {
        const val MAX_RECENTS = 30
    }
}
