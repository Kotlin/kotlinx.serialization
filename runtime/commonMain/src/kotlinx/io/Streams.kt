/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io

import kotlinx.serialization.*

@InternalSerializationApi
public abstract class Input {
    public abstract val availableBytes: Int
    public abstract fun read(): Int
    public abstract fun read(b: ByteArray, offset: Int, length: Int): Int
    public abstract fun readString(length: Int): String
    public abstract fun readVarint32(): Int
    public abstract fun readVarint64(eofAllowed: Boolean): Long
}

@InternalSerializationApi
public class ByteArrayInput(private var array: ByteArray) : Input() {

    private var position: Int = 0
    public override val availableBytes: Int get() = array.size - position

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

        // Fast-path: unrolled loop for single and two byte values
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
public abstract class Output {
    public fun write(buffer: ByteArray): Unit = write(buffer, 0, buffer.size)
    public abstract fun write(buffer: ByteArray, offset: Int, count: Int)
    public abstract fun write(byteValue: Int)
    public abstract fun writeInt(intValue: Int)
    public abstract fun writeLong(longValue: Long)
    public abstract fun encodeVarint32(value: Int)
    public abstract fun encodeVarint64(value: Long)
}

@InternalSerializationApi
public class ByteArrayOutput() : Output() {
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

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        // avoid int overflow
        if (offset < 0 || offset > buffer.size || count < 0
            || count > buffer.size - offset
        ) {
            throw IndexOutOfBoundsException()
        }
        if (count == 0) {
            return
        }

        ensureCapacity(count)
        buffer.copyInto(
            destination = array,
            destinationOffset = this.position,
            startIndex = offset,
            endIndex = offset + count
        )
        this.position += count
    }

    override fun write(byteValue: Int) {
        ensureCapacity(1)
        array[position++] = byteValue.toByte()
    }

    override fun writeInt(intValue: Int) {
        ensureCapacity(4)
        for (i in 3 downTo 0) {
            array[position++] = (intValue shr i * 8).toByte()
        }
    }

    override fun writeLong(longValue: Long) {
        ensureCapacity(8)
        for (i in 7 downTo 0) {
            array[position++] = (longValue shr i * 8).toByte()
        }
    }

    override fun encodeVarint32(value: Int) {
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

    override fun encodeVarint64(value: Long) {
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
