/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.js.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * [SerializersModule] is a collection of serializers used by [ContextualSerializer] and [PolymorphicSerializer]
 * to override or provide serializers at the runtime, whereas at the compile-time they provided by the serialization plugin.
 * It can be considered as a map where serializers can be found using their statically known KClasses.
 *
 * To enable runtime serializers resolution, one of the special annotations must be used on target types
 * ([Polymorphic] or [Contextual]), and a serial module with serializers should be used during construction of [SerialFormat].
 *
 * Serializers module can be built with `SerializersModule {}` builder function.
 * Empty module can be obtained with `EmptySerializersModule()` factory function.
 *
 * @see Contextual
 * @see Polymorphic
 */
public sealed class SerializersModule {

    @ExperimentalSerializationApi
    @Deprecated(
        "Deprecated in favor of overload with default parameter",
        ReplaceWith("getContextual(kclass)"),
        DeprecationLevel.HIDDEN
    ) // Was experimental since 1.0.0, HIDDEN in 1.2.0 in a backwards-compatible manner
    public fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>? =
        getContextual(kclass, emptyList())

    /**
     * Returns a contextual serializer associated with a given [kClass].
     * If given class has generic parameters and module has provider for [kClass],
     * [typeArgumentsSerializers] are used to create serializer.
     * This method is used in context-sensitive operations on a property marked with [Contextual] by a [ContextualSerializer].
     *
     * @see SerializersModuleBuilder.contextual
     */
    @ExperimentalSerializationApi
    public abstract fun <T : Any> getContextual(
        kClass: KClass<T>,
        typeArgumentsSerializers: List<KSerializer<*>> = emptyList()
    ): KSerializer<T>?

    /**
     * Returns a polymorphic serializer registered for a class of the given [value] in the scope of [baseClass].
     */
    @ExperimentalSerializationApi
    public abstract fun <T : Any> getPolymorphic(baseClass: KClass<in T>, value: T): SerializationStrategy<T>?

    /**
     * Returns a polymorphic deserializer registered for a [serializedClassName] in the scope of [baseClass]
     * or default value constructed from [serializedClassName] if a default serializer provider was registered.
     */
    @ExperimentalSerializationApi
    public abstract fun <T : Any> getPolymorphic(baseClass: KClass<in T>, serializedClassName: String?): DeserializationStrategy<T>?

    /**
     * Copies contents of this module to the given [collector].
     */
    @ExperimentalSerializationApi
    public abstract fun dumpTo(collector: SerializersModuleCollector)
}

/**
 * A [SerializersModule] which is empty and always returns `null`.
 */
@Deprecated("Deprecated in the favour of 'EmptySerializersModule()'",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("EmptySerializersModule()"))
@JsName("EmptySerializersModuleLegacyJs") // Compatibility with JS
public val EmptySerializersModule: SerializersModule = SerialModuleImpl(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())

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
@OptIn(ExperimentalSerializationApi::class)
public infix fun SerializersModule.overwriteWith(other: SerializersModule): SerializersModule = SerializersModule {
    include(this@overwriteWith)
    other.dumpTo(object : SerializersModuleCollector {
        override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
            registerSerializer(kClass, ContextualProvider.Argless(serializer), allowOverwrite = true)
        }

        override fun <T : Any> contextual(
            kClass: KClass<T>,
            provider: (serializers: List<KSerializer<*>>) -> KSerializer<*>
        ) {
            registerSerializer(kClass, ContextualProvider.WithTypeArguments(provider), allowOverwrite = true)
        }

        override fun <Base : Any, Sub : Base> polymorphic(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>
        ) {
            registerPolymorphicSerializer(baseClass, actualClass, actualSerializer, allowOverwrite = true)
        }

        override fun <Base : Any> polymorphicDefaultSerializer(
            baseClass: KClass<Base>,
            defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?
        ) {
            registerDefaultPolymorphicSerializer(baseClass, defaultSerializerProvider, allowOverwrite = true)
        }

        override fun <Base : Any> polymorphicDefaultDeserializer(
            baseClass: KClass<Base>,
            defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?
        ) {
            registerDefaultPolymorphicDeserializer(baseClass, defaultDeserializerProvider, allowOverwrite = true)
        }
    })
}

