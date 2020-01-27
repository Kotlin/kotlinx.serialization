/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.modules.*


/**
 * Transforms a [Serializable] class' properties into a single flat [Map] which consists of
 * string keys and primitive type values, and vice versa.
 *
 * If the given class has non-primitive property `d` of arbitrary type `D`, `D` values are inserted
 * into the same map; keys for such values are prefixed with string `d.`:
 *
 * ```kotlin
 * @Serializable
 * class Data(val property1: String)
 *
 * @Serializable
 * class DataHolder(val data: Data, val property2: String)
 *
 * val map = Mapper.store(DataHolder(Data("value1"), "value2"))
 * // map contents will be the following:
 * // property2 = value2
 * // data.property1 = value1
 * ```
 *
 * If the given class has a [List] property `l`, each value from the list
 * would be prefixed with `l.N.`, where N is an index for particular value.
 * Additional `l.size` property with a list size would be added.
 * [Map] is treated as a [key,value,...] list.
 *
 * @param context A [SerialModule] which should contain registered serializers
 * for [ContextualSerialization] and [Polymorphic] serialization, if you have any.
 */
public class Properties(context: SerialModule = EmptyModule) : AbstractSerialFormat(context) {

    internal inner class OutMapper : NamedValueEncoder() {
        override val context: SerialModule = this@Properties.context

        override fun beginCollection(
            desc: SerialDescriptor,
            collectionSize: Int,
            vararg typeParams: KSerializer<*>
        ): CompositeEncoder {
            // todo: decide whether this is responsibility of the format
            //       OR beginCollection should pass collectionSize = 2 * size in case of maps
            val size = if (desc.kind is StructureKind.MAP) collectionSize * 2 else collectionSize
            encodeTaggedInt(nested("size"), size)
            return this
        }

        private var _map: MutableMap<String, Any> = mutableMapOf()

        val map: Map<String, Any>
            get() = _map

        override fun encodeTaggedValue(tag: String, value: Any) {
            _map[tag] = value
        }

        override fun encodeTaggedNull(tag: String) {
            throw SerializationException("null is not supported. use Mapper.mapNullable()/OutNullableMapper instead")
        }
    }

    internal inner class OutNullableMapper : NamedValueEncoder() {
        override val context: SerialModule = this@Properties.context

        internal val map: MutableMap<String, Any?> = mutableMapOf()

        override fun beginCollection(
            desc: SerialDescriptor,
            collectionSize: Int,
            vararg typeParams: KSerializer<*>
        ): CompositeEncoder {
            val size = if (desc.kind is StructureKind.MAP) collectionSize * 2 else collectionSize
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

    internal inner class InMapper(private val map: Map<String, Any>) : NamedValueDecoder() {
        private var currentIndex = 0
        override val context: SerialModule = this@Properties.context

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            return InMapper(map).also { copyTagsTo(it) }
        }

        override fun decodeCollectionSize(desc: SerialDescriptor): Int {
            return decodeTaggedInt(nested("size"))
        }

        override fun decodeTaggedValue(tag: String): Any {
            return map.getValue(tag)
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            val tag = nested("size")
            val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
            while (currentIndex < size) {
                val name = descriptor.getTag(currentIndex++)
                if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
            }
            return READ_DONE
        }
    }

    internal inner class InNullableMapper(val map: Map<String, Any?>) : NamedValueDecoder() {
        override val context: SerialModule = this@Properties.context
        private var currentIndex = 0

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            return InNullableMapper(map).also { copyTagsTo(it) }
        }

        override fun decodeCollectionSize(desc: SerialDescriptor): Int {
            return decodeTaggedInt(nested("size"))
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            val tag = nested("size")
            val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
            while (currentIndex < size) {
                val name = descriptor.getTag(currentIndex++)
                if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
            }
            return READ_DONE
        }

        override fun decodeTaggedValue(tag: String): Any = map.getValue(tag)!!

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
        m.encode(strategy, value)
        return m.map
    }

    /**
     * Stores properties from given [value] into a map and returns this map.
     * This method writes `null` values to the map, if they are present in [value].
     */
    public fun <T> storeNullable(strategy: SerializationStrategy<T>, value: T): Map<String, Any?> {
        val m = OutNullableMapper()
        m.encode(strategy, value)
        return m.map
    }

    /**
     * Loads properties from given [map], assigns them to an object and returns this object.
     * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
     */
    public fun <T> load(strategy: DeserializationStrategy<T>, map: Map<String, Any>): T {
        val m = InMapper(map)
        return m.decode(strategy)
    }

    /**
     * Loads properties from given [map], assigns them to an object and returns this object.
     * Writes `null` values from [map] to nullable properties in [T].
     */
    public fun <T> loadNullable(strategy: DeserializationStrategy<T>, map: Map<String, Any?>): T {
        val m = InNullableMapper(map)
        return m.decode(strategy)
    }

    /**
     * A reified version of [store].
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> store(obj: T): Map<String, Any> =
        store(context.getContextualOrDefault(T::class), obj)

    /**
     * A reified version of [storeNullable].
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> storeNullable(obj: T): Map<String, Any?> =
        storeNullable(context.getContextualOrDefault(T::class), obj)

    /**
     * A reified version of [load].
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> load(map: Map<String, Any>): T =
        load(context.getContextualOrDefault(T::class), map)

    /**
     * A reified version of [loadNullable].
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> loadNullable(map: Map<String, Any?>): T =
        loadNullable(context.getContextualOrDefault(T::class), map)

    public companion object {
        /**
         * A [Properties] instance which
         * does not have any [SerialModule]s installed.
         */
        public val DEFAULT: Properties = Properties()

        /**
         * Shorthand for [Properties.store] call on a [DEFAULT] instance of [Properties], which
         * does not have any [SerialModule]s installed.
         */
        public fun <T> store(strategy: SerializationStrategy<T>, obj: T): Map<String, Any> =
            DEFAULT.store(strategy, obj)

        /**
         * Shorthand for [Properties.storeNullable] call on a [DEFAULT] instance of [Properties], which
         * does not have any [SerialModule]s installed.
         */
        public fun <T> storeNullable(strategy: SerializationStrategy<T>, obj: T): Map<String, Any?> =
            DEFAULT.storeNullable(strategy, obj)

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
        @ImplicitReflectionSerializer
        public inline fun <reified T : Any> store(obj: T): Map<String, Any> = DEFAULT.store(obj)

        /**
         * A reified version of [storeNullable].
         */
        @ImplicitReflectionSerializer
        public inline fun <reified T : Any> storeNullable(obj: T): Map<String, Any?> = DEFAULT.storeNullable(obj)

        /**
         * A reified version of [load].
         */
        @ImplicitReflectionSerializer
        public inline fun <reified T : Any> load(map: Map<String, Any>): T = DEFAULT.load(map)

        /**
         * A reified version of [loadNullable].
         */
        @ImplicitReflectionSerializer
        public inline fun <reified T : Any> loadNullable(map: Map<String, Any?>): T = DEFAULT.loadNullable(map)
    }
}
