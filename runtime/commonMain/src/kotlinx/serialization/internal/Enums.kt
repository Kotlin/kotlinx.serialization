/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/*
 * Descriptor used for explicitly serializable enums by the plugin.
 * Designed to be consistent with `EnumSerializer.descriptor` and weird plugin usage.
 */
@Suppress("unused") // Used by the plugin
@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal class EnumDescriptor(
    name: String,
    elementsCount: Int
) : PluginGeneratedSerialDescriptor(name, elementsCount = elementsCount) {

    override val kind: SerialKind = SerialKind.ENUM
    private val elementDescriptors by lazy {
        Array(elementsCount) { buildSerialDescriptor(name + "." + getElementName(it), StructureKind.OBJECT) }
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor = elementDescriptors.getChecked(index)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is SerialDescriptor) return false
        if (other.kind !== SerialKind.ENUM) return false
        if (serialName != other.serialName) return false
        if (cachedSerialNames() != other.cachedSerialNames()) return false
        return true
    }

    override fun toString(): String {
        return elementNames.joinToString(", ", "$serialName(", ")")
    }

    override fun hashCode(): Int {
        var result = serialName.hashCode()
        val elementsHashCode = elementNames.elementsHashCodeBy { it }
        result = 31 * result + elementsHashCode
        return result
    }
}

// Used for enums that are not explicitly serializable by the plugin
@PublishedApi
internal class EnumSerializer<T : Enum<T>>(
    serialName: String,
    private val values: Array<T>
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor(serialName, SerialKind.ENUM) {
        values.forEach {
            val fqn = "$serialName.${it.name}"
            val enumMemberDescriptor = buildSerialDescriptor(fqn, StructureKind.OBJECT)
            element(it.name, enumMemberDescriptor)
        }
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

    override fun toString(): String = "kotlinx.serialization.internal.EnumSerializer<${descriptor.serialName}>"
}
