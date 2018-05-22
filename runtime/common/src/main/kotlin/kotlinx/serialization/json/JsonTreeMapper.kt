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

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.SIZE_INDEX
import kotlin.reflect.KClass

class JsonTreeMapper(val context: SerialContext? = null) {
    inline fun <reified T : Any> readTree(tree: JsonElement): T = readTree(tree, context.klassSerializer(T::class))

    fun <T> readTree(obj: JsonElement, loader: DeserializationStrategy<T>): T {
        if (obj !is JsonObject) throw SerializationException("Can't deserialize primitive on root level")
        return JsonTreeInput(obj).decode(loader)
    }

    fun <T> writeTree(obj: T, saver: SerializationStrategy<T>): JsonElement {
        lateinit var result: JsonElement
        val output = JsonTreeOutput { result = it }
        output.encode(saver, obj)
        return result
    }

    private abstract inner class AbstractJsonTreeOutput(val nodeConsumer: (JsonElement) -> Unit) : NamedValueEncoder() {
        init {
            this.context = this@JsonTreeMapper.context
        }

        override fun composeName(parentName: String, childName: String): String = childName

        abstract fun putElement(key: String, element: JsonElement)
        abstract fun getCurrent(): JsonElement

        override fun encodeTaggedNull(tag: String) = putElement(tag, JsonNull)

        override fun encodeTaggedInt(tag: String, value: Int) = putElement(tag, JsonLiteral(value))
        override fun encodeTaggedByte(tag: String, value: Byte) = putElement(tag, JsonLiteral(value))
        override fun encodeTaggedShort(tag: String, value: Short) = putElement(tag, JsonLiteral(value))
        override fun encodeTaggedLong(tag: String, value: Long) = putElement(tag, JsonLiteral(value))
        override fun encodeTaggedFloat(tag: String, value: Float) = putElement(tag, JsonLiteral(value))
        override fun encodeTaggedDouble(tag: String, value: Double) = putElement(tag, JsonLiteral(value))
        override fun encodeTaggedBoolean(tag: String, value: Boolean) = putElement(tag, JsonLiteral(value))

        override fun encodeTaggedChar(tag: String, value: Char) = putElement(tag, JsonLiteral(value.toString()))
        override fun encodeTaggedString(tag: String, value: String) = putElement(tag, JsonLiteral(value))
        override fun <E : Enum<E>> encodeTaggedEnum(tag: String, enumClass: KClass<E>, value: E) = putElement(tag, JsonLiteral(value.toString()))

        override fun encodeTaggedValue(tag: String, value: Any) {
            putElement(tag, JsonLiteral(value.toString()))
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            val consumer =
                if (currentTagOrNull == null) nodeConsumer
                else { node -> putElement(currentTag, node) }
            return when (desc.kind) {
                KSerialClassKind.LIST, KSerialClassKind.SET -> JsonTreeListOutput(consumer)
                KSerialClassKind.MAP -> JsonTreeMapOutput(consumer)
                KSerialClassKind.ENTRY -> JsonTreeEntryOutput(this@AbstractJsonTreeOutput::putElement)
                else -> JsonTreeOutput(consumer)
            }
        }

        override fun endEncode(desc: SerialDescriptor) {
            nodeConsumer(getCurrent())
        }
    }

    private open inner class JsonTreeOutput(nodeConsumer: (JsonElement) -> Unit) :
        AbstractJsonTreeOutput(nodeConsumer) {
        private val map: MutableMap<String, JsonElement> = hashMapOf()

        override fun putElement(key: String, element: JsonElement) {
            map[key] = element
        }

        override fun getCurrent(): JsonElement = JsonObject(map)
    }

    private inner class JsonTreeMapOutput(nodeConsumer: (JsonElement) -> Unit) : JsonTreeOutput(nodeConsumer) {
        override fun shouldWriteElement(desc: SerialDescriptor, tag: String, index: Int): Boolean = index != SIZE_INDEX
    }

    private inner class JsonTreeListOutput(nodeConsumer: (JsonElement) -> Unit) : AbstractJsonTreeOutput(nodeConsumer) {
        private val array: ArrayList<JsonElement> = arrayListOf()

        override fun shouldWriteElement(desc: SerialDescriptor, tag: String, index: Int): Boolean = index != SIZE_INDEX

        override fun putElement(key: String, element: JsonElement) {
            val idx = key.toInt() - 1
            array.add(idx, element)
        }

        override fun getCurrent(): JsonElement = JsonArray(array)
    }

