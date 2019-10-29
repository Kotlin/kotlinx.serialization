/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.readall.baseline

import kotlinx.serialization.*

class ReadAllIntDecoder(private val data: IntArray) : ElementValueDecoder() {
    private var currentIndex = 0
    public fun reset() {
        currentIndex = 0
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int = CompositeDecoder.READ_ALL

    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int {
        return data[currentIndex++]
    }
}

class ReadByOneIntDecoder(private val data: IntArray, private val size: Int) : ElementValueDecoder() {
    private var currentIndex = 0

    public fun reset() {
        currentIndex = 0
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        val idx = currentIndex++
        if (size == idx) return CompositeDecoder.READ_DONE
        return idx
    }

    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int {
        return data[currentIndex]
    }
}
