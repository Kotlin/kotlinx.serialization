/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UNCHECKED_CAST", "RedundantVisibilityModifier")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlin.reflect.*

/**
 * [SerialModule] is a collection of serializers used by [ContextSerializer] and [PolymorphicSerializer]
 * to override or provide serializers at the runtime, whereas at the compile-time they provided by the serialization plugin.
 *
 * It can be considered as a map where serializers are found using statically known KClasses.
 *
 * To enable runtime resolution of serializers, one of the special annotations must be used on target types
 * and a serial module with serializers should be used during construction of [SerialFormat].
 *
 * @see ContextualSerialization
 * @see Polymorphic
 */
public interface SerialModule {

    /**
     * Returns a contextual serializer associated with a given [kclass].
     * This method is used in context-sensitive operations on a property marked with [ContextualSerialization] by a [ContextSerializer]
     */
    public fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>?

    /**
     * Returns a polymorphic serializer registered for a class of the given [value] in the scope of [baseClass].
     */
    public fun <T : Any> getPolymorphic(baseClass: KClass<T>, value: T): KSerializer<out T>?

    /**
     * Returns a polymorphic serializer registered for a [serializedClassName] in the scope of [baseClass].
     */
    public fun <T : Any> getPolymorphic(baseClass: KClass<T>, serializedClassName: String): KSerializer<out T>?

    /**
     * Copies contents of this module to the given [collector].
     */
    public fun dumpTo(collector: SerialModuleCollector)
}

/**
 * A [SerialModule] which is empty and always returns `null`.
 */
public object EmptyModule : SerialModule {
    public override fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>? = null
    public override fun <T : Any> getPolymorphic(baseClass: KClass<T>, value: T): KSerializer<out T>? = null
    public override fun <T : Any> getPolymorphic(
        baseClass: KClass<T>,
        serializedClassName: String
    ): KSerializer<out T>? = null

    public override fun dumpTo(collector: SerialModuleCollector): Unit = Unit
}
