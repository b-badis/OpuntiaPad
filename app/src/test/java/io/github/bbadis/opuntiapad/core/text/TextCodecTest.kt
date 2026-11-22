package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCodecTest {

    @Test
    fun `decodes plain utf-8`() {
        val decoded = TextCodec.decode("héllo: wörld ✓".toByteArray(Charsets.UTF_8))
        assertEquals("héllo: wörld ✓", decoded.text)
        assertEquals(Charsets.UTF_8, decoded.charset)
        assertFalse(decoded.hasBom)
    }

    @Test
    fun `strips and remembers utf-8 bom`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "a: 1".toByteArray()
        val decoded = TextCodec.decode(bytes)
        assertEquals("a: 1", decoded.text)
        assertTrue(decoded.hasBom)
        assertArrayEquals(bytes, TextCodec.encode(decoded.text, decoded.charset, decoded.hasBom))
    }

    @Test
    fun `detects utf-16 le bom`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "SELECT 1;".toByteArray(Charsets.UTF_16LE)
        val decoded = TextCodec.decode(bytes)
        assertEquals("SELECT 1;", decoded.text)
        assertEquals(Charsets.UTF_16LE, decoded.charset)
    }

    @Test
    fun `falls back to windows-1252 for invalid utf-8`() {
        val bytes = byteArrayOf('c'.code.toByte(), 0xE9.toByte(), 't'.code.toByte(), 0xE9.toByte()) // "cété" in latin-1
        val decoded = TextCodec.decode(bytes)
        assertEquals("cété", decoded.text)
        assertEquals("windows-1252", decoded.charset.name())
    }

    @Test
    fun `forced charset wins`() {
        val decoded = TextCodec.decode("abc".toByteArray(), Charsets.ISO_8859_1)
        assertEquals(Charsets.ISO_8859_1, decoded.charset)
    }

    @Test
    fun `binary detection`() {
        assertTrue(TextCodec.looksBinary(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)))
        assertFalse(TextCodec.looksBinary("key: value\n\tindented\r\n".toByteArray()))
        assertFalse(TextCodec.looksBinary(ByteArray(0)))
    }

    @Test
    fun `line ending detection`() {
        assertEquals(LineEnding.LF, LineEnding.detect("a\nb\nc"))
        assertEquals(LineEnding.CRLF, LineEnding.detect("a\r\nb\r\nc"))
        assertEquals(LineEnding.CR, LineEnding.detect("a\rb\rc"))
        assertEquals(LineEnding.LF, LineEnding.detect("single line"))
    }

    @Test
    fun `line ending conversion`() {
        assertEquals("a\r\nb\r\nc\r\n", LineEnding.convert("a\nb\r\nc\r", LineEnding.CRLF))
        assertEquals("a\nb\n", LineEnding.convert("a\r\nb\r\n", LineEnding.LF))
    }
}
