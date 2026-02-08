/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor.io.internal

import kotlinx.io.*
import kotlinx.serialization.cbor.internal.*

internal class IoStreamOutput(private val sink: Sink) : Output {

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        sink.write(buffer, startIndex = offset, endIndex = offset + count)
    }

    override fun write(byteValue: Byte) {
        sink.writeByte(byteValue)
    }
}

internal class IoStreamInput(private val source: Source): Input {
    override val availableBytes: Int
        get() = source.peek().readByteArray().size

    override fun read(): Int =
        try {
            source.readByte().toInt()
        } catch (_: EOFException) {
            return -1
        }

    override fun read(b: ByteArray, offset: Int, length: Int): Int =
        source.readAtMostTo(b, startIndex = offset, endIndex = offset + length)

    override fun skip(length: Int) {
        source.skip(length.toLong())
    }
}
