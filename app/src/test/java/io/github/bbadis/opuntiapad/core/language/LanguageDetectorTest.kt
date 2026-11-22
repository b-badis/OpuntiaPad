package io.github.bbadis.opuntiapad.core.language

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {

    private val detector = LanguageDetector(
        listOf(
            Language("json", "JSON", "source.json", listOf("json")),
            Language("yaml", "YAML", "source.yaml", listOf("yaml", "yml")),
            Language("sql", "SQL", "source.sql", listOf("sql")),
            Language("xml", "XML", "text.xml", listOf("xml")),
            Language("html", "HTML", "text.html.basic", listOf("html")),
            Language("python", "Python", "source.python", listOf("py")),
            Language("shellscript", "Shell", "source.shell", listOf("sh"), listOf(".bashrc")),
            Language("docker", "Dockerfile", "source.dockerfile", listOf("dockerfile"), listOf("Dockerfile")),
            Language("diff", "Diff", "source.diff", listOf("diff", "patch")),
            Language("ini", "INI", "source.ini", listOf("ini", "properties")),
        ),
    )

    private fun detect(name: String?, mime: String? = null, sample: String? = null) = detector.detect(name, mime, sample).id

    @Test
    fun `by extension, case insensitive`() {
        assertEquals("yaml", detect("docker-compose.YML"))
        assertEquals("json", detect("package.json"))
        assertEquals("sql", detect("schema.sql"))
        assertEquals("ini", detect("gradle.properties"))
    }

    @Test
    fun `by exact file name and prefixes`() {
        assertEquals("docker", detect("Dockerfile"))
        assertEquals("docker", detect("Dockerfile.dev"))
        assertEquals("shellscript", detect(".bashrc"))
    }

    @Test
    fun `plain text files`() {
        assertEquals(Language.PLAIN_TEXT_ID, detect("notes.txt"))
        assertEquals(Language.PLAIN_TEXT_ID, detect("unknown.zzz", sample = "hello world"))
    }

    @Test
    fun `by mime type when the name is not helpful`() {
        assertEquals("json", detect("document", "application/json"))
        assertEquals("yaml", detect(null, "application/x-yaml"))
        assertEquals("json", detect(null, "application/vnd.api+json"))
    }

    @Test
    fun `by content`() {
        assertEquals("json", detect("download", sample = "{\n  \"a\": 1\n}"))
        assertEquals("json", detect("download", sample = "[1, 2, 3]"))
        assertEquals("yaml", detect("download", sample = "---\nkey: value"))
        assertEquals("yaml", detect("download", sample = "name: opuntiapad\nversion: 1\nitems:\n  - a"))
        assertEquals("sql", detect("query", sample = "-- report\nSELECT id, name FROM users WHERE id = 1;"))
        assertEquals("xml", detect("feed", sample = "<?xml version=\"1.0\"?><a/>"))
        assertEquals("html", detect("page", sample = "<!DOCTYPE html><html></html>"))
        assertEquals("python", detect("script", sample = "#!/usr/bin/env python3\nprint('hi')"))
        assertEquals("shellscript", detect("run", sample = "#!/bin/sh\necho hi"))
        assertEquals("diff", detect("change", sample = "diff --git a/x b/x"))
    }

    @Test
    fun `extension wins over content`() {
        assertEquals("yaml", detect("config.yaml", sample = "{\"a\": 1}"))
    }
}
