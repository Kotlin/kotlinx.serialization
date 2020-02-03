/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*

/**
 * Encoder used by [Json] during serialization.
 * This interface can be used to inject desired behaviour into a serialization process of [Json].
 *
 * Typical example of the usage:
 * ```
 * // Class representing Either<Left|Right>
 * sealed class DummyEither {
 *     data class Left(val errorMsg: String) : DummyEither()
 *     data class Right(val data: Payload) : DummyEither()
 * }
 *
 * // Serializer injects custom behaviour by inspecting object content and writing
 * object EitherSerializer : KSerializer<DummyEither> {
 *     override val descriptor: SerialDescriptor = SerialClassDescImpl("DummyEither")
 *
 *     override fun deserialize(decoder: Decoder): DummyEither {
 *         val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
 *         val tree = input.decodeJson() as? JsonObject ?: throw SerializationException("Expected JsonObject")
 *         if ("error" in tree) return DummyEither.Left(tree.getPrimitive("error").content)
 *         return DummyEither.Right(input.json.decodeJson(tree, Payload.serializer()))
 *     }
 *
 *     override fun serialize(encoder: Encoder, obj: DummyEither) {
 *         val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json")
 *         val tree = when (obj) {
 *           is DummyEither.Left -> JsonObject(mapOf("error" to JsonLiteral(obj.errorMsg)))
 *           is DummyEither.Right -> output.json.toJson(obj.data, Payload.serializer())
 *         }
 *         output.encodeJson(tree)
 *     }
 * }
 * ```
 */
public interface JsonOutput: Encoder, CompositeEncoder {
    /**
     * An instance of the current [Json].
     */
    public val json: Json

    /**
     * Appends given [element] to the output.
     */
    public fun encodeJson(element: JsonElement)
}

internal fun Encoder.asJsonOutput() = this as? JsonOutput
        ?: throw SerializationException(
            "This serializer can be used only with Json format." +
                    "Expected Encoder to be JsonOutput, got ${this::class}"
        )
