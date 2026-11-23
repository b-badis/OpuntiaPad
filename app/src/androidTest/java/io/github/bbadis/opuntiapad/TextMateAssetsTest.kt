package io.github.bbadis.opuntiapad

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bbadis.opuntiapad.core.language.LanguageCatalog
import io.github.bbadis.opuntiapad.editor.TextMateSupport
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import kotlinx.coroutines.runBlocking
import org.eclipse.tm4e.core.grammar.IStateStack
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies, on a real Android runtime (Joni regex engine, R8-processed classes in CI), that
 * every bundled grammar loads and tokenizes, and that every theme can be applied.
 */
@RunWith(AndroidJUnit4::class)
class TextMateAssetsTest {

    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OpuntiaPadApp
    private val catalog: LanguageCatalog = app.catalog

    @Test
    fun everyGrammarLoadsAndTokenizes() = runBlocking {
        assertTrue("grammars failed to load", TextMateSupport.awaitReady())
        val samples = listOf("key: \"value\" # comment", "{ \"a\": [1, 2.5, true, null] }", "SELECT * FROM t WHERE id = 1; -- x", "")
        for (language in catalog.languages) {
            val scopeName = requireNotNull(language.scopeName)
            assertTrue("cannot load ${language.id}", TextMateSupport.ensureGrammarLoaded(scopeName))
            val grammar = GrammarRegistry.getInstance().findGrammar(scopeName)
            assertNotNull("missing grammar for ${language.id}", grammar)
            var state: IStateStack? = null
            for (line in samples) {
                state = grammar!!.tokenizeLine(line, state, null).ruleStack
            }
        }
    }

    @Test
    fun everyThemeApplies() = runBlocking {
        TextMateSupport.awaitReady()
        for (theme in catalog.themes) TextMateSupport.applyTheme(theme)
    }

    @Test
    fun catalogIsConsistent() {
        assertTrue(catalog.languages.size > 50)
        assertTrue(catalog.themes.any { it.dark } && catalog.themes.any { !it.dark })
        val ids = catalog.languages.map { it.id }
        assertTrue("duplicate ids", ids.size == ids.toSet().size)
    }
}
