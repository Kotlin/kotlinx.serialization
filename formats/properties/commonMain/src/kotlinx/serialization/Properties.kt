/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

/**
 * Transforms a [Serializable] class' properties into a single flat [Map] which consists of
 * string keys and primitive type values, and vice versa. Located in separated `kotlinx-serialization-properties` artifact.
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
 * Additional `l.size` property with a list size would be added.
 * [Map] is treated as a [key,value,...] list.
 *
 * @param context A [SerialModule] which should contain registered serializers
 * for [ContextualSerialization] and [Polymorphic] serialization, if you have any.
 */
public class Properties(override val context: SerialModule = EmptyModule) : SerialFormat {

    private inner class OutMapper : NamedValueEncoder() {
        override val serializersModule: SerialModule = this@Properties.context

        internal val map: MutableMap<String, Any> = mutableMapOf()

        override fun beginCollection(
            descriptor: SerialDescriptor,
            collectionSize: Int
        ): CompositeEncoder {
            // todo: decide whether this is responsibility of the format
            //       OR beginCollection should pass collectionSize = 2 * size in case of maps
            val size = if (descriptor.kind is StructureKind.MAP) collectionSize * 2 else collectionSize
            encodeTaggedInt(nested("size"), size)
            return this
        }

        override fun encodeTaggedValue(tag: String, value: Any) {
            map[tag] = value
        }

        override fun encodeTaggedNull(tag: String) {
            // ignore nulls in output
        }
    }

    private inner class OutNullableMapper : NamedValueEncoder() {
        override val serializersModule: SerialModule = this@Properties.context

        internal val map: MutableMap<String, Any?> = mutableMapOf()

        override fun beginCollection(
            descriptor: SerialDescriptor,
            collectionSize: Int
        ): CompositeEncoder {
            val size = if (descriptor.kind is StructureKind.MAP) collectionSize * 2 else collectionSize
            encodeTaggedInt(nested("size"), size)
            return this
        }

        override fun encodeTaggedValue(tag: String, value: Any) {
            map[tag] = value
        }

        override fun encodeTaggedNull(tag: String) {
            map[tag] = null
        }
    }

    private inner class InMapper(private val map: Map<String, Any>) : NamedValueDecoder() {
        override val serializersModule: SerialModule = this@Properties.context

        private var currentIndex = 0

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return InMapper(map).also { copyTagsTo(it) }
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
            return decodeTaggedInt(nested("size"))
        }

        override fun decodeTaggedValue(tag: String): Any {
            return map.getValue(tag)
        }

