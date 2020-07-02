/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.jvm.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

/**
 * [SerializersModule] is a collection of serializers used by [ContextSerializer] and [PolymorphicSerializer]
 * to override or provide serializers at the runtime, whereas at the compile-time they provided by the serialization plugin.
 * It can be considered as a map where serializers can be found using their statically known KClasses.
 *
 * To enable runtime serializers resolution, one of the special annotations must be used on target types
 * ([Polymorphic] or [ContextualSerialization]), and a serial module with serializers should be used during construction of [SerialFormat].
 *
 * @see ContextualSerialization
 * @see Polymorphic
 */
public sealed class SerializersModule {

    /**
     * Returns a contextual serializer associated with a given [kclass].
     * This method is used in context-sensitive operations on a property marked with [ContextualSerialization] by a [ContextSerializer]
     */
    public abstract fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>?

    /**
     * Returns a polymorphic serializer registered for a class of the given [value] in the scope of [baseClass].
     */
    public abstract fun <T : Any> getPolymorphic(baseClass: KClass<in T>, value: T): SerializationStrategy<T>?

    /**
     * Returns a polymorphic deserializer registered for a [serializedClassName] in the scope of [baseClass]
     * or default value constructed from [serializedClassName] if default serializer provider was registered.
     */
    public abstract fun <T : Any> getPolymorphic(baseClass: KClass<in T>, serializedClassName: String): DeserializationStrategy<out T>?

    /**
     * Copies contents of this module to the given [collector].
     */
    public abstract fun dumpTo(collector: SerializersModuleCollector)
}

/**
 * A [SerializersModule] which is empty and always returns `null`.
 */
@SharedImmutable
public val EmptySerializersModule: SerializersModule = SerialModuleImpl(emptyMap(), emptyMap(), emptyMap(), emptyMap())

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

        override fun <Base : Any> polymorphicDefault(
            baseClass: KClass<Base>,
            defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?
        ) {
            registerDefaultPolymorphicSerializer(baseClass, defaultSerializerProvider, allowOverwrite = true)
        }
    })
}

// Implementation details below

/**
 * A default implementation of [SerializersModule]
 * which uses hash maps to store serializers associated with KClasses.
 */
@Suppress("UNCHECKED_CAST")
internal class SerialModuleImpl(
    private val class2Serializer: Map<KClass<*>, KSerializer<*>>,
    @JvmField val polyBase2Serializers: Map<KClass<*>, Map<KClass<*>, KSerializer<*>>>,
    private val polyBase2NamedSerializers: Map<KClass<*>, Map<String, KSerializer<*>>>,
    private val polyBase2DefaultProvider: Map<KClass<*>, PolymorphicProvider<*>>
) : SerializersModule() {

    override fun <T : Any> getPolymorphic(baseClass: KClass<in T>, value: T): SerializationStrategy<T>? {
        if (!value.isInstanceOf(baseClass)) return null
        val custom = polyBase2Serializers[baseClass]?.get(value::class) as? SerializationStrategy<T>
        if (custom != null) return custom
        if (baseClass == Any::class) {
            val serializer = StandardSubtypesOfAny.getSubclassSerializer(value)
            return serializer as? SerializationStrategy<T>
        }
        return null
    }

    override fun <T : Any> getPolymorphic(baseClass: KClass<in T>, serializedClassName: String): DeserializationStrategy<out T>? {
        // Registered
        val registered = polyBase2NamedSerializers[baseClass]?.get(serializedClassName) as? KSerializer<out T>
        if (registered != null) return registered
        // Default
        val default = (polyBase2DefaultProvider[baseClass] as? PolymorphicProvider<T>)?.invoke(serializedClassName)
        if (default != null) return default
        // Any subtypes
        return if (baseClass == Any::class)
            StandardSubtypesOfAny.getDefaultDeserializer(serializedClassName)?.cast()
        else null
    }

    override fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>? =
        class2Serializer[kclass] as? KSerializer<T>

    override fun dumpTo(collector: SerializersModuleCollector) {
        class2Serializer.forEach { (kclass, serial) ->
            collector.contextual(
                kclass as KClass<Any>,
                serial.cast()
            )
        }

        polyBase2Serializers.forEach { (baseClass, classMap) ->
            classMap.forEach { (actualClass, serializer) ->
                collector.polymorphic(
                    baseClass as KClass<Any>,
                    actualClass as KClass<Any>,
                    serializer.cast()
                )
            }
        }

        polyBase2DefaultProvider.forEach { (baseClass, provider) ->
            collector.polymorphicDefault(baseClass as KClass<Any>, provider as (PolymorphicProvider<out Any>))
        }
    }
}

internal typealias PolymorphicProvider<Base> = (className: String) -> DeserializationStrategy<out Base>?
