package io.github.bbadis.opuntiapad.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.SelectionMovement

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

    fun insert(text: String, cursorOffset: Int = text.length) {
        val e = editor ?: return
        if (!e.isEditable) return
        e.insertText(text, cursorOffset)
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
}
