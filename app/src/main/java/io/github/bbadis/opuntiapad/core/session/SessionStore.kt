package io.github.bbadis.opuntiapad.core.session

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What is needed to reopen one tab. Unsaved text lives in a separate draft file. */
data class SavedTab(
    val uri: String?,
    val name: String,
    val languageId: String,
    val charset: String,
    val hasBom: Boolean,
    val lineEnding: String,
    val readOnly: Boolean,
    val selectionLine: Int,
    val selectionColumn: Int,
    /** Draft file name inside the drafts directory, for unsaved or untitled tabs. */
    val draft: String?,
)

data class SavedSession(val tabs: List<SavedTab>, val activeIndex: Int)

/**
 * Persists the open tabs and their unsaved changes in the app's private storage, so they survive
 * Android closing the app. Writes are atomic; drafts above [MAX_DRAFT_CHARS] are not kept (the tab
 * then reopens from its file, without the unsaved edits) to bound storage and save time.
 */
class SessionStore(context: Context) {

    private val dir = File(context.applicationContext.filesDir, "session")
    private val draftsDir = File(dir, "drafts")
    private val sessionFile = AtomicFile(File(dir, "session.json"))

    suspend fun save(session: SavedSession, drafts: Map<String, CharSequence>) = withContext(Dispatchers.IO) {
        runCatching {
            draftsDir.mkdirs()
            drafts.forEach { (name, text) -> writeAtomically(AtomicFile(File(draftsDir, name)), text.toString()) }
            // Remove drafts that no longer belong to a tab.
            val keep = session.tabs.mapNotNull { it.draft }.toSet()
            draftsDir.listFiles()?.filter { it.name !in keep && !it.name.endsWith(".bak") }?.forEach { it.delete() }
            writeAtomically(sessionFile, encode(session).toString())
        }.onFailure { Log.w(TAG, "Cannot save session", it) }
    }

    suspend fun load(): SavedSession? = withContext(Dispatchers.IO) {
        runCatching {
            if (!sessionFile.baseFile.exists()) return@runCatching null
            decode(JSONObject(sessionFile.readFully().toString(Charsets.UTF_8)))
        }.onFailure { Log.w(TAG, "Cannot read session", it) }.getOrNull()
    }

    suspend fun readDraft(name: String): String? = withContext(Dispatchers.IO) {
        runCatching { AtomicFile(File(draftsDir, name)).readFully().toString(Charsets.UTF_8) }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) { dir.deleteRecursively() }

    private fun writeAtomically(file: AtomicFile, text: String) {
        val out = file.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    private fun encode(session: SavedSession) = JSONObject()
        .put("version", 1)
        .put("active", session.activeIndex)
        .put(
            "tabs",
            JSONArray().apply {
                session.tabs.forEach { t ->
                    put(
                        JSONObject()
                            .put("uri", t.uri ?: JSONObject.NULL).put("name", t.name).put("language", t.languageId)
                            .put("charset", t.charset).put("bom", t.hasBom).put("lineEnding", t.lineEnding)
                            .put("readOnly", t.readOnly).put("line", t.selectionLine).put("column", t.selectionColumn)
                            .put("draft", t.draft ?: JSONObject.NULL),
                    )
                }
            },
        )

    private fun decode(json: JSONObject): SavedSession {
        val array = json.getJSONArray("tabs")
        val tabs = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            SavedTab(
                uri = o.optStringOrNull("uri"), name = o.getString("name"), languageId = o.getString("language"),
                charset = o.getString("charset"), hasBom = o.optBoolean("bom"), lineEnding = o.optString("lineEnding", "LF"),
                readOnly = o.optBoolean("readOnly"), selectionLine = o.optInt("line"), selectionColumn = o.optInt("column"),
                draft = o.optStringOrNull("draft"),
            )
        }
        return SavedSession(tabs, json.optInt("active", 0))
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

    companion object {
        private const val TAG = "SessionStore"

        /** Unsaved changes bigger than this are not kept as drafts. */
        const val MAX_DRAFT_CHARS = 4 * 1024 * 1024
    }
}
