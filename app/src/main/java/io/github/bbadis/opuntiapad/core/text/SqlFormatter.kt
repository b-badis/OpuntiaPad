package io.github.bbadis.opuntiapad.core.text

import java.util.Locale

/**
 * Readable layout for SQL: one major clause per line, select lists and conditions indented,
 * sub-queries indented, keywords in upper case.
 *
 * Only whitespace and keyword case are changed: strings, quoted identifiers and comments are
 * copied untouched, so the statement means exactly the same thing.
 */
object SqlFormatter {

    private enum class Kind { WORD, STRING, COMMENT, SYMBOL }

    private data class Token(val kind: Kind, val text: String)

    /** Clauses that start a new line at the current depth. */
    private val CLAUSES = listOf(
        "SELECT DISTINCT", "SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET",
        "UNION ALL", "UNION", "INTERSECT", "EXCEPT", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM",
        "RETURNING", "WITH", "CREATE TABLE", "CREATE VIEW", "CREATE INDEX", "ALTER TABLE", "DROP TABLE",
        "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "FULL OUTER JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN",
        "FULL JOIN", "CROSS JOIN", "JOIN", "ON CONFLICT",
    ).map { it.split(' ') }

    /** Conditions that start an indented continuation line. */
    private val CONDITIONS = setOf("AND", "OR", "ON")

    private val SPACED_BEFORE_PAREN = setOf(
        "IN", "VALUES", "AS", "EXISTS", "ON", "AND", "OR", "FROM", "JOIN", "WHERE", "SELECT", "NOT", "ALL", "ANY", "USING",
    )

    private val KEYWORDS = setOf(
        "SELECT", "DISTINCT", "FROM", "WHERE", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL",
        "INTERSECT", "EXCEPT", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "RETURNING", "WITH", "AS",
        "CREATE", "TABLE", "VIEW", "INDEX", "ALTER", "DROP", "ADD", "COLUMN", "PRIMARY", "KEY", "FOREIGN",
        "REFERENCES", "NOT", "NULL", "DEFAULT", "UNIQUE", "CHECK", "CONSTRAINT", "LEFT", "RIGHT", "INNER", "OUTER",
        "FULL", "CROSS", "JOIN", "ON", "AND", "OR", "IN", "IS", "LIKE", "ILIKE", "BETWEEN", "EXISTS", "CASE", "WHEN",
        "THEN", "ELSE", "END", "ASC", "DESC", "COUNT", "SUM", "AVG", "MIN", "MAX", "IF", "CONFLICT", "DO", "NOTHING",
        "TRUE", "FALSE", "CAST", "OVER", "PARTITION", "INTEGER", "INT", "BIGINT", "TEXT", "VARCHAR", "BOOLEAN",
        "DATE", "TIMESTAMP", "SERIAL", "NUMERIC", "DECIMAL", "REAL", "FLOAT",
    )

