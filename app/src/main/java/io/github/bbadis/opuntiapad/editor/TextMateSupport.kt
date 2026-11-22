package io.github.bbadis.opuntiapad.editor

import android.content.Context
import android.util.Log
import io.github.bbadis.opuntiapad.core.language.EditorTheme
import io.github.bbadis.opuntiapad.core.language.Language
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource
import java.util.concurrent.atomic.AtomicBoolean
import io.github.rosemoe.sora.lang.Language as SoraLanguage

/**
 * Owns the TextMate grammar & theme registries (process-wide singletons in Sora).
 * Grammars are parsed once, in the background, at application start.
 */
object TextMateSupport {
    private const val TAG = "TextMateSupport"
    private const val LANGUAGES_PATH = "textmate/languages.json"

    private val initialized = AtomicBoolean(false)
    private val ready = CompletableDeferred<Boolean>()
    private val loadedThemes = mutableSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun initialize(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.applicationContext.assets))
            scope.launch {
                val ok = runCatching { GrammarRegistry.getInstance().loadGrammars(LANGUAGES_PATH) }
                    .onFailure { Log.e(TAG, "Unable to load grammars", it) }
                    .isSuccess
                ready.complete(ok)
            }
        }
    }

    suspend fun awaitReady(): Boolean = ready.await()

    /** Makes [theme] the active TextMate theme, loading it on first use. */
    suspend fun applyTheme(theme: EditorTheme) = withContext(Dispatchers.Default) {
        val registry = ThemeRegistry.getInstance()
        synchronized(loadedThemes) {
            if (theme.id !in loadedThemes) {
                val stream = FileProviderRegistry.getInstance().tryGetInputStream(theme.assetPath)
                    ?: error("Theme asset not found: ${theme.assetPath}")
                val model = ThemeModel(IThemeSource.fromInputStream(stream, theme.assetPath, null), theme.id)
                model.isDark = theme.dark
                registry.loadTheme(model)
                loadedThemes += theme.id
            }
        }
        withContext(Dispatchers.Main) { registry.setTheme(theme.id) }
    }

    /** Creates a fresh editor language for [language] (each editor/tab switch gets its own instance). */
    fun createLanguage(language: Language, tabSize: Int, useSpaces: Boolean, autoComplete: Boolean): SoraLanguage {
        val scopeName = language.scopeName ?: return EmptyLanguage()
        if (!ready.isCompleted) return EmptyLanguage()
        return runCatching {
            TextMateLanguage.create(scopeName, autoComplete).apply {
                this.tabSize = tabSize
                useTab(!useSpaces)
            }
        }.getOrElse {
            Log.w(TAG, "No grammar for $scopeName", it)
            EmptyLanguage()
        }
    }
}
