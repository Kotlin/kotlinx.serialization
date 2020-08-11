/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*

/**
 * Encoder used by [Json] during serialization.
 * This interface can be used to inject desired behaviour into a serialization process of [Json].
 *
 * Typical example of the usage:
 * ```
 * // Class representing Either<Left|Right>
 * sealed class Either {
 *     data class Left(val errorMsg: String) : Either()
 *     data class Right(val data: Payload) : Either()
 * }
 *
 * // Serializer injects custom behaviour by inspecting object content and writing
 * object EitherSerializer : KSerializer<Either> {
 *     override val descriptor: SerialDescriptor = SerialDescriptor("package.Either", PolymorphicKind.SEALED) {
 *          // ..
 *      }
 *
 *     override fun deserialize(decoder: Decoder): Either {
 *         val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
 *         val tree = input.decodeJson() as? JsonObject ?: throw SerializationException("Expected JsonObject")
 *         if ("error" in tree) return Either.Left(tree.getPrimitive("error").content)
 *         return Either.Right(input.json.fromJson(Payload.serializer(), tree))
 *     }
 *
 *     override fun serialize(encoder: Encoder, value: Either) {
 *         val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
 *         val tree = when (value) {
 *           is Either.Left -> JsonObject(mapOf("error" to JsonLiteral(value.errorMsg)))
 *           is Either.Right -> output.json.toJson(Payload.serializer(), value.data)
 *         }
 *         output.encodeJson(tree)
 *     }
 * }
 * ```
 */
public interface JsonEncoder : Encoder, CompositeEncoder {
    /**
     * An instance of the current [Json].
     */
    public val json: Json

    /**
     * Appends the given JSON [element] to the current output.
     * This method is allowed to invoke only as the part of the whole serialization process of the class,
     * calling this method after invoking [beginStructure] or any `encode*` method will lead to unspecified behaviour
     * and may produce an invalid JSON result.
     * For example:
     * ```
     * class Holder(val value: Int, val list: List<Int>())
     *
     * // Holder serialize method
     * fun serialize(encoder: Encoder, value: Holder) {
     *     // Completely okay, the whole Holder object is read
     *     val jsonObject = JsonObject(...) // build a JsonObject from Holder
     *     (encoder as JsonEncoder).encodeJson(jsonObject) // Write it
     * }
     *
     * // Incorrect Holder serialize method
     * fun serialize(encoder: Encoder, value: Holder) {
     *     val composite = encoder.beginStructure(descriptor)
     *     composite.encodeSerializableElement(descriptor, 0, Int.serializer(), value.value)
     *     val array = JsonArray(value.list)
     *     // Incorrect, encoder is already in an intermediate state after encodeSerializableElement
     *     (composite as JsonEncoder).encodeJson(array)
     *     composite.endStructure(descriptor)
     *     // ...
     * }
     * ```
     */
    public fun encodeJsonElement(element: JsonElement)
}
