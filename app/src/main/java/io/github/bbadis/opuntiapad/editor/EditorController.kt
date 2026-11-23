package io.github.bbadis.opuntiapad.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.bbadis.opuntiapad.core.text.LineEdits
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.SelectionMovement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class SearchQuery(
    val text: String = "",
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false,
)

/**
 * Bridges Compose UI and the (View-based) [CodeEditor]: exposes editor commands and
 * observable editor state (cursor, undo availability, search results, zoom).
 */
@Stable
class EditorController {
    var editor: CodeEditor? = null
        private set

    var line by mutableIntStateOf(1)
        private set
    var column by mutableIntStateOf(1)
        private set
    var selectionLength by mutableIntStateOf(0)
        private set
    var lineCount by mutableIntStateOf(1)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    var matchCount by mutableIntStateOf(0)
        private set
    var matchIndex by mutableIntStateOf(-1)
        private set
    var searchError by mutableStateOf(false)
        private set

    /** Current font size in sp, including pinch-zoom changes. */
    var fontSize by mutableFloatStateOf(14f)
        internal set

    internal fun attach(editor: CodeEditor) {
        this.editor = editor
        refresh()
    }

    internal fun detach(editor: CodeEditor) {
        if (this.editor === editor) this.editor = null
    }

    internal fun refresh() {
        val e = editor ?: return
        val cursor = e.cursor
        line = cursor.leftLine + 1
        column = cursor.leftColumn + 1
        selectionLength = if (cursor.isSelected) cursor.right - cursor.left else 0
        lineCount = e.lineCount
        canUndo = e.text.canUndo()
        canRedo = e.text.canRedo()
    }

    internal fun refreshSearch() {
        val searcher = editor?.searcher ?: return
        matchCount = if (searcher.hasQuery()) searcher.matchedPositionCount else 0
        matchIndex = if (searcher.hasQuery()) searcher.currentMatchedPositionIndex else -1
    }

    fun undo() = editor?.run { undo(); refresh() }
    fun redo() = editor?.run { redo(); refresh() }
    fun selectAll() = editor?.selectAll()
    fun copy() = editor?.copyText()
    fun cut() = editor?.cutText()
    fun paste() = editor?.pasteText()

    fun indent() = editor?.indentOrCommitTab()
    fun unindent() = editor?.unindentSelection()
    fun move(movement: SelectionMovement) = editor?.moveSelection(movement)

    fun insert(text: String, cursorOffset: Int = text.length) = whenEditable { e -> e.insertText(text, cursorOffset) }

    /**
     * Runs [action] once the editor accepts edits. Sora's `isEditable` is also false for a moment
     * while it lays out text in the background (e.g. right after opening a file): instead of
     * silently dropping the user's action, retry shortly. A read-only document never becomes
     * editable, so the action is dropped after [EDITABLE_TIMEOUT_MS].
     */
    private fun whenEditable(attempt: Int = 0, action: (CodeEditor) -> Unit) {
        val e = editor ?: return
        when {
            e.isEditable -> action(e)
            attempt * RETRY_DELAY_MS < EDITABLE_TIMEOUT_MS -> e.postDelayed({ whenEditable(attempt + 1, action) }, RETRY_DELAY_MS)
        }
    }

    private suspend fun awaitEditable(e: CodeEditor): Boolean {
        var waited = 0L
        while (!e.isEditable && waited < EDITABLE_TIMEOUT_MS) {
            delay(RETRY_DELAY_MS)
            waited += RETRY_DELAY_MS
        }
        return e.isEditable && editor === e
    }

    fun goToLine(lineNumber: Int) {
        val e = editor ?: return
        val target = (lineNumber - 1).coerceIn(0, e.lineCount - 1)
        e.setSelection(target, 0)
        e.ensureSelectionVisible()
        e.requestFocus()
    }

    fun search(query: SearchQuery) {
        val e = editor ?: return
        if (query.text.isEmpty()) {
            stopSearch()
            return
        }
        val type = when {
            query.regex -> EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION
            query.wholeWord -> EditorSearcher.SearchOptions.TYPE_WHOLE_WORD
            else -> EditorSearcher.SearchOptions.TYPE_NORMAL
        }
        searchError = try {
            if (query.regex) Regex(query.text)
            e.searcher.search(query.text, EditorSearcher.SearchOptions(type, !query.matchCase))
            false
        } catch (_: Exception) {
            e.searcher.stopSearch()
            true
        }
        refreshSearch()
    }

    fun findNext() = editor?.searcher?.run { if (hasQuery()) gotoNext(); refreshSearch() }
    fun findPrevious() = editor?.searcher?.run { if (hasQuery()) gotoPrevious(); refreshSearch() }

    fun replaceCurrent(replacement: String) = editor?.searcher?.run {
        if (hasQuery() && isMatchedPositionSelected) replaceCurrentMatch(replacement) else if (hasQuery()) gotoNext()
        refreshSearch()
    }