    private inner class JsonTreeEntryOutput(val entryConsumer: (String, JsonElement) -> Unit) :
        AbstractJsonTreeOutput({ throw IllegalStateException("Use entryConsumer instead") }) {

        private lateinit var elem: JsonElement
        private lateinit var tag: String

        override fun putElement(key: String, element: JsonElement) {
            if (key != "key") {
                elem = element
            } else {
                check(element is JsonLiteral) { "Expected tag to be JsonLiteral" }
                tag = (element as JsonLiteral).content
            }
        }

        override fun getCurrent(): JsonElement = elem

        override fun endEncode(desc: SerialDescriptor) {
            entryConsumer(tag, elem)
        }
    }

    private abstract inner class AbstractJsonTreeInput(open val obj: JsonElement): NamedValueDecoder() {
        init {
            this.context = this@JsonTreeMapper.context
        }

        override fun composeName(parentName: String, childName: String): String = childName

        private inline fun <reified T: JsonElement> checkCast(obj: JsonElement): T {
            check(obj is T) { "Expected ${T::class} but found ${obj::class}" }
            return obj as T
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            val curObj = currentTagOrNull?.let { currentElement(it) } ?: obj
            return when (desc.kind) {
                KSerialClassKind.LIST, KSerialClassKind.SET -> JsonTreeListInput(checkCast(curObj))
                KSerialClassKind.MAP -> JsonTreeMapInput(checkCast(curObj))
                KSerialClassKind.ENTRY -> JsonTreeMapEntryInput(curObj, currentTag)
                else -> JsonTreeInput(checkCast(curObj))
            }
        }

        protected open fun getValue(tag: String): JsonPrimitive {
            val currentElement = currentElement(tag)
            return currentElement as? JsonPrimitive ?: throw SerializationException("Expected from $tag to be primitive but found $currentElement")
        }

        protected abstract fun currentElement(tag: String): JsonElement

        override fun decodeTaggedChar(tag: String): Char {
            val o = getValue(tag)
            return if (o.content.length == 1) o.content[0] else throw SerializationException("$o can't be represented as Char")
        }

        override fun <E : Enum<E>> decodeTaggedEnum(tag: String, enumLoader: EnumCreator<E>): E =
            enumLoader.createFromName(getValue(tag).content)

        override fun decodeTaggedNull(tag: String): Nothing? = null
        override fun decodeTaggedNotNullMark(tag: String) = currentElement(tag) !== JsonNull

        override fun decodeTaggedUnit(tag: String) {
            return
        }

        override fun decodeTaggedBoolean(tag: String): Boolean = getValue(tag).boolean
        override fun decodeTaggedByte(tag: String): Byte = getValue(tag).int.toByte()
        override fun decodeTaggedShort(tag: String) = getValue(tag).int.toShort()
        override fun decodeTaggedInt(tag: String) = getValue(tag).int
        override fun decodeTaggedLong(tag: String) = getValue(tag).long
        override fun decodeTaggedFloat(tag: String) = getValue(tag).float
        override fun decodeTaggedDouble(tag: String) = getValue(tag).double
        override fun decodeTaggedString(tag: String) = getValue(tag).content
    }

    private open inner class JsonTreeInput(override val obj: JsonObject) : AbstractJsonTreeInput(obj) {

        private var pos = 0

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < desc.associatedFieldsCount) {
                val name = desc.getTag(pos++)
                if (name in obj) return pos - 1
            }
            return READ_DONE
        }

        override fun currentElement(tag: String): JsonElement = obj.getValue(tag)

    }

    private inner class JsonTreeMapEntryInput(override val obj: JsonElement, val cTag: String): AbstractJsonTreeInput(obj) {

        override fun currentElement(tag: String): JsonElement = if (tag == "key") {
            JsonLiteral(cTag)
        } else {
            check(tag == "value") { "Found unexpected tag: $tag" }
            obj
        }
    }

    private inner class JsonTreeMapInput(override val obj: JsonObject): JsonTreeInput(obj) {

        private val keys = obj.keys.toList()
        private val size: Int = keys.size
        private var pos = 0

        override fun elementName(desc: SerialDescriptor, index: Int): String {
            val i = index - 1
            return keys[i]
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < size) {
                pos++
                return pos
            }
            return READ_DONE
        }
    }

    private inner class JsonTreeListInput(override val obj: JsonArray): AbstractJsonTreeInput(obj) {

        override fun currentElement(tag: String): JsonElement {
            return obj[tag.toInt()]
        }

        private val size = obj.content.size
        private var pos = 0 // 0st element is SIZE. use it?

        override fun elementName(desc: SerialDescriptor, index: Int): String = (index - 1).toString()

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            while (pos < size) {
                pos++
                return pos
            }
            return READ_DONE
        }
    }
}
