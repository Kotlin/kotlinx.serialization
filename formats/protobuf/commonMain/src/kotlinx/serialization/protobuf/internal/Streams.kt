/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*

internal class ByteArrayInput(private var array: ByteArray, private val endIndex: Int = array.size) {
    private var position: Int = 0
    public val availableBytes: Int get() = endIndex - position

    fun slice(size: Int): ByteArrayInput {
        ensureEnoughBytes(size)
        val result = ByteArrayInput(array, position + size)
        result.position = position
        position += size
        return result
    }

    fun read(): Int {
        return if (position < endIndex) array[position++].toInt() and 0xFF else -1
    }

    fun readExactNBytes(bytesCount: Int): ByteArray {
        ensureEnoughBytes(bytesCount)
        val b = ByteArray(bytesCount)
        val length = b.size
        // Are there any bytes available?
        val copied = if (endIndex - position < length) endIndex - position else length
        array.copyInto(destination = b, destinationOffset = 0, startIndex = position, endIndex = position + copied)
        position += copied
        return b
    }

    private fun ensureEnoughBytes(bytesCount: Int) {
        if (bytesCount > availableBytes) {
            throw SerializationException("Unexpected EOF, available $availableBytes bytes, requested: $bytesCount")
        }
    }

    fun readString(length: Int): String {
        val result = array.decodeToString(position, position + length)
        position += length
        return result
    }

    fun readVarint32(): Int {
        if (position == endIndex) {
            eof()
        }

        // Fast-path: unrolled loop for single and two byte values
        var currentPosition = position
        var result = array[currentPosition++].toInt()
        if (result >= 0) {
            position  = currentPosition
            return result
        } else if (endIndex - position > 1) {
            result = result xor (array[currentPosition++].toInt() shl 7)
            if (result < 0) {
                position = currentPosition
                return result xor (0.inv() shl 7)
            }
        }

        return readVarint32SlowPath()
    }

    fun readVarint64(eofAllowed: Boolean): Long {
        if (position == endIndex) {
            if (eofAllowed) return -1
            else eof()
        }

        // Fast-path: single and two byte values
        var currentPosition = position
        var result = array[currentPosition++].toLong()
        if (result >= 0) {
            position  = currentPosition
            return result
        } else if (endIndex - position > 1) {
            result = result xor (array[currentPosition++].toLong() shl 7)
            if (result < 0) {
                position = currentPosition
                return result xor (0L.inv() shl 7)
            }
        }

        return readVarint64SlowPath()
    }

    private fun eof() {
        throw SerializationException("Unexpected EOF")
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

internal class ByteArrayOutput {
    private var array: ByteArray = ByteArray(32)
    private var position: Int = 0

    private fun ensureCapacity(elementsToAppend: Int) {
        if (position + elementsToAppend <= array.size) {
            return
        }
        val newArray = ByteArray((position + elementsToAppend).takeHighestOneBit() shl 1)
        array.copyInto(newArray)
        array = newArray
    }

    public fun size(): Int {
        return position
    }

    public fun toByteArray(): ByteArray {
        val newArray = ByteArray(position)
        array.copyInto(newArray, startIndex = 0, endIndex = this.position)
        return newArray
    }

    fun write(buffer: ByteArray) {
        val count = buffer.size
        if (count == 0) {
            return
        }

        ensureCapacity(count)
        buffer.copyInto(
            destination = array,
            destinationOffset = this.position,
            startIndex = 0,
            endIndex = count
        )
        this.position += count
    }

    fun write(output: ByteArrayOutput) {
        val count = output.size()
        ensureCapacity(count)
        output.array.copyInto(
            destination = array,
            destinationOffset = this.position,
            startIndex = 0,
            endIndex = count
        )
        this.position += count
    }

    fun writeInt(intValue: Int) {
        ensureCapacity(4)
        for (i in 3 downTo 0) {
            array[position++] = (intValue shr i * 8).toByte()
        }
    }

    fun writeLong(longValue: Long) {
        ensureCapacity(8)
        for (i in 7 downTo 0) {
            array[position++] = (longValue shr i * 8).toByte()
        }
    }

    fun encodeVarint32(value: Int) {
        ensureCapacity(5)
        // Fast-path: unrolled loop for single byte
        if (value and 0x7F.inv() == 0) {
            array[position++] = value.toByte()
            return
        }
        // Fast-path: unrolled loop for two bytes
        var current = value
        array[position++] = (current or 0x80).toByte()
        current = current ushr 7
        if (value and 0x7F.inv() == 0) {
            array[position++] = value.toByte()
            return
        }
        encodeVarint32SlowPath(current)
    }

    private fun encodeVarint32SlowPath(value: Int) {
        var current = value
        while (current and 0x7F.inv() != 0) {
            array[position++] = ((current and 0x7F) or 0x80).toByte()
            current = current ushr 7
        }
        array[position++] = (current and 0x7F).toByte()
    }

    fun encodeVarint64(value: Long) {
        ensureCapacity(10)
        var currentValue = value
        while (true) {
            if (currentValue and 0x7F.inv() == 0L) {
                array[position++] = currentValue.toByte()
                return
            }
            array[position++] = (currentValue.toInt() and 0x7F or 0x80).toByte()
            currentValue = currentValue ushr 7
        }
    }
}
