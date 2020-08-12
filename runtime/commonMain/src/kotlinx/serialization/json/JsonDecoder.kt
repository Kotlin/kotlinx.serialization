/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.updateModeDeprecated

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
     *     val jsonObject = (decoder as JsonDecoder).decodeJson()
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

    // Class 'JsonDecoder' must override public open val updateMode: UpdateMode defined in kotlinx.serialization.encoding.Decoder
    // because it inherits multiple interface methods of it
    @Suppress("DEPRECATION")
    @Deprecated(updateModeDeprecated, level = DeprecationLevel.HIDDEN)
    override val updateMode: UpdateMode
        get() = UpdateMode.OVERWRITE
}
