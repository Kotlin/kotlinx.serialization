/*
 * Copyright 2017 JetBrains s.r.o.
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
import kotlin.reflect.KClass

const val SIZE_INDEX = 0

// ============================= serializers =============================

sealed class ListLikeSerializer<E,C,B>(private val eSerializer: KSerializer<E>) : KSerializer<C> {
    override abstract val serialClassDesc: ListLikeDesc

    abstract fun C.objSize(): Int
    abstract fun C.objIterator(): Iterator<E>
    abstract fun builder(): B
    abstract fun B.builderSize(): Int
    abstract fun B.toResult(): C
    abstract fun C.toBuilder(): B
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

    override fun update(input: KInput, old: C): C {
        val builder = old.toBuilder()
        val startIndex = builder.builderSize()
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(serialClassDesc, *typeParams)
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
                    if (builder.builderSize() == startIndex + index - 1)
                        readItem(input, index, builder)
                    else
                        throw SerializationException("Elements should be in order, unexpected index $index")
                }
            }

        }
        input.readEnd(serialClassDesc)
        return builder.toResult()
    }

    override fun load(input: KInput): C {
        val builder = builder()
        return update(input, builder.toResult())
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
class ReferenceArraySerializer<T: Any, E: T?>(private val kClass: KClass<T>, eSerializer: KSerializer<E>):
        ListLikeSerializer<E, Array<E>, ArrayList<E>>(eSerializer) {
    override val serialClassDesc = ArrayClassDesc

    override fun Array<E>.objSize(): Int = size
    override fun Array<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): ArrayList<E> = arrayListOf()
    override fun ArrayList<E>.builderSize(): Int = size
    @Suppress("UNCHECKED_CAST")
    override fun ArrayList<E>.toResult(): Array<E> = toNativeArray<T, E>(kClass)
    override fun Array<E>.toBuilder(): ArrayList<E> = ArrayList(this.asList())
    override fun ArrayList<E>.ensureCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<E>.add(index: Int, element: E) { add(element) }
}

class ArrayListSerializer<E>(element: KSerializer<E>) : ListLikeSerializer<E, List<E>, ArrayList<E>>(element) {
    override val serialClassDesc = ArrayListClassDesc

    override fun List<E>.objSize(): Int = size
    override fun List<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): ArrayList<E> = arrayListOf()
    override fun ArrayList<E>.builderSize(): Int = size
    override fun ArrayList<E>.toResult(): List<E> = this
    override fun List<E>.toBuilder(): ArrayList<E> = this as? ArrayList<E> ?: ArrayList(this)
    override fun ArrayList<E>.ensureCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<E>.add(index: Int, element: E) { add(element) }
}

class LinkedHashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, LinkedHashSet<E>>(eSerializer) {
    override val serialClassDesc = LinkedHashSetClassDesc

    override fun Set<E>.objSize(): Int = size
    override fun Set<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): LinkedHashSet<E> = linkedSetOf()
    override fun LinkedHashSet<E>.builderSize(): Int = size
    override fun LinkedHashSet<E>.toResult(): Set<E> = this
    override fun Set<E>.toBuilder(): LinkedHashSet<E> = this as? LinkedHashSet<E> ?: LinkedHashSet(this)
    override fun LinkedHashSet<E>.ensureCapacity(size: Int) {}
    override fun LinkedHashSet<E>.add(index: Int, element: E) { add(element) }
}

class HashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, HashSet<E>>(eSerializer) {
    override val serialClassDesc = HashSetClassDesc

    override fun Set<E>.objSize(): Int = size
    override fun Set<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): HashSet<E> = HashSet()
    override fun HashSet<E>.builderSize(): Int = size
    override fun HashSet<E>.toResult(): Set<E> = this
    override fun Set<E>.toBuilder(): HashSet<E> = this as? HashSet<E> ?: HashSet(this)
    override fun HashSet<E>.ensureCapacity(size: Int) {}
    override fun HashSet<E>.add(index: Int, element: E) { add(element) }
}

class LinkedHashMapSerializer<K,V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
        ListLikeSerializer<Map.Entry<K, V>, Map<K, V>, LinkedHashMap<K, V>>(MapEntrySerializer<K, V>(kSerializer, vSerializer)) {
    override val serialClassDesc = LinkedHashMapClassDesc
    override val typeParams: Array<KSerializer<*>> = arrayOf(kSerializer, vSerializer)

    override fun Map<K, V>.objSize(): Int = size
    override fun Map<K, V>.objIterator(): Iterator<Map.Entry<K, V>> = iterator()
    override fun builder(): LinkedHashMap<K, V> = LinkedHashMap()
    override fun LinkedHashMap<K, V>.builderSize(): Int = size
    override fun LinkedHashMap<K, V>.toResult(): Map<K, V> = this
    override fun Map<K, V>.toBuilder(): LinkedHashMap<K, V> = this as? LinkedHashMap<K, V> ?: LinkedHashMap(this)
    override fun LinkedHashMap<K, V>.ensureCapacity(size: Int) {}
    override fun LinkedHashMap<K, V>.add(index: Int, element: Map.Entry<K, V>) { put(element.key, element.value) }
}

class HashMapSerializer<K,V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
        ListLikeSerializer<Map.Entry<K, V>, Map<K, V>, HashMap<K, V>>(MapEntrySerializer<K, V>(kSerializer, vSerializer)) {
    override val serialClassDesc: ListLikeDesc = HashMapClassDesc
    override val typeParams: Array<KSerializer<*>> = arrayOf(kSerializer, vSerializer)

    override fun Map<K, V>.objSize(): Int = size
    override fun Map<K, V>.objIterator(): Iterator<Map.Entry<K, V>> = iterator()
    override fun builder(): HashMap<K, V> = HashMap()
    override fun HashMap<K, V>.builderSize(): Int = size
    override fun HashMap<K, V>.toResult(): Map<K, V> = this
    override fun Map<K, V>.toBuilder(): HashMap<K, V> = this as? HashMap<K, V> ?: HashMap(this)
    override fun HashMap<K, V>.ensureCapacity(size: Int) {}
    override fun HashMap<K, V>.add(index: Int, element: Map.Entry<K, V>) { put(element.key, element.value) }
}

const val KEY_INDEX = 0
const val VALUE_INDEX = 1

sealed class KeyValueSerializer<K, V, R>(private val kSerializer: KSerializer<K>, private val vSerializer: KSerializer<V>): KSerializer<R> {
    override abstract val serialClassDesc: KSerialClassDesc
    abstract fun toResult(key: K, value: V): R
    abstract val R.key: K
    abstract val R.value: V

    override fun save(output: KOutput, obj: R) {
        @Suppress("NAME_SHADOWING")
        val output = output.writeBegin(serialClassDesc, kSerializer, vSerializer)
        output.writeSerializableElementValue(serialClassDesc, KEY_INDEX, kSerializer, obj.key)
        output.writeSerializableElementValue(serialClassDesc, VALUE_INDEX, vSerializer, obj.value)
        output.writeEnd(serialClassDesc)
    }

    override fun load(input: KInput): R {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(serialClassDesc, kSerializer, vSerializer)
        var kSet = false
        var vSet = false
        var k: Any? = null
        var v: Any? = null
        mainLoop@ while (true) {
            when (input.readElement(serialClassDesc)) {
                READ_ALL -> {
                    k = input.readSerializableElementValue(serialClassDesc, KEY_INDEX, kSerializer)
                    kSet = true
                    v = input.readSerializableElementValue(serialClassDesc, VALUE_INDEX, vSerializer)
                    vSet = true
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                KEY_INDEX -> {
                    k = input.readSerializableElementValue(serialClassDesc, KEY_INDEX, kSerializer)
                    kSet = true
                }
                VALUE_INDEX -> {
                    v = input.readSerializableElementValue(serialClassDesc, VALUE_INDEX, vSerializer)
                    vSet = true
                }
                else -> throw SerializationException("Invalid index")
            }
        }
        input.readEnd(serialClassDesc)
        if (!kSet) throw SerializationException("Required key is missing")
        if (!vSet) throw SerializationException("Required value is missing")
        @Suppress("UNCHECKED_CAST")
        return toResult(k as K, v as V)
    }
}

class MapEntrySerializer<K, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
        KeyValueSerializer<K, V, Map.Entry<K, V>>(kSerializer, vSerializer) {
    override val serialClassDesc = MapEntryClassDesc
    override fun toResult(key: K, value: V): Map.Entry<K, V> = MapEntry(key, value)

    override val Map.Entry<K, V>.key: K
        get() = this.key
    override val Map.Entry<K, V>.value: V
        get() = this.value
}

class PairSerializer<K, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
        KeyValueSerializer<K, V, Pair<K, V>>(kSerializer, vSerializer) {
    override val serialClassDesc = PairClassDesc
    override fun toResult(key: K, value: V) = key to value

    override val Pair<K, V>.key: K
        get() = this.first
    override val Pair<K, V>.value: V
        get() = this.second
}

// ============================= class descriptors =============================

sealed class ListLikeDesc : KSerialClassDesc {
    override fun getElementName(index: Int): String = if (index == SIZE_INDEX) "size" else index.toString()
    override fun getElementIndex(name: String): Int = if (name == "size") SIZE_INDEX else name.toInt()
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

object PairClassDesc: SerialClassDescImpl("kotlin.Pair") {
    init {
        addElement("first")
        addElement("second")
    }
}

class TripleSerializer<A, B, C>(
        private val aSerializer: KSerializer<A>,
        private val bSerializer: KSerializer<B>,
        private val cSerializer: KSerializer<C>
) : KSerializer<Triple<A, B, C>> {
    object TripleDesc: SerialClassDescImpl("kotlin.Triple") {
        init {
            addElement("first")
            addElement("second")
            addElement("third")
        }
    }

    override val serialClassDesc: KSerialClassDesc = TripleDesc

    override fun save(output: KOutput, obj: Triple<A, B, C>) {
        @Suppress("NAME_SHADOWING")
        val output = output.writeBegin(serialClassDesc, aSerializer, bSerializer, cSerializer)
        output.writeSerializableElementValue(serialClassDesc, 0, aSerializer, obj.first)
        output.writeSerializableElementValue(serialClassDesc, 1, bSerializer, obj.second)
        output.writeSerializableElementValue(serialClassDesc, 2, cSerializer, obj.third)
        output.writeEnd(serialClassDesc)
    }

    override fun load(input: KInput): Triple<A, B, C> {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(serialClassDesc, aSerializer, bSerializer, cSerializer)
        var aSet = false
        var bSet = false
        var cSet = false
        var a: Any? = null
        var b: Any? = null
        var c: Any? = null
        mainLoop@ while (true) {
            when (input.readElement(serialClassDesc)) {
                READ_ALL -> {
                    a = input.readSerializableElementValue(serialClassDesc, 0, aSerializer)
                    aSet = true
                    b = input.readSerializableElementValue(serialClassDesc, 1, bSerializer)
                    bSet = true
                    c = input.readSerializableElementValue(serialClassDesc, 2, cSerializer)
                    cSet = true
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    a = input.readSerializableElementValue(serialClassDesc, 0, aSerializer)
                    aSet = true
                }
                1 -> {
                    b = input.readSerializableElementValue(serialClassDesc, 1, bSerializer)
                    bSet = true
                }
                2 -> {
                    c = input.readSerializableElementValue(serialClassDesc, 2, cSerializer)
                    cSet = true
                }
                else -> throw SerializationException("Invalid index")
            }
        }
        input.readEnd(serialClassDesc)
        if (!aSet) throw SerializationException("Required first is missing")
        if (!bSet) throw SerializationException("Required second is missing")
        if (!cSet) throw SerializationException("Required third is missing")
        @Suppress("UNCHECKED_CAST")
        return Triple(a as A, b as B, c as C)
    }
}