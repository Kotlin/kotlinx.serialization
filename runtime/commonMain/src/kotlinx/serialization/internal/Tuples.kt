/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*

private val NULL = Any()

internal sealed class KeyValueSerializer<K, V, R>(
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>
) : KSerializer<R> {

    abstract val R.key: K
    abstract val R.value: V
    abstract fun toResult(key: K, value: V): R

    override fun serialize(encoder: Encoder, obj: R) {
        val structuredEncoder = encoder.beginStructure(descriptor, keySerializer, valueSerializer)
        structuredEncoder.encodeSerializableElement(descriptor, 0, keySerializer, obj.key)
        structuredEncoder.encodeSerializableElement(descriptor, 1, valueSerializer, obj.value)
        structuredEncoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): R {
        val composite = decoder.beginStructure(descriptor, keySerializer, valueSerializer)
        if (composite.decodeSequentially()) {
            val key = composite.decodeSerializableElement(descriptor, 0, keySerializer)
            val value = composite.decodeSerializableElement(descriptor, 1, valueSerializer)
            return toResult(key, value)
        }

        var key: Any? = NULL
        var value: Any? = NULL
        mainLoop@ while (true) {
            when (val idx = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    key = composite.decodeSerializableElement(descriptor, 0, keySerializer)
                }
                1 -> {
                    value = composite.decodeSerializableElement(descriptor, 1, valueSerializer)
                }
                else -> throw SerializationException("Invalid index: $idx")
            }
        }
        composite.endStructure(descriptor)
        if (key === NULL) throw SerializationException("Element 'key' is missing")
        if (value === NULL) throw SerializationException("Element 'value' is missing")
        @Suppress("UNCHECKED_CAST")
        return toResult(key as K, value as V)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
internal class MapEntrySerializer<K, V>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
) : KeyValueSerializer<K, V, Map.Entry<K, V>>(keySerializer, valueSerializer) {
    private object MapEntryClassDesc : SerialClassDescImpl("kotlin.collections.Map.Entry") {
        override val kind = StructureKind.MAP

        init {
            addElement("key")
            addElement("value")
        }
    }

    override val descriptor: SerialDescriptor = MapEntryClassDesc
    override val Map.Entry<K, V>.key: K get() = this.key
    override val Map.Entry<K, V>.value: V get() = this.value
    override fun toResult(key: K, value: V): Map.Entry<K, V> = MapEntry(key, value)
}

internal class PairSerializer<K, V>(
    ketSerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
) : KeyValueSerializer<K, V, Pair<K, V>>(ketSerializer, valueSerializer) {
    private object PairClassDesc : SerialClassDescImpl("kotlin.Pair") {
        init {
            addElement("first")
            addElement("second")
        }
    }

    override val descriptor: SerialDescriptor get() = PairClassDesc
    override val Pair<K, V>.key: K get() = this.first
    override val Pair<K, V>.value: V get() = this.second

    override fun toResult(key: K, value: V) = key to value
}

private data class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

internal class TripleSerializer<A, B, C>(
    private val aSerializer: KSerializer<A>,
    private val bSerializer: KSerializer<B>,
    private val cSerializer: KSerializer<C>
) : KSerializer<Triple<A, B, C>> {
    private object TripleDesc : SerialClassDescImpl("kotlin.Triple") {
        init {
            addElement("first")
            addElement("second")
            addElement("third")
        }
    }

    override val descriptor: SerialDescriptor get() = TripleDesc

    override fun serialize(encoder: Encoder, obj: Triple<A, B, C>) {
        val structuredEncoder = encoder.beginStructure(descriptor, aSerializer, bSerializer, cSerializer)
        structuredEncoder.encodeSerializableElement(descriptor, 0, aSerializer, obj.first)
        structuredEncoder.encodeSerializableElement(descriptor, 1, bSerializer, obj.second)
        structuredEncoder.encodeSerializableElement(descriptor, 2, cSerializer, obj.third)
        structuredEncoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Triple<A, B, C> {
        val composite = decoder.beginStructure(descriptor, aSerializer, bSerializer, cSerializer)
        if (composite.decodeSequentially()) {
            return decodeSequentially(composite)
        }
        return decodeStructure(composite)
    }

    private fun decodeSequentially(composite: CompositeDecoder): Triple<A, B, C> {
        val a = composite.decodeSerializableElement(descriptor, 0, aSerializer)
        val b = composite.decodeSerializableElement(descriptor, 1, bSerializer)
        val c = composite.decodeSerializableElement(descriptor, 2, cSerializer)
        composite.endStructure(descriptor)
        return Triple(a, b, c)
    }

    private fun decodeStructure(composite: CompositeDecoder): Triple<A, B, C> {
        var a: Any? = NULL
        var b: Any? = NULL
        var c: Any? = NULL
        mainLoop@ while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    a = composite.decodeSerializableElement(descriptor, 0, aSerializer)
                }
                1 -> {
                    b = composite.decodeSerializableElement(descriptor, 1, bSerializer)
                }
                2 -> {
                    c = composite.decodeSerializableElement(descriptor, 2, cSerializer)
                }
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        composite.endStructure(descriptor)
        if (a === NULL) throw SerializationException("Element 'first' is missing")
        if (b === NULL) throw SerializationException("Element 'second' is missing")
        if (c === NULL) throw SerializationException("Element 'third' is missing")
        @Suppress("UNCHECKED_CAST")
        return Triple(a as A, b as B, c as C)
    }
}
