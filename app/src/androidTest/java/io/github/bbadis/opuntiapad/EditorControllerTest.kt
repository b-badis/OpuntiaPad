package io.github.bbadis.opuntiapad

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bbadis.opuntiapad.core.text.TextTools
import io.github.bbadis.opuntiapad.editor.EditorController
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Line edits and transformations on a real Sora editor: results and single-step undo. */
@RunWith(AndroidJUnit4::class)
class EditorControllerTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun withEditor(text: String, block: (CodeEditor, EditorController) -> Unit) {
        instrumentation.runOnMainSync {
            val editor = CodeEditor(instrumentation.targetContext)
            editor.setText(Content(text))
            val controller = EditorController()
            controller.attach(editor)
            block(editor, controller)
            editor.release()
        }
    }

    @Test
    fun toggleCommentAndUndo() = withEditor("a = 1\nb = 2\nc = 3") { editor, c ->
        editor.setSelectionRegion(0, 0, 1, 3)
        c.toggleComment("#", null, "\n")
        assertEquals("# a = 1\n# b = 2\nc = 3", editor.text.toString())
        editor.undo()
        assertEquals("a = 1\nb = 2\nc = 3", editor.text.toString())
    }

    @Test
    fun duplicateAndMove() = withEditor("1\n2\n3") { editor, c ->
        editor.setSelection(1, 0)
        c.duplicateLines("\n")
        assertEquals("1\n2\n2\n3", editor.text.toString())
        c.moveLines(1, "\n")
        assertEquals("1\n2\n3\n2", editor.text.toString())
        c.moveLines(1, "\n") // already last: no change
        assertEquals("1\n2\n3\n2", editor.text.toString())
    }

    @Test
    fun transformSelectionIsOneUndoStep() {
        lateinit var editor: CodeEditor
        lateinit var controller: EditorController
        instrumentation.runOnMainSync {
            editor = CodeEditor(instrumentation.targetContext).apply { setText(Content("keep\nhello world\nkeep")) }
            controller = EditorController()
            controller.attach(editor)
            editor.setSelectionRegion(1, 0, 1, 11)
        }
        val error = runBlocking(Dispatchers.Main) { controller.transform { TextTools.changeCase(it, TextTools.Case.UPPER) } }
        assertNull(error)
        instrumentation.runOnMainSync {
            assertEquals("keep\nHELLO WORLD\nkeep", editor.text.toString())
            editor.undo()
            assertEquals("keep\nhello world\nkeep", editor.text.toString())
            editor.release()
        }
    }
}
