package io.github.bbadis.opuntiapad.assets

import org.eclipse.tm4e.core.grammar.IStateStack
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IRegistryOptions
import org.eclipse.tm4e.core.registry.IThemeSource
import org.eclipse.tm4e.core.registry.Registry
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Duration

/**
 * Validates the generated TextMate assets on the JVM (fast, no device needed):
 * theme colours use the only formats Sora Editor accepts, themes / language configurations
 * parse, and representative files tokenize with the bundled grammars.
 */
class TextMateAssetsJvmTest {

    private val assets = File("src/main/assets/textmate")
    private val grammarsByScope: Map<String, String> = Regex("\"scopeName\":\\s*\"([^\"]+)\",\\s*\"grammar\":\\s*\"([^\"]+)\"")
        .findAll(File(assets, "languages.json").readText())
        .associate { it.groupValues[1] to it.groupValues[2] }

    private fun registry() = Registry(object : IRegistryOptions {
        override fun getGrammarSource(scopeName: String): IGrammarSource? =
            grammarsByScope[scopeName]?.let { IGrammarSource.fromFile(File(assets.parentFile, it)) }
    })

    /** Regression test: "#fff"-style colours crashed the editor ("Unknown color"). */
    @Test
    fun themeColorsUseSupportedFormat() {
        val valid = Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")
        val colorValue = Regex("\"(?:[\\w.]+|foreground|background)\":\\s*\"(#[^\"]*|inherit|[a-z]+)\"")
        val themes = File(assets, "themes").listFiles()!!.filter { it.extension == "json" }
        assertTrue(themes.isNotEmpty())
        for (theme in themes) {
            val text = theme.readText()
            val colorsBlock = text.substringAfter("\"colors\":").substringBefore("\"tokenColors\":")
            val bad = colorValue.findAll(colorsBlock).map { it.groupValues[1] }.filterNot { valid.matches(it) }.toList() +
                Regex("\"(?:foreground|background)\":\\s*\"([^\"]*)\"").findAll(text.substringAfter("\"tokenColors\":"))
                    .map { it.groupValues[1] }.filterNot { valid.matches(it) }.toList()
            assertTrue("${theme.name} has unsupported colours: $bad", bad.isEmpty())
        }
    }

    @Test
    fun themesParse() {
        val registry = registry()
        File(assets, "themes").listFiles()!!.forEach { registry.setTheme(IThemeSource.fromFile(it)) }
    }

    @Test
    fun languageConfigurationsParse() {
        File(assets, "config").listFiles()!!.forEach { f ->
            assertNotNull(f.name, f.reader().use { LanguageConfiguration.load(it) })
        }
    }

    /** Regression test: malformed captures in xml.json broke 11 grammars (Markdown, PHP, Ruby…). */
    @Test
    fun everyCatalogLanguageLoadsAndTokenizes() {
        val registry = registry()
        val scopes = Regex("\"scopeName\":\\s*\"([^\"]+)\"").findAll(File(assets, "catalog.json").readText()).map { it.groupValues[1] }.toList()
        assertTrue(scopes.size > 50)
        val lines = listOf("key: \"value\" # comment", "{ \"a\": [1, 2.5, true, null] }", "SELECT * FROM t WHERE id = 1; -- x", "<a href=\"x\">y</a>", "")
        val failures = scopes.mapNotNull { scope ->
            runCatching {
                val grammar = requireNotNull(registry.loadGrammar(scope)) { "grammar not found" }
                var state: IStateStack? = null
                lines.forEach { state = grammar.tokenizeLine(it, state, Duration.ofSeconds(5)).ruleStack }
            }.exceptionOrNull()?.let { "$scope: $it" }
        }
        assertTrue("grammars failing:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun samplesTokenize() {
        val registry = registry()
        val samples = mapOf("sample.yaml" to "source.yaml", "sample.json" to "source.json", "sample.sql" to "source.sql")
        for ((file, scope) in samples) {
            val grammar = requireNotNull(registry.loadGrammar(scope)) { "no grammar $scope" }
            var state: IStateStack? = null
            javaClass.getResource("/samples/$file")!!.readText().lines().forEachIndexed { i, line ->
                try {
                    state = grammar.tokenizeLine(line, state, Duration.ofSeconds(5)).ruleStack
                } catch (t: Throwable) {
                    throw AssertionError("$file:${i + 1}", t)
                }
            }
        }
    }
}
