/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.okio.internal

import kotlinx.serialization.json.internal.*
import okio.*

private const val QUOTE_CODE = '"'.code

internal class JsonToOkioStreamWriter(private val sink: BufferedSink) : InternalJsonWriter {
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
        sink.writeUtf8CodePoint(QUOTE_CODE)
        InternalJsonWriter.doWriteEscaping(text) { s, start, end -> sink.writeUtf8(s, start, end) }
        sink.writeUtf8CodePoint(QUOTE_CODE)
    }

    override fun release() {
        // no-op, see https://github.com/Kotlin/kotlinx.serialization/pull/1982#discussion_r915043700
    }
}

internal class OkioSerialReader(private val source: BufferedSource): InternalJsonReaderCodePointImpl() {
    override fun exhausted(): Boolean = source.exhausted()
    override fun nextCodePoint(): Int = source.readUtf8CodePoint()
}

