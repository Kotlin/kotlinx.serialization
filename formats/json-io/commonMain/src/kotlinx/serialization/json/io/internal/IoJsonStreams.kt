/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.io.internal

import kotlinx.io.*
import kotlinx.io.unsafe.*
import kotlinx.serialization.json.internal.*

private const val QUOTE_CODE = '"'.code

internal class JsonToIoStreamWriter(private val sink: Sink) : InternalJsonWriter {

    override fun writeLong(value: Long) {
        sink.writeDecimalLong(value)
    }

    override fun writeChar(char: Char) {
        sink.writeCodePointValue(char.code)
    }

    override fun write(text: String) {
        sink.writeString(text)
    }

    override fun writeQuoted(text: String) {
        sink.writeCodePointValue(QUOTE_CODE)
        InternalJsonWriter.doWriteEscaping(text) { s, start, end -> sink.writeString(s, start, end) }
        sink.writeCodePointValue(QUOTE_CODE)
    }

    override fun release() {
        // no-op, see https://github.com/Kotlin/kotlinx.serialization/pull/1982#discussion_r915043700
    }
}

// Max value for a code point placed in one Char
private const val SINGLE_CHAR_MAX_CODEPOINT = Char.MAX_VALUE.code
// Value added to the high UTF-16 surrogate after shifting
private const val HIGH_SURROGATE_HEADER = 0xd800 - (0x010000 ushr 10)
// Value added to the low UTF-16 surrogate after masking
private const val LOW_SURROGATE_HEADER = 0xdc00

@OptIn(UnsafeIoApi::class, InternalIoApi::class)
internal class IoSerialReader(private val source: Source) : InternalJsonReader {
    // When the last (count'th) byte is a high surrogate, we save it here and merge with the low one on the next read()
    private var bufferedChar: Char? = null

    final override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        var written = 0
        bufferedChar?.let {
            buffer[bufferOffset] = it
            bufferedChar = null
            written++
        }

        while (written < count) {
            val remaining = count - written
            // Nothing left, bail out
            if (!source.request(1)) break

            var asciiRead = 0
            UnsafeBufferOperations.readFromHead(source.buffer) { data, start, end ->
                val toRead = minOf(end - start, remaining)
                while (asciiRead < toRead) {
                    val byte = data[start + asciiRead]
                    if (byte < 0) break // Non-ascii found, bail out
                    buffer[bufferOffset + written + asciiRead] = byte.toInt().toChar()
                    asciiRead++
                }
                asciiRead
            }

            if (asciiRead != 0) {
                written += asciiRead
                continue
            }

            val codePoint = source.readCodePointValue()
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
