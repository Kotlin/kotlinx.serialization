/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME

internal sealed class ListLikeDescriptor(val elementDescriptor: SerialDescriptor) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.LIST
    override val elementsCount: Int = 1

    override fun getElementName(index: Int): String {
        requireNonNegativeIndex(index, serialName)
        return index.toString()
    }

    override fun getElementIndex(name: String): Int =
        name.toIntOrNull()?.takeIf { it >= 0 } ?: UNKNOWN_NAME

    override fun isElementOptional(index: Int): Boolean {
        requireNonNegativeIndex(index, serialName)
        return false
    }

    override fun getElementAnnotations(index: Int): List<Annotation> {
        requireNonNegativeIndex(index, serialName)
        return emptyList()
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        requireNonNegativeIndex(index, serialName)
        return elementDescriptor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListLikeDescriptor) return false
        if (elementDescriptor == other.elementDescriptor && serialName == other.serialName) return true
        return false
    }

    override fun hashCode(): Int {
        return elementDescriptor.hashCode() * 31 + serialName.hashCode()
    }

    override fun toString(): String = "$serialName($elementDescriptor)"
}

internal sealed class MapLikeDescriptor(
    override val serialName: String,
    val keyDescriptor: SerialDescriptor,
    val valueDescriptor: SerialDescriptor
) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.MAP
    override val elementsCount: Int = 2

    override fun getElementName(index: Int): String {
        requireNonNegativeIndex(index, serialName)
        return index.toString()
    }

    override fun getElementIndex(name: String): Int =
        name.toIntOrNull()?.takeIf { it >= 0 } ?: UNKNOWN_NAME

    override fun isElementOptional(index: Int): Boolean {
        requireNonNegativeIndex(index, serialName)
        return false
    }

    override fun getElementAnnotations(index: Int): List<Annotation> {
        requireNonNegativeIndex(index, serialName)
        return emptyList()
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        requireNonNegativeIndex(index, serialName)
        return when (index % 2) {
            0 -> keyDescriptor
            1 -> valueDescriptor
            else -> error("Unreached")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapLikeDescriptor) return false
        if (serialName != other.serialName) return false
        if (keyDescriptor != other.keyDescriptor) return false
        if (valueDescriptor != other.valueDescriptor) return false
        return true
    }

    override fun hashCode(): Int {
        var result = serialName.hashCode()
        result = 31 * result + keyDescriptor.hashCode()
        result = 31 * result + valueDescriptor.hashCode()
        return result
    }

    override fun toString(): String = "$serialName($keyDescriptor, $valueDescriptor)"
}

private fun requireNonNegativeIndex(index: Int, name: String) {
    if (index < 0) throw IndexOutOfBoundsException("Illegal index $index, $name expects only non-negative indices")
}

internal const val ARRAY_NAME = "kotlin.Array"
internal const val ARRAY_LIST_NAME = "kotlin.collections.ArrayList"
internal const val LINKED_HASH_SET_NAME = "kotlin.collections.LinkedHashSet"
internal const val HASH_SET_NAME = "kotlin.collections.HashSet"
internal const val LINKED_HASH_MAP_NAME = "kotlin.collections.LinkedHashMap"
internal const val HASH_MAP_NAME = "kotlin.collections.HashMap"

/**
 * Descriptor for primitive arrays, such as [IntArray], [DoubleArray], etc...
 *
 * Can be obtained from corresponding serializers (e.g. [ByteArraySerializer.descriptor])
 */
internal class PrimitiveArrayDescriptor internal constructor(
    primitive: SerialDescriptor
) : ListLikeDescriptor(primitive) {
    override val serialName: String = "${primitive.serialName}Array"
}

internal class ArrayClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val serialName: String get() = ARRAY_NAME
}

internal class ArrayListClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val serialName: String get() = ARRAY_LIST_NAME
}

internal class LinkedHashSetClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val serialName: String get() = LINKED_HASH_SET_NAME
}

internal class HashSetClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val serialName: String get() = HASH_SET_NAME
}

internal class LinkedHashMapClassDesc(keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) :
    MapLikeDescriptor(LINKED_HASH_MAP_NAME, keyDesc, valueDesc)

internal class HashMapClassDesc(keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) :
    MapLikeDescriptor(HASH_MAP_NAME, keyDesc, valueDesc)
