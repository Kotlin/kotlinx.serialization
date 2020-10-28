/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName", "DeprecatedCallableAddReplaceWith")

package kotlinx.serialization.properties

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

/**
 * Transforms a [Serializable] class' properties into a single flat [Map] consisting of
 * string keys and primitive type values, and vice versa.
 *
 * If the given class has non-primitive property `d` of arbitrary type `D`, `D` values are inserted
 * into the same map; keys for such values are prefixed with string `d.`:
 *
 * ```
 * @Serializable
 * class Data(val property1: String)
 *
 * @Serializable
 * class DataHolder(val data: Data, val property2: String)
 *
 * val map = Properties.store(DataHolder(Data("value1"), "value2"))
 * // map contents will be the following:
 * // property2 = value2
 * // data.property1 = value1
 * ```
 *
 * If the given class has a [List] property `l`, each value from the list
 * would be prefixed with `l.N.`, where N is an index for a particular value.
 * [Map] is treated as a `[key,value,...]` list.
 *
 * @param serializersModule A [SerializersModule] which should contain registered serializers
 * for [Contextual] and [Polymorphic] serialization, if you have any.
 */
@ExperimentalSerializationApi
public sealed class Properties(
    override val serializersModule: SerializersModule,
    ctorMarker: Nothing?
) : SerialFormat {

    private abstract inner class OutMapper<Value : Any> : NamedValueEncoder() {
        override val serializersModule: SerializersModule = this@Properties.serializersModule

        val map: MutableMap<String, Value> = mutableMapOf()

        protected abstract fun encode(value: Any): Value

        override fun encodeTaggedValue(tag: String, value: Any) {
            map[tag] = encode(value)
        }

        override fun encodeTaggedNull(tag: String) {
            // ignore nulls in output
        }

        override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
            map[tag] = encode(enumDescriptor.getElementName(ordinal))
        }
    }

    private inner class OutAnyMapper : OutMapper<Any>() {
        override fun encode(value: Any): Any = value
    }

    private inner class OutStringMapper : OutMapper<String>() {
        override fun encode(value: Any): String = value.toString()
    }

    private abstract inner class InMapper<Value : Any>(
        protected val map: Map<String, Value>, descriptor: SerialDescriptor
    ) : NamedValueDecoder() {
        override val serializersModule: SerializersModule = this@Properties.serializersModule

        private var currentIndex = 0
        private val isCollection = descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP
        private val size = if (isCollection) Int.MAX_VALUE else descriptor.elementsCount

        protected abstract fun structure(descriptor: SerialDescriptor): InMapper<Value>

        final override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return structure(descriptor).also { copyTagsTo(it) }
        }

        final override fun decodeTaggedValue(tag: String): Value {
            return map.getValue(tag)
        }

        final override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
            return when (val taggedValue = map.getValue(tag)) {
                is Int -> taggedValue
                is String -> enumDescriptor.getElementIndex(taggedValue)
                else -> throw SerializationException("Value of enum entry '$tag' is neither an Int nor a String")
            }
        }

        final override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (currentIndex < size) {
                val name = descriptor.getTag(currentIndex++)
                if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
                if (isCollection) {
                    // if map does not contain key we look for, then indices in collection have ended
                    break
                }
            }
            return CompositeDecoder.DECODE_DONE
        }
    }

    private inner class InAnyMapper(
        map: Map<String, Any>, descriptor: SerialDescriptor
    ) : InMapper<Any>(map, descriptor) {
        override fun structure(descriptor: SerialDescriptor): InAnyMapper =
            InAnyMapper(map, descriptor)
    }

    private inner class InStringMapper(
        map: Map<String, String>, descriptor: SerialDescriptor
    ) : InMapper<String>(map, descriptor) {
        override fun structure(descriptor: SerialDescriptor): InStringMapper =
            InStringMapper(map, descriptor)

        override fun decodeTaggedBoolean(tag: String): Boolean = decodeTaggedValue(tag).toBoolean()
        override fun decodeTaggedByte(tag: String): Byte = decodeTaggedValue(tag).toByte()
        override fun decodeTaggedShort(tag: String): Short = decodeTaggedValue(tag).toShort()
        override fun decodeTaggedInt(tag: String): Int = decodeTaggedValue(tag).toInt()
        override fun decodeTaggedLong(tag: String): Long = decodeTaggedValue(tag).toLong()
        override fun decodeTaggedFloat(tag: String): Float = decodeTaggedValue(tag).toFloat()
        override fun decodeTaggedDouble(tag: String): Double = decodeTaggedValue(tag).toDouble()
        override fun decodeTaggedChar(tag: String): Char = decodeTaggedValue(tag).single()
    }

    /**
     * Encodes properties from the given [value] to a map using the given [serializer].
     * `null` values are omitted from the output.
     */
    @ExperimentalSerializationApi
    public fun <T> encodeToMap(serializer: SerializationStrategy<T>, value: T): Map<String, Any> {
        val m = OutAnyMapper()
        m.encodeSerializableValue(serializer, value)
        return m.map
    }

    /**
     * Encodes properties from the given [value] to a map using the given [serializer].
     * Converts all primitive types to [String] using [toString] method.
     * `null` values are omitted from the output.
     */
    @ExperimentalSerializationApi
    public fun <T> encodeToStringMap(serializer: SerializationStrategy<T>, value: T): Map<String, String> {
        val m = OutStringMapper()
        m.encodeSerializableValue(serializer, value)
        return m.map
    }

    /**
     * Decodes properties from the given [map] to a value of type [T] using the given [deserializer].
     * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
     */
    @ExperimentalSerializationApi
    public fun <T> decodeFromMap(deserializer: DeserializationStrategy<T>, map: Map<String, Any>): T {
        val m = InAnyMapper(map, deserializer.descriptor)
        return m.decodeSerializableValue(deserializer)
    }

    /**
     * Decodes properties from the given [map] to a value of type [T] using the given [deserializer].
     * [String] values are converted to respective primitive types using default conversion methods.
     * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
     */
    @ExperimentalSerializationApi
    public fun <T> decodeFromStringMap(deserializer: DeserializationStrategy<T>, map: Map<String, String>): T {
        val m = InStringMapper(map, deserializer.descriptor)
        return m.decodeSerializableValue(deserializer)
    }

    /**
     * A [Properties] instance that can be used as default and does not have any [SerializersModule] installed.
     */
    @ExperimentalSerializationApi
    public companion object Default : Properties(EmptySerializersModule, null)
}

