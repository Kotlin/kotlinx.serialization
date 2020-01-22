/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization


interface SerializationStrategy<in T> {
    val descriptor: SerialDescriptor

    fun serialize(encoder: Encoder, value: T)
}

interface DeserializationStrategy<T> {
    val descriptor: SerialDescriptor
    fun deserialize(decoder: Decoder): T
    fun patch(decoder: Decoder, old: T): T
}

enum class UpdateMode {
    BANNED, OVERWRITE, UPDATE
}

interface KSerializer<T>: SerializationStrategy<T>, DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor
    override fun patch(decoder: Decoder, old: T): T = throw UpdateNotSupportedException(descriptor.serialName)
}


@Suppress("UNUSED")
@Deprecated("Inserted into generated code and should not be used directly", level = DeprecationLevel.HIDDEN)
public class SerializationConstructorMarker private constructor()


@ImplicitReflectionSerializer
inline fun <reified T : Any> Encoder.encode(obj: T) { encode(T::class.serializer(), obj) }

fun <T : Any?> Encoder.encode(strategy: SerializationStrategy<T>, obj: T) {
    encodeSerializableValue(strategy, obj)
}

@ImplicitReflectionSerializer
inline fun <reified T: Any> Decoder.decode(): T = this.decode(T::class.serializer())

fun <T : Any?> Decoder.decode(deserializer: DeserializationStrategy<T>): T = decodeSerializableValue(deserializer)
