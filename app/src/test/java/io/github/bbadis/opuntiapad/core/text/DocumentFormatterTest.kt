package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentFormatterTest {

    @Test
    fun `capabilities per language`() {
        assertTrue(DocumentFormatter.canFormat("json") && DocumentFormatter.canMinify("json"))
        assertTrue(DocumentFormatter.canFormat("xml") && DocumentFormatter.canMinify("xml"))
        assertTrue(DocumentFormatter.canFormat("sql") && DocumentFormatter.canFormat("plsql"))
        assertFalse(DocumentFormatter.canMinify("sql"))
        assertFalse(DocumentFormatter.canFormat("jsonc")) // comments would be rejected by the strict formatter
        assertFalse(DocumentFormatter.canFormat("yaml"))
        assertTrue(DocumentFormatter.canConvertToYaml("json") && DocumentFormatter.canConvertToJson("yaml"))
        assertFalse(DocumentFormatter.canConvertToYaml("yaml") || DocumentFormatter.canConvertToJson("json"))
    }

    @Test
    fun `dispatches to the right formatter`() {
        assertEquals("{\n  \"a\": 1\n}", DocumentFormatter.format("json", "{\"a\":1}", "  "))
        assertEquals("<a>\n  <b/>\n</a>", DocumentFormatter.format("xml", "<a><b/></a>", "  "))
        assertEquals("SELECT a\nFROM t", DocumentFormatter.format("sql", "select a from t", "  "))
        assertEquals("{\"a\":1}", DocumentFormatter.minify("json", "{ \"a\" : 1 }"))
        assertEquals("<a><b/></a>", DocumentFormatter.minify("xml", "<a>\n <b/>\n</a>"))
    }

    @Test
    fun `unsupported languages are rejected`() {
        assertThrows(UnsupportedOperationException::class.java) { DocumentFormatter.format("yaml", "a: 1", "  ") }
        assertThrows(UnsupportedOperationException::class.java) { DocumentFormatter.minify("sql", "select 1", ) }
    }
}
