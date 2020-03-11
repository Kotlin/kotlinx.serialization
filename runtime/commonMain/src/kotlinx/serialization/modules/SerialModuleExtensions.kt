/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * Returns a dependent serializer associated with a given reified type.
 */
public inline fun <reified T : Any> SerialModule.getContextual(): KSerializer<T>? = getContextual(T::class)

/**
 * Returns a serializer associated with KClass of the given [value].
 */
public fun <T : Any> SerialModule.getContextual(value: T): KSerializer<T>? {
    return getContextual(value::class)?.cast()
}

@ImplicitReflectionSerializer
public fun <T : Any> SerialModule.getContextualOrDefault(klass: KClass<T>): KSerializer<T> =
    getContextual(klass) ?: klass.serializer()

@ImplicitReflectionSerializer
public fun <T : Any> SerialModule.getContextualOrDefault(value: T): KSerializer<T> =
    getContextual(value) ?: value::class.serializer().cast()

/**
 * Returns a combination of two serial modules
 *
 * If some serializer present in both modules, an [SerializerAlreadyRegisteredException] is thrown.
 * To overwrite serializers, use [SerialModule.overwriteWith] function.
 */
public operator fun SerialModule.plus(other: SerialModule): SerialModule = SerializersModule {
    include(this@plus)
    include(other)
}

/**
 * Returns a combination of two serial modules
 *
 * If some serializers present in both modules, result module
 * will contain serializer from [other] module.
 */
public infix fun SerialModule.overwriteWith(other: SerialModule): SerialModule = SerializersModule {
    include(this@overwriteWith)
    other.dumpTo(object : SerialModuleCollector {
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
    })
}
