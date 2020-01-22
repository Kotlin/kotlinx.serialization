/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlin.jvm.*
import kotlin.reflect.*

@InternalSerializationApi
sealed class AbstractCollectionSerializer<Element, Collection, Builder> : KSerializer<Collection> {
    protected abstract fun Collection.collectionSize(): Int
    protected abstract fun Collection.collectionIterator(): Iterator<Element>
    protected abstract fun builder(): Builder
    protected abstract fun Builder.builderSize(): Int
    protected abstract fun Builder.toResult(): Collection
    protected abstract fun Collection.toBuilder(): Builder
    protected abstract fun Builder.checkCapacity(size: Int)

    abstract val typeParams: Array<KSerializer<*>>

    abstract override fun serialize(encoder: Encoder, value: Collection)

    final override fun patch(decoder: Decoder, old: Collection): Collection {
        val builder = old.toBuilder()
        val startIndex = builder.builderSize()
        val compositeDecoder = decoder.beginStructure(descriptor, *typeParams)
        if (compositeDecoder.decodeSequentially()) {
            readAll(compositeDecoder, builder, startIndex, readSize(compositeDecoder, builder))
        } else {
            while (true) {
                val index = compositeDecoder.decodeElementIndex(descriptor)
                if (index == READ_DONE) break
                readElement(compositeDecoder, startIndex + index, builder)
            }
        }
        compositeDecoder.endStructure(descriptor)
        return builder.toResult()
    }

    override fun deserialize(decoder: Decoder): Collection {
        val builder = builder()
        return patch(decoder, builder.toResult())
    }

    private fun readSize(decoder: CompositeDecoder, builder: Builder): Int {
        val size = decoder.decodeCollectionSize(descriptor)
        builder.checkCapacity(size)
        return size
    }

    protected abstract fun readElement(decoder: CompositeDecoder, index: Int, builder: Builder, checkIndex: Boolean = true)

    protected abstract fun readAll(decoder: CompositeDecoder, builder: Builder, startIndex: Int, size: Int)
}

sealed class ListLikeSerializer<Element, Collection, Builder>(
    private val elementSerializer: KSerializer<Element>
) : AbstractCollectionSerializer<Element, Collection, Builder>() {

    abstract fun Builder.insert(index: Int, element: Element)
    abstract override val descriptor: ListLikeDescriptor

    final override val typeParams: Array<KSerializer<*>> = arrayOf(elementSerializer)

    override fun serialize(encoder: Encoder, value: Collection) {
        val size = value.collectionSize()
        @Suppress("NAME_SHADOWING")
        val encoder = encoder.beginCollection(descriptor, size, *typeParams)
        val iterator = value.collectionIterator()
        for (index in 0 until size)
            encoder.encodeSerializableElement(descriptor, index, elementSerializer, iterator.next())
        encoder.endStructure(descriptor)
    }

    protected final override fun readAll(decoder: CompositeDecoder, builder: Builder, startIndex: Int, size: Int) {
        require(size >= 0) { "Size must be known in advance when using READ_ALL" }
        for (index in 0 until size)
            readElement(decoder, startIndex + index, builder, checkIndex = false)
    }

    protected override fun readElement(decoder: CompositeDecoder, index: Int, builder: Builder, checkIndex: Boolean) {
        builder.insert(index, decoder.decodeSerializableElement(descriptor, index, elementSerializer))
    }
}

