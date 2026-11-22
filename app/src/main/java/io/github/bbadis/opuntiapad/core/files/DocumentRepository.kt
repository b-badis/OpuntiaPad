package io.github.bbadis.opuntiapad.core.files

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import io.github.bbadis.opuntiapad.core.text.DecodedText
import io.github.bbadis.opuntiapad.core.text.TextCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.Charset

/** Metadata about a document reachable through a [Uri]. */
data class DocumentInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val size: Long?,
    val writable: Boolean,
)

class FileTooLargeException(val size: Long, val limit: Long) : IOException("File too large: $size bytes")

/** Reads and writes text documents through the Storage Access Framework / content providers. */
class DocumentRepository(context: Context) {

    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    suspend fun info(uri: Uri): DocumentInfo = withContext(Dispatchers.IO) {
        var name: String? = null
        var size: Long? = null
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val file = File(requireNotNull(uri.path))
            name = file.name
            size = file.length()
        } else {
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val s = c.getColumnIndex(OpenableColumns.SIZE)
                        if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                        if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                    }
                }
            }
        }
        DocumentInfo(
            uri = uri,
            displayName = name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "untitled",
            mimeType = runCatching { resolver.getType(uri) }.getOrNull(),
            size = size,
            writable = isWritable(uri),
        )
    }

    suspend fun read(uri: Uri, maxBytes: Long = MAX_FILE_SIZE, charset: Charset? = null): DecodedText = withContext(Dispatchers.IO) {
        val stream = resolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
        val bytes = stream.use { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(chunk)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw FileTooLargeException(total, maxBytes)
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        }
        TextCodec.decode(bytes, charset)
    }

    suspend fun write(uri: Uri, text: CharSequence, charset: Charset, withBom: Boolean) = withContext(Dispatchers.IO) {
        val bytes = TextCodec.encode(text, charset, withBom)
        // "wt" truncates; some providers only support "w" (and then truncate themselves).
        val stream = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: resolver.openOutputStream(uri, "w")
            ?: throw IOException("Cannot open $uri for writing")
        stream.use { it.write(bytes) }
    }

    /** Keeps access to a document chosen through the system picker across restarts. */
    fun persistPermission(uri: Uri, write: Boolean) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or (if (write) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        runCatching { resolver.takePersistableUriPermission(uri, flags) }
            .recoverCatching { if (write) resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    fun releasePermission(uri: Uri) {
        runCatching {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    fun hasReadAccess(uri: Uri): Boolean =
        uri.scheme == ContentResolver.SCHEME_FILE ||
            resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

    // Only checks a permission on the URI; nothing is launched (lint false positive).
    @SuppressLint("UnsafeIntentLaunch")
    private fun isWritable(uri: Uri): Boolean {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.path?.let { File(it).canWrite() } == true
        if (DocumentsContract.isDocumentUri(appContext, uri)) {
            val flags = runCatching {
                resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getInt(0) else 0
                } ?: 0
            }.getOrDefault(0)
            if (flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE == 0) return false
        }
        return appContext.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        /** Files above this size are refused (the editor keeps the whole text in memory). */
        const val MAX_FILE_SIZE: Long = 32L * 1024 * 1024
    }
}
