/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * Returns a [SerialModule] which has one class with one serializer for [ContextSerializer].
 */
public fun <T : Any> serializersModuleOf(kClass: KClass<T>, serializer: KSerializer<T>): SerialModule =
    SerializersModule { contextual(kClass, serializer) }

/**
 * Shortcut for [serializersModuleOf] function with type parameter.
 */
// it could be named `serializersModuleOf`, too, but https://youtrack.jetbrains.com/issue/KT-30176.
public inline fun <reified T : Any> serializersModule(serializer: KSerializer<T>): SerialModule =
    serializersModuleOf(T::class, serializer)

/**
 * Returns a [SerialModule] which has multiple classes with its serializers for [ContextSerializer].
 */
@Suppress("UNCHECKED_CAST")
public fun serializersModuleOf(map: Map<KClass<*>, KSerializer<*>>): SerialModule = SerializersModule {
    map.forEach { (kclass, serializer) -> contextual(kclass as KClass<Any>, serializer.cast()) }
}

/**
 * A builder function for creating a [SerialModule].
 * Serializers can be add via [SerializersModuleBuilder.contextual] or [SerializersModuleBuilder.polymorphic].
 * Since [SerializersModuleBuilder] also implements [SerialModuleCollector], it is possible to copy whole another module to this builder with [SerialModule.dumpTo]
 */
@Suppress("FunctionName")
public fun SerializersModule(buildAction: SerializersModuleBuilder.() -> Unit): SerialModule {
    val builder = SerializersModuleBuilder()
    builder.buildAction()
    return builder.build()
}

/**
 * A builder class for [SerializersModule] DSL. To create an instance of builder, use [SerializersModule] factory function.
 */
public class SerializersModuleBuilder internal constructor() : SerialModuleCollector {
    private val class2Serializer: MutableMap<KClass<*>, KSerializer<*>> = hashMapOf()
    private val polyBase2Serializers: MutableMap<KClass<*>, MutableMap<KClass<*>, KSerializer<*>>> = hashMapOf()
    private val polyBase2NamedSerializers: MutableMap<KClass<*>, MutableMap<String, KSerializer<*>>> = hashMapOf()
    private val polyBase2DefaultProvider: MutableMap<KClass<*>, PolymorphicProvider<*>> = hashMapOf()

    /**
     * Adds [serializer] associated with given [kClass] for contextual serialization.
     * Throws [SerializationException] if a module already has serializer associated with a [kClass].
     * To overwrite an already registered serializer, [SerialModule.overwriteWith] can be used.
     */
    public override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>): Unit =
        registerSerializer(kClass, serializer)

    /**
     * A reified version of `contextual(KClass, Serializer)`
     */
    public inline fun <reified T : Any> contextual(): Unit = contextual(T::class, serializer())

    /**
     * Adds [serializer][actualSerializer] associated with given [actualClass] in the scope of [baseClass] for polymorphic serialization.
     * Throws [SerializationException] if a module already has serializer associated with a [actualClass].
     * To overwrite an already registered serializer, [SerialModule.overwriteWith] can be used.
     */
    public override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        registerPolymorphicSerializer(baseClass, actualClass, actualSerializer)
    }

    public override fun <Base : Any> defaultPolymorphic(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?
    ) {
        registerDefaultPolymorphicSerializer(baseClass, defaultSerializerProvider, false)
    }

    /**
     * Copies contents of [other] module into the current builder.
     */
    public fun include(other: SerialModule) {
        other.dumpTo(this)
    }

    /**
     * Creates a builder to register all subclasses of a given [baseClass]
     * for polymorphic serialization. If [baseSerializer] is not null, registers it as a serializer for [baseClass]
     * (which is useful if base class is serializable). To add subclasses, use
     * [PolymorphicModuleBuilder.subclass] or [PolymorphicModuleBuilder.with].
     *
     * If serializer already registered for the given KClass in the given scope, a [SerializerAlreadyRegisteredException] is thrown.
     * To override registered serializers, combine built module with another using
     * [SerialModule.overwriteWith].
     *
     * @see PolymorphicSerializer
     */
    public fun <Base : Any> polymorphic(
        baseClass: KClass<Base>,
        baseSerializer: KSerializer<Base>? = null,
        buildAction: PolymorphicModuleBuilder<Base>.() -> Unit = {}
    ) {
        val builder = PolymorphicModuleBuilder(baseClass, baseSerializer)
        builder.buildAction()
        builder.buildTo(this)
    }

    public inline fun <reified Base : Any> polymorphic(
        baseSerializer: KSerializer<Base>? = null,
        noinline buildAction: PolymorphicModuleBuilder<Base>.() -> Unit = {}
    ): Unit = polymorphic(Base::class, baseSerializer, buildAction)

    /**
     * Creates a builder to register all serializable subclasses for polymorphic serialization
     * for multiple base classes. This is useful when you have more two or more super classes in a large hierarchy, e.g.:
     *
     * ```
     * interface I
     * @Serializable abstract class A() : I
     * @Serializable final class B : A()
     * @Serializable class Message(@Polymorphic val i: I, @Polymorphic val a: A)
     * ```
     *
     * In this case, you have to register B as subclass for two base classes: I and A.
     *
     * Note that serializer (if present) for each of the [baseClasses] should be
     * registered separately inside [buildAction] to avoid duplicates, e.g.:
     *
     * ```
     * polymorphic(Any::class, PolyBase::class) {
     *   PolyBase::class with PolyBase.serializer()
     *   subclass<PolyDerived>() // Shorthand with default serializer
     * }
     * ```
     *
     * If serializer already registered for the given KClass in the given scope, a [SerializerAlreadyRegisteredException] is thrown.
     * To override registered serializers, combine built module with another using
     * [SerialModule.overwriteWith].
     *
     * @see PolymorphicSerializer
     */
    @Suppress("UNCHECKED_CAST")
    public fun polymorphic(
        baseClass: KClass<*>,
        vararg baseClasses: KClass<*>,
        buildAction: PolymorphicModuleBuilder<Any>.() -> Unit = {}
    ) {
        val builder = PolymorphicModuleBuilder(baseClass as KClass<Any>)
        builder.buildAction()
        builder.buildTo(this)
        for (base in baseClasses) {
            builder.changeBase(base as KClass<Any>, null).buildTo(this)
        }
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
        defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?,
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

    internal fun build(): SerialModule =
        SerialModuleImpl(class2Serializer, polyBase2Serializers, polyBase2NamedSerializers, polyBase2DefaultProvider)
}
