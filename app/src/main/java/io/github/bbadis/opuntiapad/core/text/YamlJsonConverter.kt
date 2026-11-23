package io.github.bbadis.opuntiapad.core.text

import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.exceptions.YamlEngineException

/**
 * JSON ↔ YAML conversion (SnakeYAML Engine, YAML 1.2). The result is a new document: comments in
 * the source are not carried over, which is why the UI opens it in a new tab instead of replacing.
 */
object YamlJsonConverter {

    class ConversionException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

    private val loadSettings = LoadSettings.builder()
        .setAllowDuplicateKeys(false)
        .setCodePointLimit(64 * 1024 * 1024)
        .setMaxAliasesForCollections(200) // protects against "billion laughs" documents
        .build()

    /** Converts YAML to pretty-printed JSON. Several YAML documents become a JSON array. */
    fun yamlToJson(yaml: CharSequence, indent: String = "  "): String {
        val documents = try {
            Load(loadSettings).loadAllFromString(yaml.toString()).toList()
        } catch (e: YamlEngineException) {
            throw ConversionException(e.message?.lineSequence()?.firstOrNull() ?: "Invalid YAML", e)
        }
        val value: Any? = if (documents.size == 1) documents.first() else documents
        return StringBuilder().also { writeJson(it, value, indent, 0) }.append('\n').toString()
    }

    /** Converts JSON to block-style YAML. */
    fun jsonToYaml(json: CharSequence, indent: Int = 2): String {
        // Validate as strict JSON first (YAML would accept many things JSON doesn't).
        try {
            JsonFormatter.minify(json)
        } catch (e: JsonFormatter.JsonSyntaxException) {
            throw ConversionException(e.message ?: "Invalid JSON", e)
        }
        val value = try {
            Load(loadSettings).loadFromString(json.toString())
        } catch (e: YamlEngineException) {
            throw ConversionException(e.message?.lineSequence()?.firstOrNull() ?: "Invalid JSON", e)
        }
        val settings = DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(indent)
            .setIndicatorIndent(indent)
            .setIndentWithIndicator(true)
            .setWidth(Int.MAX_VALUE)
            .build()
        return Dump(settings).dumpToString(value)
    }

    private fun writeJson(out: StringBuilder, value: Any?, indent: String, depth: Int) {
        fun pad(d: Int) = repeat(d) { out.append(indent) }
        when (value) {
            null -> out.append("null")
            is String -> writeString(out, value)
            is Boolean, is Int, is Long, is java.math.BigInteger -> out.append(value.toString())
            is Double -> out.append(if (value.isFinite()) jsonNumber(value) else "null")
            is Float -> out.append(if (value.isFinite()) jsonNumber(value.toDouble()) else "null")
            is Number -> out.append(value.toString())
            is Map<*, *> -> {
                if (value.isEmpty()) { out.append("{}"); return }
                out.append("{\n")
                value.entries.forEachIndexed { index, (k, v) ->
                    pad(depth + 1)
                    writeString(out, k.toString())
                    out.append(": ")
                    writeJson(out, v, indent, depth + 1)
                    if (index < value.size - 1) out.append(',')
                    out.append('\n')
                }
                pad(depth)
                out.append('}')
            }
            is Iterable<*> -> {
                val items = value.toList()
                if (items.isEmpty()) { out.append("[]"); return }
                out.append("[\n")
                items.forEachIndexed { index, v ->
                    pad(depth + 1)
                    writeJson(out, v, indent, depth + 1)
                    if (index < items.size - 1) out.append(',')
                    out.append('\n')
                }
                pad(depth)
                out.append(']')
            }
            is ByteArray -> writeString(out, java.util.Base64.getEncoder().encodeToString(value))
            else -> writeString(out, value.toString()) // dates, timestamps…
        }
    }

    private fun jsonNumber(d: Double): String =
        if (d == Math.rint(d) && kotlin.math.abs(d) < 1e15) d.toLong().toString() + ".0" else d.toString()

    private fun writeString(out: StringBuilder, s: String) {
        out.append('"')
        for (c in s) {
            when (c) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> if (c < ' ') out.append(String.format("\\u%04x", c.code)) else out.append(c)
            }
        }
        out.append('"')
    }
}
