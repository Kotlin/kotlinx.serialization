/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.modules

import kotlinx.serialization.*
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
    map.forEach { (kclass, serializer) -> contextual(kclass as KClass<Any>, serializer as KSerializer<Any>) }
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
 * Reified version of `SerializersModuleBuilder.contextual(KClass, Serializer)`
 */
@ImplicitReflectionSerializer
public inline fun <reified T : Any> SerializersModuleBuilder.contextual() = contextual(T::class, serializer())

/**
 * A builder class for [SerializersModule] DSL.
 */
public class SerializersModuleBuilder internal constructor() : SerialModuleCollector {
    private val class2Serializer: MutableMap<KClass<*>, KSerializer<*>> = hashMapOf()
    private val polyBase2Serializers: MutableMap<KClass<*>, MutableMap<KClass<*>, KSerializer<*>>> = hashMapOf()
    private val polyBase2NamedSerializers: MutableMap<KClass<*>, MutableMap<String, KSerializer<*>>> = hashMapOf()
    /**
     * Adds [serializer] associated with given [kClass] for contextual serialization.
     * Throws [SerializationException] if a module already has serializer associated with a [kClass].
     * To overwrite an already registered serializer, [SerialModule.overwriteWith] can be used.
     */
    public override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) = registerSerializer(kClass, serializer)

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

    /**
     * Copies contents of [other] module into current builder.
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
    ) = polymorphic(Base::class, baseSerializer, buildAction)

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

    internal fun <T : Any> registerSerializer(
        forClass: KClass<T>,
        serializer: KSerializer<T>,
        allowOverwrite: Boolean = false
    ) {
        if (!allowOverwrite && forClass in class2Serializer) throw SerializerAlreadyRegisteredException(forClass)
        class2Serializer[forClass] = serializer
    }

    internal fun <Base : Any, Sub : Base> registerPolymorphicSerializer(
        baseClass: KClass<Base>,
        concreteClass: KClass<Sub>,
        concreteSerializer: KSerializer<Sub>,
        allowOverwrite: Boolean = false
    ) {
        val name = concreteSerializer.descriptor.serialName
        val baseClassSerializers = polyBase2Serializers.getOrPut(baseClass, ::hashMapOf)
        if (!allowOverwrite && concreteClass in baseClassSerializers) throw SerializerAlreadyRegisteredException(
            baseClass,
            concreteClass
        )
        baseClassSerializers[concreteClass] = concreteSerializer

        val names = polyBase2NamedSerializers.getOrPut(baseClass, ::hashMapOf)
        val previous = names.put(name, concreteSerializer)
        if (previous != null) {
            val conflictingClass = polyBase2Serializers[baseClass]!!.filter { it.value === previous }.keys.firstOrNull()
            throw IllegalArgumentException("Multiple polymorphic serializers for base class '$baseClass' " +
                    "have the same serial name '$name': '$concreteClass' and '$conflictingClass'")
        }
    }

    internal fun build(): SerialModule = SerialModuleImpl(class2Serializer, polyBase2Serializers, polyBase2NamedSerializers)
}
