/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

/**
 * Decoder used by [Json] during deserialization.
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
 *     override val descriptor: SerialDescriptor = buildSerialDescriptor("package.Either", PolymorphicKind.SEALED) {
 *          // ..
 *      }
 *
 *     override fun deserialize(decoder: Decoder): Either {
 *         val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be decoded only by Json format")
 *         val tree = input.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected JsonObject")
 *         if ("error" in tree) return Either.Left(tree["error"]!!.jsonPrimitive.content)
 *         return Either.Right(input.json.decodeFromJsonElement(Payload.serializer(), tree))
 *     }
 *
 *     override fun serialize(encoder: Encoder, value: Either) {
 *         val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be encoded only by Json format")
 *         val tree = when (value) {
 *           is Either.Left -> JsonObject(mapOf("error" to JsonPrimitive(value.errorMsg)))
 *           is Either.Right -> output.json.encodeToJsonElement(Payload.serializer(), value.data)
 *         }
 *         output.encodeJsonElement(tree)
 *     }
 * }
 * ```
 *
 * ### Not stable for inheritance
 *
 * `JsonDecoder` interface is not stable for inheritance in 3rd party libraries, as new methods
 * might be added to this interface or contracts of the existing methods can be changed.
 * Accepting this interface in your API methods, casting [Decoder] to [JsonDecoder] and invoking its
 * methods is considered stable.
 */
public interface JsonDecoder : Decoder, CompositeDecoder {
    /**
     * An instance of the current [Json].
     */
    public val json: Json

    /**
     * Decodes the next element in the current input as [JsonElement].
     * The type of the decoded element depends on the current state of the input and, when received
     * by [serializer][KSerializer] in its [KSerializer.serialize] method, the type of the token directly matches
     * the [kind][SerialDescriptor.kind].
     *
     * This method is allowed to invoke only as the part of the whole deserialization process of the class,
     * calling this method after invoking [beginStructure] or any `decode*` method will lead to unspecified behaviour.
     * For example:
     * ```
     * class Holder(val value: Int, val list: List<Int>())
     *
     * // Holder deserialize method
     * fun deserialize(decoder: Decoder): Holder {
     *     // Completely okay, the whole Holder object is read
     *     val jsonObject = (decoder as JsonDecoder).decodeJsonElement()
     *     // ...
     * }
     *
     * // Incorrect Holder deserialize method
     * fun deserialize(decoder: Decoder): Holder {
     *     // decode "value" key unconditionally
     *     decoder.decodeElementIndex(descriptor)
     *     val value = decode.decodeInt()
     *     // Incorrect, decoder is already in an intermediate state after decodeInt
     *     val json = (decoder as JsonDecoder).decodeJsonElement()
     *     // ...
     * }
     * ```
     */
    public fun decodeJsonElement(): JsonElement
}
