package io.github.bbadis.opuntiapad.core.text

/**
 * Pretty-printer / minifier for XML (also SVG, plist, Android resources, XHTML…).
 *
 * Works on tokens (tags, text, comments, CDATA, processing instructions, doctype), never builds a
 * tree, and checks that tags are balanced. Text content is trimmed; whitespace inside CDATA,
 * comments and attribute values is preserved exactly.
 */
object XmlFormatter {

    class XmlSyntaxException(message: String, val offset: Int) : IllegalArgumentException(message)

    fun format(input: CharSequence, indent: String = "  "): String = rewrite(input.toString(), indent)

    fun minify(input: CharSequence): String = rewrite(input.toString(), null)

    private sealed interface Token {
        val text: String
        data class Open(override val text: String, val name: String, val selfClosing: Boolean) : Token
        data class Close(override val text: String, val name: String, val offset: Int) : Token
        data class Text(override val text: String) : Token
        data class Other(override val text: String) : Token // comment, CDATA, <?pi?>, <!DOCTYPE>
    }

    private fun tokenize(s: String): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < s.length) {
            if (s[i] != '<') {
                val end = s.indexOf('<', i).let { if (it < 0) s.length else it }
                tokens += Token.Text(s.substring(i, end))
                i = end
                continue
            }
            val (end, kind) = when {
                s.startsWith("<!--", i) -> s.indexOf("-->", i + 4).let { if (it < 0) -1 else it + 3 } to 'c'
                s.startsWith("<![CDATA[", i) -> s.indexOf("]]>", i + 9).let { if (it < 0) -1 else it + 3 } to 'c'
                s.startsWith("<?", i) -> s.indexOf("?>", i + 2).let { if (it < 0) -1 else it + 2 } to 'c'
                s.startsWith("<!", i) -> endOfTag(s, i) to 'c'
                else -> endOfTag(s, i) to 't'
            }
            if (end < 0) throw XmlSyntaxException("Unterminated markup", i)
            val markup = s.substring(i, end)
            tokens += when {
                kind == 'c' -> Token.Other(markup)
                markup.startsWith("</") -> Token.Close(markup, tagName(markup, 2), i)
                else -> Token.Open(markup, tagName(markup, 1), markup.endsWith("/>"))
            }
            i = end
        }
        return tokens
    }

    /** End (exclusive) of a tag starting at [start], honouring quoted attribute values. */
    private fun endOfTag(s: String, start: Int): Int {
        var quote: Char? = null
        for (j in start + 1 until s.length) {
            val c = s[j]
            when {
                quote != null -> if (c == quote) quote = null
                c == '"' || c == '\'' -> quote = c
                c == '>' -> return j + 1
            }
        }
        return -1
    }

    private fun tagName(markup: String, from: Int): String =
        markup.substring(from).takeWhile { !it.isWhitespace() && it != '>' && it != '/' }

    private fun rewrite(s: String, indent: String?): String {
        val tokens = tokenize(s)
        val out = StringBuilder(s.length)
        val stack = ArrayDeque<String>()
        var i = 0
        fun line(depth: Int, text: String) {
            if (indent == null) {
                out.append(text)
            } else {
                if (out.isNotEmpty()) out.append('\n')
                repeat(depth) { out.append(indent) }
                out.append(text)
            }
        }
        while (i < tokens.size) {
            when (val t = tokens[i]) {
                is Token.Text -> {
                    val text = t.text.trim()
                    if (text.isNotEmpty()) line(stack.size, text)
                }
                is Token.Other -> line(stack.size, t.text)
                is Token.Open -> {
                    if (t.selfClosing) {
                        line(stack.size, t.text)
                    } else {
                        // Keep <a>short text</a> on one line.
                        val next = tokens.getOrNull(i + 1)
                        val after = tokens.getOrNull(i + 2)
                        if (next is Token.Text && after is Token.Close && after.name == t.name && !next.text.trim().contains('\n')) {
                            line(stack.size, t.text + next.text.trim() + after.text)
                            i += 3
                            continue
                        }
                        if (next is Token.Close && next.name == t.name) {
                            line(stack.size, t.text + next.text)
                            i += 2
                            continue
                        }
                        line(stack.size, t.text)
                        stack.addLast(t.name)
                    }
                }
                is Token.Close -> {
                    val open = stack.removeLastOrNull() ?: throw XmlSyntaxException("Unexpected </${t.name}>", t.offset)
                    if (open != t.name) throw XmlSyntaxException("Expected </$open> but found </${t.name}>", t.offset)
                    line(stack.size, t.text)
                }
            }
            i++
        }
        if (stack.isNotEmpty()) throw XmlSyntaxException("Unclosed <${stack.last()}>", s.length)
        if (indent != null && s.endsWith("\n")) out.append('\n')
        return out.toString()
    }
}
