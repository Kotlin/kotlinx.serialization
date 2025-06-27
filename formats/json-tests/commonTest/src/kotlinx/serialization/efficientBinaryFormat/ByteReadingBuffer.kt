/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.efficientBinaryFormat

class ByteReadingBuffer(val buffer: ByteArray) {
    private var next = 0

    private fun nextByte(): Int {
        return buffer[next++].toInt() and 0xff
    }

    private fun nextByteL(): Long {
        return buffer[next++].toLong() and 0xffL
    }

    operator fun get(pos: Int): Byte {
        if(pos !in 0..<buffer.size) { throw IndexOutOfBoundsException("Position $pos out of range") }
        return buffer[pos]
    }

    fun readByte(): Byte {
        return buffer[next++]
    }

    fun readShort(): Short {
        return (nextByte() or (nextByte() shl 8)).toShort()
    }

    fun readInt(): Int {
        return nextByte() or
            (nextByte() shl 8) or
            (nextByte() shl 16) or
            (nextByte() shl 24)
    }

    fun readLong(): Long {
        return nextByteL() or
            (nextByteL() shl 8) or
            (nextByteL() shl 16) or
            (nextByteL() shl 24) or
            (nextByteL() shl 32) or
            (nextByteL() shl 40) or
            (nextByteL() shl 48) or
            (nextByteL() shl 56)
    }

    fun readFloat(): Float {
        return Float.fromBits(readInt())
    }

    fun readDouble(): Double {
        val l = readLong()
        return Double.fromBits(l)
    }

    fun readChar(): Char {
        return (nextByte() or (nextByte() shl 8)).toChar()
    }

    fun readString(): String {
        val len = readInt()
        val chars = CharArray(len) { readChar() }
        return chars.concatToString()
    }

    fun readString(consumeChunk: (String) -> Unit) {
        val len = readInt()
        var remaining = len
        while (remaining > 1024) {
            remaining -= 1024
            val chunk = CharArray(1024) { readChar() }
            consumeChunk(chunk.concatToString())
        }
        val chars = CharArray(remaining) { readChar() }
        consumeChunk(chars.concatToString())
    }

}