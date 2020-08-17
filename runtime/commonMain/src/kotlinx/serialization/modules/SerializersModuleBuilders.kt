/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.json.*
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
    private val class2Serializer: MutableMap<KClass<*>, KSerializer<*>> = hashMapOf()
    private val polyBase2Serializers: MutableMap<KClass<*>, MutableMap<KClass<*>, KSerializer<*>>> = hashMapOf()
    private val polyBase2NamedSerializers: MutableMap<KClass<*>, MutableMap<String, KSerializer<*>>> = hashMapOf()
    private val polyBase2DefaultProvider: MutableMap<KClass<*>, PolymorphicProvider<*>> = hashMapOf()

    /**
     * Adds [serializer] associated with given [kClass] for contextual serialization.
     * Throws [SerializationException] if a module already has serializer associated with a [kClass].
     * To overwrite an already registered serializer, [SerializersModule.overwriteWith] can be used.
     */
    public override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>): Unit =
        registerSerializer(kClass, serializer)

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
     * [defaultSerializerProvider] is invoked when no polymorphic serializers associated with the `className`
     * were found. `className` could be `null` for formats that support nullable class discriminators
     * (currently only [Json] with [useArrayPolymorphism][JsonBuilder.useArrayPolymorphism] set to `false`)
     *
     * @see PolymorphicModuleBuilder.default
     */
    public override fun <Base : Any> polymorphicDefault(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
    ) {
        registerDefaultPolymorphicSerializer(baseClass, defaultSerializerProvider, false)
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
        serializer: KSerializer<T>,
        allowOverwrite: Boolean = false
    ) {
        if (!allowOverwrite) {
            val previous = class2Serializer[forClass]
            if (previous != null && previous != serializer) {
                // TODO when working on SD rework, provide a way to properly stringify serializer as its FQN
                val currentName = serializer.descriptor.serialName
                val previousName = previous.descriptor.serialName
                throw SerializerAlreadyRegisteredException(
                    "Serializer for $forClass already registered in this module: $previous ($previousName), " +
                            "attempted to register $serializer ($currentName)"
                )
            }
        }
        class2Serializer[forClass] = serializer
    }

    @JvmName("registerDefaultPolymorphicSerializer") // Don't mangle method name for prettier stack traces
    internal fun <Base : Any> registerDefaultPolymorphicSerializer(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?,
        allowOverwrite: Boolean
    ) {
        val previous = polyBase2DefaultProvider[baseClass]
        if (previous != null && previous != defaultSerializerProvider && !allowOverwrite) {
            throw IllegalArgumentException("Default serializers provider for class $baseClass is already registered: $previous")
        }
        polyBase2DefaultProvider[baseClass] = defaultSerializerProvider
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
        SerialModuleImpl(class2Serializer, polyBase2Serializers, polyBase2NamedSerializers, polyBase2DefaultProvider)
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
