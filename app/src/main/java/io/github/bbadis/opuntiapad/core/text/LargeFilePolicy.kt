package io.github.bbadis.opuntiapad.core.text

/**
 * Decides when syntax colouring is turned off by default for big documents.
 *
 * Measured (JVM desktop, a phone is ~3-5× slower): highlighting 1 MB costs ~0.35 s for JSON but
 * ~6 s for YAML, whose grammar is much heavier. Colouring runs in the background, but on big files
 * it keeps a core busy for a long time, so it becomes opt-in above these sizes.
 */
object LargeFilePolicy {

    /** Languages whose grammars are expensive per line. */
    private val HEAVY = setOf("yaml", "markdown", "latex", "php", "html", "vue", "svelte", "perl", "ruby")

    const val HEAVY_LIMIT_CHARS = 256 * 1024
    const val DEFAULT_LIMIT_CHARS = 2 * 1024 * 1024

    fun highlightByDefault(languageId: String, length: Int): Boolean =
        length <= if (languageId in HEAVY) HEAVY_LIMIT_CHARS else DEFAULT_LIMIT_CHARS
}
