package io.github.bbadis.opuntiapad.core.language

import java.util.Locale

/**
 * Guesses the language of a document from (in order of confidence):
 * exact file name → extension → MIME type → shebang / content sniffing.
 */
class LanguageDetector(languages: List<Language>) {

    private val all: List<Language> = listOf(Language.PLAIN_TEXT) + languages.filterNot { it.id == Language.PLAIN_TEXT_ID }
    private val byId: Map<String, Language> = all.associateBy { it.id }
    private val byFileName: Map<String, Language> =
        all.flatMap { lang -> lang.fileNames.map { it.lowercase(Locale.ROOT) to lang } }.toMap()
    private val byExtension: Map<String, Language> =
        all.flatMap { lang -> lang.extensions.map { it.lowercase(Locale.ROOT) to lang } }.toMap()

    val languages: List<Language> get() = all

    fun byId(id: String?): Language? = id?.let(byId::get)

    fun detect(fileName: String?, mimeType: String? = null, sample: CharSequence? = null): Language {
        fileName?.let { name ->
            fromFileName(name)?.let { return it }
        }
        mimeType?.let { fromMime(it) }?.let { return it }
        sample?.let { fromContent(it) }?.let { return it }
        return Language.PLAIN_TEXT
    }

    fun fromFileName(fileName: String): Language? {
        val name = fileName.substringAfterLast('/').trim()
        val lower = name.lowercase(Locale.ROOT)
        byFileName[lower]?.let { return it }
        // Compound extensions such as "docker-compose.override.yml" or "file.blade.php": try longest first.
        var dot = lower.indexOf('.', startIndex = 1)
        while (dot >= 0 && dot < lower.length - 1) {
            byExtension[lower.substring(dot + 1)]?.let { return it }
            dot = lower.indexOf('.', dot + 1)
        }
        // Dotfiles like ".bashrc" are matched by file name above; ".foo" alone → extension "foo".
        if (lower.startsWith('.') && lower.count { it == '.' } == 1) byExtension[lower.substring(1)]?.let { return it }
        // "Dockerfile.dev", "Makefile.am"…
        val base = lower.substringBefore('.')
        if (base.isNotEmpty()) byFileName[base]?.let { return it }
        return null
    }

    fun fromMime(mimeType: String): Language? {
        val mime = mimeType.lowercase(Locale.ROOT).substringBefore(';').trim()
        val id = MIME_TO_ID[mime] ?: when {
            mime.endsWith("+json") -> "json"
            mime.endsWith("+xml") -> "xml"
            mime.endsWith("+yaml") -> "yaml"
            else -> null
        }
        return id?.let(byId::get)
    }

    fun fromContent(sample: CharSequence): Language? {
        val head = sample.subSequence(0, minOf(sample.length, 4096)).toString()
        val trimmed = head.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("#!")) {
            val shebang = trimmed.lineSequence().first()
            SHEBANGS.firstOrNull { (key, _) -> shebang.contains(key) }?.let { return byId[it.second] }
        }
        if (trimmed.startsWith("<?xml")) return byId["xml"]
        if (trimmed.startsWith("<?php")) return byId["php"]
        if (trimmed.startsWith("<!doctype html", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) return byId["html"]
        if ((trimmed.startsWith("{") || trimmed.startsWith("[")) && JSON_START.containsMatchIn(trimmed)) return byId["json"]
        if (trimmed.startsWith("---")) return byId["yaml"]
        val firstLines = trimmed.lineSequence().filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.take(5).toList()
        if (firstLines.size >= 2 && firstLines.count { YAML_LINE.matches(it) } >= 2 && !trimmed.contains(';')) return byId["yaml"]
        if (SQL.containsMatchIn(trimmed)) return byId["sql"]
        if (trimmed.startsWith("diff --git") || trimmed.startsWith("--- a/")) return byId["diff"]
        return null
    }

    private companion object {
        val JSON_START = Regex("^[\\[{]\\s*(\"|[\\[{\\d-]|true|false|null|[}\\]]|$)")
        val YAML_LINE = Regex("^\\s*(- )?[A-Za-z_\"'][\\w .\"'-]*:(\\s.*)?$")
        val SQL = Regex(
            "^\\s*(SELECT\\s.+\\sFROM\\s|INSERT\\s+INTO\\s|UPDATE\\s+\\w+\\s+SET\\s|DELETE\\s+FROM\\s|CREATE\\s+(OR\\s+REPLACE\\s+)?(TABLE|VIEW|INDEX|DATABASE|SCHEMA|PROCEDURE|FUNCTION|TRIGGER)\\s|ALTER\\s+TABLE\\s|DROP\\s+(TABLE|VIEW|INDEX)\\s|WITH\\s+\\w+\\s+AS\\s*\\()",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )
        val SHEBANGS = listOf(
            "python" to "python", "node" to "javascript", "deno" to "typescript", "bash" to "shellscript", "/sh" to "shellscript",
            "zsh" to "shellscript", "ruby" to "ruby", "perl" to "perl", "php" to "php", "lua" to "lua", "pwsh" to "powershell",
            "Rscript" to "r", "kotlin" to "kotlin", "groovy" to "groovy",
        )
        val MIME_TO_ID = mapOf(
            "application/json" to "json", "text/json" to "json", "application/x-ndjson" to "jsonl",
            "application/yaml" to "yaml", "application/x-yaml" to "yaml", "text/yaml" to "yaml", "text/x-yaml" to "yaml",
            "application/xml" to "xml", "text/xml" to "xml", "text/html" to "html", "application/xhtml+xml" to "html",
            "text/css" to "css", "text/markdown" to "markdown", "text/x-markdown" to "markdown",
            "application/sql" to "sql", "application/x-sql" to "sql", "text/x-sql" to "sql",
            "application/javascript" to "javascript", "text/javascript" to "javascript", "application/x-javascript" to "javascript",
            "application/typescript" to "typescript", "text/x-python" to "python", "application/x-python" to "python", "text/x-python-script" to "python",
            "text/x-java" to "java", "text/x-java-source" to "java", "text/x-kotlin" to "kotlin", "text/x-c" to "c", "text/x-csrc" to "c",
            "text/x-c++src" to "cpp", "text/x-chdr" to "c", "text/x-csharp" to "csharp", "text/x-go" to "go", "text/rust" to "rust",
            "application/x-sh" to "shellscript", "application/x-shellscript" to "shellscript", "text/x-sh" to "shellscript", "text/x-shellscript" to "shellscript",
            "application/x-php" to "php", "application/x-httpd-php" to "php", "text/x-php" to "php", "text/x-ruby" to "ruby", "application/x-ruby" to "ruby",
            "text/x-perl" to "perl", "application/x-perl" to "perl", "text/x-lua" to "lua", "application/toml" to "toml", "application/x-toml" to "toml",
            "text/x-properties" to "ini", "text/x-java-properties" to "ini", "application/x-wine-extension-ini" to "ini", "application/x-desktop" to "ini",
            "text/x-tex" to "latex", "application/x-tex" to "latex", "application/x-latex" to "latex", "text/x-diff" to "diff", "text/x-patch" to "diff",
            "text/csv" to "csv", "text/comma-separated-values" to "csv", "text/tab-separated-values" to "tsv", "text/x-log" to "log",
            "application/graphql" to "graphql", "text/x-dockerfile" to "docker", "text/x-makefile" to "make", "text/x-cmake" to "cmake",
        )
    }
}
