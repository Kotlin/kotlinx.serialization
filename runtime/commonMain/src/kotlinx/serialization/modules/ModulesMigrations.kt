/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlin.reflect.*

private fun noImpl(): Nothing = throw UnsupportedOperationException("Not implemented, should not be called")

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "SerialModule was renamed to SerializersModule during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("SerializersModule")
)
public typealias SerialModule = SerializersModule

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "EmptyModule was renamed to EmptySerializersModule during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("EmptySerializersModule")
)
public val EmptyModule = EmptySerializersModule

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "SerialModuleCollector was renamed to SerializersModuleCollector during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("SerializersModuleCollector")
)
public typealias  SerialModuleCollector = SerializersModuleCollector

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Renamed to serializersModuleOf during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("serializersModuleOf(serializer)")
)
public fun <T : Any> serializersModule(serializer: KSerializer<T>): SerializersModule = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was removed during serialization 1.0 API stabilization, " +
            "please use SerializersModule builder instead" // No replacement deliberately
)
public fun serializersModuleOf(map: Map<KClass<*>, KSerializer<*>>): SerializersModule = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was removed during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("polymorphic(Base::class, baseSerializer, builderAction)")
)
@kotlin.internal.LowPriorityInOverloadResolution
public fun <Base : Any> SerializersModuleBuilder.polymorphic(
    baseSerializer: KSerializer<Base>? = null,
    builderAction: PolymorphicModuleBuilder<Base>.() -> Unit = {}
): Unit = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was removed during serialization 1.0 API stabilization, please replace it with a hand-rolled loop" // No replacement deliberately
)
@kotlin.internal.LowPriorityInOverloadResolution
public fun SerializersModuleBuilder.polymorphic(
    baseClass: KClass<*>,
    vararg baseClasses: KClass<*>,
    buildAction: PolymorphicModuleBuilder<Any>.() -> Unit
): Unit = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was renamed to polymorphicDefault during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("polymorphicDefault(baseClass, defaultSerializerProvider)")
)
public fun <Base : Any> SerializersModuleCollector.defaultPolymorphic(
    baseClass: KClass<Base>,
    defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?
): Unit = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was removed during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("getContextual(T::class) as KSerializer<T>")
)
public fun <T : Any> SerializersModule.getContextual(): KSerializer<T>? = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was removed during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith("getContextual(value::class) as KSerializer<T>")
)
public fun <T : Any> SerializersModule.getContextual(value: T): KSerializer<T>? = noImpl()
