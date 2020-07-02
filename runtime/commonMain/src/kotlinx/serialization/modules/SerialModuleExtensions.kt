/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * Returns a dependent serializer associated with a given reified type.
 */
public inline fun <reified T : Any> SerializersModule.getContextual(): KSerializer<T>? = getContextual(T::class)

/**
 * Returns a serializer associated with KClass of the given [value].
 */
public fun <T : Any> SerializersModule.getContextual(value: T): KSerializer<T>? {
    return getContextual(value::class)?.cast()
}

/**
 * Attempts to retrieve a serializer from the current module and, if not found, fallbacks to [serializer] method
 */
@OptIn(UnsafeSerializationApi::class)
public inline fun <reified T : Any> SerializersModule.getContextualOrDefault(): KSerializer<T> =
    // Even though serializer(KType) also invokes serializerOrNull, it is a significant performance optimization
    // TODO replace with serializer(typeOf<T>()) when intrinsics are here
    getContextual(T::class) ?: T::class.serializerOrNull() ?: serializer(typeOf<T>()).cast()

/**
 * Attempts to retrieve a serializer from the current module using the given [type] and, if not found, fallbacks to [serializer] method
 */
@OptIn(UnsafeSerializationApi::class)
public fun <T : Any> SerializersModule.getContextualOrDefault(type: KType): KSerializer<T> {
    // Even though serializer(KType) also invokes serializerOrNull, it is a significant performance optimization
    // TODO replace with serializer(typeOf<T>()) when intrinsics are here
    val kclass = type.kclass()
    return (getContextual(kclass) ?: kclass.serializerOrNull() ?: serializer(type)).cast()
}

/**
 * Returns a combination of two serial modules
 *
 * If serializer for some class presents in both modules, a [SerializerAlreadyRegisteredException] is thrown.
 * To overwrite serializers, use [SerializersModule.overwriteWith] function.
 */
public operator fun SerializersModule.plus(other: SerializersModule): SerializersModule = SerializersModule {
    include(this@plus)
    include(other)
}

/**
 * Returns a combination of two serial modules
 *
 * If serializer for some class presents in both modules, result module
 * will contain serializer from [other] module.
 */
public infix fun SerializersModule.overwriteWith(other: SerializersModule): SerializersModule = SerializersModule {
    include(this@overwriteWith)
    other.dumpTo(object : SerializersModuleCollector {
        override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
            registerSerializer(kClass, serializer, allowOverwrite = true)
        }

        override fun <Base : Any, Sub : Base> polymorphic(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>
        ) {
            registerPolymorphicSerializer(baseClass, actualClass, actualSerializer, allowOverwrite = true)
        }

        override fun <Base : Any> defaultPolymorphic(
            baseClass: KClass<Base>,
            defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?
        ) {
            registerDefaultPolymorphicSerializer(baseClass, defaultSerializerProvider, allowOverwrite = true)
        }
    })
}

/**
 * Looks up a descriptor of serializer registered for contextual serialization in [this],
 * using [SerialDescriptor.capturedKClass] as a key.
 *
 * @see SerializersModule.getContextual
 * @see SerializersModuleBuilder.contextual
 */
public fun SerializersModule.getContextualDescriptor(descriptor: SerialDescriptor): SerialDescriptor? =
    descriptor.capturedKClass?.let { klass -> getContextual(klass)?.descriptor }

/**
 * Retrieves a collection of descriptors which serializers are registered for polymorphic serialization in [this]
 * with base class equal to [descriptor]'s [SerialDescriptor.capturedKClass].
 *
 * @see SerializersModule.getPolymorphic
 * @see SerializersModuleBuilder.polymorphic
 */
public fun SerializersModule.getPolymorphicDescriptors(descriptor: SerialDescriptor): List<SerialDescriptor> {
    val kClass = descriptor.capturedKClass ?: return emptyList()
    // shortcut
    if (this is SerialModuleImpl) return this.polyBase2Serializers[kClass]?.values.orEmpty()
        .map { it.descriptor }

    val builder = ArrayList<SerialDescriptor>()
    dumpTo(object : SerializersModuleCollector {
        override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) { /*noop*/
        }

        override fun <Base : Any, Sub : Base> polymorphic(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>
        ) {
            if (baseClass == kClass) builder.add(actualSerializer.descriptor)
        }

        override fun <Base : Any> defaultPolymorphic(
            baseClass: KClass<Base>,
            defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?
        ) {
            // Nothing
        }
    })
    return builder
}