sealed class MapLikeSerializer<Key, Value, Collection, Builder : MutableMap<Key, Value>>(
    @JvmField val keySerializer: KSerializer<Key>,
    @JvmField val valueSerializer: KSerializer<Value>
) : AbstractCollectionSerializer<Map.Entry<Key, Value>, Collection, Builder>() {

    abstract fun Builder.insertKeyValuePair(index: Int, key: Key, value: Value)
    abstract override val descriptor: MapLikeDescriptor

    final override val typeParams = arrayOf(keySerializer, valueSerializer)

    protected final override fun readAll(decoder: CompositeDecoder, builder: Builder, startIndex: Int, size: Int) {
        require(size >= 0) { "Size must be known in advance when using READ_ALL" }
        for (index in 0 until size * 2 step 2)
            readElement(decoder, startIndex + index, builder, checkIndex = false)
    }

    final override fun readElement(decoder: CompositeDecoder, index: Int, builder: Builder, checkIndex: Boolean) {
        val key: Key = decoder.decodeSerializableElement(descriptor, index, keySerializer)
        val vIndex = if (checkIndex) {
            decoder.decodeElementIndex(descriptor).also {
                require(it == index + 1) { "Value must follow key in a map, index for key: $index, returned index for value: $it" }
            }
        } else {
            index + 1
        }
        val value: Value = if (builder.containsKey(key) && valueSerializer.descriptor.kind !is PrimitiveKind) {
            decoder.updateSerializableElement(descriptor, vIndex, valueSerializer, builder.getValue(key))
        } else {
            decoder.decodeSerializableElement(descriptor, vIndex, valueSerializer)
        }
        builder[key] = value
    }

    override fun serialize(encoder: Encoder, value: Collection) {
        val size = value.collectionSize()
        @Suppress("NAME_SHADOWING")
        val encoder = encoder.beginCollection(descriptor, size, *typeParams)
        val iterator = value.collectionIterator()
        var index = 0
        iterator.forEach { (k, v) ->
            encoder.encodeSerializableElement(descriptor, index++, keySerializer, k)
            encoder.encodeSerializableElement(descriptor, index++, valueSerializer, v)
        }
        encoder.endStructure(descriptor)
    }
}

@InternalSerializationApi
public abstract class PrimitiveArrayBuilder<Array> internal constructor() {
    abstract val position: Int
    abstract fun ensureCapacity(requiredCapacity: Int = position + 1)
    abstract fun build(): Array
}

/**
 * Base serializer for all serializers for primitive arrays.
 *
 * It exists only to avoid code duplication and should not be used or implemented directly.
 * Use concrete serializers ([ByteArraySerializer], etc) instead.
 */
@InternalSerializationApi
public abstract class PrimitiveArraySerializer<Element, Array, Builder : PrimitiveArrayBuilder<Array>>
internal constructor(
    primitiveSerializer: KSerializer<Element>
) : ListLikeSerializer<Element, Array, Builder>(primitiveSerializer) {
    final override val descriptor: PrimitiveArrayDescriptor = PrimitiveArrayDescriptor(primitiveSerializer.descriptor)

    final override fun Builder.builderSize() = position
    final override fun Builder.toResult(): Array = build()
    final override fun Builder.checkCapacity(size: Int) = ensureCapacity(size)

    final override fun Array.collectionIterator(): Iterator<Element> =
        error("This method lead to boxing and must not be used, use writeContents instead")

    final override fun Builder.insert(index: Int, element: Element): Unit =
        error("This method lead to boxing and must not be used, use Builder.append instead")

    final override fun builder(): Builder = error("Use empty().toBuilder() instead")

    protected abstract fun empty(): Array

    protected abstract override fun readElement(
        decoder: CompositeDecoder,
        index: Int,
        builder: Builder,
        checkIndex: Boolean
    )

    protected abstract fun writeContent(encoder: CompositeEncoder, content: Array, size: Int)

    final override fun serialize(encoder: Encoder, value: Array) {
        val size = value.collectionSize()
        @Suppress("NAME_SHADOWING")
        val encoder = encoder.beginCollection(descriptor, size, *typeParams)
        writeContent(encoder, value, size)
        encoder.endStructure(descriptor)
    }

    final override fun deserialize(decoder: Decoder): Array {
        // here we use empty() instead of builder().toResult() in AbstractCollectionSerializer
        // because, unlike with ArrayLists, transformation builder(initialSize) > array > builder
        // requires additional allocations
        return patch(decoder, empty())
    }
}

