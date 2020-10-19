package kotlinx.serialization.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.descriptors.SerialDescriptor

@OptIn(ExperimentalSerializationApi::class)
@InternalSerializationApi
public fun throwMissingFieldException(seen: Int, goldenMask: Int, descriptor: SerialDescriptor) {
    val missingFields = mutableListOf<String>()

    var missingFieldsBits = goldenMask and seen.inv()
    for (i in 0 until 32) {
        if (missingFieldsBits and 1 != 0) {
            missingFields += descriptor.getElementName(i)
        }
        missingFieldsBits = missingFieldsBits ushr 1
    }
    throw MissingFieldException(missingFields, descriptor.serialName)
}

@OptIn(ExperimentalSerializationApi::class)
@InternalSerializationApi
public fun throwArrayMissingFieldException(seenArray: IntArray, goldenMaskArray: IntArray, descriptor: SerialDescriptor) {
    val missingFields = mutableListOf<String>()

    for (maskSlot in goldenMaskArray.indices) {
        var missingFieldsBits = goldenMaskArray[maskSlot] and seenArray[maskSlot].inv()
        if (missingFieldsBits != 0) {
            for (i in 0 until 32) {
                if (missingFieldsBits and 1 != 0) {
                    missingFields += descriptor.getElementName(maskSlot * 32 + i)
                }
                missingFieldsBits = missingFieldsBits ushr 1
            }
        }
    }
    throw MissingFieldException(missingFields, descriptor.serialName)
}
