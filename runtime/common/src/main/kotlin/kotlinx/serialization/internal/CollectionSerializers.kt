/*
 * Copyright 2018 JetBrains s.r.o.
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
import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlin.reflect.KClass

sealed class AbstractCollectionSerializer<TElement, TCollection, TBuilder>: KSerializer<TCollection> {
    abstract fun TCollection.objSize(): Int
    abstract fun TCollection.objIterator(): Iterator<TElement>
    abstract fun builder(): TBuilder
    abstract fun TBuilder.builderSize(): Int
    abstract fun TBuilder.toResult(): TCollection
    abstract fun TCollection.toBuilder(): TBuilder
    abstract fun TBuilder.checkCapacity(size: Int)

    abstract val typeParams: Array<KSerializer<*>>

    abstract override fun serialize(output: Encoder, obj: TCollection)

    final override fun patch(input: Decoder, old: TCollection): TCollection {
        val builder = old.toBuilder()
        val startIndex = builder.builderSize()
        @Suppress("NAME_SHADOWING")
        val input = input.beginStructure(descriptor, *typeParams)
        readSize(input, builder)
        mainLoop@ while (true) {
            val index = input.decodeElementIndex(descriptor)
            when (index) {
                READ_ALL -> {
                    readAll(input, builder, startIndex)
                    break@mainLoop
                }
                READ_DONE -> break@mainLoop
                else -> readItem(input, startIndex + index, builder)
            }

        }
        input.endStructure(descriptor)
        return builder.toResult()
    }

    final override fun deserialize(input: Decoder): TCollection {
        val builder = builder()
        return patch(input, builder.toResult())
    }

    private fun readSize(input: CompositeDecoder, builder: TBuilder): Int {
        val size = input.decodeCollectionSize(descriptor)
        builder.checkCapacity(size)
        return size
    }

    protected abstract fun readItem(input: CompositeDecoder, index: Int, builder: TBuilder)

    private fun readAll(input: CompositeDecoder, builder: TBuilder, startIndex: Int) {
        val size = readSize(input, builder)
        require(size >= 0) { "Size must be known in advance when using READ_ALL" }
        for (index in 0 until size)
            readItem(input, startIndex + index, builder)
    }
}

sealed class ListLikeSerializer<TElement, TCollection, TBuilder>(private val eSerializer: KSerializer<TElement>) :
    AbstractCollectionSerializer<TElement, TCollection, TBuilder>() {

    abstract fun TBuilder.insert(index: Int, element: TElement)
    abstract override val descriptor: ListLikeDesc

    final override val typeParams: Array<KSerializer<*>> = arrayOf(eSerializer)

    override fun serialize(output: Encoder, obj: TCollection) {
        val size = obj.objSize()
        @Suppress("NAME_SHADOWING")
        val output = output.beginCollection(descriptor, size, *typeParams)
        val iterator = obj.objIterator()
        for (index in 0 until size)
            output.encodeSerializableElement(descriptor, index, eSerializer, iterator.next())
        output.endStructure(descriptor)
    }

    protected override fun readItem(input: CompositeDecoder, index: Int, builder: TBuilder) {
        builder.insert(index, input.decodeSerializableElement(descriptor, index, eSerializer))
    }
}

sealed class MapLikeSerializer<TKey, TVal, TCollection, TBuilder: MutableMap<TKey, TVal>>(
    private val kSerializer: KSerializer<TKey>,
    private val vSerializer: KSerializer<TVal>
) : AbstractCollectionSerializer<Map.Entry<TKey, TVal>, TCollection, TBuilder>() {

    abstract fun TBuilder.insertKeyValuePair(index: Int, key: TKey, value: TVal)
    abstract override val descriptor: MapLikeDesc

    final override val typeParams = arrayOf(kSerializer, vSerializer)

    final override fun readItem(input: CompositeDecoder, index: Int, builder: TBuilder) {
        val key: TKey = input.decodeSerializableElement(descriptor, index, kSerializer)
        val vIndex = input.decodeElementIndex(descriptor)
        require(vIndex == index + 1) { "Value must follow key in a map, index for key: $index, returned index for value: $vIndex" }
        val value: TVal = if (builder.containsKey(key) && vSerializer.descriptor.kind !is PrimitiveKind) {
            input.updateSerializableElement(descriptor, vIndex, vSerializer, builder.getValue(key))
        } else {
            input.decodeSerializableElement(descriptor, vIndex, vSerializer)
        }
        builder[key] = value
    }

    override fun serialize(output: Encoder, obj: TCollection) {
        val size = obj.objSize()
        @Suppress("NAME_SHADOWING")
        val output = output.beginCollection(descriptor, size, *typeParams)
        val iterator = obj.objIterator()
        var index = 0
        iterator.forEach { (k, v) ->
            output.encodeSerializableElement(descriptor, index++, kSerializer, k)
            output.encodeSerializableElement(descriptor, index++, vSerializer, v)
        }
        output.endStructure(descriptor)
    }
}

// todo: can be more efficient when array size is know in advance, this one always uses temporary ArrayList as builder
class ReferenceArraySerializer<T: Any, E: T?>(private val kClass: KClass<T>, eSerializer: KSerializer<E>):
        ListLikeSerializer<E, Array<E>, ArrayList<E>>(eSerializer) {
    override val descriptor = ArrayClassDesc(eSerializer.descriptor)

    override fun Array<E>.objSize(): Int = size
    override fun Array<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): ArrayList<E> = arrayListOf()
    override fun ArrayList<E>.builderSize(): Int = size
    @Suppress("UNCHECKED_CAST")
    override fun ArrayList<E>.toResult(): Array<E> = toNativeArray<T, E>(kClass)
    override fun Array<E>.toBuilder(): ArrayList<E> = ArrayList(this.asList())
    override fun ArrayList<E>.checkCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<E>.insert(index: Int, element: E) { add(index, element) }
}

class ArrayListSerializer<E>(element: KSerializer<E>) : ListLikeSerializer<E, List<E>, ArrayList<E>>(element) {
    override val descriptor = ArrayListClassDesc(element.descriptor)

    override fun List<E>.objSize(): Int = size
    override fun List<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): ArrayList<E> = arrayListOf()
    override fun ArrayList<E>.builderSize(): Int = size
    override fun ArrayList<E>.toResult(): List<E> = this
    override fun List<E>.toBuilder(): ArrayList<E> = this as? ArrayList<E> ?: ArrayList(this)
    override fun ArrayList<E>.checkCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<E>.insert(index: Int, element: E) { add(index, element) }
}

class LinkedHashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, LinkedHashSet<E>>(eSerializer) {
    override val descriptor = LinkedHashSetClassDesc(eSerializer.descriptor)

    override fun Set<E>.objSize(): Int = size
    override fun Set<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): LinkedHashSet<E> = linkedSetOf()
    override fun LinkedHashSet<E>.builderSize(): Int = size
    override fun LinkedHashSet<E>.toResult(): Set<E> = this
    override fun Set<E>.toBuilder(): LinkedHashSet<E> = this as? LinkedHashSet<E> ?: LinkedHashSet(this)
    override fun LinkedHashSet<E>.checkCapacity(size: Int) {}
    override fun LinkedHashSet<E>.insert(index: Int, element: E) { add(element) }
}

class HashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, HashSet<E>>(eSerializer) {
    override val descriptor = HashSetClassDesc(eSerializer.descriptor)

    override fun Set<E>.objSize(): Int = size
    override fun Set<E>.objIterator(): Iterator<E> = iterator()
    override fun builder(): HashSet<E> = HashSet()
    override fun HashSet<E>.builderSize(): Int = size
    override fun HashSet<E>.toResult(): Set<E> = this
    override fun Set<E>.toBuilder(): HashSet<E> = this as? HashSet<E> ?: HashSet(this)
    override fun HashSet<E>.checkCapacity(size: Int) {}
    override fun HashSet<E>.insert(index: Int, element: E) { add(element) }
}

class LinkedHashMapSerializer<K, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
    MapLikeSerializer<K, V, Map<K, V>, LinkedHashMap<K, V>>(kSerializer, vSerializer) {
    override val descriptor = LinkedHashMapClassDesc(kSerializer.descriptor, vSerializer.descriptor)

    override fun Map<K, V>.objSize(): Int = size
    override fun Map<K, V>.objIterator(): Iterator<Map.Entry<K, V>> = iterator()
    override fun builder(): LinkedHashMap<K, V> = LinkedHashMap()
    override fun LinkedHashMap<K, V>.builderSize(): Int = size
    override fun LinkedHashMap<K, V>.toResult(): Map<K, V> = this
    override fun Map<K, V>.toBuilder(): LinkedHashMap<K, V> = this as? LinkedHashMap<K, V> ?: LinkedHashMap(this)
    override fun LinkedHashMap<K, V>.checkCapacity(size: Int) {}
    override fun LinkedHashMap<K, V>.insertKeyValuePair(index: Int, key: K, value: V) = set(key, value)
}

class HashMapSerializer<K, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
    MapLikeSerializer<K, V, Map<K, V>, HashMap<K, V>>(kSerializer, vSerializer) {
    override val descriptor = HashMapClassDesc(kSerializer.descriptor, vSerializer.descriptor)

    override fun Map<K, V>.objSize(): Int = size
    override fun Map<K, V>.objIterator(): Iterator<Map.Entry<K, V>> = iterator()
    override fun builder(): HashMap<K, V> = HashMap()
    override fun HashMap<K, V>.builderSize(): Int = size
    override fun HashMap<K, V>.toResult(): Map<K, V> = this
    override fun Map<K, V>.toBuilder(): HashMap<K, V> = this as? HashMap<K, V> ?: HashMap(this)
    override fun HashMap<K, V>.checkCapacity(size: Int) {}
    override fun HashMap<K, V>.insertKeyValuePair(index: Int, key: K, value: V) = set(key, value)
}
