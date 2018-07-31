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

@file:Suppress("FunctionName")

package kotlinx.serialization.schema

import kotlinx.serialization.*
import kotlinx.serialization.json.*

internal val SerialDescriptor.jsonType
    get() = when (this.kind) {
        StructureKind.LIST -> "array"
        PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG,
        PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> "number"
        PrimitiveKind.STRING, PrimitiveKind.CHAR, UnionKind.ENUM_KIND -> "string"
        PrimitiveKind.BOOLEAN -> "boolean"
        else -> "object"
    }


/**
 * Creates an [JsonObject] which contains Json Schema of given [descriptor].
 *
 * Schema can contain following fields:
 * `description`, `type` for all descriptors;
 * `properties` and `required` for objects;
 * `enum` for enums;
 * `items` for arrays.
 *
 * User can modify this schema to add additional validation keywords
 * (as per [https://json-schema.org/latest/json-schema-validation.html])
 * if they want.
 */
fun JsonSchema(descriptor: SerialDescriptor): JsonObject {
    val properties: MutableMap<String, JsonObject> = mutableMapOf()
    val requiredProperties: MutableSet<String> = mutableSetOf()
    val isEnum = descriptor.kind == UnionKind.ENUM_KIND

    if (!isEnum) descriptor.elementDescriptors().forEachIndexed { index, child ->
        val elementName = descriptor.getElementName(index)
        properties[elementName] = child.accept(::JsonSchema)
        if (!descriptor.isElementOptional(index)) requiredProperties.add(elementName)
    }

    val jsonType = descriptor.jsonType
    val objectData: MutableMap<String, JsonElement> = mutableMapOf(
        "description" to JsonLiteral(descriptor.name),
        "type" to JsonLiteral(jsonType)
    )
    if (isEnum) {
        val allElementNames = (0 until descriptor.elementsCount).map(descriptor::getElementName)
        objectData += "enum" to JsonArray(allElementNames.map(::JsonLiteral))
    }
    when (jsonType) {
        "object" -> {
            objectData["properties"] = JsonObject(properties)
            objectData["required"] = JsonArray(requiredProperties.map { JsonLiteral(it) })
        }
        "array" -> objectData["items"] = properties.values.let {
            check(it.size == 1) { "Array descriptor has returned inconsistent number of elements: expected 1, found ${it.size}" }
            it.first()
        }
        else -> { /* no-op */ }
    }
    return JsonObject(objectData)
}
