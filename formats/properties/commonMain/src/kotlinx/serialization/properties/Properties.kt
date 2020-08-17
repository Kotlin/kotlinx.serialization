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

    private inner class OutMapper : NamedValueEncoder() {
        override val serializersModule: SerializersModule = this@Properties.serializersModule

        internal val map: MutableMap<String, Any> = mutableMapOf()

        override fun encodeTaggedValue(tag: String, value: Any) {
            map[tag] = value
        }

        override fun encodeTaggedNull(tag: String) {
            // ignore nulls in output
        }

        override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
            map[tag] = enumDescriptor.getElementName(ordinal)
        }
    }

    private inner class InMapper(
        private val map: Map<String, Any>, descriptor: SerialDescriptor
    ) : NamedValueDecoder() {
        override val serializersModule: SerializersModule = this@Properties.serializersModule

        private var currentIndex = 0
        private val isCollection = descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP
        private val size = if (isCollection) Int.MAX_VALUE else descriptor.elementsCount

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return InMapper(map, descriptor).also { copyTagsTo(it) }
        }

        override fun decodeTaggedValue(tag: String): Any {
            return map.getValue(tag)
        }

        override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
            return when (val taggedValue = map.getValue(tag)) {
                is Int -> taggedValue
                is String -> enumDescriptor.getElementIndex(taggedValue)
                else -> throw SerializationException("Value of enum entry '$tag' is neither an Int nor a String")
            }
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
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

    /**
     * Encodes properties from the given [value] to a map using the given [serializer].
     * `null` values are omitted from the output.
     */
    @ExperimentalSerializationApi
    public fun <T> encodeToMap(serializer: SerializationStrategy<T>, value: T): Map<String, Any> {
        val m = OutMapper()
        m.encodeSerializableValue(serializer, value)
        return m.map
    }

    /**
     * Decodes properties from the given [map] to a value of type [T] using the given [deserializer].
     * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
     */
    @ExperimentalSerializationApi
    public fun <T> decodeFromMap(deserializer: DeserializationStrategy<T>, map: Map<String, Any>): T {
        val m = InMapper(map, deserializer.descriptor)
        return m.decodeSerializableValue(deserializer)
    }

    @Deprecated(removedMsg, level = DeprecationLevel.ERROR)
    public fun <T> storeNullable(strategy: SerializationStrategy<T>, value: T): Map<String, Any?> = noImpl()

    @Deprecated(removedMsg, level = DeprecationLevel.ERROR)
    public fun <T> loadNullable(strategy: DeserializationStrategy<T>, map: Map<String, Any?>): T = noImpl()

    @Deprecated(removedMsg, level = DeprecationLevel.ERROR)
    public inline fun <reified T : Any> storeNullable(value: T): Map<String, Any?> =
        noImpl()

    @Deprecated(removedMsg, level = DeprecationLevel.ERROR)
    public inline fun <reified T : Any> loadNullable(map: Map<String, Any?>): T = noImpl()

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
 * Decodes properties from given [map], assigns them to an object using serializer for reified type [T] and returns this object.
 * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Properties.decodeFromMap(map: Map<String, Any>): T =
    decodeFromMap(serializersModule.serializer(), map)

// Migrations below

@PublishedApi
internal fun noImpl(): Nothing = throw UnsupportedOperationException("Not implemented, should not be called")

@Deprecated(renamedMsg, ReplaceWith("this.encodeToMap(strategy, value)"), DeprecationLevel.ERROR)
public fun <T> Properties.store(strategy: SerializationStrategy<T>, value: T): Map<String, Any> = encodeToMap(strategy, value)

@Deprecated(renamedMsg, ReplaceWith("this.decodeFromMap(strategy, map)"), DeprecationLevel.ERROR)
public fun <T> Properties.load(strategy: DeserializationStrategy<T>, map: Map<String, Any>): T = decodeFromMap(strategy, map)

@Deprecated(renamedMsg, ReplaceWith("this.encodeToMap(value)"), DeprecationLevel.ERROR)
public inline fun <reified T : Any> Properties.store(value: T): Map<String, Any> = encodeToMap(value)

@Deprecated(renamedMsg, ReplaceWith("this.decodeFromMap(map)"), DeprecationLevel.ERROR)
public inline fun <reified T : Any> Properties.load(map: Map<String, Any>): T = decodeFromMap(map)


internal const val renamedMsg = "This method was renamed during serialization 1.0 API stabilization"
internal const val removedMsg =
    "This method was removed without replacement during serialization 1.0 API stabilization due to unclear amount of use-cases. " +
            "If you have a compelling use-case for it, please report to our issue tracker."
