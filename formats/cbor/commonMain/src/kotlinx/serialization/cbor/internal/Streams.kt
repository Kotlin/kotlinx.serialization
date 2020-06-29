/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor.internal

internal class ByteArrayInput(private var array: ByteArray) {
    private var position: Int = 0
    public val availableBytes: Int get() = array.size - position

    fun read(): Int {
        return if (position < array.size) array[position++].toInt() and 0xFF else -1
    }

    fun read(b: ByteArray, offset: Int, length: Int): Int {
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

    public fun toByteArray(): ByteArray {
        val newArray = ByteArray(position)
        array.copyInto(newArray, startIndex = 0, endIndex = this.position)
        return newArray
    }

    fun write(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size) {
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

    fun write(byteValue: Int) {
        ensureCapacity(1)
        array[position++] = byteValue.toByte()
    }
}
