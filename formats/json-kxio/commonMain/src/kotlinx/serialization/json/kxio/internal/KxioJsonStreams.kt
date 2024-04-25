/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.kxio.internal

import kotlinx.io.*
import kotlinx.serialization.json.internal.*

private const val QUOTE_CODE = '"'.code

internal class JsonToKxioStreamWriter(private val sink: Sink) : InternalJsonWriter {

    override fun writeLong(value: Long) {
        write(value.toString())
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

internal class KxioSerialReader(private val source: Source): InternalJsonReaderCodePointImpl() {
    override fun exhausted(): Boolean = source.exhausted()
    override fun nextCodePoint(): Int = source.readCodePointValue()
}
