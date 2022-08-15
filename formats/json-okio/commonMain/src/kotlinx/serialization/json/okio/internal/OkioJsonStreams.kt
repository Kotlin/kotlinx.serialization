/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package kotlinx.serialization.json.okio.internal

import kotlinx.serialization.json.internal.ESCAPE_STRINGS
import kotlinx.serialization.json.internal.JsonWriter
import kotlinx.serialization.json.internal.SerialReader
import okio.*

internal class JsonToOkioStreamWriter(private val target: BufferedSink) : JsonWriter {
    override fun writeLong(value: Long) {
        write(value.toString())
    }

    override fun writeChar(char: Char) {
        target.writeUtf8CodePoint(char.code)
    }

    override fun write(text: String) {
        target.writeUtf8(text)
    }

    override fun writeQuoted(text: String) {
        target.writeUtf8CodePoint('"'.code)
        var lastPos = 0
        for (i in text.indices) {
            val c = text[i].code
            if (c < ESCAPE_STRINGS.size && ESCAPE_STRINGS[c] != null) {
                target.writeUtf8(text, lastPos, i) // flush prev
                target.writeUtf8(ESCAPE_STRINGS[c]!!)
                lastPos = i + 1
            }
        }

        if (lastPos != 0) target.writeUtf8(text, lastPos, text.length)
        else target.writeUtf8(text)
        target.writeUtf8CodePoint('"'.code)
    }

    override fun release() {
        // no-op, see https://github.com/Kotlin/kotlinx.serialization/pull/1982#discussion_r915043700
    }
}

internal class OkioSerialReader(private val source: BufferedSource): SerialReader {
    override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        var i = 0
        while (i < count && !source.exhausted()) {
            buffer[bufferOffset + i] = source.readUtf8CodePoint().toChar()
            i++
        }
        return if (i > 0) i else -1
    }
}

