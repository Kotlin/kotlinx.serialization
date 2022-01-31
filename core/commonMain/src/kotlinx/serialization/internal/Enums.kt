/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.jvm.Volatile

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

@OptIn(ExperimentalSerializationApi::class)
@InternalSerializationApi
internal fun <T : Enum<T>> createSimpleEnumSerializer(serialName: String, values: Array<T>): KSerializer<T> {
    return EnumSerializer(serialName, values)
}

@OptIn(ExperimentalSerializationApi::class)
@InternalSerializationApi
internal fun <T : Enum<T>> createMarkedEnumSerializer(
    serialName: String,
    values: Array<T>,
    names: Array<String?>,
    annotations: Array<Array<Annotation>?>
): KSerializer<T> {
    val descriptor = EnumDescriptor(serialName, values.size)
    values.forEachIndexed { i, v ->
        val elementName = names.getOrNull(i) ?: v.name
        descriptor.addElement(elementName)
        annotations.getOrNull(i)?.forEach {
            descriptor.pushAnnotation(it)
        }
    }

    return EnumSerializer(serialName, values, descriptor)
}

// TODO we can create another class for factories, it may be a copy-paste of this class or a subclass for some `AbstractEnumSerializer` with common `serialize`, `deserialize` and `toString` functions
@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal class EnumSerializer<T : Enum<T>>(
    serialName: String,
    private val values: Array<T>
) : KSerializer<T> {
    @Volatile
    private var overriddenDescriptor: SerialDescriptor? = null

    internal constructor(serialName: String, values: Array<T>, descriptor: SerialDescriptor) : this(serialName, values) {
        overriddenDescriptor = descriptor
    }

    override val descriptor: SerialDescriptor by lazy {
        overriddenDescriptor ?: createUnmarkedDescriptor(serialName)
    }

    private fun createUnmarkedDescriptor(serialName: String): SerialDescriptor {
        val d = EnumDescriptor(serialName, values.size)
        values.forEach { d.addElement(it.name) }
        return d
    }

    override fun serialize(encoder: Encoder, value: T) {
        val index = values.indexOf(value)
        if (index == -1) {
            throw SerializationException(
                "$value is not a valid enum ${descriptor.serialName}, " +
                        "must be one of ${values.contentToString()}"
            )
        }
        encoder.encodeEnum(descriptor, index)
    }

    override fun deserialize(decoder: Decoder): T {
        val index = decoder.decodeEnum(descriptor)
        if (index !in values.indices) {
            throw SerializationException(
                "$index is not among valid ${descriptor.serialName} enum values, " +
                        "values size is ${values.size}"
            )
        }
        return values[index]
    }

    override fun toString(): String = "kotlinx.serialization.internal.EnumSerializer<${descriptor.serialName}>"
}
