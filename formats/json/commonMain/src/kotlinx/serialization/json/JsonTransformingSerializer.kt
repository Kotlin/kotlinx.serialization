/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith")

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.internal.*

/**
 * Base class for custom serializers that allows manipulating an abstract JSON
 * representation of the class before serialization or deserialization.
 *
 * [JsonTransformingSerializer] provides capabilities to manipulate [JsonElement] representation
 * directly instead of interacting with [Encoder] and [Decoder] in order to apply a custom
 * transformation to the JSON.
 * Please note that this class expects that [Encoder] and [Decoder] are implemented by [JsonDecoder] and [JsonEncoder],
 * i.e. serializers derived from this class work only with [Json] format.
 *
 * There are two methods in which JSON transformation can be defined: [transformSerialize] and [transformDeserialize].
 * You can override one or both of them. Consult their documentation for details.
 *
 * Usage example:
 *
 * ```
 * @Serializable
 * data class Example(
 *     @Serializable(UnwrappingJsonListSerializer::class) val data: String
 * )
 * // Unwraps a list to a single object
 * object UnwrappingJsonListSerializer :
 *     JsonTransformingSerializer<String>(String.serializer()) {
 *     override fun transformDeserialize(element: JsonElement): JsonElement {
 *         if (element !is JsonArray) return element
 *         require(element.size == 1) { "Array size must be equal to 1 to unwrap it" }
 *         return element.first()
 *     }
 * }
 * // Now these functions both yield correct result:
 * Json.parse(Example.serializer(), """{"data":["str1"]}""")
 * Json.parse(Example.serializer(), """{"data":"str1"}""")
 * ```
 *
 * @param T A type for Kotlin property for which this serializer could be applied.
 *        **Not** the type that you may encounter in JSON. (e.g. if you unwrap a list
 *        to a single value `T`, use `T`, not `List<T>`)
 * @param tSerializer A serializer for type [T]. Determines [JsonElement] which is passed to [transformSerialize].
 *        Should be able to parse [JsonElement] from [transformDeserialize] function.
 *        Usually, default [serializer] is sufficient.
 */
public abstract class JsonTransformingSerializer<T : Any>(
    private val tSerializer: KSerializer<T>
) : KSerializer<T> {

    /**
     * A descriptor for this transformation.
     * By default, it delegates to [tSerializer]'s descriptor.
     *
     * However, this descriptor can be overridden to achieve better representation of the resulting JSON shape
     * for schema generating or introspection purposes.
     */
    override val descriptor: SerialDescriptor get() = tSerializer.descriptor

    final override fun serialize(encoder: Encoder, value: T) {
        val output = encoder.asJsonEncoder()
        var element = output.json.writeJson(value, tSerializer)
        element = transformSerialize(element)
        output.encodeJsonElement(element)
    }

    final override fun deserialize(decoder: Decoder): T {
        val input = decoder.asJsonDecoder()
        val element = input.decodeJsonElement()
        return input.json.decodeFromJsonElement(tSerializer, transformDeserialize(element))
    }

    /**
     * Transformation that happens during [deserialize] call.
     * Does nothing by default.
     *
     * During deserialization, a value from JSON is firstly decoded to a [JsonElement],
     * user transformation in [transformDeserialize] is applied,
     * and then resulting [JsonElement] is deserialized to [T] with [tSerializer].
     */
    protected open fun transformDeserialize(element: JsonElement): JsonElement = element

    /**
     * Transformation that happens during [serialize] call.
     * Does nothing by default.
     *
     * During serialization, a value of type [T] is serialized with [tSerializer] to a [JsonElement],
     * user transformation in [transformSerialize] is applied, and then resulting [JsonElement] is encoded to a JSON string.
     */
    protected open fun transformSerialize(element: JsonElement): JsonElement = element
}
