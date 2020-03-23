/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * [SerialModule] is a collection of serializers used by [ContextSerializer] and [PolymorphicSerializer]
 * to override or provide serializers at the runtime, whereas at the compile-time they provided by the serialization plugin.
 * It can be considered as a map where serializers can be found using their statically known KClasses.
 *
 * To enable runtime serializers resolution, one of the special annotations must be used on target types
 * ([Polymorphic] or [ContextualSerialization]), and a serial module with serializers should be used during construction of [SerialFormat].
 *
 * @see ContextualSerialization
 * @see Polymorphic
 */
public sealed class SerialModule {

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
    public abstract fun dumpTo(collector: SerialModuleCollector)
}

/**
 * A [SerialModule] which is empty and always returns `null`.
 */
public object EmptyModule : SerialModule() {
    public override fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>? = null
    public override fun <T : Any> getPolymorphic(baseClass: KClass<in T>, value: T): SerializationStrategy<T>? = null
    public override fun <T : Any> getPolymorphic(
        baseClass: KClass<in T>,
        serializedClassName: String
    ): DeserializationStrategy<out T>? = null

    public override fun dumpTo(collector: SerialModuleCollector): Unit = Unit
}

internal typealias PolymorphicProvider<Base> = (className: String) -> DeserializationStrategy<out Base>?

/**
 * A default implementation of [SerialModule]
 * which uses hash maps to store serializers associated with KClasses.
 */
@Suppress("UNCHECKED_CAST")
internal class SerialModuleImpl(
    private val class2Serializer: Map<KClass<*>, KSerializer<*>>,
    @JvmField val polyBase2Serializers: Map<KClass<*>, Map<KClass<*>, KSerializer<*>>>,
    private val polyBase2NamedSerializers: Map<KClass<*>, Map<String, KSerializer<*>>>,
    private val polyBase2DefaultProvider: Map<KClass<*>, PolymorphicProvider<*>>
) : SerialModule() {

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

    override fun dumpTo(collector: SerialModuleCollector) {
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
            collector.defaultPolymorphic(baseClass as KClass<Any>, provider as (PolymorphicProvider<out Any>))
        }
    }
}