        override fun decodeTaggedEnum(tag: String, enumDescription: SerialDescriptor): Int {
            return when (val taggedValue = map.getValue(tag)) {
                is Int -> taggedValue
                is String -> enumDescription.getElementIndex(taggedValue)
                else -> throw SerializationException("Value of enum entry '$tag' is neither an Int nor a String")
            }
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            val tag = nested("size")
            val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
            while (currentIndex < size) {
                val name = descriptor.getTag(currentIndex++)
                if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
            }
            return CompositeDecoder.DECODE_DONE
        }
    }

    private inner class InNullableMapper(val map: Map<String, Any?>) : NamedValueDecoder() {
        override val serializersModule: SerialModule = this@Properties.context

        private var currentIndex = 0

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return InNullableMapper(map).also { copyTagsTo(it) }
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
            return decodeTaggedInt(nested("size"))
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            val tag = nested("size")
            val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
            while (currentIndex < size) {
                val name = descriptor.getTag(currentIndex++)
                if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
            }
            return CompositeDecoder.DECODE_DONE
        }

        override fun decodeTaggedValue(tag: String): Any = map.getValue(tag)!!

        override fun decodeTaggedEnum(tag: String, enumDescription: SerialDescriptor): Int {
            return when (val taggedValue = map.getValue(tag)!!) {
                is Int -> taggedValue
                is String -> enumDescription.getElementIndex(taggedValue)
                else -> throw SerializationException("Value of enum entry '$tag' is neither an Int nor a String")
            }
        }

        override fun decodeTaggedNotNullMark(tag: String): Boolean {
            return tag !in map || // in case of complex object, its fields are
                    // prefixed with dot and there are no 'clean' tag with object name.
                    // Invalid tags can be handled later, in .decodeValue
                    map.getValue(tag) != null
        }
    }

    /**
     * Stores properties from given [value] to a map and returns this map.
     * `null` values are omitted from the output.
     */
    public fun <T> store(strategy: SerializationStrategy<T>, value: T): Map<String, Any> {
        val m = OutMapper()
        m.encodeSerializableValue(strategy, value)
        return m.map
    }

    /**
     * Stores properties from given [value] into a map and returns this map.
     * This method writes `null` values to the map, if they are present in [value].
     */
    public fun <T> storeNullable(strategy: SerializationStrategy<T>, value: T): Map<String, Any?> {
        val m = OutNullableMapper()
        m.encodeSerializableValue(strategy, value)
        return m.map
    }

    /**
     * Loads properties from given [map], assigns them to an object and returns this object.
     * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
     */
    public fun <T> load(strategy: DeserializationStrategy<T>, map: Map<String, Any>): T {
        val m = InMapper(map)
        return m.decodeSerializableValue(strategy)
    }

    /**
     * Loads properties from given [map], assigns them to an object and returns this object.
     * Writes `null` values from [map] to nullable properties in [T].
     */
    public fun <T> loadNullable(strategy: DeserializationStrategy<T>, map: Map<String, Any?>): T {
        val m = InNullableMapper(map)
        return m.decodeSerializableValue(strategy)
    }

    /**
     * A reified version of [store].
     */
    public inline fun <reified T : Any> store(value: T): Map<String, Any> =
        store(context.getContextualOrDefault(), value)

    /**
     * A reified version of [storeNullable].
     */
    public inline fun <reified T : Any> storeNullable(value: T): Map<String, Any?> =
        storeNullable(context.getContextualOrDefault(), value)

    /**
     * A reified version of [load].
     */
    public inline fun <reified T : Any> load(map: Map<String, Any>): T =
        load(context.getContextualOrDefault(), map)

    /**
     * A reified version of [loadNullable].
     */
    public inline fun <reified T : Any> loadNullable(map: Map<String, Any?>): T =
        loadNullable(context.getContextualOrDefault(), map)

    /**
     * A top-level [SerialFormat] instance that mimic an instance of [Properties] and does not have any [SerialModule] installed.
     */
    public companion object Default : SerialFormat {

        override val context: SerialModule
            get() = DEFAULT.context

        @PublishedApi
        internal val DEFAULT: Properties = Properties()

        /**
         * Shorthand for [Properties.store] call on a [DEFAULT] instance of [Properties], which
         * does not have any [SerialModule]s installed.
         */
        public fun <T> store(strategy: SerializationStrategy<T>, value: T): Map<String, Any> =
            DEFAULT.store(strategy, value)

        /**
         * Shorthand for [Properties.storeNullable] call on a [DEFAULT] instance of [Properties], which
         * does not have any [SerialModule]s installed.
         */
        public fun <T> storeNullable(strategy: SerializationStrategy<T>, value: T): Map<String, Any?> =
            DEFAULT.storeNullable(strategy, value)

        /**
         * Shorthand for [Properties.load] call on a [DEFAULT] instance of [Properties], which
         * does not have any [SerialModule]s installed.
         */
        public fun <T> load(strategy: DeserializationStrategy<T>, map: Map<String, Any>): T =
            DEFAULT.load(strategy, map)

        /**
         * Shorthand for [Properties.loadNullable] call on a [DEFAULT] instance of [Properties], which
         * does not have any [SerialModule]s installed.
         */
        public fun <T> loadNullable(strategy: DeserializationStrategy<T>, map: Map<String, Any?>): T =
            DEFAULT.loadNullable(strategy, map)

        /**
         * A reified version of [store].
         */
        public inline fun <reified T : Any> store(value: T): Map<String, Any> = DEFAULT.store(value)

        /**
         * A reified version of [storeNullable].
         */
        public inline fun <reified T : Any> storeNullable(value: T): Map<String, Any?> = DEFAULT.storeNullable(value)

        /**
         * A reified version of [load].
         */
        public inline fun <reified T : Any> load(map: Map<String, Any>): T = DEFAULT.load(map)

        /**
         * A reified version of [loadNullable].
         */
        public inline fun <reified T : Any> loadNullable(map: Map<String, Any?>): T = DEFAULT.loadNullable(map)
    }
}
