/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization

import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * This class provides support for retrieving a serializer in runtime, instead of using the one precompiled by the serialization plugin.
 * This serializer is enabled by [ContextualSerialization].
 *
 * Typical usage of ContextSerializer would be a serialization of a class which does not have
 * static serializer (e.g. Java class or class from 3rd party library);
 * or desire to override serialized class form in one dedicated output format.
 *
 * Serializers are being looked for in a [SerialModule] from the target [Encoder] or [Decoder], using statically known [KClass].
 * To create a serial module, use [SerializersModule] factory function.
 * To pass it to encoder and decoder, refer to particular [SerialFormat]'s documentation.
 */
@ImplicitReflectionSerializer
public class ContextSerializer<T : Any>(private val serializableClass: KClass<T>) : KSerializer<T> {
    public override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.ContextSerializer", UnionKind.CONTEXTUAL).withContext(serializableClass)

    public override fun serialize(encoder: Encoder, value: T) {
        val serializer = encoder.context.getContextualOrDefault(value)
        encoder.encodeSerializableValue(serializer, value)
    }

    public override fun deserialize(decoder: Decoder): T {
        val serializer = decoder.context.getContextualOrDefault(serializableClass)
        return decoder.decodeSerializableValue(serializer)
    }
}