    fun replaceAll(replacement: String) = editor?.searcher?.run {
        if (hasQuery()) replaceAll(replacement) { refreshSearch() }
    }

    fun stopSearch() {
        editor?.searcher?.stopSearch()
        searchError = false
        matchCount = 0
        matchIndex = -1
    }

    fun zoomBy(deltaSp: Float) {
        fontSize = (fontSize + deltaSp).coerceIn(8f, 40f)
        editor?.setTextSize(fontSize)
    }

    fun setZoom(sizeSp: Float) {
        fontSize = sizeSp
        editor?.setTextSize(sizeSp)
    }

    // --- Text editing helpers used by the Tools sheet, the symbol bar and shortcuts ---------------

    /** True while a long transformation (formatting a big file…) runs in the background. */
    var busy by mutableStateOf(false)
        private set

    val hasSelection: Boolean get() = editor?.cursor?.isSelected == true

    fun selectedText(): String? {
        val e = editor ?: return null
        val c = e.cursor
        return if (c.isSelected) e.text.substring(c.left, c.right) else null
    }

    /**
     * Transforms the selection, or the whole document if nothing is selected, as one undoable edit.
     * The transformation runs off the main thread so big documents don't freeze the UI.
     * @return the failure, if [transform] threw (e.g. invalid JSON).
     */
    suspend fun transform(transform: (String) -> String): Throwable? {
        val e = editor ?: return null
        if (!awaitEditable(e)) return null
        val c = e.cursor
        val selection = c.isSelected
        val startLine = if (selection) c.leftLine else 0
        val startColumn = if (selection) c.leftColumn else 0
        val endLine = if (selection) c.rightLine else e.text.lineCount - 1
        val endColumn = if (selection) c.rightColumn else e.text.getColumnCount(endLine)
        val input = if (selection) e.text.substring(c.left, c.right) else e.text.toString()
        busy = true
        val result = try {
            withContext(Dispatchers.Default) { runCatching { transform(input) } }
        } finally {
            busy = false
        }
        val output = result.getOrElse { return it }
        if (output == input || !awaitEditable(e)) return null
        e.text.replace(startLine, startColumn, endLine, endColumn, output)
        if (selection) {
            val end = e.text.indexer.getCharPosition(e.text.getCharIndex(startLine, startColumn) + output.length)
            e.setSelectionRegion(startLine, startColumn, end.line, end.column)
        } else {
            e.setSelection(0, 0)
        }
        refresh()
        return null
    }

    /** Comments or uncomments the lines touched by the selection (or the caret line). */
    fun toggleComment(lineComment: String?, blockComment: Pair<String, String>?, separator: String): Boolean {
        if (lineComment == null && blockComment == null) return false
        editLines(separator) { lines, range ->
            val edited = LineEdits.toggleComment(lines.subList(range.first, range.last + 1), lineComment, blockComment)
            Edit(lines.subList(0, range.first) + edited + lines.subList(range.last + 1, lines.size), range)
        }
        return true
    }

    fun duplicateLines(separator: String) = editLines(separator) { lines, range ->
        val size = range.last - range.first + 1
        Edit(LineEdits.duplicate(lines, range), (range.first + size)..(range.last + size))
    }

    fun moveLines(delta: Int, separator: String) = editLines(separator) { lines, range ->
        LineEdits.move(lines, range, delta)?.let { Edit(it, (range.first + delta)..(range.last + delta)) }
    }

    private data class Edit(val lines: List<String>, val selectedLines: IntRange)

    /**
     * Applies a line-based edit to a small window around the selected lines (never the whole file),
     * as a single undoable change, then reselects the affected lines.
     */
    private fun editLines(separator: String, edit: (List<String>, IntRange) -> Edit?) = whenEditable { e ->
        val text = e.text
        val c = e.cursor
        val first = c.leftLine
        // A selection ending at column 0 doesn't include that last line.
        val last = if (c.isSelected && c.rightColumn == 0 && c.rightLine > c.leftLine) c.rightLine - 1 else c.rightLine
        val windowStart = (first - 1).coerceAtLeast(0)
        val windowEnd = (last + 1).coerceAtMost(text.lineCount - 1)
        val window = (windowStart..windowEnd).map { text.getLineString(it) }
        val result = edit(window, (first - windowStart)..(last - windowStart)) ?: return@whenEditable
        text.replace(windowStart, 0, windowEnd, text.getColumnCount(windowEnd), result.lines.joinToString(separator))
        val selFirst = windowStart + result.selectedLines.first
        val selLast = windowStart + result.selectedLines.last
        e.setSelectionRegion(selFirst, 0, selLast, text.getColumnCount(selLast))
        e.ensureSelectionVisible()
        refresh()
    }

    private companion object {
        const val RETRY_DELAY_MS = 25L
        const val EDITABLE_TIMEOUT_MS = 2_000L
    }
}