// todo: can be more efficient when array size is know in advance, this one always uses temporary ArrayList as builder
class ReferenceArraySerializer<ElementKlass : Any, Element : ElementKlass?>(
    private val kClass: KClass<ElementKlass>,
    eSerializer: KSerializer<Element>
) :
    ListLikeSerializer<Element, Array<Element>, ArrayList<Element>>(eSerializer) {
    override val descriptor = ArrayClassDesc(eSerializer.descriptor)

    override fun Array<Element>.collectionSize(): Int = size
    override fun Array<Element>.collectionIterator(): Iterator<Element> = iterator()
    override fun builder(): ArrayList<Element> = arrayListOf()
    override fun ArrayList<Element>.builderSize(): Int = size
    @Suppress("UNCHECKED_CAST")
    override fun ArrayList<Element>.toResult(): Array<Element> = toNativeArray<ElementKlass, Element>(kClass)

    override fun Array<Element>.toBuilder(): ArrayList<Element> = ArrayList(this.asList())
    override fun ArrayList<Element>.checkCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<Element>.insert(index: Int, element: Element) {
        add(index, element)
    }
}

class ArrayListSerializer<E>(element: KSerializer<E>) : ListLikeSerializer<E, List<E>, ArrayList<E>>(element) {
    override val descriptor = ArrayListClassDesc(element.descriptor)

    override fun List<E>.collectionSize(): Int = size
    override fun List<E>.collectionIterator(): Iterator<E> = iterator()
    override fun builder(): ArrayList<E> = arrayListOf()
    override fun ArrayList<E>.builderSize(): Int = size
    override fun ArrayList<E>.toResult(): List<E> = this
    override fun List<E>.toBuilder(): ArrayList<E> = this as? ArrayList<E> ?: ArrayList(this)
    override fun ArrayList<E>.checkCapacity(size: Int) = ensureCapacity(size)
    override fun ArrayList<E>.insert(index: Int, element: E) { add(index, element) }
}

class LinkedHashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, LinkedHashSet<E>>(eSerializer) {
    override val descriptor = LinkedHashSetClassDesc(eSerializer.descriptor)

    override fun Set<E>.collectionSize(): Int = size
    override fun Set<E>.collectionIterator(): Iterator<E> = iterator()
    override fun builder(): LinkedHashSet<E> = linkedSetOf()
    override fun LinkedHashSet<E>.builderSize(): Int = size
    override fun LinkedHashSet<E>.toResult(): Set<E> = this
    override fun Set<E>.toBuilder(): LinkedHashSet<E> = this as? LinkedHashSet<E> ?: LinkedHashSet(this)
    override fun LinkedHashSet<E>.checkCapacity(size: Int) {}
    override fun LinkedHashSet<E>.insert(index: Int, element: E) { add(element) }
}

class HashSetSerializer<E>(eSerializer: KSerializer<E>) : ListLikeSerializer<E, Set<E>, HashSet<E>>(eSerializer) {
    override val descriptor = HashSetClassDesc(eSerializer.descriptor)

    override fun Set<E>.collectionSize(): Int = size
    override fun Set<E>.collectionIterator(): Iterator<E> = iterator()
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

    override fun Map<K, V>.collectionSize(): Int = size
    override fun Map<K, V>.collectionIterator(): Iterator<Map.Entry<K, V>> = iterator()
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

    override fun Map<K, V>.collectionSize(): Int = size
    override fun Map<K, V>.collectionIterator(): Iterator<Map.Entry<K, V>> = iterator()
    override fun builder(): HashMap<K, V> = HashMap()
    override fun HashMap<K, V>.builderSize(): Int = size
    override fun HashMap<K, V>.toResult(): Map<K, V> = this
    override fun Map<K, V>.toBuilder(): HashMap<K, V> = this as? HashMap<K, V> ?: HashMap(this)
    override fun HashMap<K, V>.checkCapacity(size: Int) {}
    override fun HashMap<K, V>.insertKeyValuePair(index: Int, key: K, value: V) = set(key, value)
}
