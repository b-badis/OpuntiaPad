package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineEditsTest {

    @Test
    fun `line comments are added at the common indentation and removed again`() {
        val code = listOf("  a = 1", "", "    b = 2")
        val commented = LineEdits.toggleComment(code, "#", null)
        assertEquals(listOf("  # a = 1", "", "  #   b = 2"), commented)
        assertEquals(code, LineEdits.toggleComment(commented, "#", null))
    }

    @Test
    fun `block comments wrap the selection`() {
        val css = listOf("  a { color: red; }")
        val commented = LineEdits.toggleComment(css, null, "/*" to "*/")
        assertEquals(listOf("  /* a { color: red; } */"), commented)
        assertEquals(css, LineEdits.toggleComment(commented, null, "/*" to "*/"))
    }

    @Test
    fun `duplicate and move`() {
        val lines = listOf("1", "2", "3", "4")
        assertEquals(listOf("1", "2", "3", "2", "3", "4"), LineEdits.duplicate(lines, 1..2))
        assertEquals(listOf("2", "3", "1", "4"), LineEdits.move(lines, 1..2, -1))
        assertEquals(listOf("1", "4", "2", "3"), LineEdits.move(lines, 1..2, 1))
        assertNull(LineEdits.move(lines, 0..0, -1))
        assertNull(LineEdits.move(lines, 3..3, 1))
    }
}
