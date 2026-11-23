package io.github.bbadis.opuntiapad.ui.tools

import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.core.settings.EditorSettings
import io.github.bbadis.opuntiapad.core.text.DocumentFormatter
import io.github.bbadis.opuntiapad.core.text.JsonFormatter
import io.github.bbadis.opuntiapad.core.text.XmlFormatter
import io.github.bbadis.opuntiapad.core.text.TextTools
import io.github.bbadis.opuntiapad.core.text.YamlJsonConverter
import io.github.bbadis.opuntiapad.editor.EditorController
import io.github.bbadis.opuntiapad.editor.EditorDocument
import io.github.bbadis.opuntiapad.ui.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Checksums shown in the hash dialog. */
data class Checksums(val sha256: String, val sha1: String, val md5: String)

/**
 * The actions of the Tools sheet. Text transformations apply to the selection, or to the whole
 * document when nothing is selected, as one undoable edit computed off the main thread.
 */
class DocumentTools(
    private val viewModel: MainViewModel,
    private val controller: EditorController,
    private val scope: CoroutineScope,
    private val settings: () -> EditorSettings,
) {
    private val indent: String
        get() = settings().let { if (it.useSpaces) " ".repeat(it.tabSize) else "\t" }

    fun format(document: EditorDocument) = transform(R.string.msg_format_failed) {
        withSyntaxLine(it) { DocumentFormatter.format(document.language.id, it, indent).withLineEnding(document) }
    }

    fun minify(document: EditorDocument) = transform(R.string.msg_format_failed) {
        withSyntaxLine(it) { DocumentFormatter.minify(document.language.id, it) }
    }

    fun convertToYaml(document: EditorDocument) = convert(document, "yaml") { YamlJsonConverter.jsonToYaml(it, settings().tabSize) }

    fun convertToJson(document: EditorDocument) = convert(document, "json") { YamlJsonConverter.yamlToJson(it, indent) }

    fun sortLines(descending: Boolean) = transform { TextTools.sortLines(it, descending) }
    fun removeDuplicateLines() = transform { TextTools.removeDuplicateLines(it) }
    fun trimTrailingWhitespace() = transform { TextTools.trimTrailingWhitespace(it) }
    fun changeCase(case: TextTools.Case) = transform { TextTools.changeCase(it, case) }
    fun tabsToSpaces() = transform { TextTools.tabsToSpaces(it, settings().tabSize) }
    fun spacesToTabs() = transform { TextTools.spacesToTabs(it, settings().tabSize) }
    fun base64Encode() = transform { TextTools.base64Encode(it) }
    fun base64Decode() = transform { TextTools.base64Decode(it) }
    fun urlEncode() = transform { TextTools.urlEncode(it) }
    fun urlDecode() = transform { TextTools.urlDecode(it) }

    fun toggleComment(document: EditorDocument) {
        val lang = document.language
        if (!controller.toggleComment(lang.lineComment, lang.blockComment, document.lineEnding.separator)) {
            viewModel.showMessage(R.string.msg_no_comment_syntax, lang.name)
        }
    }

    fun duplicateLines(document: EditorDocument) = controller.duplicateLines(document.lineEnding.separator)

    fun moveLines(document: EditorDocument, delta: Int) = controller.moveLines(delta, document.lineEnding.separator)

    /** Checksums of the selection, or of the whole document. */
    suspend fun checksums(document: EditorDocument): Checksums {
        val text = controller.selectedText() ?: document.content.toString()
        return withContext(Dispatchers.Default) {
            Checksums(TextTools.hash(text, "SHA-256"), TextTools.hash(text, "SHA-1"), TextTools.hash(text, "MD5"))
        }
    }

    private fun transform(failureMessage: Int = R.string.msg_tool_failed, block: (String) -> String) {
        scope.launch {
            when (val error = controller.transform(block)) {
                null -> Unit
                is SyntaxError -> viewModel.showMessage(R.string.msg_format_failed_line, error.message.orEmpty(), error.line)
                else -> viewModel.showMessage(failureMessage, error.message ?: error.javaClass.simpleName)
            }
        }
    }

    /** A formatter error located on a line (1-based, relative to the transformed text). */
    private class SyntaxError(message: String, val line: Int) : IllegalArgumentException(message)

    /** Converts formatter exceptions carrying a character offset into a [SyntaxError] with its line. */
    private inline fun withSyntaxLine(text: String, block: () -> String): String {
        val offset: Int
        val message: String?
        try {
            return block()
        } catch (e: JsonFormatter.JsonSyntaxException) {
            offset = e.offset; message = e.message
        } catch (e: XmlFormatter.XmlSyntaxException) {
            offset = e.offset; message = e.message
        }
        val line = text.subSequence(0, offset.coerceIn(0, text.length)).count { it == '\n' } + 1
        throw SyntaxError(message.orEmpty(), line)
    }

    private fun convert(document: EditorDocument, targetLanguage: String, block: (String) -> String) {
        val source = controller.selectedText() ?: document.content.toString()
        scope.launch {
            withContext(Dispatchers.Default) { runCatching { block(source) } }
                .onSuccess { viewModel.openConverted(it, document.name, targetLanguage) }
                .onFailure { viewModel.showMessage(R.string.msg_convert_failed, it.message ?: it.javaClass.simpleName) }
        }
    }

    private fun String.withLineEnding(document: EditorDocument): String =
        if (document.lineEnding.separator == "\n") this else replace("\n", document.lineEnding.separator)
}
