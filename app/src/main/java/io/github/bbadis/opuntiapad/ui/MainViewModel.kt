package io.github.bbadis.opuntiapad.ui

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
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
import io.github.bbadis.opuntiapad.core.session.SavedSession
import io.github.bbadis.opuntiapad.core.session.SavedTab
import io.github.bbadis.opuntiapad.core.session.SessionStore
import io.github.bbadis.opuntiapad.core.language.Language
import io.github.bbadis.opuntiapad.core.settings.EditorSettings
import io.github.bbadis.opuntiapad.core.settings.RecentFile
import io.github.bbadis.opuntiapad.core.text.LineEnding
import io.github.bbadis.opuntiapad.core.text.TextCodec
import io.github.bbadis.opuntiapad.editor.EditorDocument
import io.github.bbadis.opuntiapad.editor.TextMateSupport
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

/** A localisable message. */
/** A localisable message: a string resource, or a plurals resource when shown with `plural = true`. */
data class UiText(val res: Int, val args: List<Any> = emptyList())

sealed interface UiEvent {
    /** [plural]: [text] is a plurals resource whose quantity is its first argument. */
    data class Message(val text: UiText, val plural: Boolean = false) : UiEvent

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

    private val sessionStore = app.sessionStore

    /** Restores the previous session once; files opened meanwhile (e.g. "Open with") wait for it. */
    private val restoreJob: Job = viewModelScope.launch {
        if (settingsRepository.settings.first().restoreSession) restoreSession()
    }

    init {
        viewModelScope.launch { grammarsReady = TextMateSupport.awaitReady() }
    }

    // --- Session -----------------------------------------------------------------------------

    private suspend fun restoreSession() {
        val session = sessionStore.load() ?: return
        var failed = 0
        for (saved in session.tabs) {
            val language = catalog.detector.byId(saved.languageId) ?: Language.PLAIN_TEXT
            val charset = runCatching { Charset.forName(saved.charset) }.getOrDefault(TextCodec.UTF_8)
            val lineEnding = runCatching { LineEnding.valueOf(saved.lineEnding) }.getOrDefault(LineEnding.LF)
            val uri = saved.uri?.toUri()
            val draft = saved.draft?.let { sessionStore.readDraft(it) }
            val text: String = draft ?: uri?.let { u -> runCatching { documents.read(u, charset = charset).text }.getOrNull() } ?: run {
                failed++
                null
            } ?: continue
            val writable = uri != null && runCatching { documents.info(uri).writable }.getOrDefault(false)
            tabs += EditorDocument(
                id = nextId++, uri = uri, name = saved.name, content = withContext(Dispatchers.Default) { Content(text) },
                language = language, charset = charset, hasBom = saved.hasBom, lineEnding = lineEnding, writable = writable,
                restoredUnsaved = draft != null,
            ).apply {
                readOnly = saved.readOnly
                selectionLine = saved.selectionLine
                selectionColumn = saved.selectionColumn
            }
        }
        if (tabs.isNotEmpty()) activeIndex = session.activeIndex.coerceIn(0, tabs.lastIndex)
        if (failed > 0) events.trySend(UiEvent.Message(UiText(R.plurals.msg_restore_failed, listOf(failed)), plural = true))
    }

    /** Saves open tabs and unsaved changes (called when the app goes to the background). */
    fun saveSession() {
        viewModelScope.launch(NonCancellable) {
            restoreJob.join()
            if (!settings.value.restoreSession || tabs.isEmpty()) {
                sessionStore.clear()
                return@launch
            }
            val drafts = mutableMapOf<String, CharSequence>()
            val saved = tabs.mapIndexed { index, doc ->
                val keepDraft = (doc.isDirty || doc.isUntitled) && doc.content.length <= SessionStore.MAX_DRAFT_CHARS
                val draftName = if (keepDraft) "draft-$index.txt" else null
                if (draftName != null) drafts[draftName] = doc.content.toString()
                SavedTab(
                    uri = doc.uri?.toString(), name = doc.name, languageId = doc.language.id, charset = doc.charset.name(),
                    hasBom = doc.hasBom, lineEnding = doc.lineEnding.name, readOnly = doc.readOnly,
                    selectionLine = doc.selectionLine, selectionColumn = doc.selectionColumn, draft = draftName,
                )
            }.filter { it.uri != null || it.draft != null }
            sessionStore.save(SavedSession(saved, activeIndex.coerceAtLeast(0)), drafts)
        }
    }

    // --- Opening -----------------------------------------------------------------------------

    fun open(uri: Uri, persistPermission: Boolean = false) {
        tabs.indexOfFirst { it.uri == uri }.takeIf { it >= 0 }?.let {
            activeIndex = it
            return
        }
        viewModelScope.launch {
            restoreJob.join()
            tabs.indexOfFirst { it.uri == uri }.takeIf { it >= 0 }?.let {
                activeIndex = it
                return@launch
            }
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
        viewModelScope.launch {
            restoreJob.join()
            val name = title?.takeIf { it.isNotBlank() } ?: nextUntitledName()
            val language = catalog.detector.detect(title, null, text)
            addTab(newDocument(name, text, language))
        }
    }

    /** Opens the result of a conversion (e.g. JSON → YAML) in a new untitled tab. */
    fun openConverted(text: String, sourceName: String, languageId: String) {
        val language = catalog.detector.byId(languageId) ?: Language.PLAIN_TEXT
        val ext = language.extensions.firstOrNull() ?: "txt"
        val name = sourceName.substringBeforeLast('.', sourceName) + "." + ext
        addTab(newDocument(name, text, language))
    }

    fun newFile() {
        viewModelScope.launch {
            restoreJob.join()
            addTab(newDocument(nextUntitledName(), "", Language.PLAIN_TEXT))
        }
    }

    fun setHighlighting(document: EditorDocument, enabled: Boolean) {
        document.highlighting = enabled
    }

    /** Shows a localised message from the UI layer (e.g. a tool failure). */
    fun showMessage(@StringRes res: Int, vararg args: Any) = message(res, *args)

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
