/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * Base class for custom serializers that allows selecting polymorphic serializer
 * without a dedicated class discriminator, on a content basis.
 *
 * Usually, polymorphic serialization (represented by [PolymorphicSerializer] and [SealedClassSerializer])
 * requires a dedicated `"type"` property in the JSON to
 * determine actual serializer that is used to deserialize Kotlin class.
 *
 * However, sometimes (e.g. when interacting with external API) type property is not present in the input
 * and it is expected to guess the actual type by the shape of JSON, for example by the presence of specific key.
 * [JsonParametricSerializer] provides a skeleton implementation for such strategy. Please note that
 * since JSON content is represented by [JsonElement] class and could be read only with [JsonInput] decoder,
 * this class works only with [Json] format.
 *
 * Deserialization happens in two stages: first, a value from the input JSON is read
 * to as a [JsonElement]. Second, [selectSerializer] function is called to determine which serializer should be used.
 * The returned serializer is used to deserialize [JsonElement] back to Kotlin object.
 *
 * It is possible to serialize values this serializer. In that case, class discriminator property won't
 * be added to JSON stream, i.e., deserializing a class from the string and serializing it back yields the original string.
 * However, to determine a serializer, a standard polymorphic mechanism represented by [SerialModule] is used.
 * For convenience, [serialize] method can lookup default serializer, but it is recommended to follow
 * standard procedure with [registering][SerializersModuleBuilder.polymorphic].
 *
 * Usage example:
 * ```
 * interface Payment {
 *     val amount: String
 * }
 *
 * @Serializable
 * data class SuccessfulPayment(override val amount: String, val date: String) : Payment
 *
 * @Serializable
 * data class RefundedPayment(override val amount: String, val date: String, val reason: String) : Payment
 *
 * object PaymentSerializer : JsonParametricSerializer<Payment>(Payment::class) {
 *     override fun selectSerializer(element: JsonElement): KSerializer<out Payment> = when {
 *         "reason" in element -> RefundedPayment.serializer()
 *         else -> SuccessfulPayment.serializer()
 *     }
 * }
 *
 * // Now both statements will yield different subclasses of Payment:
 *
 * Json.parse(PaymentSerializer, """{"amount":"1.0","date":"03.02.2020"}""")
 * Json.parse(PaymentSerializer, """{"amount":"2.0","date":"03.02.2020","reason":"complaint"}""")
 * ```
 *
 * @param T A root class for all classes that could be possibly encountered during serialization and deserialization.
 * @param baseClass A class token for [T].
 */
public abstract class JsonParametricSerializer<T : Any>(private val baseClass: KClass<T>) : KSerializer<T> {
    /**
     * A descriptor for this set of parametric serializers.
     * By default, it uses the name composed of [baseClass] simple name,
     * kind is set to [PolymorphicKind.SEALED] and contains 0 elements.
     *
     * However, this descriptor can be overridden to achieve better representation of custom transformed JSON shape
     * for schema generating/introspection purposes.
     */
    override val descriptor: SerialDescriptor =
        SerialDescriptor("JsonParametricSerializer<${baseClass.simpleName()}>", PolymorphicKind.OPEN)

    @OptIn(UnsafeSerializationApi::class)
    final override fun serialize(encoder: Encoder, value: T) {
        val actualSerializer =
            encoder.context.getPolymorphic(baseClass, value)
                    ?: value::class.serializerOrNull()
                    ?: throwSubtypeNotRegistered(value::class, baseClass)
        @Suppress("UNCHECKED_CAST")
        (actualSerializer as KSerializer<T>).serialize(encoder, value)
    }

    final override fun deserialize(decoder: Decoder): T {
        val input = decoder.asJsonInput()
        val tree = input.decodeJson()

        @Suppress("UNCHECKED_CAST")
        val actualSerializer = selectSerializer(tree) as KSerializer<T>
        return input.json.fromJson(actualSerializer, tree)
    }

    /**
     * Determines a particular serializer by looking on a parsed JSON [element].
     */
    protected abstract fun selectSerializer(element: JsonElement): KSerializer<out T>
}
