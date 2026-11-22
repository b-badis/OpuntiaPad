package io.github.bbadis.opuntiapad.core.text

/**
 * Small, dependency-free JSON pretty-printer / minifier.
 *
 * It works on tokens rather than a parsed tree, so it preserves number literals and string
 * escapes exactly, and it validates structure (balanced brackets, terminated strings).
 */
object JsonFormatter {

    class JsonSyntaxException(message: String, val offset: Int) : IllegalArgumentException(message)

    fun format(input: CharSequence, indent: String = "  "): String = rewrite(input, indent)

    fun minify(input: CharSequence): String = rewrite(input, null)

    private fun rewrite(input: CharSequence, indent: String?): String {
        val out = StringBuilder(input.length + input.length / 4)
        val stack = ArrayDeque<Char>()
        var depth = 0
        var i = 0
        val n = input.length
        var emptyContainer = false

        fun newline() {
            if (indent == null) return
            out.append('\n')
            repeat(depth) { out.append(indent) }
        }

        while (i < n) {
            val c = input[i]
            when {
                c == '"' -> {
                    if (emptyContainer) { newline(); emptyContainer = false }
                    val start = i
                    i++
                    while (i < n && input[i] != '"') {
                        if (input[i] == '\\') i++
                        if (i < n && (input[i] == '\n')) throw JsonSyntaxException("Unterminated string", start)
                        i++
                    }
                    if (i >= n) throw JsonSyntaxException("Unterminated string", start)
                    out.append(input, start, i + 1)
                }
                c == '{' || c == '[' -> {
                    if (emptyContainer) newline()
                    out.append(c)
                    stack.addLast(c)
                    depth++
                    emptyContainer = true
                }
                c == '}' || c == ']' -> {
                    val open = stack.removeLastOrNull() ?: throw JsonSyntaxException("Unexpected '$c'", i)
                    if ((open == '{') != (c == '}')) throw JsonSyntaxException("Mismatched '$c'", i)
                    depth--
                    if (!emptyContainer) newline()
                    emptyContainer = false
                    out.append(c)
                }
                c == ',' -> {
                    out.append(',')
                    newline()
                }
                c == ':' -> out.append(if (indent == null) ":" else ": ")
                c.isWhitespace() -> Unit
                else -> {
                    if (emptyContainer) { newline(); emptyContainer = false }
                    val start = i
                    while (i + 1 < n && !input[i + 1].isWhitespace() && input[i + 1] !in ",:]}[{\"") i++
                    val literal = input.subSequence(start, i + 1).toString()
                    if (!LITERAL.matches(literal)) throw JsonSyntaxException("Unexpected token '$literal'", start)
                    out.append(literal)
                }
            }
            if (c == '"' || c.isLetterOrDigit() || c == '-') emptyContainer = false
            i++
        }
        if (stack.isNotEmpty()) throw JsonSyntaxException("Unclosed '${stack.last()}'", n)
        if (indent != null && input.endsWith("\n")) out.append('\n')
        return out.toString()
    }

    private val LITERAL = Regex("true|false|null|-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?")
}
