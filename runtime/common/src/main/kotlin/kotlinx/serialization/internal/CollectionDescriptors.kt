package kotlinx.serialization.internal

import kotlinx.serialization.*


sealed class ListLikeDescriptor(val elementDesc: SerialDescriptor) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.LIST
    override val elementsCount: Int = 1
    override fun getElementName(index: Int): String = index.toString()
    override fun getElementIndex(name: String): Int = name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid list index")
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

sealed class MapLikeDescriptor(override val name: String, val keyDesc: SerialDescriptor, val valueDesc: SerialDescriptor): SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.MAP
    override val elementsCount: Int = 2
    override fun getElementName(index: Int): String = index.toString()
    override fun getElementIndex(name: String): Int = name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid map index")
    override fun getElementDescriptor(index: Int): SerialDescriptor = if (index % 2 == 0) keyDesc else valueDesc
    override fun isElementOptional(index: Int): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapLikeDescriptor) return false

        if (name != other.name) return false
        if (keyDesc != other.keyDesc) return false
        if (valueDesc != other.valueDesc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + keyDesc.hashCode()
        result = 31 * result + valueDesc.hashCode()
        return result
    }
}

internal val ARRAY_NAME = "kotlin.Array"
internal val ARRAYLIST_NAME = "kotlin.collections.ArrayList"
internal val LINKEDHASHSET_NAME = "kotlin.collections.LinkedHashSet"
internal val HASHSET_NAME = "kotlin.collections.HashSet"
internal val LINKEDHASHMAP_NAME = "kotlin.collections.LinkedHashMap"
internal val HASHMAP_NAME = "kotlin.collections.HashMap"

class ArrayClassDesc(elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = ARRAY_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class ArrayListClassDesc (elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = ARRAYLIST_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class LinkedHashSetClassDesc (elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = LINKEDHASHSET_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class HashSetClassDesc (elementDesc: SerialDescriptor) : ListLikeDescriptor(elementDesc) {
    override val name: String get() = HASHSET_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class LinkedHashMapClassDesc (keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) : MapLikeDescriptor(LINKEDHASHMAP_NAME, keyDesc, valueDesc)

class HashMapClassDesc (keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) : MapLikeDescriptor(HASHMAP_NAME, keyDesc, valueDesc)
