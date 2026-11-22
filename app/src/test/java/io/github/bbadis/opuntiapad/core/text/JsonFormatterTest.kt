package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JsonFormatterTest {

    @Test
    fun `pretty prints nested structures`() {
        val input = """{"name":"OpuntiaPad","tags":["yaml","json"],"nested":{"a":1,"b":[]},"empty":{}}"""
        val expected = """
            {
              "name": "OpuntiaPad",
              "tags": [
                "yaml",
                "json"
              ],
              "nested": {
                "a": 1,
                "b": []
              },
              "empty": {}
            }
        """.trimIndent()
        assertEquals(expected, JsonFormatter.format(input))
    }

    @Test
    fun `minifies and keeps string content intact`() {
        val input = "{\n  \"text\": \"a , b : { c } \\\" d\",\n  \"n\": -1.5e3\n}"
        assertEquals("{\"text\":\"a , b : { c } \\\" d\",\"n\":-1.5e3}", JsonFormatter.minify(input))
    }

    @Test
    fun `round trip is stable`() {
        val input = """[{"a":true,"b":null},{"c":[1,2,[3]]}]"""
        assertEquals(input, JsonFormatter.minify(JsonFormatter.format(input, "\t")))
    }

    @Test
    fun `rejects malformed json`() {
        assertThrows(JsonFormatter.JsonSyntaxException::class.java) { JsonFormatter.format("{\"a\": [1, 2}") }
        assertThrows(JsonFormatter.JsonSyntaxException::class.java) { JsonFormatter.format("{\"a\": \"unterminated}") }
        assertThrows(JsonFormatter.JsonSyntaxException::class.java) { JsonFormatter.format("{\"a\": tru}") }
        assertThrows(JsonFormatter.JsonSyntaxException::class.java) { JsonFormatter.format("{\"a\": 1") }
    }
}
