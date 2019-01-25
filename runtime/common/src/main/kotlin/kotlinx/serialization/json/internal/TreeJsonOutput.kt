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

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.collections.set

internal fun <T> Json.writeJson(value: T, serializer: SerializationStrategy<T>): JsonElement {
    lateinit var result: JsonElement
    val encoder = JsonTreeOutput(this) { result = it }
    encoder.encode(serializer, value)
    return result
}

private sealed class AbstractJsonTreeOutput(
    override val json: Json,
    val nodeConsumer: (JsonElement) -> Unit
) : NamedValueEncoder(), JsonOutput {

    override val context: SerialModule
        get() = json.context

    private var writePolymorphic = false

    override fun encodeJson(element: JsonElement) {
        encodeSerializableValue(JsonElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean = json.encodeDefaults
    override fun composeName(parentName: String, childName: String): String = childName
    abstract fun putElement(key: String, element: JsonElement)
    abstract fun getCurrent(): JsonElement

    override fun encodeTaggedNull(tag: String) = putElement(tag, JsonNull)

    override fun encodeTaggedInt(tag: String, value: Int) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedByte(tag: String, value: Byte) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedShort(tag: String, value: Short) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedLong(tag: String, value: Long) = putElement(tag, JsonLiteral(value))

    override fun encodeTaggedFloat(tag: String, value: Float) {
        if (json.strictMode && !value.isFinite()) {
            throw JsonInvalidValueInStrictModeException(value)
        }

        putElement(tag, JsonLiteral(value))
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        encodePolymorphically(serializer, value) {
            writePolymorphic = true
        }
    }

    override fun encodeTaggedDouble(tag: String, value: Double) {
        if (json.strictMode && !value.isFinite()) {
            throw JsonInvalidValueInStrictModeException(value)
        }

        putElement(tag, JsonLiteral(value))
    }

    override fun encodeTaggedBoolean(tag: String, value: Boolean) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedChar(tag: String, value: Char) = putElement(tag, JsonLiteral(value.toString()))
    override fun encodeTaggedString(tag: String, value: String) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedEnum(
        tag: String,
        enumDescription: EnumDescriptor,
        ordinal: Int
    ) = putElement(tag, JsonLiteral(enumDescription.getElementName(ordinal)))

    override fun encodeTaggedValue(tag: String, value: Any) {
        putElement(tag, JsonLiteral(value.toString()))
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        val consumer =
            if (currentTagOrNull == null) nodeConsumer
            else { node -> putElement(currentTag, node) }

        val encoder = when (desc.kind) {
            StructureKind.LIST, UnionKind.POLYMORPHIC -> JsonTreeListOutput(json, consumer)
            StructureKind.MAP -> JsonTreeMapOutput(json, consumer)
            else -> JsonTreeOutput(json, consumer)
        }

        if (writePolymorphic) {
            writePolymorphic = false
            encoder.putElement(json.classDiscriminator, JsonPrimitive(desc.name))
        }

        return encoder
    }

    override fun endEncode(desc: SerialDescriptor) {
        nodeConsumer(getCurrent())
    }
}

private open class JsonTreeOutput(final override val json: Json, nodeConsumer: (JsonElement) -> Unit) :
    AbstractJsonTreeOutput(json, nodeConsumer) {

    protected val content: MutableMap<String, JsonElement> = linkedMapOf()

    override fun putElement(key: String, element: JsonElement) {
        content[key] = element
    }

    override fun getCurrent(): JsonElement = JsonObject(content)
}

private class JsonTreeMapOutput(json: Json, nodeConsumer: (JsonElement) -> Unit) : JsonTreeOutput(json, nodeConsumer) {
    private lateinit var tag: String

    override fun putElement(key: String, element: JsonElement) {
        val idx = key.toInt()
        if (idx % 2 == 0) { // writing key
            check(element is JsonLiteral) { "Expected JsonLiteral, but has $element" }
            tag = element.content
        } else {
            content[tag] = element
        }
    }

    override fun getCurrent(): JsonElement {
        return JsonObject(content)
    }

    override fun shouldWriteElement(desc: SerialDescriptor, tag: String, index: Int): Boolean = true
}

private class JsonTreeListOutput(json: Json, nodeConsumer: (JsonElement) -> Unit) :
    AbstractJsonTreeOutput(json, nodeConsumer) {
    private val array: ArrayList<JsonElement> = arrayListOf()

    override fun shouldWriteElement(desc: SerialDescriptor, tag: String, index: Int): Boolean = true

    override fun putElement(key: String, element: JsonElement) {
        val idx = key.toInt()
        array.add(idx, element)
    }

    override fun getCurrent(): JsonElement = JsonArray(array)
}

@Suppress("USELESS_CAST") // Contracts does not work in K/N
internal inline fun <reified T : JsonElement> cast(obj: JsonElement): T {
    check(obj is T) { "Expected ${T::class} but found ${obj::class}" }
    return obj as T
}
