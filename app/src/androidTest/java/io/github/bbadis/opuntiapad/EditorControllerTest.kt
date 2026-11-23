package io.github.bbadis.opuntiapad

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bbadis.opuntiapad.core.text.TextTools
import io.github.bbadis.opuntiapad.editor.EditorController
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Line edits and transformations on a real Sora editor: results and single-step undo. */
@RunWith(AndroidJUnit4::class)
class EditorControllerTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var editor: CodeEditor
    private lateinit var controller: EditorController

    @Before
    fun setUp() {
        instrumentation.runOnMainSync {
            editor = CodeEditor(instrumentation.targetContext)
            // The copy/paste popup needs a window, which this off-screen editor doesn't have.
            editor.getComponent(EditorTextActionWindow::class.java).isEnabled = false
            controller = EditorController().also { it.attach(editor) }
        }
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync { editor.release() }
    }

    private fun main(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
    }

    private fun setText(text: String) = main { editor.setText(Content(text)) }

    /** Edits may wait for a background layout pass to finish: poll for the expected text. */
    private fun assertText(expected: String) {
        val deadline = System.currentTimeMillis() + 5_000
        var actual = ""
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            main { actual = editor.text.toString() }
            if (actual == expected) return
            Thread.sleep(25)
        }
        assertEquals(expected, actual)
    }

    @Test
    fun toggleCommentAndUndo() {
        setText("a = 1\nb = 2\nc = 3")
        main {
            editor.setSelectionRegion(0, 0, 1, 3)
            controller.toggleComment("#", null, "\n")
        }
        assertText("# a = 1\n# b = 2\nc = 3")
        main { editor.undo() }
        assertText("a = 1\nb = 2\nc = 3")
    }

    @Test
    fun duplicateAndMove() {
        setText("1\n2\n3")
        main {
            editor.setSelection(1, 0)
            controller.duplicateLines("\n")
        }
        assertText("1\n2\n2\n3")
        main { controller.moveLines(1, "\n") }
        assertText("1\n2\n3\n2")
        main { controller.moveLines(1, "\n") } // already the last line: no change
        assertText("1\n2\n3\n2")
    }

    @Test
    fun transformSelectionIsOneUndoStep() {
        setText("keep\nhello world\nkeep")
        main { editor.setSelectionRegion(1, 0, 1, 11) }
        val error = runBlocking(Dispatchers.Main) { controller.transform { TextTools.changeCase(it, TextTools.Case.UPPER) } }
        assertNull(error)
        assertText("keep\nHELLO WORLD\nkeep")
        main { editor.undo() }
        assertText("keep\nhello world\nkeep")
    }
}
