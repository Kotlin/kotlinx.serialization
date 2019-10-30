/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.KClass

/**
 * Base class for providing multiplatform polymorphic serialization.
 *
 * This class cannot be implemented by library users. To learn how to use it for your case,
 * see [PolymorphicSerializer] for interfaces/abstract classes and [SealedClassSerializer] for sealed classes.
 *
 * By default, (without special support from [Encoder]), polymorphic values are serialized as list with
 * two elements: fully-qualified class name (String) and the object itself.
 */
@InternalSerializationApi
public abstract class AbstractPolymorphicSerializer<T : Any> internal constructor() : KSerializer<T> {

    /**
     * Base class for all classes that this polymorphic serializer can serialize or deserialize.
     */
    public abstract val baseClass: KClass<T>

    public final override fun serialize(encoder: Encoder, obj: T) {
        val actualSerializer = findPolymorphicSerializer(encoder, obj)
        val compositeEncoder = encoder.beginStructure(descriptor)
        compositeEncoder.encodeStringElement(descriptor, 0, actualSerializer.descriptor.name)

        @Suppress("UNCHECKED_CAST")
        compositeEncoder.encodeSerializableElement(descriptor, 1, actualSerializer as KSerializer<Any?>, obj)
        compositeEncoder.endStructure(descriptor)
    }

    public final override fun deserialize(decoder: Decoder): T {
        val compositeDecoder = decoder.beginStructure(descriptor)
        var klassName: String? = null
        var value: Any? = null
        mainLoop@ while (true) {
            when (val index = compositeDecoder.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_ALL -> {
                    klassName = compositeDecoder.decodeStringElement(descriptor, 0)
                    val serializer = findPolymorphicSerializer(compositeDecoder, klassName)
                    value = compositeDecoder.decodeSerializableElement(descriptor, 1, serializer)
                    break@mainLoop
                }
                CompositeDecoder.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = compositeDecoder.decodeStringElement(descriptor, index)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val serializer = findPolymorphicSerializer(compositeDecoder, klassName)
                    value = compositeDecoder.decodeSerializableElement(descriptor, index, serializer)
                }
                else -> throw SerializationException(
                    "Invalid index in polymorphic deserialization of " +
                            (klassName ?: "unknown class") +
                            "\n Expected 0, 1, READ_ALL(-2) or READ_DONE(-1), but found $index"
                )
            }
        }

        compositeDecoder.endStructure(descriptor)
        @Suppress("UNCHECKED_CAST")
        return requireNotNull(value) { "Polymorphic value has not been read for class $klassName" } as T
    }

    /**
     * Lookups an actual serializer for given [klassName] withing the current [base class][baseClass].
     * May use context from the [decoder].
     * Throws [SerializationException] if serializer is not found.
     */
    public open fun findPolymorphicSerializer(
        decoder: CompositeDecoder,
        klassName: String
    ): KSerializer<out T> = decoder.context.getPolymorphic(baseClass, klassName)
            ?: throwSubtypeNotRegistered(klassName, baseClass)


    /**
     * Lookups an actual serializer for given [value] within the current [base class][baseClass].
     * May use context from the [encoder].
     * Throws [SerializationException] if serializer is not found.
     */
    public open fun findPolymorphicSerializer(
        encoder: Encoder,
        value: T
    ): KSerializer<out T> =
        encoder.context.getPolymorphic(baseClass, value) ?: throwSubtypeNotRegistered(value::class, baseClass)
}


private fun throwSubtypeNotRegistered(subClassName: String, baseClass: KClass<*>): Nothing =
    throw SerializationException("$subClassName is not registered for polymorphic serialization in the scope of $baseClass")

private fun throwSubtypeNotRegistered(subClass: KClass<*>, baseClass: KClass<*>): Nothing =
    throwSubtypeNotRegistered(subClass.toString(), baseClass)
