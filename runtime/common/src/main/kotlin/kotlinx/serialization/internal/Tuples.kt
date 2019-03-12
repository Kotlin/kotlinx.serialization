package kotlinx.serialization.internal

import kotlinx.serialization.*


const val KEY_INDEX = 0
const val VALUE_INDEX = 1

sealed class KeyValueSerializer<K, V, R>(val kSerializer: KSerializer<K>, val vSerializer: KSerializer<V>) :
    KSerializer<R> {
    abstract override val descriptor: SerialDescriptor
    abstract fun toResult(key: K, value: V): R
    abstract val R.key: K
    abstract val R.value: V

    override fun serialize(encoder: Encoder, obj: R) {
        @Suppress("NAME_SHADOWING")
        val encoder = encoder.beginStructure(descriptor, kSerializer, vSerializer)
        encoder.encodeSerializableElement(descriptor, KEY_INDEX, kSerializer, obj.key)
        encoder.encodeSerializableElement(descriptor, VALUE_INDEX, vSerializer, obj.value)
        encoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): R {
        @Suppress("NAME_SHADOWING")
        val input = decoder.beginStructure(descriptor, kSerializer, vSerializer)
        var kSet = false
        var vSet = false
        var k: Any? = null
        var v: Any? = null
        mainLoop@ while (true) {
            when (input.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_ALL -> {
                    k = readKey(input)
                    kSet = true
                    v = readValue(input, k, kSet)
                    vSet = true
                    break@mainLoop
                }
                CompositeDecoder.READ_DONE -> {
                    break@mainLoop
                }
                KEY_INDEX -> {
                    k = readKey(input)
                    kSet = true
                }
                VALUE_INDEX -> {
                    v = readValue(input, k, kSet)
                    vSet = true
                }
                else -> throw SerializationException("Invalid index")
            }
        }
        input.endStructure(descriptor)
        if (!kSet) throw SerializationException("Required key is missing")
        if (!vSet) throw SerializationException("Required value is missing")
        @Suppress("UNCHECKED_CAST")
        return toResult(k as K, v as V)
    }

    protected open fun readKey(decoder: CompositeDecoder): K {
        return decoder.decodeSerializableElement(descriptor, KEY_INDEX, kSerializer)
    }

    protected open fun readValue(decoder: CompositeDecoder, k: Any?, kSet: Boolean): V {
        return decoder.decodeSerializableElement(descriptor, VALUE_INDEX, vSerializer)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
class MapEntryUpdatingSerializer<K, V>(mSerializer: MapEntrySerializer<K, V>, private val mapBuilder: MutableMap<K, V>) :
    KeyValueSerializer<K, V, Map.Entry<K, V>>(mSerializer.kSerializer, mSerializer.vSerializer) {

    override val descriptor = MapEntryClassDesc
    override fun toResult(key: K, value: V): Map.Entry<K, V> = MapEntry(key, value)

    override fun readValue(decoder: CompositeDecoder, k: Any?, kSet: Boolean): V {
        if (!kSet) throw SerializationException("Key must be before value in serialization stream")
        @Suppress("UNCHECKED_CAST")
        val key = k as K
        val v = if (mapBuilder.containsKey(key) && vSerializer.descriptor.kind !is PrimitiveKind) {
            decoder.updateSerializableElement(descriptor, VALUE_INDEX, vSerializer, mapBuilder.getValue(key))
        } else {
            decoder.decodeSerializableElement(descriptor, VALUE_INDEX, vSerializer)
        }
        mapBuilder[key] = v
        return v
    }

    override val Map.Entry<K, V>.key: K
        get() = this.key
    override val Map.Entry<K, V>.value: V
        get() = this.value

}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
class MapEntrySerializer<K, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
    KeyValueSerializer<K, V, Map.Entry<K, V>>(kSerializer, vSerializer) {
    override val descriptor = MapEntryClassDesc
    override fun toResult(key: K, value: V): Map.Entry<K, V> = MapEntry(key, value)

    override val Map.Entry<K, V>.key: K
        get() = this.key
    override val Map.Entry<K, V>.value: V
        get() = this.value
}

class PairSerializer<K, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
    KeyValueSerializer<K, V, Pair<K, V>>(kSerializer, vSerializer) {
    override val descriptor = PairClassDesc
    override fun toResult(key: K, value: V) = key to value

    override val Pair<K, V>.key: K
        get() = this.first
    override val Pair<K, V>.value: V
        get() = this.second
}

data class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

object MapEntryClassDesc : SerialClassDescImpl("kotlin.collections.Map.Entry") {
    override val kind = StructureKind.MAP

    init {
        addElement("key")
        addElement("value")
    }
}

object PairClassDesc : SerialClassDescImpl("kotlin.Pair") {
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
    object TripleDesc : SerialClassDescImpl("kotlin.Triple") {
        init {
            addElement("first")
            addElement("second")
            addElement("third")
        }
    }

    override val descriptor: SerialDescriptor = TripleDesc

    override fun serialize(encoder: Encoder, obj: Triple<A, B, C>) {
        @Suppress("NAME_SHADOWING")
        val encoder = encoder.beginStructure(descriptor, aSerializer, bSerializer, cSerializer)
        encoder.encodeSerializableElement(descriptor, 0, aSerializer, obj.first)
        encoder.encodeSerializableElement(descriptor, 1, bSerializer, obj.second)
        encoder.encodeSerializableElement(descriptor, 2, cSerializer, obj.third)
        encoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Triple<A, B, C> {
        @Suppress("NAME_SHADOWING")
        val input = decoder.beginStructure(descriptor, aSerializer, bSerializer, cSerializer)
        var aSet = false
        var bSet = false
        var cSet = false
        var a: Any? = null
        var b: Any? = null
        var c: Any? = null
        mainLoop@ while (true) {
            when (input.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_ALL -> {
                    a = input.decodeSerializableElement(descriptor, 0, aSerializer)
                    aSet = true
                    b = input.decodeSerializableElement(descriptor, 1, bSerializer)
                    bSet = true
                    c = input.decodeSerializableElement(descriptor, 2, cSerializer)
                    cSet = true
                    break@mainLoop
                }
                CompositeDecoder.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    a = input.decodeSerializableElement(descriptor, 0, aSerializer)
                    aSet = true
                }
                1 -> {
                    b = input.decodeSerializableElement(descriptor, 1, bSerializer)
                    bSet = true
                }
                2 -> {
                    c = input.decodeSerializableElement(descriptor, 2, cSerializer)
                    cSet = true
                }
                else -> throw SerializationException("Invalid index")
            }
        }
        input.endStructure(descriptor)
        if (!aSet) throw SerializationException("Required first is missing")
        if (!bSet) throw SerializationException("Required second is missing")
        if (!cSet) throw SerializationException("Required third is missing")
        @Suppress("UNCHECKED_CAST")
        return Triple(a as A, b as B, c as C)
    }
}
