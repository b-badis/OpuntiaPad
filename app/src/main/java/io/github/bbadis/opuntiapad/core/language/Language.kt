package io.github.bbadis.opuntiapad.core.language

/** A language OpuntiaPad can highlight. [scopeName] is null for plain text. */
data class Language(
    val id: String,
    val name: String,
    val scopeName: String?,
    val extensions: List<String> = emptyList(),
    val fileNames: List<String> = emptyList(),
    val lineComment: String? = null,
    val blockComment: Pair<String, String>? = null,
) {
    val isPlainText: Boolean get() = scopeName == null

    companion object {
        const val PLAIN_TEXT_ID = "text"
        val PLAIN_TEXT = Language(PLAIN_TEXT_ID, "Plain text", null, listOf("txt", "text", "nfo", "readme"))
    }
}

/** An editor colour theme bundled in the assets. */
data class EditorTheme(val id: String, val name: String, val dark: Boolean, val assetPath: String)
