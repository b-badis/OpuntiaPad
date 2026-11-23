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
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.langs.textmate.registry.reader.LanguageDefinitionReader
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
 *
 * Only the small list of grammar definitions is read at start-up; each grammar (and the grammars it
 * embeds, e.g. CSS and JavaScript for HTML) is parsed the first time a document needs it. Loading all
 * 88 grammars eagerly cost ~0.5 s of CPU on a desktop (~2 s on a phone) at every launch; loading one
 * takes a few milliseconds.
 */
object TextMateSupport {
    private const val TAG = "TextMateSupport"
    private const val LANGUAGES_PATH = "textmate/languages.json"

    private val initialized = AtomicBoolean(false)
    private val definitions = CompletableDeferred<Map<String, GrammarDefinition>>()
    private val loadedThemes = mutableSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun initialize(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.applicationContext.assets))
            scope.launch {
                val defs = runCatching { LanguageDefinitionReader.read(LANGUAGES_PATH) }
                    .onFailure { Log.e(TAG, "Unable to read grammar definitions", it) }
                    .getOrDefault(emptyList())
                definitions.complete(defs.filter { it.scopeName != null }.associateBy { it.scopeName!! })
            }
        }
    }

    /** True once the grammar definitions are known (they are tiny: this happens right after start-up). */
    suspend fun awaitReady(): Boolean = definitions.await().isNotEmpty()

    /** Parses the grammar for [scopeName] and everything it embeds, if not done yet. Thread-safe. */
    suspend fun ensureGrammarLoaded(scopeName: String): Boolean = withContext(Dispatchers.Default) {
        val defs = definitions.await()
        val byName = defs.values.associateBy { it.name }
        synchronized(this@TextMateSupport) { load(scopeName, defs, byName, mutableSetOf()) }
    }

    private fun load(
        scopeName: String,
        defs: Map<String, GrammarDefinition>,
        byName: Map<String, GrammarDefinition>,
        visiting: MutableSet<String>,
    ): Boolean {
        val registry = GrammarRegistry.getInstance()
        if (registry.findGrammar(scopeName) != null) return true
        val definition = defs[scopeName] ?: return false
        if (!visiting.add(scopeName)) return true // dependency cycle: already being loaded
        // Embedded grammars must exist before the grammar that references them.
        for (name in definition.embeddedLanguages.values) {
            val embeddedScope: String = byName[name]?.scopeName ?: continue
            load(embeddedScope, defs, byName, visiting)
        }
        return runCatching { registry.loadGrammar(definition) }
            .onFailure { Log.w(TAG, "Cannot load grammar $scopeName", it) }
            .isSuccess
    }

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

    /**
     * Creates a fresh editor language for [language] (each tab switch gets its own instance), loading
     * its grammar first. Plain text, or a grammar that fails to load, gives a language without colours.
     */
    suspend fun createLanguage(language: Language, tabSize: Int, useSpaces: Boolean, autoComplete: Boolean): SoraLanguage {
        val scopeName = language.scopeName ?: return EmptyLanguage()
        if (!ensureGrammarLoaded(scopeName)) return EmptyLanguage()
        // The grammar is parsed in the background above; creating the language itself is cheap.
        return withContext(Dispatchers.Main) {
            runCatching {
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
}
