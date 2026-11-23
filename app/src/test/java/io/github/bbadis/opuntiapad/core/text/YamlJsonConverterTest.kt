package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class YamlJsonConverterTest {

    @Test
    fun `yaml to json keeps types and order`() {
        val yaml = "name: demo\nversion: 3\nratio: 0.5\nactive: true\nnothing: null\nitems:\n  - a\n  - b: 1\n"
        val expected = """
            {
              "name": "demo",
              "version": 3,
              "ratio": 0.5,
              "active": true,
              "nothing": null,
              "items": [
                "a",
                {
                  "b": 1
                }
              ]
            }
        """.trimIndent() + "\n"
        assertEquals(expected, YamlJsonConverter.yamlToJson(yaml))
    }

    @Test
    fun `json to yaml produces block style`() {
        val yaml = YamlJsonConverter.jsonToYaml("""{"name":"demo","list":[1,2],"nested":{"ok":true},"quote":"a: b"}""")
        assertEquals("name: demo\nlist:\n  - 1\n  - 2\nnested:\n  ok: true\nquote: 'a: b'\n", yaml)
    }

    @Test
    fun `round trip json - yaml - json`() {
        val json = "{\n  \"a\": [\n    1,\n    \"x\"\n  ],\n  \"é\": \"ü ✓\"\n}\n"
        assertEquals(json, YamlJsonConverter.yamlToJson(YamlJsonConverter.jsonToYaml(json)))
    }

    @Test
    fun `several yaml documents become an array`() {
        assertTrue(YamlJsonConverter.yamlToJson("a: 1\n---\nb: 2\n").startsWith("[\n"))
    }

    @Test
    fun `invalid input reports an error`() {
        assertThrows(YamlJsonConverter.ConversionException::class.java) { YamlJsonConverter.yamlToJson("a: [1, 2\n") }
        assertThrows(YamlJsonConverter.ConversionException::class.java) { YamlJsonConverter.jsonToYaml("{a: 1}") }
        assertThrows(YamlJsonConverter.ConversionException::class.java) { YamlJsonConverter.yamlToJson("a: 1\na: 2\n") }
    }
}
