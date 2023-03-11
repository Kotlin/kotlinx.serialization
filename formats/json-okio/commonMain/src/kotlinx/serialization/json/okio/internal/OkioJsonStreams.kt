/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package kotlinx.serialization.json.okio.internal

import kotlinx.serialization.json.internal.ESCAPE_STRINGS
import kotlinx.serialization.json.internal.JsonWriter
import kotlinx.serialization.json.internal.SerialReader
import okio.*

internal class JsonToOkioStreamWriter(private val sink: BufferedSink) : JsonWriter {
    override fun writeLong(value: Long) {
        write(value.toString())
    }

    override fun writeChar(char: Char) {
        sink.writeUtf8CodePoint(char.code)
    }

    override fun write(text: String) {
        sink.writeUtf8(text)
    }

    override fun writeQuoted(text: String) {
        sink.writeUtf8CodePoint('"'.code)
        var lastPos = 0
        for (i in text.indices) {
            val c = text[i].code
            if (c < ESCAPE_STRINGS.size && ESCAPE_STRINGS[c] != null) {
                sink.writeUtf8(text, lastPos, i) // flush prev
                sink.writeUtf8(ESCAPE_STRINGS[c]!!)
                lastPos = i + 1
            }
        }

        if (lastPos != 0) sink.writeUtf8(text, lastPos, text.length)
        else sink.writeUtf8(text)
        sink.writeUtf8CodePoint('"'.code)
    }

    override fun release() {
        // no-op, see https://github.com/Kotlin/kotlinx.serialization/pull/1982#discussion_r915043700
    }
}

// Max value for a code  point placed in one Char
private const val SINGLE_CHAR_MAX_CODEPOINT = Char.MAX_VALUE.code
// Value added to the high UTF-16 surrogate after shifting
private const val HIGH_SURROGATE_HEADER = 0xd800 - (0x010000 ushr 10)
// Value added to the low UTF-16 surrogate after masking
private const val LOW_SURROGATE_HEADER = 0xdc00


internal class OkioSerialReader(private val source: BufferedSource): SerialReader {
    /*
    A sequence of code points is read from UTF-8, some of it can take 2 characters.
    In case the last code point requires 2 characters, and the array is already full, we buffer the second character
     */
    private var bufferedChar: Char? = null

    override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        var i = 0

        if (bufferedChar != null) {
            buffer[bufferOffset + i] = bufferedChar!!
            i++
            bufferedChar = null
        }

        while (i < count && !source.exhausted()) {
            val codePoint = source.readUtf8CodePoint()
            if (codePoint <= SINGLE_CHAR_MAX_CODEPOINT) {
                buffer[bufferOffset + i] = codePoint.toChar()
                i++
            } else {
                // an example of working with surrogates is taken from okio library with minor changes, see https://github.com/square/okio
                // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                val upChar = ((codePoint ushr 10) + HIGH_SURROGATE_HEADER).toChar()
                val lowChar = ((codePoint and 0x03ff) + LOW_SURROGATE_HEADER).toChar()

                buffer[bufferOffset + i] = upChar
                i++

                if (i < count) {
                    buffer[bufferOffset + i] = lowChar
                    i++
                } else {
                        // if char array is full - buffer lower surrogate
                    bufferedChar = lowChar
                }
            }
        }
        return if (i > 0) i else -1
    }
}