@OptIn(ExperimentalSerializationApi::class)
private class PropertiesImpl(serializersModule: SerializersModule) : Properties(serializersModule, null)

/**
 * Creates an instance of [Properties] with a given [module].
 */
@ExperimentalSerializationApi
public fun Properties(module: SerializersModule): Properties = PropertiesImpl(module)

/**
 * Encodes properties from given [value] to a map using serializer for reified type [T] and returns this map.
 * `null` values are omitted from the output.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Properties.encodeToMap(value: T): Map<String, Any> =
    encodeToMap(serializersModule.serializer(), value)

/**
 * Encodes properties from given [value] to a map using serializer for reified type [T] and returns this map.
 * Converts all primitive types to [String] using [toString] method.
 * `null` values are omitted from the output.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Properties.encodeToStringMap(value: T): Map<String, String> =
    encodeToStringMap(serializersModule.serializer(), value)

/**
 * Decodes properties from given [map], assigns them to an object using serializer for reified type [T] and returns this object.
 * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Properties.decodeFromMap(map: Map<String, Any>): T =
    decodeFromMap(serializersModule.serializer(), map)

/**
 * Decodes properties from given [map], assigns them to an object using serializer for reified type [T] and returns this object.
 * [String] values are converted to respective primitive types using default conversion methods.
 * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Properties.decodeFromStringMap(map: Map<String, String>): T =
    decodeFromStringMap(serializersModule.serializer(), map)

// Migrations below

@PublishedApi
internal fun noImpl(): Nothing = throw UnsupportedOperationException("Not implemented, should not be called")