// Implementation details below

/**
 * A default implementation of [SerializersModule]
 * which uses hash maps to store serializers associated with KClasses.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
internal class SerialModuleImpl(
    private val class2ContextualFactory: Map<KClass<*>, ContextualProvider>,
    @JvmField val polyBase2Serializers: Map<KClass<*>, Map<KClass<*>, KSerializer<*>>>,
    private val polyBase2DefaultSerializerProvider: Map<KClass<*>, PolymorphicSerializerProvider<*>>,
    private val polyBase2NamedSerializers: Map<KClass<*>, Map<String, KSerializer<*>>>,
    private val polyBase2DefaultDeserializerProvider: Map<KClass<*>, PolymorphicDeserializerProvider<*>>
) : SerializersModule() {

    override fun <T : Any> getPolymorphic(baseClass: KClass<in T>, value: T): SerializationStrategy<T>? {
        if (!baseClass.isInstance(value)) return null
        // Registered
        val registered = polyBase2Serializers[baseClass]?.get(value::class) as? SerializationStrategy<T>
        if (registered != null) return registered
        // Default
        return (polyBase2DefaultSerializerProvider[baseClass] as? PolymorphicSerializerProvider<T>)?.invoke(value)
    }

    override fun <T : Any> getPolymorphic(baseClass: KClass<in T>, serializedClassName: String?): DeserializationStrategy<T>? {
        // Registered
        val registered = polyBase2NamedSerializers[baseClass]?.get(serializedClassName) as? KSerializer<out T>
        if (registered != null) return registered
        // Default
        return (polyBase2DefaultDeserializerProvider[baseClass] as? PolymorphicDeserializerProvider<T>)?.invoke(serializedClassName)
    }

    override fun <T : Any> getContextual(kClass: KClass<T>, typeArgumentsSerializers: List<KSerializer<*>>): KSerializer<T>? {
        return (class2ContextualFactory[kClass]?.invoke(typeArgumentsSerializers)) as? KSerializer<T>?
    }

    override fun dumpTo(collector: SerializersModuleCollector) {
        class2ContextualFactory.forEach { (kclass, serial) ->
            when (serial) {
                is ContextualProvider.Argless -> collector.contextual(
                    kclass as KClass<Any>,
                    serial.serializer as KSerializer<Any>
                )
                is ContextualProvider.WithTypeArguments -> collector.contextual(kclass, serial.provider)
            }
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

        polyBase2DefaultSerializerProvider.forEach { (baseClass, provider) ->
            collector.polymorphicDefaultSerializer(baseClass as KClass<Any>, provider as (PolymorphicSerializerProvider<Any>))
        }

        polyBase2DefaultDeserializerProvider.forEach { (baseClass, provider) ->
            collector.polymorphicDefaultDeserializer(baseClass as KClass<Any>, provider as (PolymorphicDeserializerProvider<out Any>))
        }
    }
}

internal typealias PolymorphicDeserializerProvider<Base> = (className: String?) -> DeserializationStrategy<Base>?
internal typealias PolymorphicSerializerProvider<Base> = (value: Base) -> SerializationStrategy<Base>?

/** This class is needed to support re-registering the same static (argless) serializers:
 *
 * ```
 * val m1 = serializersModuleOf(A::class, A.serializer())
 * val m2 = serializersModuleOf(A::class, A.serializer())
 * val aggregate = m1 + m2 // should not throw
 * ```
 */
internal sealed class ContextualProvider {
    abstract operator fun invoke(typeArgumentsSerializers: List<KSerializer<*>>): KSerializer<*>

    class Argless(val serializer: KSerializer<*>) : ContextualProvider() {
        override fun invoke(typeArgumentsSerializers: List<KSerializer<*>>): KSerializer<*> = serializer

        override fun equals(other: Any?): Boolean = other is Argless && other.serializer == this.serializer

        override fun hashCode(): Int = serializer.hashCode()
    }

    class WithTypeArguments(val provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>) :
        ContextualProvider() {
        override fun invoke(typeArgumentsSerializers: List<KSerializer<*>>): KSerializer<*> =
            provider(typeArgumentsSerializers)
    }

}
