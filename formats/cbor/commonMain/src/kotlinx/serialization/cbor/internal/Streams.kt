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

    fun skip(length: Int) {
        position += length
    }
}

internal class ByteArrayOutput {
    private var array: ByteArray = ByteArray(32)
    private var position: Int = 0

    private fun ensureCapacity(elementsToAppend: Int) {
        val requiredCapacityLong = position.toLong() + elementsToAppend.toLong()
        if (requiredCapacityLong > Int.MAX_VALUE) {
            throw IllegalArgumentException("Required capacity exceeds maximum array size (Int.MAX_VALUE).")
        }

        val requiredCapacity = requiredCapacityLong.toInt()
        if (requiredCapacity <= array.size) {
            return
        }

        val newCapacity = nextPowerOfTwoCapacity(requiredCapacity)
        array = array.copyOf(newCapacity)
    }

    public fun toByteArray(): ByteArray {
        val newArray = ByteArray(position)
        array.copyInto(newArray, startIndex = 0, endIndex = this.position)
        return newArray
    }

    fun copyFrom(src: ByteArrayOutput) {
        write(src.array, count = src.position)
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
    
    companion object {
        /**
         * Calculates the next power-of-two capacity based on the required minimum size.
         *
         * This function ensures the returned value is at least as large as `minCapacity`,
         * and is always a power of two, unless `minCapacity` is less than or equal to zero,
         * in which case it returns 0. If the calculated power of two exceeds `Integer.MAX_VALUE`,
         * it returns `Integer.MAX_VALUE`.
         *
         * It's useful for resizing arrays with exponential growth.
         *
         * @param minCapacity The minimum required capacity.
         * @return A capacity value that is a power of two and ≥ minCapacity, or 0 if `minCapacity` is ≤ 0.
         */
        fun nextPowerOfTwoCapacity(minCapacity: Int): Int {
            if (minCapacity <= 0) return 0

            val highestOneBit = minCapacity.takeHighestOneBit()
            val maxHighestOneBit = Integer.MAX_VALUE.takeHighestOneBit()

            // Check if shifting would exceed the maximum allowed value
            return if (highestOneBit < maxHighestOneBit) highestOneBit shl 1 else Integer.MAX_VALUE
        }
    }
}
