package io.github.bbadis.opuntiapad.core.text

/** Chooses the formatter for a language id (from the language catalog). */
object DocumentFormatter {

    // JSONC/JSON5 allow comments, which the strict JSON formatter would reject.
    private val JSON = setOf("json")
    private val XML = setOf("xml")
    private val SQL = setOf("sql", "plsql")

    fun canFormat(languageId: String): Boolean = languageId in JSON || languageId in XML || languageId in SQL

    fun canMinify(languageId: String): Boolean = languageId in JSON || languageId in XML

    fun canConvertToYaml(languageId: String): Boolean = languageId == "json"

    fun canConvertToJson(languageId: String): Boolean = languageId == "yaml"

    /** @throws IllegalArgumentException with a user-readable message if the document is invalid. */
    fun format(languageId: String, text: CharSequence, indent: String): String = when (languageId) {
        in JSON -> JsonFormatter.format(text, indent)
        in XML -> XmlFormatter.format(text, indent)
        in SQL -> SqlFormatter.format(text, indent)
        else -> throw UnsupportedOperationException("No formatter for $languageId")
    }

    fun minify(languageId: String, text: CharSequence): String = when (languageId) {
        in JSON -> JsonFormatter.minify(text)
        in XML -> XmlFormatter.minify(text)
        else -> throw UnsupportedOperationException("No minifier for $languageId")
    }
}
