package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class XmlFormatterTest {

    @Test
    fun `formats nested elements and keeps short text inline`() {
        val input = """<?xml version="1.0"?><resources><string name="a">Hello</string><!-- note --><item><x/></item><empty></empty></resources>"""
        val expected = """
            <?xml version="1.0"?>
            <resources>
              <string name="a">Hello</string>
              <!-- note -->
              <item>
                <x/>
              </item>
              <empty></empty>
            </resources>
        """.trimIndent()
        assertEquals(expected, XmlFormatter.format(input))
    }

    @Test
    fun `preserves attributes with angle brackets and CDATA`() {
        val input = "<a title=\"x > y\"><![CDATA[ keep  <this> ]]></a>"
        assertEquals("<a title=\"x > y\">\n  <![CDATA[ keep  <this> ]]>\n</a>", XmlFormatter.format(input))
    }

    @Test
    fun `minify removes indentation`() {
        assertEquals("<a><b>t</b><c/></a>", XmlFormatter.minify("<a>\n  <b>t</b>\n  <c/>\n</a>\n"))
    }

    @Test
    fun `rejects unbalanced documents`() {
        assertThrows(XmlFormatter.XmlSyntaxException::class.java) { XmlFormatter.format("<a><b></a>") }
        assertThrows(XmlFormatter.XmlSyntaxException::class.java) { XmlFormatter.format("<a>") }
        assertThrows(XmlFormatter.XmlSyntaxException::class.java) { XmlFormatter.format("<a attr=\"x>") }
    }
}
