/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

@OptIn(ExperimentalSerializationApi::class)
@CoreFriendModuleApi
public class ElementMarker(
    private val descriptor: SerialDescriptor,
    // Instead of inheritance and virtual function in order to keep cross-module internal modifier via suppresses
    // Can be reworked via public + internal api if necessary
    private val readIfAbsent: (SerialDescriptor, Int) -> Boolean
) {
    /*
     * Element decoding marks from given bytes.
     * The element index is the same as the set bit position.
     * Marks for the lowest 64 elements are always stored in a single Long value, higher elements stores in long array.
     */
    private var lowerMarks: Long
    private val highMarksArray: LongArray

    private companion object {
        private val EMPTY_HIGH_MARKS = LongArray(0)
    }

    init {
        val elementsCount = descriptor.elementsCount
        if (elementsCount <= Long.SIZE_BITS) {
            lowerMarks = if (elementsCount == Long.SIZE_BITS) {
                // number of bits in the mark is equal to the number of fields
                0L
            } else {
                // (1 - elementsCount) bits are always 1 since there are no fields for them
                -1L shl elementsCount
            }
            highMarksArray = EMPTY_HIGH_MARKS
        } else {
            lowerMarks = 0L
            highMarksArray = prepareHighMarksArray(elementsCount)
        }
    }

    public fun mark(index: Int) {
        if (index < Long.SIZE_BITS) {
            lowerMarks = lowerMarks or (1L shl index)
        } else {
            markHigh(index)
        }
    }

    public fun nextUnmarkedIndex(): Int {
        val elementsCount = descriptor.elementsCount
        while (lowerMarks != -1L) {
            val index = lowerMarks.inv().countTrailingZeroBits()
            lowerMarks = lowerMarks or (1L shl index)

            if (readIfAbsent(descriptor, index)) {
                return index
            }
        }

        if (elementsCount > Long.SIZE_BITS) {
            return nextUnmarkedHighIndex()
        }
        return CompositeDecoder.DECODE_DONE
    }

    private fun prepareHighMarksArray(elementsCount: Int): LongArray {
        // (elementsCount - 1) / Long.SIZE_BITS
        // (elementsCount - 1) because only one Long value is needed to store 64 fields etc
        val slotsCount = (elementsCount - 1) ushr 6
        // elementsCount % Long.SIZE_BITS
        val elementsInLastSlot = elementsCount and (Long.SIZE_BITS - 1)
        val highMarks = LongArray(slotsCount)
        // if (elementsCount % Long.SIZE_BITS) == 0 means that the fields occupy all bits in mark
        if (elementsInLastSlot != 0) {
            // all marks except the higher are always 0
            highMarks[highMarks.lastIndex] = -1L shl elementsCount
        }
        return highMarks
    }

    private fun markHigh(index: Int) {
        // (index / Long.SIZE_BITS) - 1
        val slot = (index ushr 6) - 1
        // index % Long.SIZE_BITS
        val offsetInSlot = index and (Long.SIZE_BITS - 1)
        highMarksArray[slot] = highMarksArray[slot] or (1L shl offsetInSlot)
    }

    private fun nextUnmarkedHighIndex(): Int {
        for (slot in highMarksArray.indices) {
            // (slot + 1) because first element in high marks has index 64
            val slotOffset = (slot + 1) * Long.SIZE_BITS
            // store in a variable so as not to frequently use the array
            var slotMarks = highMarksArray[slot]

            while (slotMarks != -1L) {
                val indexInSlot = slotMarks.inv().countTrailingZeroBits()
                slotMarks = slotMarks or (1L shl indexInSlot)

                val index = slotOffset + indexInSlot
                if (readIfAbsent(descriptor, index)) {
                    highMarksArray[slot] = slotMarks
                    return index
                }
            }
            highMarksArray[slot] = slotMarks
        }
        return CompositeDecoder.DECODE_DONE
    }
}