    fun format(input: CharSequence, indent: String = "  "): String {
        val tokens = tokenize(input.toString())
        val out = StringBuilder(input.length + input.length / 4)
        var depth = 0
        val clauseDepth = ArrayDeque<Int>() // depth at which each open parenthesis started
        var lineStart = true
        var i = 0

        fun newLine(extra: Int = 0) {
            while (out.isNotEmpty() && out.last() == ' ') out.setLength(out.length - 1)
            if (out.isNotEmpty()) out.append('\n')
            repeat(depth + extra) { out.append(indent) }
            lineStart = true
        }

        fun emit(text: String) {
            if (!lineStart && out.isNotEmpty() && out.last() != ' ' && out.last() != '(' && text != ")" && text != "," &&
                text != ";" && text != "." && out.last() != '.'
            ) {
                out.append(' ')
            }
            out.append(text)
            lineStart = false
        }

        while (i < tokens.size) {
            val t = tokens[i]
            val clause = if (t.kind == Kind.WORD) matchClause(tokens, i) else null
            when {
                clause != null -> {
                    newLine()
                    emit(clause.joinToString(" "))
                    i += clause.size
                    // Put the first item of a list clause on the same line; following items get their own line.
                    continue
                }
                t.kind == Kind.WORD && t.text.uppercase(Locale.ROOT) in CONDITIONS -> {
                    newLine(1)
                    emit(t.text.uppercase(Locale.ROOT))
                }
                t.kind == Kind.SYMBOL && t.text == "(" -> {
                    // Function calls stay attached: COUNT(*), VARCHAR(100); keywords keep a space: IN (…)
                    val prev = tokens.getOrNull(i - 1)
                    val call = prev != null && prev.kind == Kind.WORD && prev.text.uppercase(Locale.ROOT) !in SPACED_BEFORE_PAREN
                    if (call && !lineStart) out.append('(') else emit("(")
                    lineStart = false
                    clauseDepth.addLast(depth)
                    if (startsSubquery(tokens, i + 1)) depth++
                }
                t.kind == Kind.SYMBOL && t.text == ")" -> {
                    val wasSubquery = clauseDepth.lastOrNull()?.let { it != depth } == true
                    depth = clauseDepth.removeLastOrNull() ?: depth
                    if (wasSubquery) newLine()
                    emit(")")
                }
                t.kind == Kind.SYMBOL && t.text == "," -> {
                    emit(",")
                    if (clauseDepth.isEmpty() || clauseDepth.last() != depth) newLine(1)
                }
                t.kind == Kind.SYMBOL && t.text == ";" -> {
                    emit(";")
                    depth = 0
                    clauseDepth.clear()
                    if (i + 1 < tokens.size) out.append('\n')
                }
                t.kind == Kind.COMMENT -> {
                    if (t.text.startsWith("--")) {
                        emit(t.text)
                        newLine()
                    } else {
                        emit(t.text)
                    }
                }
                t.kind == Kind.WORD -> emit(if (t.text.uppercase(Locale.ROOT) in KEYWORDS) t.text.uppercase(Locale.ROOT) else t.text)
                else -> emit(t.text)
            }
            i++
        }
        while (out.isNotEmpty() && (out.last() == ' ' || out.last() == '\n')) out.setLength(out.length - 1)
        if (input.endsWith("\n")) out.append('\n')
        return out.toString()
    }

    private fun matchClause(tokens: List<Token>, at: Int): List<String>? {
        candidates@ for (words in CLAUSES) {
            var j = at
            for (w in words) {
                val t = tokens.getOrNull(j) ?: continue@candidates
                if (t.kind != Kind.WORD || !t.text.equals(w, ignoreCase = true)) continue@candidates
                j++
            }
            return words
        }
        return null
    }

    private fun startsSubquery(tokens: List<Token>, at: Int): Boolean =
        tokens.getOrNull(at)?.let { it.kind == Kind.WORD && it.text.uppercase(Locale.ROOT) in setOf("SELECT", "WITH") } == true

    private fun tokenize(s: String): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                s.startsWith("--", i) -> {
                    val end = s.indexOf('\n', i).let { if (it < 0) s.length else it }
                    tokens += Token(Kind.COMMENT, s.substring(i, end).trimEnd())
                    i = end
                }
                s.startsWith("/*", i) -> {
                    val end = s.indexOf("*/", i + 2).let { if (it < 0) s.length else it + 2 }
                    tokens += Token(Kind.COMMENT, s.substring(i, end))
                    i = end
                }
                c == '\'' || c == '"' || c == '`' || c == '[' -> {
                    val close = if (c == '[') ']' else c
                    var j = i + 1
                    while (j < s.length) {
                        if (s[j] == close) {
                            // SQL escapes a quote by doubling it: 'it''s'
                            if (close != ']' && j + 1 < s.length && s[j + 1] == close) {
                                j += 2
                                continue
                            }
                            break
                        }
                        j++
                    }
                    val end = minOf(j + 1, s.length)
                    tokens += Token(Kind.STRING, s.substring(i, end))
                    i = end
                }
                c.isLetterOrDigit() || c == '_' || c == '@' || c == '$' || c == '#' || c == ':' && s.getOrNull(i + 1)?.isLetter() == true -> {
                    var j = i + 1
                    while (j < s.length && (s[j].isLetterOrDigit() || s[j] == '_' || s[j] == '$' || s[j] == '#')) j++
                    // Keep decimal numbers together: 1.5
                    if (c.isDigit() && j < s.length && s[j] == '.' && s.getOrNull(j + 1)?.isDigit() == true) {
                        j++
                        while (j < s.length && s[j].isDigit()) j++
                    }
                    tokens += Token(Kind.WORD, s.substring(i, j))
                    i = j
                }
                else -> {
                    // Multi-character operators.
                    val op = listOf("<>", "<=", ">=", "!=", "||", "::", "=>").firstOrNull { s.startsWith(it, i) } ?: c.toString()
                    tokens += Token(Kind.SYMBOL, op)
                    i += op.length
                }
            }
        }
        return tokens
    }
}
