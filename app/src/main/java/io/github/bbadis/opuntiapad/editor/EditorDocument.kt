package io.github.bbadis.opuntiapad.editor

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.bbadis.opuntiapad.core.language.Language
import io.github.bbadis.opuntiapad.core.text.LargeFilePolicy
import io.github.bbadis.opuntiapad.core.text.LineEnding
import io.github.rosemoe.sora.text.Content
import java.nio.charset.Charset

/**
 * One open document (a tab). The [content] is Sora's piece-table-like text model; it keeps
 * its own undo history, so switching tabs preserves undo/redo per document.
 */
@Stable
class EditorDocument(
    val id: Long,
    uri: Uri?,
    name: String,
    val content: Content,
    language: Language,
    charset: Charset,
    hasBom: Boolean,
    lineEnding: LineEnding,
    writable: Boolean,
    val mimeType: String? = null,
    /** True when restored from a draft: the text differs from what is saved on disk. */
    restoredUnsaved: Boolean = false,
) {
    var uri by mutableStateOf(uri)
    var name by mutableStateOf(name)
    var language by mutableStateOf(language)
    var charset by mutableStateOf(charset)
    var hasBom by mutableStateOf(hasBom)
    var lineEnding by mutableStateOf(lineEnding)

    /** Whether the underlying document can be written in place. */
    var writable by mutableStateOf(writable)

    /** User-controlled view mode: prevents accidental edits. */
    var readOnly by mutableStateOf(false)

    private var savedVersion by mutableLongStateOf(if (restoredUnsaved) -1L else content.documentVersion)
    var version by mutableLongStateOf(content.documentVersion)
        private set

    /** Syntax colouring; off by default for very large files (see LargeFilePolicy). */
    var highlighting by mutableStateOf(LargeFilePolicy.highlightByDefault(language.id, content.length))

    /** Selection to restore when the tab becomes active again. */
    var selectionLine by mutableIntStateOf(0)
    var selectionColumn by mutableIntStateOf(0)

    val isDirty: Boolean get() = version != savedVersion
    val isUntitled: Boolean get() = uri == null

    fun onContentChanged() {
        version = content.documentVersion
    }

    fun markSaved() {
        version = content.documentVersion
        savedVersion = version
    }
}
