/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io

import kotlinx.serialization.*

@Deprecated(message = message, level = DeprecationLevel.ERROR)
expect open class IOException : Exception {
    constructor()
    constructor(message: String)
}

@InternalSerializationApi
abstract class InputStream {
    abstract fun read(): Int
    abstract fun read(b: ByteArray, offset: Int, length: Int): Int
    abstract fun readString(length: Int): String
    abstract fun readVarint32(): Int
    abstract fun readVarint64(eofAllowed: Boolean): Long
}

@InternalSerializationApi
class ByteArrayInputStream(private var array: ByteArray) : InputStream() {

    private var position: Int = 0

    override fun read(): Int {
        return if (position < array.size) array[position++].toInt() and 0xFF else -1
    }

    override fun read(b: ByteArray, offset: Int, length: Int): Int {
        // avoid int overflow
        if (offset < 0 || offset > b.size || length < 0
            || length > b.size - offset
        ) {
            throw IndexOutOfBoundsException()
        }
        // Are there any bytes available?
        if (this.position >= array.size) {
            return -1
        }
        if (length == 0) {
            return 0
        }

        val copied = if (this.array.size - position < length) this.array.size - position else length
        array.copyInto(destination = b, destinationOffset = offset, startIndex = position, endIndex = position + copied)
        position += copied
        return copied
    }

    override fun readString(length: Int): String {
        val result = array.decodeToString(position, position + length)
        position += length
        return result
    }

    override fun readVarint32(): Int {
        if (position == array.size) {
            error("Unexpected EOF")
        }

        // Fast-path: single and two byte values
        var currentPosition = position
        var result = array[currentPosition++].toInt()
        if (result >= 0) {
            position  = currentPosition
            return result
        } else if (array.size - position > 1) {
            result = result xor (array[currentPosition++].toInt() shl 7)
            if (result < 0) {
                position = currentPosition
                return result xor (0.inv() shl 7)
            }
        }

        return readVarint32SlowPath()
    }

    override fun readVarint64(eofAllowed: Boolean): Long {
        if (position == array.size) {
            if (eofAllowed) return -1
            else error("Unexpected EOF")
        }

        // Fast-path: single and two byte values
        var currentPosition = position
        var result = array[currentPosition++].toLong()
        if (result >= 0) {
            position  = currentPosition
            return result
        } else if (array.size - position > 1) {
            result = result xor (array[currentPosition++].toLong() shl 7)
            if (result < 0) {
                position = currentPosition
                return result xor (0L.inv() shl 7)
            }
        }

        return readVarint64SlowPath()
    }

    private fun readVarint64SlowPath(): Long {
        var result = 0L
        var shift = 0
        while (shift != 64) {
            val byte = read()
            result = result or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) {
                return result
            }
            shift += 7
        }
        throw SerializationException("Varint too long: exceeded 64 bits")
    }

    private fun readVarint32SlowPath(): Int {
        var result = 0
        var shift = 0
        while (shift != 32) {
            val byte = read()
            result = result or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) {
                return result
            }
            shift += 7
        }
        throw SerializationException("Varint too long: exceeded 32 bits")
    }
}

@InternalSerializationApi
expect abstract class OutputStream {
    open fun close()
    open fun flush()
    open fun write(buffer: ByteArray, offset: Int, count: Int)
    open fun write(buffer: ByteArray)
    abstract fun write(oneByte: Int)

}

@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // KT-17944
@InternalSerializationApi
expect class ByteArrayOutputStream() : OutputStream {
    override fun write(oneByte: Int)
    fun toByteArray(): ByteArray
    fun size(): Int
}
