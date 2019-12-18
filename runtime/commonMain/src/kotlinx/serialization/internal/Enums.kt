/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.jvm.*
import kotlin.reflect.*

@InternalSerializationApi
public class EnumDescriptor @JvmOverloads constructor(
    name: String,
    values: Array<String> = emptyArray()
) : SerialClassDescImpl(name) {

    init {
        values.forEach { addElement(it) }
    }

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
        var result = super.hashCode()
        result = 31 * result + serialName.hashCode()
        result = 31 * result + elementNames().hashCode()
        return result
    }
}

@InternalSerializationApi
open class CommonEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val values: Array<T>,
    valuesNames: Array<String> // TODO kill this parameter
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = EnumDescriptor(serialName, values.map { it.name }.toTypedArray())

    final override fun serialize(encoder: Encoder, obj: T) {
        val index = values.indexOf(obj)
        check(index != -1) {
            "$obj is not a valid enum ${descriptor.serialName}, must be one of ${values.contentToString()}"
        }
        encoder.encodeEnum(descriptor, index)
    }

    final override fun deserialize(decoder: Decoder): T {
        val index = decoder.decodeEnum(descriptor)
        check(index in values.indices) {
            "$index is not among valid $${descriptor.serialName} enum values, values size is ${values.size}"
        }
        return values[index]
    }
}

// Binary backwards-compatible with the plugin
// todo: replace with helper method
@InternalSerializationApi
class EnumSerializer<T : Enum<T>> @JvmOverloads constructor(
    serializableClass: KClass<T>,
    serialName: String = serializableClass.enumClassName()
) : CommonEnumSerializer<T>(
    serialName,
    serializableClass.enumMembers(),
    emptyArray()
)
