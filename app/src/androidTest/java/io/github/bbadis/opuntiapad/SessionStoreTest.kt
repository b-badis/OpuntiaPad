package io.github.bbadis.opuntiapad

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bbadis.opuntiapad.core.session.SavedSession
import io.github.bbadis.opuntiapad.core.session.SavedTab
import io.github.bbadis.opuntiapad.core.session.SessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionStoreTest {

    private val store = SessionStore(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun roundTripWithDrafts() = runBlocking {
        val tabs = listOf(
            SavedTab("content://x/a.yaml", "a.yaml", "yaml", "UTF-8", false, "LF", false, 3, 2, null),
            SavedTab(null, "Untitled 1", "text", "UTF-8", false, "CRLF", true, 0, 0, "draft-1.txt"),
        )
        store.save(SavedSession(tabs, 1), mapOf("draft-1.txt" to "unsaved é ✓\r\n"))
        val loaded = store.load()!!
        assertEquals(tabs, loaded.tabs)
        assertEquals(1, loaded.activeIndex)
        assertEquals("unsaved é ✓\r\n", store.readDraft("draft-1.txt"))

        store.clear()
        assertNull(store.load())
    }
}
