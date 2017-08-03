/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.KInput.Companion.READ_ALL
import kotlinx.serialization.KInput.Companion.READ_DONE
import java.util.*
import kotlin.collections.LinkedHashMap

const val SIZE_INDEX = 0

// ============================= serializers =============================

sealed class ListLikeSerializer<E,C,B>(private val eSerializer: KSerializer<E>) : KSerializer<C> {
    override abstract val serialClassDesc: ListLikeDesc

    abstract fun C.objSize(): Int
    abstract fun C.objIterator(): Iterator<E>
    abstract fun builder(): B
    abstract fun B.builderSize(): Int
    abstract fun B.toResult(): C
    abstract fun B.ensureCapacity(size: Int)
    abstract fun B.add(index: Int, element: E)

    open val typeParams: Array<KSerializer<*>> = arrayOf(eSerializer)

    override fun save(output: KOutput, obj: C) {
        val size = obj.objSize()
        @Suppress("NAME_SHADOWING")
        val output = output.writeBegin(serialClassDesc, size, *typeParams)
        if (output.writeElement(ArrayListClassDesc, SIZE_INDEX))
            output.writeIntValue(size)
        val iterator = obj.objIterator()
        for (index in 1..size)
            output.writeSerializableElementValue(ArrayListClassDesc, index, eSerializer, iterator.next())
        output.writeEnd(serialClassDesc)
    }

    override fun load(input: KInput): C {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(serialClassDesc, *typeParams)
        val builder = builder()
        mainLoop@ while (true) {
            val index = input.readElement(serialClassDesc)
            when (index) {
                READ_ALL -> {
                    readAll(input, builder)
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                SIZE_INDEX -> {
                    readSize(input, builder)
                }
                else -> {
                    if (builder.builderSize() == index - 1)
                        readItem(input, index, builder)
                    else
                        throw SerializationException("Elements should be in order, unexpected index $index")
                }
            }

        }
        input.readEnd(serialClassDesc)
        return builder.toResult()
    }

    private fun readSize(input: KInput, builder: B): Int {
        val size = input.readIntElementValue(ArrayListClassDesc, SIZE_INDEX)
        builder.ensureCapacity(size)
        return size
    }

    private fun readItem(input: KInput, index: Int, builder: B) {
        builder.add(index - 1, input.readSerializableElementValue(serialClassDesc, index, eSerializer))
    }

    private fun readAll(input: KInput, builder: B) {
        val size = readSize(input, builder)
        for (index in 1..size)
            readItem(input, index, builder)
    }
}

// todo: can be more efficient when array size is know in advance, this one always uses temporary ArrayList as builder
class ReferenceArraySerializer<E>(private val eClass: java.lang.Class<E>, eSerializer: KSerializer<E>) :
        ListLikeSerializer<E, Array<E>, ArrayList<E>>(eSerializer) {
    override val serialClassDesc = ArrayClassDesc
//    override val serializableClass: KClass<*> = Array<Any>::class

    override fun Array<E>.objSize(): Int = size
    override fun Array<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): ArrayList<E> = arrayListOf()
    override fun ArrayList<E>.builderSize(): Int = size
    @Suppress("UNCHECKED_CAST")
    override fun ArrayList<E>.toResult(): Array<E> = toArray<E>(java.lang.reflect.Array.newInstance(eClass, size) as Array<E>)
    override fun ArrayList<E>.ensureCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<E>.add(index: Int, element: E) { add(element) }
}

class ArrayListSerializer<E>(element: KSerializer<E>) : ListLikeSerializer<E, Collection<E>, ArrayList<E>>(element) {
    override val serialClassDesc = ArrayListClassDesc
//    override val serializableClass: KClass<*> = Collection::class

    override fun Collection<E>.objSize(): Int = size
    override fun Collection<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): ArrayList<E> = arrayListOf()
    override fun ArrayList<E>.builderSize(): Int = size
    override fun ArrayList<E>.toResult(): Collection<E> = this
    override fun ArrayList<E>.ensureCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<E>.add(index: Int, element: E) { add(element) }
}

class LinkedHashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, LinkedHashSet<E>>(eSerializer) {
    override val serialClassDesc = LinkedHashSetClassDesc
//    override val serializableClass: KClass<*> = Set::class

    override fun Set<E>.objSize(): Int = size
    override fun Set<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): LinkedHashSet<E> = linkedSetOf()
    override fun LinkedHashSet<E>.builderSize(): Int = size
    override fun LinkedHashSet<E>.toResult(): Set<E> = this
    override fun LinkedHashSet<E>.ensureCapacity(size: Int) {}
    override fun LinkedHashSet<E>.add(index: Int, element: E) { add(element) }
}

class HashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, HashSet<E>>(eSerializer) {
    override val serialClassDesc = HashSetClassDesc
//    override val serializableClass: KClass<*> = HashSet::class

    override fun Set<E>.objSize(): Int = size
    override fun Set<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): HashSet<E> = HashSet()
    override fun HashSet<E>.builderSize(): Int = size
    override fun HashSet<E>.toResult(): Set<E> = this
    override fun HashSet<E>.ensureCapacity(size: Int) {}
    override fun HashSet<E>.add(index: Int, element: E) { add(element) }
}

class LinkedHashMapSerializer<K,V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
        ListLikeSerializer<Map.Entry<K, V>, Map<K, V>, LinkedHashMap<K, V>>(MapEntrySerializer<K, V>(kSerializer, vSerializer)) {
    override val serialClassDesc = LinkedHashMapClassDesc
    //    override val serializableClass: KClass<*> = LinkedHashMap::class
    override val typeParams: Array<KSerializer<*>> = arrayOf(kSerializer, vSerializer)

    override fun Map<K, V>.objSize(): Int = size
    override fun Map<K, V>.objIterator(): Iterator<Map.Entry<K, V>> = iterator()
    override fun builder(): LinkedHashMap<K, V> = LinkedHashMap()
    override fun LinkedHashMap<K, V>.builderSize(): Int = size
    override fun LinkedHashMap<K, V>.toResult(): Map<K, V> = this
    override fun LinkedHashMap<K, V>.ensureCapacity(size: Int) {}
    override fun LinkedHashMap<K, V>.add(index: Int, element: Map.Entry<K, V>) { put(element.key, element.value) }
}

class HashMapSerializer<K,V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
        ListLikeSerializer<Map.Entry<K, V>, Map<K, V>, HashMap<K, V>>(MapEntrySerializer<K, V>(kSerializer, vSerializer)) {
    override val serialClassDesc: ListLikeDesc = HashMapClassDesc
    //    override val serializableClass: KClass<*> = HashMap::class
    override val typeParams: Array<KSerializer<*>> = arrayOf(kSerializer, vSerializer)

    override fun Map<K, V>.objSize(): Int = size
    override fun Map<K, V>.objIterator(): Iterator<Map.Entry<K, V>> = iterator()
    override fun builder(): HashMap<K, V> = HashMap()
    override fun HashMap<K, V>.builderSize(): Int = size
    override fun HashMap<K, V>.toResult(): Map<K, V> = this
    override fun HashMap<K, V>.ensureCapacity(size: Int) {}
    override fun HashMap<K, V>.add(index: Int, element: Map.Entry<K, V>) { put(element.key, element.value) }
}

const val KEY_INDEX = 0
const val VALUE_INDEX = 1

