/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlin.reflect.*

/**
 * [SerialModuleCollector] can introspect and accumulate content of any [SerialModule] via [SerialModule.dumpTo],
 * using a visitor-like pattern: [contextual] and [polymorphic] functions are invoked for each registered serializer.
 */
public interface SerialModuleCollector {

    /**
     * Accept a serializer, associated with [kClass] for contextual serialization.
     */
    public fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>)

    /**
     * Accept a serializer, associated with [actualClass] for polymorphic serialization.
     */
    public fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    )

    /**
     * Accept a default serializer provider, associated with the [baseClass] for polymorphic serialization.
     */
    public fun <Base : Any> defaultPolymorphic(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?
    )
}

/**
 * Version of [SerialModuleCollector.contextual] with reified argument.
 */
public inline fun <reified T : Any> SerialModuleCollector.contextual(serializer: KSerializer<T>) {
    contextual(T::class, serializer)
}
