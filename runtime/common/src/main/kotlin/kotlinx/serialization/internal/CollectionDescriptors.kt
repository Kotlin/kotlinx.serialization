package kotlinx.serialization.internal

import kotlinx.serialization.*


sealed class ListLikeDesc(private val elementDesc: SerialDescriptor) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.LIST
    override fun getElementName(index: Int): String = index.toString()
    override fun getElementIndex(name: String): Int = name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid list index")
    override fun getElementDescriptor(index: Int): SerialDescriptor = elementDesc
    override fun isElementOptional(index: Int): Boolean = false
}

sealed class MapLikeDesc(override val name: String, private val keyDesc: SerialDescriptor, private val valueDesc: SerialDescriptor): SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.MAP
    override fun getElementName(index: Int): String = index.toString()
    override fun getElementIndex(name: String): Int = name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid map index")
    override fun getElementDescriptor(index: Int): SerialDescriptor = if (index % 2 == 0) keyDesc else valueDesc
    override fun isElementOptional(index: Int): Boolean = false
}

internal val ARRAY_NAME = "kotlin.Array"
internal val ARRAYLIST_NAME = "kotlin.collections.ArrayList"
internal val LINKEDHASHSET_NAME = "kotlin.collections.LinkedHashSet"
internal val HASHSET_NAME = "kotlin.collections.HashSet"
internal val LINKEDHASHMAP_NAME = "kotlin.collections.LinkedHashMap"
internal val HASHMAP_NAME = "kotlin.collections.HashMap"

class ArrayClassDesc(elementDesc: SerialDescriptor) : ListLikeDesc(elementDesc) {
    override val name: String get() = ARRAY_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class ArrayListClassDesc (elementDesc: SerialDescriptor) : ListLikeDesc(elementDesc) {
    override val name: String get() = ARRAYLIST_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class LinkedHashSetClassDesc (elementDesc: SerialDescriptor) : ListLikeDesc(elementDesc) {
    override val name: String get() = LINKEDHASHSET_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class HashSetClassDesc (elementDesc: SerialDescriptor) : ListLikeDesc(elementDesc) {
    override val name: String get() = HASHSET_NAME
    override val kind: SerialKind get() = StructureKind.LIST
}

class LinkedHashMapClassDesc (keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) : MapLikeDesc(LINKEDHASHMAP_NAME, keyDesc, valueDesc)

class HashMapClassDesc (keyDesc: SerialDescriptor, valueDesc: SerialDescriptor) : MapLikeDesc(HASHMAP_NAME, keyDesc, valueDesc)
