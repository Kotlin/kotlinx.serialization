/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.efficientBinaryFormat

import kotlin.experimental.and

class ByteWritingBuffer() {
    private var buffer = ByteArray(8192)
    private var next = 0
    val size
        get() = next

    operator fun get(pos: Int): Byte {
        if(pos !in 0..<size) { throw IndexOutOfBoundsException("Position $pos out of range") }
        return buffer[pos]
    }

    private fun growIfNeeded(additionalNeeded: Int = 1) {
        val minNew = size + additionalNeeded
        if (minNew < buffer.size) return

        var newSize = buffer.size shl 1
        while (newSize < minNew) { newSize = newSize shl 1}

        buffer = buffer.copyOf(newSize)
    }

    fun toByteArray(): ByteArray {
        return buffer.copyOf(size)
    }

    fun writeByte(b: Byte) {
        growIfNeeded(1)
        buffer[next++] = b
    }

    fun writeShort(s: Short) {
        growIfNeeded(2)
        buffer[next++] = (s and 0xff).toByte()
        buffer[next++] = ((s.toInt() shr 8) and 0xff).toByte()
    }

    fun writeInt(i: Int) {
        growIfNeeded(4)
        buffer[next++] = (i and 0xff).toByte()
        buffer[next++] = ((i shr 8) and 0xff).toByte()
        buffer[next++] = ((i shr 16) and 0xff).toByte()
        buffer[next++] = ((i shr 24) and 0xff).toByte()
    }

    fun writeLong(l: Long) {
        growIfNeeded(4)
        buffer[next++] = (l and 0xff).toByte()
        buffer[next++] = ((l shr 8) and 0xff).toByte()
        buffer[next++] = ((l shr 16) and 0xff).toByte()
        buffer[next++] = ((l shr 24) and 0xff).toByte()
        buffer[next++] = ((l shr 32) and 0xff).toByte()
        buffer[next++] = ((l shr 40) and 0xff).toByte()
        buffer[next++] = ((l shr 48) and 0xff).toByte()
        buffer[next++] = ((l shr 56) and 0xff).toByte()
    }

    fun writeFloat(f: Float) {
        writeInt(f.toBits())
    }

    fun writeDouble(d: Double) {
        writeLong(d.toBits())
    }

    fun writeChar(c: Char) {
        growIfNeeded(2)
        buffer[next++] = (c.code and 0xff).toByte()
        buffer[next++] = ((c.code shr 8) and 0xff).toByte()
    }

    fun writeString(s: String) {
        growIfNeeded(s.length * 2+4)
        writeInt(s.length)
        for (c in s) {
            buffer[next++] = (c.code and 0xff).toByte()
            buffer[next++] = ((c.code shr 8) and 0xff).toByte()
        }
    }

}

