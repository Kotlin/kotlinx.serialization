/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlin.reflect.*

/**
 * [SerializersModuleCollector] can introspect and accumulate content of any [SerializersModule] via [SerializersModule.dumpTo],
 * using a visitor-like pattern: [contextual] and [polymorphic] functions are invoked for each registered serializer.
 *
 * ### Not stable for inheritance
 *
 * `SerializersModuleCollector` interface is not stable for inheritance in 3rd party libraries, as new methods
 * might be added to this interface or contracts of the existing methods can be changed.
 */
@ExperimentalSerializationApi
public interface SerializersModuleCollector {

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
     *
     * @see SerializersModuleBuilder.polymorphicDefault
     * @see PolymorphicModuleBuilder.default
     */
    public fun <Base : Any> polymorphicDefault(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
    )
}
