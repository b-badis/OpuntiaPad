package io.github.bbadis.opuntiapad.core.text

/**
 * Line-based edits (comment toggling, duplicating, moving), computed on plain strings so they
 * can be unit-tested and applied to the editor as one undoable replacement.
 */
object LineEdits {

    /**
     * Comments or uncomments [lines]. With a line comment ("//", "#", "--"…), every non-blank line
     * gets the marker at the common indentation; if all are already commented, markers are removed.
     * Languages with only block comments (CSS, HTML, XML…) wrap the lines in [blockComment].
     */
    fun toggleComment(lines: List<String>, lineComment: String?, blockComment: Pair<String, String>?): List<String> {
        if (lineComment != null) return toggleLineComment(lines, lineComment)
        if (blockComment != null) return toggleBlockComment(lines, blockComment)
        return lines
    }

    private fun toggleLineComment(lines: List<String>, marker: String): List<String> {
        val content = lines.filter { it.isNotBlank() }
        if (content.isEmpty()) return lines
        val allCommented = content.all { it.trimStart().startsWith(marker) }
        if (allCommented) {
            return lines.map { line ->
                val i = line.indexOf(marker)
                if (line.isBlank() || i < 0) line else {
                    val after = i + marker.length
                    line.substring(0, i) + line.substring(if (line.getOrNull(after) == ' ') after + 1 else after)
                }
            }
        }
        val indent = content.minOf { line -> line.takeWhile { it == ' ' || it == '\t' }.length }
        return lines.map { line -> if (line.isBlank()) line else line.substring(0, indent) + "$marker " + line.substring(indent) }
    }

    private fun toggleBlockComment(lines: List<String>, block: Pair<String, String>): List<String> {
        val (open, close) = block
        val first = lines.indexOfFirst { it.isNotBlank() }
        val last = lines.indexOfLast { it.isNotBlank() }
        if (first < 0) return lines
        val result = lines.toMutableList()
        val head = result[first].trimStart()
        val tail = result[last].trimEnd()
        if (head.startsWith(open) && tail.endsWith(close)) {
            val headIndent = result[first].length - head.length
            result[first] = result[first].substring(0, headIndent) + head.removePrefix(open).removePrefix(" ")
            val t = result[last].trimEnd()
            result[last] = t.removeSuffix(close).removeSuffix(" ")
            return result
        }
        val indent = result[first].takeWhile { it == ' ' || it == '\t' }
        result[first] = indent + "$open " + result[first].substring(indent.length)
        result[last] = result[last] + " $close"
        return result
    }

    /** Returns the lines with [range] duplicated just below it. */
    fun duplicate(lines: List<String>, range: IntRange): List<String> =
        lines.subList(0, range.last + 1) + lines.subList(range.first, range.last + 1) + lines.subList(range.last + 1, lines.size)

    /** Moves the lines in [range] one line up (delta = -1) or down (delta = +1); null if impossible. */
    fun move(lines: List<String>, range: IntRange, delta: Int): List<String>? {
        require(delta == 1 || delta == -1)
        if (delta < 0 && range.first == 0 || delta > 0 && range.last >= lines.lastIndex) return null
        val block = lines.subList(range.first, range.last + 1)
        val rest = lines.toMutableList().apply { subList(range.first, range.last + 1).clear() }
        rest.addAll(range.first + delta, block)
        return rest
    }
}
