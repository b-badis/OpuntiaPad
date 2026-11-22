package io.github.bbadis.opuntiapad.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.bbadis.opuntiapad.OpuntiaPadApp
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.core.files.FileTooLargeException
import io.github.bbadis.opuntiapad.core.language.Language
import io.github.bbadis.opuntiapad.core.settings.EditorSettings
import io.github.bbadis.opuntiapad.core.settings.RecentFile
import io.github.bbadis.opuntiapad.core.text.JsonFormatter
import io.github.bbadis.opuntiapad.core.text.LineEnding
import io.github.bbadis.opuntiapad.core.text.TextCodec
import io.github.bbadis.opuntiapad.editor.EditorDocument
import io.github.bbadis.opuntiapad.editor.TextMateSupport
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

/** A localisable message. */
data class UiText(@param:StringRes val res: Int, val args: List<Any> = emptyList())

sealed interface UiEvent {
    data class Message(val text: UiText) : UiEvent

    /** The UI must ask the user for a destination (system "Save as" picker). */
    data class RequestSaveAs(val documentId: Long, val suggestedName: String, val mimeType: String) : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OpuntiaPadApp
    private val documents = app.documentRepository
    private val settingsRepository = app.settingsRepository
    val catalog = app.catalog

    val settings: StateFlow<EditorSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, EditorSettings())
    val recentFiles: StateFlow<List<RecentFile>> =
        settingsRepository.recentFiles.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tabs = mutableStateListOf<EditorDocument>()
    var activeIndex by mutableIntStateOf(-1)
        private set
    val activeDocument: EditorDocument? get() = tabs.getOrNull(activeIndex)

    var grammarsReady by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set

    /** Document whose close is waiting for a "discard changes?" answer. */
    var pendingClose by mutableStateOf<EditorDocument?>(null)
        private set

    private val events = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    private var nextId = 1L
    private var untitledCounter = 1
    private var fontSaveJob: Job? = null
    private var closeAfterSave: Long? = null

    init {
        viewModelScope.launch { grammarsReady = TextMateSupport.awaitReady() }
    }

    // --- Opening -----------------------------------------------------------------------------

    fun open(uri: Uri, persistPermission: Boolean = false) {
        tabs.indexOfFirst { it.uri == uri }.takeIf { it >= 0 }?.let {
            activeIndex = it
            return
        }
        viewModelScope.launch {
            loading = true
            try {
                if (persistPermission) documents.persistPermission(uri, write = true)
                val info = documents.info(uri)
                val decoded = documents.read(uri)
                if (decoded.looksBinary) message(R.string.msg_binary_file, info.displayName)
                val language = catalog.detector.detect(info.displayName, info.mimeType, decoded.text)
                val content = withContext(Dispatchers.Default) { Content(decoded.text) }
                addTab(
                    EditorDocument(
                        id = nextId++, uri = uri, name = info.displayName, content = content, language = language,
                        charset = decoded.charset, hasBom = decoded.hasBom, lineEnding = decoded.lineEnding,
                        writable = info.writable, mimeType = info.mimeType,
                    ).also { if (decoded.looksBinary) it.readOnly = true },
                )
                settingsRepository.addRecent(uri, info.displayName)
            } catch (e: FileTooLargeException) {
                message(R.string.msg_file_too_large, e.limit / (1024 * 1024))
            } catch (e: SecurityException) {
                message(R.string.msg_permission_denied)
                settingsRepository.removeRecent(uri)
            } catch (e: Exception) {
                message(R.string.msg_open_failed, e.localizedMessage ?: e.javaClass.simpleName)
            } finally {
                loading = false
            }
        }
    }

    /** Opens shared plain text (e.g. "Share → OpuntiaPad" from a chat app) as an untitled document. */
    fun openText(text: String, title: String?) {
        val name = title?.takeIf { it.isNotBlank() } ?: nextUntitledName()
        val language = catalog.detector.detect(title, null, text)
        addTab(newDocument(name, text, language))
    }

    fun newFile() {
        addTab(newDocument(nextUntitledName(), "", Language.PLAIN_TEXT))
    }

    private fun newDocument(name: String, text: String, language: Language) = EditorDocument(
        id = nextId++, uri = null, name = name, content = Content(text), language = language,
        charset = TextCodec.UTF_8, hasBom = false, lineEnding = LineEnding.detect(text), writable = false,
    )

    private fun nextUntitledName() = app.getString(R.string.untitled_name, untitledCounter++)

    private fun addTab(document: EditorDocument) {
        tabs += document
        activeIndex = tabs.lastIndex
    }

    fun select(index: Int) {
        if (index in tabs.indices) activeIndex = index
    }

    // --- Closing -----------------------------------------------------------------------------

    fun requestClose(document: EditorDocument) {
        if (document.isDirty) pendingClose = document else close(document)
    }

    fun dismissClose() {
        pendingClose = null
    }

    fun discardAndClose() {
        pendingClose?.let(::close)
        pendingClose = null
    }

    fun saveAndClose() {
        val doc = pendingClose ?: return
        pendingClose = null
        closeAfterSave = doc.id
        save(doc)
    }

    private fun close(document: EditorDocument) {
        val index = tabs.indexOf(document)
        if (index < 0) return
        tabs.removeAt(index)
        activeIndex = when {
            tabs.isEmpty() -> -1
            activeIndex > index -> activeIndex - 1
            activeIndex == index -> index.coerceAtMost(tabs.lastIndex)
            else -> activeIndex
        }
    }

    fun closeAll() {
        tabs.clear()
        activeIndex = -1
    }

    val hasUnsavedChanges: Boolean get() = tabs.any { it.isDirty }

    // --- Saving ------------------------------------------------------------------------------

    fun save(document: EditorDocument) {
        val uri = document.uri
        if (uri == null || !document.writable) {
            requestSaveAs(document)
            return
        }
        viewModelScope.launch {
            try {
                val text = document.content.toString()
                documents.write(uri, text, document.charset, document.hasBom)
                document.markSaved()
                message(R.string.msg_saved, document.name)
                if (closeAfterSave == document.id) close(document)
            } catch (e: Exception) {
                document.writable = false
                message(R.string.msg_save_failed, e.localizedMessage ?: e.javaClass.simpleName)
                requestSaveAs(document)
            } finally {
                closeAfterSave = null
            }
        }
    }

    fun requestSaveAs(document: EditorDocument) {
        val ext = document.language.extensions.firstOrNull() ?: "txt"
        val name = if (document.isUntitled && !document.name.contains('.')) "${document.name}.$ext" else document.name
        val mime = document.mimeType?.takeUnless { it == "application/octet-stream" } ?: "text/plain"
        events.trySend(UiEvent.RequestSaveAs(document.id, name, mime))
    }

    fun saveAs(documentId: Long, target: Uri?) {
        val document = tabs.firstOrNull { it.id == documentId } ?: return
        if (target == null) {
            closeAfterSave = null
            return
        }
        viewModelScope.launch {
            try {
                documents.persistPermission(target, write = true)
                documents.write(target, document.content.toString(), document.charset, document.hasBom)
                val info = documents.info(target)
                document.uri = target
                document.name = info.displayName
                document.writable = true
                if (document.language.isPlainText) document.language = catalog.detector.detect(info.displayName, info.mimeType, document.content)
                document.markSaved()
                settingsRepository.addRecent(target, info.displayName)
                message(R.string.msg_saved, info.displayName)
                if (closeAfterSave == document.id) close(document)
            } catch (e: Exception) {
                message(R.string.msg_save_failed, e.localizedMessage ?: e.javaClass.simpleName)
            } finally {
                closeAfterSave = null
            }
        }
    }

    // --- Document operations -----------------------------------------------------------------

    fun setLanguage(document: EditorDocument, language: Language) {
        document.language = language
    }

    fun toggleReadOnly(document: EditorDocument) {
        document.readOnly = !document.readOnly
    }

    fun reopenWithEncoding(document: EditorDocument, charsetName: String) {
        val uri = document.uri ?: return
        val charset = runCatching { Charset.forName(charsetName) }.getOrNull() ?: return
        if (document.isDirty) {
            message(R.string.msg_save_before_reencode)
            return
        }
        viewModelScope.launch {
            try {
                val decoded = documents.read(uri, charset = charset)
                replaceAll(document, decoded.text)
                document.charset = charset
                document.hasBom = decoded.hasBom
                document.markSaved()
            } catch (e: Exception) {
                message(R.string.msg_open_failed, e.localizedMessage ?: e.javaClass.simpleName)
            }
        }
    }

    fun convertLineEndings(document: EditorDocument, target: LineEnding) {
        if (document.lineEnding == target) return
        replaceAll(document, LineEnding.convert(document.content, target))
        document.lineEnding = target
    }

    fun formatJson(document: EditorDocument, minify: Boolean) {
        try {
            val indent = if (settings.value.useSpaces) " ".repeat(settings.value.tabSize) else "\t"
            val text = if (minify) JsonFormatter.minify(document.content) else JsonFormatter.format(document.content, indent)
            replaceAll(document, if (document.lineEnding == LineEnding.LF) text else LineEnding.convert(text, document.lineEnding))
        } catch (e: JsonFormatter.JsonSyntaxException) {
            val line = document.content.indexer.getCharPosition(e.offset.coerceAtMost(document.content.length)).line + 1
            message(R.string.msg_invalid_json, e.message.orEmpty(), line)
        }
    }

    /** Replaces the whole text as a single undoable edit. */
    private fun replaceAll(document: EditorDocument, text: String) {
        if (document.readOnly) return
        val content = document.content
        val lastLine = content.lineCount - 1
        content.replace(0, 0, lastLine, content.getColumnCount(lastLine), text)
        document.onContentChanged()
    }

    // --- Settings & recents ------------------------------------------------------------------

    fun updateSettings(transform: (EditorSettings) -> EditorSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    /** Persists pinch-zoom changes, debounced. */
    fun onFontSizeChanged(sizeSp: Float) {
        fontSaveJob?.cancel()
        fontSaveJob = viewModelScope.launch {
            delay(600)
            val rounded = (sizeSp * 2).toInt() / 2f
            if (rounded != settings.value.fontSize) settingsRepository.update { it.copy(fontSize = rounded) }
        }
    }

    fun removeRecent(file: RecentFile) {
        viewModelScope.launch {
            settingsRepository.removeRecent(file.uri)
            documents.releasePermission(file.uri)
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            recentFiles.value.forEach { documents.releasePermission(it.uri) }
            settingsRepository.clearRecents()
        }
    }

    private fun message(@StringRes res: Int, vararg args: Any) {
        events.trySend(UiEvent.Message(UiText(res, args.toList())))
    }
}