class MapEntrySerializer<K, V>(private val kSerializer: KSerializer<K>, private val vSerializer: KSerializer<V>) :
        KSerializer<Map.Entry<K, V>> {
    override val serialClassDesc = MapEntryClassDesc//To change initializer of created properties use File | Settings | File Templates.
//    override val serializableClass: KClass<*> = Map.Entry::class

    override fun save(output: KOutput, obj: Map.Entry<K, V>) {
        @Suppress("NAME_SHADOWING")
        val output = output.writeBegin(MapEntryClassDesc, kSerializer, vSerializer)
        output.writeSerializableElementValue(MapEntryClassDesc, KEY_INDEX, kSerializer, obj.key)
        output.writeSerializableElementValue(MapEntryClassDesc, VALUE_INDEX, vSerializer, obj.value)
        output.writeEnd(MapEntryClassDesc)
    }

    override fun load(input: KInput): Map.Entry<K, V> {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(MapEntryClassDesc, kSerializer, vSerializer)
        var kSet = false
        var vSet = false
        var k: Any? = null
        var v: Any? = null
        mainLoop@ while (true) {
            when (input.readElement(MapEntryClassDesc)) {
                READ_ALL -> {
                    k = input.readSerializableElementValue(MapEntryClassDesc, KEY_INDEX, kSerializer)
                    kSet = true
                    v = input.readSerializableElementValue(MapEntryClassDesc, VALUE_INDEX, vSerializer)
                    vSet = true
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                KEY_INDEX -> {
                    k = input.readSerializableElementValue(MapEntryClassDesc, KEY_INDEX, kSerializer)
                    kSet = true
                }
                VALUE_INDEX -> {
                    v = input.readSerializableElementValue(MapEntryClassDesc, VALUE_INDEX, vSerializer)
                    vSet = true
                }
                else -> throw SerializationException("Invalid index")
            }
        }
        input.readEnd(MapEntryClassDesc)
        if (!kSet) throw SerializationException("Required key is missing")
        if (!vSet) throw SerializationException("Required value is missing")
        @Suppress("UNCHECKED_CAST")
        return MapEntry<K, V>(k as K, v as V)
    }
}

// ============================= class descriptors =============================

sealed class ListLikeDesc : KSerialClassDesc {
    override fun getElementName(index: Int): String = if (index == SIZE_INDEX) "size" else index.toString()
    override fun getElementIndex(name: String): Int = if (name == "size") SIZE_INDEX else parseInt(name)

    // todo: kludge: becaues JS does not have String.toInt() KT-4497
    private fun parseInt(name: String): Int {
        check(name != "") { "empty name" }
        var result = 0
        for (c in name) {
            check(c in '0'..'9') { "Invalid digit $c" }
            result = result * 10 + (c.toInt() - '0'.toInt())
        }
        return result
    }
}

object ArrayClassDesc : ListLikeDesc() {
    override val name: String get() = "kotlin.Array"
    override val kind: KSerialClassKind get() = KSerialClassKind.LIST
}

object ArrayListClassDesc : ListLikeDesc() {
    override val name: String get() = "kotlin.collections.ArrayList"
    override val kind: KSerialClassKind get() = KSerialClassKind.LIST
}

object LinkedHashSetClassDesc : ListLikeDesc() {
    override val name: String get() = "kotlin.collections.LinkedHashSet"
    override val kind: KSerialClassKind get() = KSerialClassKind.SET
}

object HashSetClassDesc : ListLikeDesc() {
    override val name: String get() = "kotlin.collections.HashSet"
    override val kind: KSerialClassKind get() = KSerialClassKind.SET
}

object LinkedHashMapClassDesc : ListLikeDesc() {
    override val name: String get() = "kotlin.collections.LinkedHashMap"
    override val kind: KSerialClassKind get() = KSerialClassKind.MAP
}

object HashMapClassDesc : ListLikeDesc() {
    override val name: String get() = "kotlin.collections.HashMap"
    override val kind: KSerialClassKind get() = KSerialClassKind.MAP
}

data class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

object MapEntryClassDesc : SerialClassDescImpl("kotlin.collections.Map.Entry") {
    override val kind: KSerialClassKind = KSerialClassKind.ENTRY

    init {
        addElement("key")
        addElement("value")
    }
}
