package kotlinx.serialization.internal

import kotlinx.serialization.*


sealed class ListLikeDescriptor(val elementDesc: SerialDescriptor) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.LIST
    override val elementsCount: Int = 1
    override fun getElementName(index: Int): String = index.toString()
    override fun getElementIndex(name: String): Int =
        name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid list index")

    override fun getElementDescriptor(index: Int): SerialDescriptor = elementDesc
    override fun isElementOptional(index: Int): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListLikeDescriptor) return false

        if (elementDesc == other.elementDesc && name == other.name) return true

        return false
    }

    override fun hashCode(): Int {
        return elementDesc.hashCode() * 31 + name.hashCode()
    }
}

sealed class MapLikeDescriptor(
    override val name: String,
    val keyDescriptor: SerialDescriptor,
    val valueDescriptor: SerialDescriptor
) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.MAP
    override val elementsCount: Int = 2
    override fun getElementName(index: Int): String = index.toString()
    override fun getElementIndex(name: String): Int =
        name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid map index")

    override fun getElementDescriptor(index: Int): SerialDescriptor =
        if (index % 2 == 0) keyDescriptor else valueDescriptor
    override fun isElementOptional(index: Int): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapLikeDescriptor) return false

        if (name != other.name) return false
        if (keyDescriptor != other.keyDescriptor) return false
        if (valueDescriptor != other.valueDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + keyDescriptor.hashCode()
        result = 31 * result + valueDescriptor.hashCode()
        return result
    }
}

internal const val ARRAY_NAME = "kotlin.Array"
internal const val ARRAY_LIST_NAME = "kotlin.collections.ArrayList"
internal const val LINKED_HASH_SET_NAME = "kotlin.collections.LinkedHashSet"
internal const val HASH_SET_NAME = "kotlin.collections.HashSet"
internal const val LINKED_HASH_MAP_NAME = "kotlin.collections.LinkedHashMap"
internal const val HASH_MAP_NAME = "kotlin.collections.HashMap"

class ArrayClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = ARRAY_NAME
}

class ArrayListClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = ARRAY_LIST_NAME
}

class NamedListClassDescriptor(override val name: String, elementDescriptor: SerialDescriptor)
    : ListLikeDescriptor(elementDescriptor)

class LinkedHashSetClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = LINKED_HASH_SET_NAME
}

class HashSetClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = HASH_SET_NAME
}

// TODO revisit this whole hierarchy
class NamedMapClassDescriptor(name: String, keyDescriptor: SerialDescriptor, valueDescriptor: SerialDescriptor) :
    MapLikeDescriptor(name, keyDescriptor, valueDescriptor)

class LinkedHashMapClassDesc(keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) :
    MapLikeDescriptor(LINKED_HASH_MAP_NAME, keyDesc, valueDesc)

class HashMapClassDesc(keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) :
    MapLikeDescriptor(HASH_MAP_NAME, keyDesc, valueDesc)
