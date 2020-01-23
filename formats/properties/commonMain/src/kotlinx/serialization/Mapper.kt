/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.modules.*

class Mapper(context: SerialModule = EmptyModule) : AbstractSerialFormat(context) {

    internal inner class OutMapper : NamedValueEncoder() {
        override val context: SerialModule = this@Mapper.context

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
        override val context: SerialModule = this@Mapper.context

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
        private var currentIndex = -1
        override val context: SerialModule = this@Mapper.context

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
            if (++currentIndex == size) {
                return READ_DONE
            }
            return currentIndex
        }
    }

    internal inner class InNullableMapper(val map: Map<String, Any?>) : NamedValueDecoder() {
        override val context: SerialModule = this@Mapper.context
        private var currentIndex = -1

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            return InNullableMapper(map).also { copyTagsTo(it) }
        }

        override fun decodeCollectionSize(desc: SerialDescriptor): Int {
            return decodeTaggedInt(nested("size"))
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            val tag = nested("size")
            val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
            if (++currentIndex == size) {
                return READ_DONE
            }
            return currentIndex
        }

        override fun decodeTaggedValue(tag: String): Any = map.getValue(tag)!!

        override fun decodeTaggedNotNullMark(tag: String): Boolean {
            return tag !in map || // in case of complex object, its fields are
                    // prefixed with dot and there are no 'clean' tag with object name.
                    // Invalid tags can be handled later, in .decodeValue
                    map.getValue(tag) != null
        }
    }

    fun <T> map(strategy: SerializationStrategy<T>, obj: T): Map<String, Any> {
        val m = OutMapper()
        m.encode(strategy, obj)
        return m.map
    }

    fun <T> mapNullable(strategy: SerializationStrategy<T>, obj: T): Map<String, Any?> {
        val m = OutNullableMapper()
        m.encode(strategy, obj)
        return m.map
    }

    fun <T> unmap(strategy: DeserializationStrategy<T>, map: Map<String, Any>): T {
        val m = InMapper(map)
        return m.decode(strategy)
    }

    fun <T> unmapNullable(strategy: DeserializationStrategy<T>, map: Map<String, Any?>): T {
        val m = InNullableMapper(map)
        return m.decode(strategy)
    }

    @ImplicitReflectionSerializer
    inline fun <reified T : Any> map(obj: T): Map<String, Any> = map(context.getContextualOrDefault(T::class), obj)

    @ImplicitReflectionSerializer
    inline fun <reified T : Any> mapNullable(obj: T): Map<String, Any?> =
        mapNullable(context.getContextualOrDefault(T::class), obj)

    @ImplicitReflectionSerializer
    inline fun <reified T : Any> unmap(map: Map<String, Any>): T = unmap(context.getContextualOrDefault(T::class), map)

    @ImplicitReflectionSerializer
    inline fun <reified T : Any> unmapNullable(map: Map<String, Any?>): T =
        unmapNullable(context.getContextualOrDefault(T::class), map)

    companion object {
        val default = Mapper()

        fun <T> map(strategy: SerializationStrategy<T>, obj: T): Map<String, Any> = default.map(strategy, obj)
        fun <T> mapNullable(strategy: SerializationStrategy<T>, obj: T): Map<String, Any?> =
            default.mapNullable(strategy, obj)
        fun <T> unmap(strategy: DeserializationStrategy<T>, map: Map<String, Any>): T = default.unmap(strategy, map)
        fun <T> unmapNullable(strategy: DeserializationStrategy<T>, map: Map<String, Any?>): T =
            default.unmapNullable(strategy, map)

        @ImplicitReflectionSerializer
        inline fun <reified T : Any> map(obj: T): Map<String, Any> = default.map(obj)
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> mapNullable(obj: T): Map<String, Any?> = default.mapNullable(obj)
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> unmap(map: Map<String, Any>): T = default.unmap(map)
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> unmapNullable(map: Map<String, Any?>): T = default.unmapNullable(map)
    }
}
