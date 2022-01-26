/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * Returns a [SerializersModule] which has one class with one [serializer] for [ContextualSerializer].
 */
public fun <T : Any> serializersModuleOf(kClass: KClass<T>, serializer: KSerializer<T>): SerializersModule =
    SerializersModule { contextual(kClass, serializer) }

/**
 * Returns a [SerializersModule] which has one class with one [serializer] for [ContextualSerializer].
 */
public inline fun <reified T : Any> serializersModuleOf(serializer: KSerializer<T>): SerializersModule =
    serializersModuleOf(T::class, serializer)

/**
 * A builder function for creating a [SerializersModule].
 * Serializers can be added via [SerializersModuleBuilder.contextual] or [SerializersModuleBuilder.polymorphic].
 * Since [SerializersModuleBuilder] also implements [SerialModuleCollector],
 * it is possible to copy whole another module to this builder with [SerializersModule.dumpTo]
 */
@Suppress("FunctionName")
public inline fun SerializersModule(builderAction: SerializersModuleBuilder.() -> Unit): SerializersModule {
    val builder = SerializersModuleBuilder()
    builder.builderAction()
    return builder.build()
}

/**
 * A builder class for [SerializersModule] DSL. To create an instance of builder, use [SerializersModule] factory function.
 */
@OptIn(ExperimentalSerializationApi::class)
public class SerializersModuleBuilder @PublishedApi internal constructor() : SerializersModuleCollector {
    private val class2ContextualProvider: MutableMap<KClass<*>, ContextualProvider> = hashMapOf()
    private val polyBase2Serializers: MutableMap<KClass<*>, MutableMap<KClass<*>, KSerializer<*>>> = hashMapOf()
    private val polyBase2DefaultSerializerProvider: MutableMap<KClass<*>, PolymorphicSerializerProvider<*>> = hashMapOf()
    private val polyBase2NamedSerializers: MutableMap<KClass<*>, MutableMap<String, KSerializer<*>>> = hashMapOf()
    private val polyBase2DefaultDeserializerProvider: MutableMap<KClass<*>, PolymorphicDeserializerProvider<*>> = hashMapOf()

