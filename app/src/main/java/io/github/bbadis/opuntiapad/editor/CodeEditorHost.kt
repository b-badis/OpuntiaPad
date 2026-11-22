package io.github.bbadis.opuntiapad.editor

import android.graphics.Typeface
import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bbadis.opuntiapad.core.language.EditorTheme
import io.github.bbadis.opuntiapad.core.settings.EditorSettings
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CancellationException

/**
 * Hosts a single Sora [CodeEditor] and swaps the active [document] into it.
 * All editor features (selection handles, pinch-zoom, line numbers, search highlight…)
 * come from Sora; this composable only wires state in and events out.
 */
@Composable
fun CodeEditorHost(
    document: EditorDocument,
    settings: EditorSettings,
    editorTheme: EditorTheme,
    grammarsReady: Boolean,
    controller: EditorController,
    onFontSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val currentDocument by rememberUpdatedState(document)
    val currentOnFontSizeChanged by rememberUpdatedState(onFontSizeChanged)
    var themeReady by remember { mutableStateOf(false) }

    val editor = remember {
        CodeEditor(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            typefaceText = Typeface.MONOSPACE
            typefaceLineNumber = Typeface.MONOSPACE
            isScalable = true
            setLigatureEnabled(false)
            isHighlightBracketPair = true
            props.apply {
                autoIndent = true
                deleteEmptyLineFast = false
                highlightMatchingDelimiters = true
                stickyScrollMaxLines = 3
            }
            searcher.setCyclicJumping(true)

            subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                currentDocument.onContentChanged()
                controller.refresh()
            }
            subscribeEvent(SelectionChangeEvent::class.java) { _, _ -> controller.refresh() }
            subscribeEvent(PublishSearchResultEvent::class.java) { _, _ -> controller.refreshSearch() }
            subscribeEvent(TextSizeChangeEvent::class.java) { event, _ ->
                val sp = with(density) { event.newTextSize.toSp().value }
                controller.fontSize = sp
                currentOnFontSizeChanged(sp)
            }
        }
    }

    DisposableEffect(editor) {
        controller.attach(editor)
        onDispose {
            currentDocument.rememberSelection(editor)
            controller.detach(editor)
            editor.release()
        }
    }

    // Swap the document into the editor; remember the caret of the previous one.
    DisposableEffect(document.id) {
        editor.setText(document.content, true, null)
        val line = document.selectionLine.coerceIn(0, maxOf(0, document.content.lineCount - 1))
        val column = document.selectionColumn.coerceIn(0, document.content.getColumnCount(line))
        editor.setSelection(line, column)
        controller.stopSearch()
        controller.refresh()
        onDispose { document.rememberSelection(editor) }
    }

    LaunchedEffect(editorTheme.id) {
        // A broken theme must never take the app down: fall back to the editor's default colours.
        try {
            TextMateSupport.applyTheme(editorTheme)
            if (editor.colorScheme !is TextMateColorScheme) {
                editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("CodeEditorHost", "Cannot apply theme ${editorTheme.id}", e)
            if (editor.colorScheme is TextMateColorScheme) editor.colorScheme = EditorColorScheme()
        }
        editor.getComponent(EditorAutoCompletion::class.java).applyColorScheme()
        themeReady = true
    }

    LaunchedEffect(document.id, document.language, grammarsReady, themeReady, settings.tabSize, settings.useSpaces, settings.autoComplete) {
        if (!themeReady) return@LaunchedEffect
        editor.setEditorLanguage(
            TextMateSupport.createLanguage(document.language, settings.tabSize, settings.useSpaces, settings.autoComplete),
        )
    }

    LaunchedEffect(settings.fontSize) { controller.setZoom(settings.fontSize) }

    LaunchedEffect(document.id, document.readOnly) { editor.isEditable = !document.readOnly }

    AndroidView(
        factory = { editor },
        modifier = modifier,
        update = { e ->
            if (e.isWordwrap != settings.wordWrap) e.setWordwrap(settings.wordWrap)
            e.isLineNumberEnabled = settings.lineNumbers
            e.setPinLineNumber(settings.pinLineNumbers)
            e.isHighlightCurrentLine = settings.highlightCurrentLine
            e.tabWidth = settings.tabSize
            e.props.symbolPairAutoCompletion = settings.autoCloseBrackets
            e.props.stickyScroll = settings.stickyScroll
            e.getComponent(EditorAutoCompletion::class.java).isEnabled = settings.autoComplete
            e.nonPrintablePaintingFlags = if (settings.showWhitespace) {
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_WHITESPACE_INNER or
                    CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR
            } else {
                0
            }
            e.keepScreenOn = settings.keepScreenOn
        },
    )
}

private fun EditorDocument.rememberSelection(editor: CodeEditor) {
    if (editor.text !== content) return
    selectionLine = editor.cursor.leftLine
    selectionColumn = editor.cursor.leftColumn
}
