/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.*

/**
 * Base class for providing multiplatform polymorphic serialization.
 *
 * This class cannot be implemented by library users. To learn how to use it for your case,
 * please refer to [PolymorphicSerializer] for interfaces/abstract classes and [SealedClassSerializer] for sealed classes.
 *
 * By default, without special support from [Encoder], polymorphic types are serialized as list with
 * two elements: class [serial name][SerialDescriptor.serialName] (String) and the object itself.
 * Serial name equals to fully-qualified class name by default and can be changed via @[SerialName] annotation.
 */
@InternalSerializationApi
public abstract class AbstractPolymorphicSerializer<T : Any> internal constructor() : KSerializer<T> {

    /**
     * Base class for all classes that this polymorphic serializer can serialize or deserialize.
     */
    public abstract val baseClass: KClass<T>

    public final override fun serialize(encoder: Encoder, value: T) {
        val actualSerializer = findPolymorphicSerializer(encoder, value)
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, actualSerializer.descriptor.serialName)
            encodeSerializableElement(descriptor, 1, actualSerializer.cast(), value)
        }
    }

    public final override fun deserialize(decoder: Decoder): T = decoder.decodeStructure(descriptor) {
        var klassName: String? = null
        var value: Any? = null
        if (decodeSequentially()) {
            return@decodeStructure decodeSequentially(this)
        }

        mainLoop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = decodeStringElement(descriptor, index)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val serializer = findPolymorphicSerializer(this, klassName)
                    value = decodeSerializableElement(descriptor, index, serializer)
                }
                else -> throw SerializationException(
                    "Invalid index in polymorphic deserialization of " +
                            (klassName ?: "unknown class") +
                            "\n Expected 0, 1 or DECODE_DONE(-1), but found $index"
                )
            }
        }
        @Suppress("UNCHECKED_CAST")
        requireNotNull(value) { "Polymorphic value has not been read for class $klassName" } as T
    }

    private fun decodeSequentially(compositeDecoder: CompositeDecoder): T {
        val klassName = compositeDecoder.decodeStringElement(descriptor, 0)
        val serializer = findPolymorphicSerializer(compositeDecoder, klassName)
        val value = compositeDecoder.decodeSerializableElement(descriptor, 1, serializer)
        compositeDecoder.endStructure(descriptor)
        return value
    }

    /**
     * Lookups an actual serializer for given [klassName] withing the current [base class][baseClass].
     * May use context from the [decoder].
     * Throws [SerializationException] if serializer is not found.
     */
    public open fun findPolymorphicSerializer(
        decoder: CompositeDecoder,
        klassName: String
    ): DeserializationStrategy<out T> = decoder.serializersModule.getPolymorphic(baseClass, klassName)
        ?: throwSubtypeNotRegistered(klassName, baseClass)


    /**
     * Lookups an actual serializer for given [value] within the current [base class][baseClass].
     * May use context from the [encoder].
     * Throws [SerializationException] if serializer is not found.
     */
    public open fun findPolymorphicSerializer(
        encoder: Encoder,
        value: T
    ): SerializationStrategy<T> =
        encoder.serializersModule.getPolymorphic(baseClass, value) ?: throwSubtypeNotRegistered(value::class, baseClass)
}

private fun throwSubtypeNotRegistered(subClassName: String, baseClass: KClass<*>): Nothing =
    throw SerializationException("$subClassName is not registered for polymorphic serialization in the scope of $baseClass")

internal fun throwSubtypeNotRegistered(subClass: KClass<*>, baseClass: KClass<*>): Nothing =
    throwSubtypeNotRegistered(subClass.toString(), baseClass)