    /**
     * Adds [serializer] associated with given [kClass] for contextual serialization.
     * If [kClass] has generic type parameters, consider registering provider instead.
     *
     * Throws [SerializationException] if a module already has serializer or provider associated with a [kClass].
     * To overwrite an already registered serializer, [SerializersModule.overwriteWith] can be used.
     */
    public override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>): Unit =
        registerSerializer(kClass, ContextualProvider.Argless(serializer))

    /**
     * Registers [provider] associated with given generic [kClass] for contextual serialization.
     * When a serializer is requested from a module, provider is being called with type arguments serializers
     * of the particular [kClass] usage.
     *
     * Example:
     * ```
     * class Holder(@Contextual val boxI: Box<Int>, @Contextual val boxS: Box<String>)
     *
     * val module = SerializersModule {
     *   // args[0] contains Int.serializer() or String.serializer(), depending on the property
     *   contextual(Box::class) { args -> BoxSerializer(args[0]) }
     * }
     * ```
     *
     * Throws [SerializationException] if a module already has provider or serializer associated with a [kClass].
     * To overwrite an already registered serializer, [SerializersModule.overwriteWith] can be used.
     */
    public override fun <T : Any> contextual(
        kClass: KClass<T>,
        provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
    ): Unit = registerSerializer(kClass, ContextualProvider.WithTypeArguments(provider))

    /**
     * Adds [serializer][actualSerializer] associated with given [actualClass] in the scope of [baseClass] for polymorphic serialization.
     * Throws [SerializationException] if a module already has serializer associated with a [actualClass].
     * To overwrite an already registered serializer, [SerializersModule.overwriteWith] can be used.
     */
    public override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        registerPolymorphicSerializer(baseClass, actualClass, actualSerializer)
    }

    /**
     * Adds a default serializers provider associated with the given [baseClass] to the resulting module.
     * [defaultSerializerProvider] is invoked when no polymorphic serializers for `value` were found.
     *
     * This will not affect deserialization.
     */
    @ExperimentalSerializationApi
    public override fun <Base : Any> polymorphicDefaultSerializer(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?
    ) {
        registerDefaultPolymorphicSerializer(baseClass, defaultSerializerProvider, false)
    }

    /**
     * Adds a default deserializers provider associated with the given [baseClass] to the resulting module.
     * [defaultDeserializerProvider] is invoked when no polymorphic serializers associated with the `className`
     * were found. `className` could be `null` for formats that support nullable class discriminators
     * (currently only `Json` with `useArrayPolymorphism` set to `false`).
     *
     * This will not affect serialization.
     *
     * @see PolymorphicModuleBuilder.defaultDeserializer
     */
    @ExperimentalSerializationApi
    public override fun <Base : Any> polymorphicDefaultDeserializer(
        baseClass: KClass<Base>,
        defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
    ) {
        registerDefaultPolymorphicDeserializer(baseClass, defaultDeserializerProvider, false)
    }

    /**
     * Copies the content of [module] module into the current builder.
     */
    public fun include(module: SerializersModule) {
        module.dumpTo(this)
    }

    @JvmName("registerSerializer") // Don't mangle method name for prettier stack traces
    internal fun <T : Any> registerSerializer(
        forClass: KClass<T>,
        provider: ContextualProvider,
        allowOverwrite: Boolean = false
    ) {
        if (!allowOverwrite) {
            val previous = class2ContextualProvider[forClass]
            if (previous != null && previous != provider) {
                // How can we provide meaningful name for WithTypeArgumentsProvider ?
                throw SerializerAlreadyRegisteredException(
                    "Contextual serializer or serializer provider for $forClass already registered in this module"
                )
            }
        }
        class2ContextualProvider[forClass] = provider
    }

    @JvmName("registerDefaultPolymorphicSerializer") // Don't mangle method name for prettier stack traces
    internal fun <Base : Any> registerDefaultPolymorphicSerializer(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
        allowOverwrite: Boolean
    ) {
        val previous = polyBase2DefaultSerializerProvider[baseClass]
        if (previous != null && previous != defaultSerializerProvider && !allowOverwrite) {
            throw IllegalArgumentException("Default serializers provider for $baseClass is already registered: $previous")
        }
        polyBase2DefaultSerializerProvider[baseClass] = defaultSerializerProvider
    }

    @JvmName("registerDefaultPolymorphicDeserializer") // Don't mangle method name for prettier stack traces
    internal fun <Base : Any> registerDefaultPolymorphicDeserializer(
        baseClass: KClass<Base>,
        defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<out Base>?,
        allowOverwrite: Boolean
    ) {
        val previous = polyBase2DefaultDeserializerProvider[baseClass]
        if (previous != null && previous != defaultDeserializerProvider && !allowOverwrite) {
            throw IllegalArgumentException("Default deserializers provider for $baseClass is already registered: $previous")
        }
        polyBase2DefaultDeserializerProvider[baseClass] = defaultDeserializerProvider
    }

    @JvmName("registerPolymorphicSerializer") // Don't mangle method name for prettier stack traces
    internal fun <Base : Any, Sub : Base> registerPolymorphicSerializer(
        baseClass: KClass<Base>,
        concreteClass: KClass<Sub>,
        concreteSerializer: KSerializer<Sub>,
        allowOverwrite: Boolean = false
    ) {
        // Check for overwrite
        val name = concreteSerializer.descriptor.serialName
        val baseClassSerializers = polyBase2Serializers.getOrPut(baseClass, ::hashMapOf)
        val previousSerializer = baseClassSerializers[concreteClass]
        val names = polyBase2NamedSerializers.getOrPut(baseClass, ::hashMapOf)
        if (allowOverwrite) {
            // Remove previous serializers from name mapping
            if (previousSerializer != null) {
                names.remove(previousSerializer.descriptor.serialName)
            }
            // Update mappings
            baseClassSerializers[concreteClass] = concreteSerializer
            names[name] = concreteSerializer
            return
        }
        // Overwrite prohibited
        if (previousSerializer != null) {
            if (previousSerializer != concreteSerializer) {
                throw SerializerAlreadyRegisteredException(baseClass, concreteClass)
            } else {
                // Cleanup name mapping
                names.remove(previousSerializer.descriptor.serialName)
            }
        }
        val previousByName = names[name]
        if (previousByName != null) {
            val conflictingClass = polyBase2Serializers[baseClass]!!.asSequence().find { it.value === previousByName }
            throw IllegalArgumentException(
                "Multiple polymorphic serializers for base class '$baseClass' " +
                        "have the same serial name '$name': '$concreteClass' and '$conflictingClass'"
            )
        }
        // Overwrite if no conflicts
        baseClassSerializers[concreteClass] = concreteSerializer
        names[name] = concreteSerializer
    }

    @PublishedApi
    internal fun build(): SerializersModule =
        SerialModuleImpl(class2ContextualProvider, polyBase2Serializers, polyBase2DefaultSerializerProvider, polyBase2NamedSerializers, polyBase2DefaultDeserializerProvider)
}

/**
 * Adds [serializer] associated with given type [T] for contextual serialization.
 * Throws [SerializationException] if a module already has serializer associated with the given type.
 * To overwrite an already registered serializer, [SerializersModule.overwriteWith] can be used.
 */
public inline fun <reified T : Any> SerializersModuleBuilder.contextual(serializer: KSerializer<T>): Unit =
    contextual(T::class, serializer)

/**
 * Creates a builder to register subclasses of a given [baseClass] for polymorphic serialization.
 * If [baseSerializer] is not null, registers it as a serializer for [baseClass],
 * which is useful if the base class is serializable itself. To register subclasses,
 * [PolymorphicModuleBuilder.subclass] builder function can be used.
 *
 * If a serializer already registered for the given KClass in the given scope, an [IllegalArgumentException] is thrown.
 * To override registered serializers, combine built module with another using [SerializersModule.overwriteWith].
 *
 * @see PolymorphicSerializer
 */
public inline fun <Base : Any> SerializersModuleBuilder.polymorphic(
    baseClass: KClass<Base>,
    baseSerializer: KSerializer<Base>? = null,
    builderAction: PolymorphicModuleBuilder<Base>.() -> Unit = {}
) {
    val builder = PolymorphicModuleBuilder(baseClass, baseSerializer)
    builder.builderAction()
    builder.buildTo(this)
}

private class SerializerAlreadyRegisteredException internal constructor(msg: String) : IllegalArgumentException(msg) {
    internal constructor(
        baseClass: KClass<*>,
        concreteClass: KClass<*>
    ) : this("Serializer for $concreteClass already registered in the scope of $baseClass")
}
