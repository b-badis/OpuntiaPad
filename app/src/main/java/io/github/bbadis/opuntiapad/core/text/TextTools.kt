package io.github.bbadis.opuntiapad.core.text

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Collator
import java.util.Base64
import java.util.Locale

/** Text transformations offered in the Tools sheet. All are pure functions of their input. */
object TextTools {

    enum class Case { UPPER, LOWER, TITLE }

    private val LINE_BREAK = Regex("\r\n|\r|\n")

    /** Sorts lines with the user's locale rules ("é" next to "e"), keeping the original line separator. */
    fun sortLines(text: String, descending: Boolean = false, locale: Locale = Locale.getDefault()): String =
        mapLines(text) { lines ->
            val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
            lines.sortedWith { a, b -> collator.compare(a, b) }.let { if (descending) it.reversed() else it }
        }

    fun removeDuplicateLines(text: String): String = mapLines(text) { it.distinct() }

    fun trimTrailingWhitespace(text: String): String = mapLines(text) { lines -> lines.map { it.trimEnd(' ', '\t') } }

    fun changeCase(text: String, case: Case, locale: Locale = Locale.getDefault()): String = when (case) {
        Case.UPPER -> text.uppercase(locale)
        Case.LOWER -> text.lowercase(locale)
        Case.TITLE -> Regex("\\p{L}[\\p{L}\\p{N}'’]*").replace(text.lowercase(locale)) { m ->
            m.value.replaceFirstChar { it.titlecase(locale) }
        }
    }

    /** Replaces leading tabs by [tabSize] spaces (indentation only, never inside the line). */
    fun tabsToSpaces(text: String, tabSize: Int): String = mapLines(text) { lines ->
        lines.map { line ->
            val indent = line.takeWhile { it == ' ' || it == '\t' }
            buildString {
                var col = 0
                for (c in indent) {
                    if (c == '\t') { val n = tabSize - col % tabSize; repeat(n) { append(' ') }; col += n } else { append(c); col++ }
                }
                append(line.substring(indent.length))
            }
        }
    }

    /** Replaces leading groups of [tabSize] spaces by tabs (indentation only). */
    fun spacesToTabs(text: String, tabSize: Int): String = mapLines(text) { lines ->
        lines.map { line ->
            val indent = line.takeWhile { it == ' ' || it == '\t' }
            val width = indent.fold(0) { col, c -> if (c == '\t') col + tabSize - col % tabSize else col + 1 }
            "\t".repeat(width / tabSize) + " ".repeat(width % tabSize) + line.substring(indent.length)
        }
    }

    fun base64Encode(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

    /** Accepts standard and URL-safe Base64, with or without padding and line breaks. */
    fun base64Decode(text: String): String {
        val compact = text.filterNot { it.isWhitespace() }
        val bytes = try {
            if (compact.contains('-') || compact.contains('_')) Base64.getUrlDecoder().decode(compact) else Base64.getDecoder().decode(compact)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Not valid Base64", e)
        }
        return String(bytes, Charsets.UTF_8)
    }

    fun urlEncode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")

    fun urlDecode(text: String): String = URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")

    /** Hex digest of the UTF-8 text, e.g. algorithm "SHA-256", "SHA-1" or "MD5". */
    fun hash(text: String, algorithm: String): String =
        MessageDigest.getInstance(algorithm).digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    /** Applies [transform] to the lines of [text], preserving its line separator and final newline. */
    private inline fun mapLines(text: String, transform: (List<String>) -> List<String>): String {
        if (text.isEmpty()) return text
        val separator = LineEnding.detect(text).separator
        val trailing = text.endsWith("\n") || text.endsWith("\r")
        val lines = text.split(LINE_BREAK).let { if (trailing) it.dropLast(1) else it }
        return transform(lines).joinToString(separator) + if (trailing) separator else ""
    }
}
