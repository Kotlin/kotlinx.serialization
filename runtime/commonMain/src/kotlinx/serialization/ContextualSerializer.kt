/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * This class provides support for retrieving a serializer in runtime, instead of using the one precompiled by the serialization plugin.
 * This serializer is enabled by [Contextual] or [UseContextualSerialization].
 *
 * Typical usage of ContextualSerializer would be a serialization of a class which does not have
 * static serializer (e.g. Java class or class from 3rd party library);
 * or desire to override serialized class form in one dedicated output format.
 *
 * Serializers are being looked for in a [SerializersModule] from the target [Encoder] or [Decoder], using statically known [KClass].
 * To create a serial module, use [SerializersModule] factory function.
 * To pass it to encoder and decoder, refer to particular [SerialFormat]'s documentation.
 *
 * Usage of contextual serializer can be demonstrated by the following example:
 * ```
 * import java.util.Date
 *
 * @Serializable
 * class ClassWithDate(val data: String, @Contextual val timestamp: Date)
 *
 * val moduleForDate = serializersModule(MyISO8601DateSerializer)
 * val json = Json { serializersModule = moduleForDate }
 * json.encodeToString(ClassWithDate("foo", Date())
 * ```
 */
@ExperimentalSerializationApi
public class ContextualSerializer<T : Any>(
    private val serializableClass: KClass<T>,
    private val fallbackSerializer: KSerializer<T>?,
    private val typeParametersSerializers: Array<KSerializer<*>>
) : KSerializer<T> {

    // Used from auto-generated code
    public constructor(serializableClass: KClass<T>) : this(serializableClass, null, EMPTY_SERIALIZER_ARRAY)

    public override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.ContextualSerializer", SerialKind.CONTEXTUAL).withContext(serializableClass)

    public override fun serialize(encoder: Encoder, value: T) {
        val clz = value::class
        val serializer = encoder.serializersModule.getContextual(clz) ?: fallbackSerializer ?: serializableClass.serializerNotRegistered()
        @Suppress("UNCHECKED_CAST")
        encoder.encodeSerializableValue(serializer as SerializationStrategy<T>, value)
    }

    public override fun deserialize(decoder: Decoder): T {
        val serializer = decoder.serializersModule.getContextual(serializableClass) ?: fallbackSerializer ?: serializableClass.serializerNotRegistered()
        return decoder.decodeSerializableValue(serializer)
    }
}

@Deprecated("Renamed", ReplaceWith("ContextualSerializer"), level = DeprecationLevel.ERROR)
public typealias ContextSerializer<T> = ContextualSerializer<T>
