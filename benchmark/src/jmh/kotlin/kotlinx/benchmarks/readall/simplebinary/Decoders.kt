/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.benchmarks.readall.simplebinary

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE

class ReadAllBinaryDecoder(private val data: ByteArray) : ElementValueDecoder() {

    private var currentIndex = 0

    public fun reset() {
        currentIndex = 0
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int = CompositeDecoder.READ_ALL

    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int {
        val idx = currentIndex
        val b1 = data[idx + 1].toInt()
        val b2 = data[idx + 2].toInt()
        val b3 = data[idx + 3].toInt()
        val b4 = data[idx + 4].toInt()
        currentIndex = idx + 4
        return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
    }

    override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float {
        val int = decodeIntElement(desc, index)
        return Float.fromBits(int)
    }

    override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte {
        return data[currentIndex++]
    }

    override fun decodeNotNullMark(): Boolean = false
}

class ReadByOneBinaryDecoder(private val data: ByteArray, private val fields: Int) : ElementValueDecoder() {
    private var currentIndex = 0
    private var currentDataIndex = 0

    public fun reset() {
        currentIndex = 0
        currentDataIndex = 0
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        if (currentDataIndex == fields) return READ_DONE
        return currentDataIndex++
    }

    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int {
        val idx = currentIndex
        val b1 = data[idx + 1].toInt()
        val b2 = data[idx + 2].toInt()
        val b3 = data[idx + 3].toInt()
        val b4 = data[idx + 4].toInt()
        currentIndex = idx + 4
        return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
    }

    override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float {
        val int = decodeIntElement(desc, index)
        return Float.fromBits(int)
    }

    override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte {
        return data[currentIndex++]
    }

    override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short {
        val idx = currentIndex
        val b1 = data[idx + 1].toInt()
        val b2 = data[idx + 2].toInt()
        currentIndex = idx + 2
        return ((b1 shl 8) or b2).toShort()
    }

    override fun decodeNotNullMark(): Boolean = false
}