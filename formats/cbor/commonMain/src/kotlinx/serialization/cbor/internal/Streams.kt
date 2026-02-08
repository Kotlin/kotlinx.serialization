/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor.internal

@CborFriendModuleApi
public interface Input {
    public val availableBytes: Int
    /** Returns a -1 if no bytes are available. Otherwise returns a value between 0 and 255 (inclusive). */
    public fun read(): Int
    public fun read(b: ByteArray, offset: Int, length: Int): Int
    public fun skip(length: Int)
}

@CborFriendModuleApi
public interface Output {
    public fun write(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size)
    public fun write(byteValue: Byte)
}

internal class ByteArrayInput(private var array: ByteArray) : Input {
    private var position: Int = 0
    override val availableBytes: Int get() = array.size - position

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

    override fun skip(length: Int) {
        position += length
    }
}

internal class ByteArrayOutput : Output {
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

    fun toByteArray(): ByteArray {
        val newArray = ByteArray(position)
        array.copyInto(newArray, startIndex = 0, endIndex = this.position)
        return newArray
    }

    fun copyInto(other: Output) {
        other.write(array, 0, position)
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

    override fun write(byteValue: Byte) {
        ensureCapacity(1)
        array[position++] = byteValue
    }
}
