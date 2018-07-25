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

package kotlinx.serialization

object Mapper {

    class OutMapper : NamedValueEncoder() {
        override fun beginCollection(
            desc: SerialDescriptor,
            collectionSize: Int,
            vararg typeParams: KSerializer<*>
        ): CompositeEncoder {
            encodeTaggedInt(nested("size"), collectionSize)
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

    class OutNullableMapper : NamedValueEncoder() {
        private var _map: MutableMap<String, Any?> = mutableMapOf()

        val map: Map<String, Any?>
            get() = _map

        override fun beginCollection(
            desc: SerialDescriptor,
            collectionSize: Int,
            vararg typeParams: KSerializer<*>
        ): CompositeEncoder {
            encodeTaggedInt(nested("size"), collectionSize)
            return this
        }

        override fun encodeTaggedValue(tag: String, value: Any) {
            _map[tag] = value
        }

        override fun encodeTaggedNull(tag: String) {
            _map[tag] = null
        }
    }

    class InMapper(val map: Map<String, Any>) : NamedValueDecoder() {
        override fun decodeCollectionSize(desc: SerialDescriptor): Int {
            return decodeTaggedInt(nested("size"))
        }

        override fun decodeTaggedValue(tag: String): Any = map.getValue(tag)
    }

    class InNullableMapper(val map: Map<String, Any?>) : NamedValueDecoder() {
        override fun decodeCollectionSize(desc: SerialDescriptor): Int {
            return decodeTaggedInt(nested("size"))
        }

        override fun decodeTaggedValue(tag: String): Any = map.getValue(tag)!!

        override fun decodeTaggedNotNullMark(tag: String): Boolean = map.getValue(tag) != null
    }

    inline fun <reified T : Any> map(obj: T): Map<String, Any> {
        val m = OutMapper()
        m.encode(obj)
        return m.map
    }

    inline fun <reified T : Any> mapNullable(obj: T): Map<String, Any?> {
        val m = OutNullableMapper()
        m.encode(obj)
        return m.map
    }

    inline fun <reified T : Any> unmap(map: Map<String, Any>): T {
        val m = InMapper(map)
        return m.decode()
    }

    inline fun <reified T : Any> unmapNullable(map: Map<String, Any?>): T {
        val m = InNullableMapper(map)
        return m.decode()
    }
}
