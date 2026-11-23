package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

class TextToolsTest {

    @Test
    fun `sort keeps separator and final newline`() {
        assertEquals("apple\r\nÉcole\r\nzebra\r\n", TextTools.sortLines("zebra\r\napple\r\nÉcole\r\n", locale = Locale.FRENCH))
        assertEquals("c\nb\na", TextTools.sortLines("a\nc\nb", descending = true))
    }

    @Test
    fun `duplicates and trailing whitespace`() {
        assertEquals("a\nb\n", TextTools.removeDuplicateLines("a\nb\na\nb\n"))
        assertEquals("a\n\tb\nc", TextTools.trimTrailingWhitespace("a  \n\tb\t\nc "))
    }

    @Test
    fun `case changes`() {
        assertEquals("HELLO WORLD", TextTools.changeCase("hello world", TextTools.Case.UPPER, Locale.ROOT))
        assertEquals("Hello World L'été", TextTools.changeCase("hELLO wORLD l'ÉTÉ", TextTools.Case.TITLE, Locale.FRENCH))
    }

    @Test
    fun `indentation conversion only touches leading whitespace`() {
        assertEquals("    a\tb\n  c", TextTools.tabsToSpaces("\ta\tb\n  c", 4))
        assertEquals("\ta  b\n\t  c", TextTools.spacesToTabs("    a  b\n      c", 4))
    }

    @Test
    fun `encodings and hashes`() {
        assertEquals("w6l0w6kg4pyT", TextTools.base64Encode("été ✓"))
        assertEquals("été ✓", TextTools.base64Decode("w6l0w6kg4pyT"))
        assertEquals("a b+c", TextTools.base64Decode(TextTools.base64Encode("a b+c")))
        assertThrows(IllegalArgumentException::class.java) { TextTools.base64Decode("not base64!") }
        assertEquals("a%20b%26c%3D%C3%A9", TextTools.urlEncode("a b&c=é"))
        assertEquals("a b&c=é+", TextTools.urlDecode("a%20b%26c%3D%C3%A9+"))
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", TextTools.hash("hello", "SHA-256"))
        assertEquals("5d41402abc4b2a76b9719d911017c592", TextTools.hash("hello", "MD5"))
    }
}
