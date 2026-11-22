package io.github.bbadis.opuntiapad.core.text

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Line separator style of a document. */
enum class LineEnding(val separator: String, val label: String) {
    LF("\n", "LF"),
    CRLF("\r\n", "CRLF"),
    CR("\r", "CR");

    companion object {
        /** Detects the dominant line ending; defaults to [LF] for single-line text. */
        fun detect(text: CharSequence): LineEnding {
            var lf = 0
            var crlf = 0
            var cr = 0
            var i = 0
            val n = text.length
            while (i < n) {
                when (text[i]) {
                    '\r' -> if (i + 1 < n && text[i + 1] == '\n') {
                        crlf++
                        i++
                    } else {
                        cr++
                    }
                    '\n' -> lf++
                }
                i++
            }
            return when {
                crlf >= lf && crlf >= cr && crlf > 0 -> CRLF
                cr > lf && cr > crlf -> CR
                else -> LF
            }
        }

        /** Rewrites every line break of [text] with [target]. */
        fun convert(text: CharSequence, target: LineEnding): String =
            text.toString().replace(LINE_BREAK, target.separator)

        private val LINE_BREAK = Regex("\r\n|\r|\n")
    }
}

/** Result of decoding raw bytes into text. */
data class DecodedText(
    val text: String,
    val charset: Charset,
    val hasBom: Boolean,
    val lineEnding: LineEnding,
    val looksBinary: Boolean,
)

/**
 * Detects encodings and converts between bytes and text.
 *
 * Strategy: byte order mark → strict UTF-8 → windows-1252 fallback (never fails).
 */
object TextCodec {
    val UTF_8: Charset = Charsets.UTF_8
    private val FALLBACK: Charset = runCatching { Charset.forName("windows-1252") }.getOrDefault(Charsets.ISO_8859_1)

    /** Encodings offered in the "Reopen with encoding" menu. */
    val COMMON_CHARSETS: List<String> = listOf(
        "UTF-8", "UTF-16LE", "UTF-16BE", "ISO-8859-1", "windows-1252", "ISO-8859-15", "windows-1256", "windows-1251", "Shift_JIS", "GB18030", "EUC-KR",
    )

    private val BOM_UTF8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val BOM_UTF16LE = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val BOM_UTF16BE = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

    fun decode(bytes: ByteArray, forced: Charset? = null): DecodedText {
        val (charset, bomLength) = when {
            bytes.startsWith(BOM_UTF8) -> UTF_8 to BOM_UTF8.size
            bytes.startsWith(BOM_UTF16LE) -> Charsets.UTF_16LE to BOM_UTF16LE.size
            bytes.startsWith(BOM_UTF16BE) -> Charsets.UTF_16BE to BOM_UTF16BE.size
            else -> null to 0
        }
        val looksBinary = charset == null && looksBinary(bytes)
        val body = ByteBuffer.wrap(bytes, bomLength, bytes.size - bomLength)

        val text: String
        val used: Charset
        when {
            forced != null -> {
                used = forced
                text = forced.decode(body).toString()
            }
            charset != null -> {
                used = charset
                text = charset.decode(body).toString()
            }
            else -> {
                val utf8 = decodeStrict(body.duplicate(), UTF_8)
                if (utf8 != null) {
                    used = UTF_8
                    text = utf8
                } else {
                    used = FALLBACK
                    text = FALLBACK.decode(body).toString()
                }
            }
        }
        return DecodedText(text, used, bomLength > 0 && (forced == null || forced == charset), LineEnding.detect(text), looksBinary)
    }

    fun encode(text: CharSequence, charset: Charset, withBom: Boolean): ByteArray {
        val body = text.toString().toByteArray(charset)
        if (!withBom) return body
        val bom = when (charset) {
            UTF_8 -> BOM_UTF8
            Charsets.UTF_16LE -> BOM_UTF16LE
            Charsets.UTF_16BE -> BOM_UTF16BE
            else -> return body
        }
        return bom + body
    }

    /** Heuristic: NUL bytes or many control characters in the first 8 KiB. */
    fun looksBinary(bytes: ByteArray): Boolean {
        val n = minOf(bytes.size, 8192)
        if (n == 0) return false
        var control = 0
        for (i in 0 until n) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0) return true
            if (b < 0x09 || (b in 0x0E..0x1F && b != 0x1B)) control++
        }
        return control * 10 > n
    }

    private fun decodeStrict(buffer: ByteBuffer, charset: Charset): String? = try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(buffer)
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
