/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.okio.internal

import kotlinx.serialization.json.internal.*
import okio.*

private const val QUOTE_CODE = '"'.code

internal class JsonToOkioStreamWriter(private val sink: BufferedSink) : InternalJsonWriter {
    override fun writeLong(value: Long) {
        sink.writeDecimalLong(value)
    }

    override fun writeChar(char: Char) {
        sink.writeUtf8CodePoint(char.code)
    }

    override fun write(text: String) {
        sink.writeUtf8(text)
    }

    override fun writeQuoted(text: String) {
        sink.writeUtf8CodePoint(QUOTE_CODE)
        InternalJsonWriter.doWriteEscaping(text) { s, start, end -> sink.writeUtf8(s, start, end) }
        sink.writeUtf8CodePoint(QUOTE_CODE)
    }

    override fun release() {
        // no-op, see https://github.com/Kotlin/kotlinx.serialization/pull/1982#discussion_r915043700
    }
}

// Value added to the high UTF-16 surrogate after shifting
private const val HIGH_SURROGATE_HEADER = 0xd800 - (0x010000 ushr 10)
// Value added to the low UTF-16 surrogate after masking
private const val LOW_SURROGATE_HEADER = 0xdc00

internal class OkioReader(private val source: BufferedSource) : InternalJsonReader {
    private val cursor = Buffer.UnsafeCursor()
    // When the last (count'th) byte is a high surrogate, we save it here and merge with the low one on the next read()
    // \u0000 is a placeholder for "no high surrogate stored"
    private var bufferedChar: Char = '\u0000'

    final override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        var written = 0
         if (bufferedChar != '\u0000') {
            buffer[bufferOffset] = bufferedChar
            bufferedChar = '\u0000'
            written++
        }

        while (written < count) {
            val remaining = count - written
            // Nothing left, bail out
            if (source.exhausted()) break

            var asciiRead = 0
            source.buffer.readUnsafe(cursor)
            val readableByteCount = cursor.next()
            val data = cursor.data!!
            val start = cursor.start
            val toRead = minOf(readableByteCount, remaining)
            while (asciiRead < toRead) {
                val byte = data[start + asciiRead]
                if (byte < 0) break // Non-ascii found, bail out
                buffer[bufferOffset + written + asciiRead] = byte.toInt().toChar()
                asciiRead++
            }
            cursor.close()

            if (asciiRead != 0) {
                source.skip(asciiRead.toLong())
                written += asciiRead
                continue
            }

            val codePoint = source.readUtf8CodePoint()
            if (codePoint <= Char.MAX_VALUE.code) {
                buffer[bufferOffset + written++] = codePoint.toChar()
            } else {
                buffer[bufferOffset + written++] = ((codePoint ushr 10) + HIGH_SURROGATE_HEADER).toChar()
                val lowSurrogate = ((codePoint and 0x03ff) + LOW_SURROGATE_HEADER).toChar()
                if (written < count) {
                    buffer[bufferOffset + written++] = lowSurrogate
                } else {
                    bufferedChar = lowSurrogate
                }
            }
        }

        return if (written == 0) -1 else written
    }
}
