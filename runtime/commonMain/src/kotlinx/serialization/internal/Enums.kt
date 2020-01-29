/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*

@InternalSerializationApi
public class EnumDescriptor(
    name: String,
    elementsCount: Int = 1
) : SerialClassDescImpl(name, elementsCount = elementsCount) {

    override val kind: SerialKind = UnionKind.ENUM_KIND

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        if (index !in 0 until elementsCount) throw IndexOutOfBoundsException("Index $index out of bounds ${0 until elementsCount}")
        return this
    }

    override fun isElementOptional(index: Int): Boolean {
        throw IllegalStateException("Enums do not have elements, " +
                "thus calling 'isElementOptional' does not make any sense")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is SerialDescriptor) return false
        if (other.kind !== UnionKind.ENUM_KIND) return false
        if (serialName != other.serialName) return false
        if (elementNames() != other.elementNames()) return false
        return true
    }

    override fun toString(): String {
        return elementNames().joinToString(", ", "$serialName(", ")")
    }

    override fun hashCode(): Int {
        var result = serialName.hashCode()
        result = 31 * result + elementNames().hashCode()
        return result
    }
}

@InternalSerializationApi
public class EnumSerializer<T : Enum<T>>(
    serialName: String,
    private val values: Array<T>
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = EnumDescriptor(serialName, values.size).apply {
        values.forEach { addElement(it.name) }
    }

    override fun serialize(encoder: Encoder, value: T) {
        val index = values.indexOf(value)
        check(index != -1) {
            "$value is not a valid enum ${descriptor.serialName}, must be one of ${values.contentToString()}"
        }
        encoder.encodeEnum(descriptor, index)
    }

    override fun deserialize(decoder: Decoder): T {
        val index = decoder.decodeEnum(descriptor)
        check(index in values.indices) {
            "$index is not among valid $${descriptor.serialName} enum values, values size is ${values.size}"
        }
        return values[index]
    }
}
