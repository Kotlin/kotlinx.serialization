/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*

/**
 * Base class for custom serializers that allows manipulating an abstract JSON
 * representation of the class before serialization or deserialization.
 *
 * [JsonTransformingSerializer] provides capabilities to manipulate [JsonElement] representation
 * directly instead of interacting with [Encoder] and [Decoder] in order to apply a custom
 * transformation to the JSON.
 * Please note that this class expects that [Encoder] and [Decoder] are implemented by [JsonInput] and [JsonOutput],
 * i.e. serializers derived from this class work only with [Json] format.
 *
 * During serialization, this class first serializes original value with [tSerializer] to a [JsonElement],
 * then calls [writeTransform] method, which may contain a user-defined transformation, such as
 * wrapping a value into [JsonArray], filtering keys, adding keys, etc.
 *
 * During deserialization, the opposite process happens: first, value from JSON stream is read
 * to a [JsonElement], second, user transformation in [readTransform] is applied,
 * and then JSON tree is deserialized back to [T] with [tSerializer].
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
 *     JsonTransformingSerializer<String>(String.serializer(), "UnwrappingList") {
 *     override fun readTransform(element: JsonElement): JsonElement {
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
 * @param tSerializer A serializer for type [T]. Determines [JsonElement] which is passed to [writeTransform].
 *        Should be able to parse [JsonElement] from [readTransform] function.
 *        Usually, default serializer is sufficient.
 * @param transformationName A name for the particular implementation to fulfill [SerialDescriptor.serialName] uniqueness guarantee.
 */
public abstract class JsonTransformingSerializer<T : Any>(
    private val tSerializer: KSerializer<T>,
    transformationName: String
) : KSerializer<T> {
    /**
     * A descriptor for this transformation.
     * By default, it uses the name composed of [tSerializer]'s descriptor and transformation name,
     * kind of [tSerializer]'s descriptor and contains 0 elements.
     *
     * However, this descriptor can be overridden to achieve better representation of the resulting JSON shape
     * for schema generating or introspection purposes.
     */
    override val descriptor: SerialDescriptor = SerialDescriptor(
        "JsonTransformingSerializer<${tSerializer.descriptor.serialName}>($transformationName)",
        tSerializer.descriptor.kind
    )

    final override fun serialize(encoder: Encoder, value: T) {
        val output = encoder.asJsonOutput()
        var element = output.json.writeJson(value, tSerializer)
        element = writeTransform(element)
        output.encodeJson(element)
    }

    final override fun deserialize(decoder: Decoder): T {
        val input = decoder.asJsonInput()
        var element = input.decodeJson()
        element = readTransform(element)
        return input.json.fromJson(tSerializer, element)
    }

    /**
     * Transformation which happens during [serialize] call.
     * Does nothing by default.
     */
    protected open fun readTransform(element: JsonElement): JsonElement = element

    /**
     * Transformation which happens during [deserialize] call.
     * Does nothing by default.
     */
    protected open fun writeTransform(element: JsonElement): JsonElement = element
}
