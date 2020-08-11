/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:JvmMultifileClass
@file:JvmName("SerializersKt")
@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import java.lang.reflect.*
import kotlin.reflect.*

/**
 * Constructs a serializer for the given reflective Java [type].
 * [serializer] is intended to be used as an interoperability layer for libraries like GSON and Retrofit,
 * that operate with reflective Java [Type] and cannot use [typeOf].
 *
 * For application-level serialization, it is recommended to use `serializer<T>()` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object.
 */
@ExperimentalSerializationApi
public fun serializer(type: Type): KSerializer<Any> = EmptySerializersModule.serializer(type)

/**
 * Retrieves serializer for the given reflective Java [type] using
 * reflective construction and [contextual][SerializersModule.getContextual] lookup for non-serializable types.
 *
 * [serializer] is intended to be used as an interoperability layer for libraries like GSON and Retrofit,
 * that operate with reflective Java [Type] and cannot use [typeOf].
 *
 * For application-level serialization, it is recommended to use `serializer<T>()` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 */
@ExperimentalSerializationApi
public fun SerializersModule.serializer(type: Type): KSerializer<Any> = when (type) {
    is GenericArrayType -> {
        genericArraySerializer(type)
    }
    is Class<*> -> typeSerializer(type)
    is ParameterizedType -> {
        val rootClass = (type.rawType as Class<*>)
        val args = (type.actualTypeArguments)
        when {
            List::class.java.isAssignableFrom(rootClass) -> ListSerializer(serializer(args[0])) as KSerializer<Any>
            Set::class.java.isAssignableFrom(rootClass) -> SetSerializer(serializer(args[0])) as KSerializer<Any>
            Map::class.java.isAssignableFrom(rootClass) -> MapSerializer(
                serializer(args[0]),
                serializer(args[1])
            ) as KSerializer<Any>
            Map.Entry::class.java.isAssignableFrom(rootClass) -> MapEntrySerializer(
                serializer(args[0]),
                serializer(args[1])
            ) as KSerializer<Any>

            else -> {
                // probably we should deprecate this method because it can't differ nullable vs non-nullable types
                // since it uses Java TypeToken, not Kotlin one
                val varargs = args.map { serializer(it) as KSerializer<Any?> }.toTypedArray()
                (rootClass.kotlin.constructSerializerForGivenTypeArgs(*varargs) as? KSerializer<Any>)
                    ?: reflectiveOrContextual(rootClass.kotlin as KClass<Any>)
            }
        }
    }
    is WildcardType -> serializer(type.upperBounds.first())
    else -> throw IllegalArgumentException("typeToken should be an instance of Class<?>, GenericArray, ParametrizedType or WildcardType, but actual type is $type ${type::class}")
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.typeSerializer(type: Class<*>): KSerializer<Any> {
    return if (!type.isArray) {
        reflectiveOrContextual(type.kotlin as KClass<Any>)
    } else {
        val eType: Class<*> = type.componentType
        val s = serializer(eType)
        val arraySerializer = ArraySerializer(eType.kotlin as KClass<Any>, s)
        arraySerializer as KSerializer<Any>
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.genericArraySerializer(type: GenericArrayType): KSerializer<Any> {
    val eType = type.genericComponentType.let {
        when (it) {
            is WildcardType -> it.upperBounds.first()
            else -> it
        }
    }
    val serializer = serializer(eType)
    val kclass = when (eType) {
        is ParameterizedType -> (eType.rawType as Class<*>).kotlin
        is KClass<*> -> eType
        else -> throw IllegalStateException("unsupported type in GenericArray: ${eType::class}")
    } as KClass<Any>
    return ArraySerializer(kclass, serializer) as KSerializer<Any>
}

@OptIn(ExperimentalSerializationApi::class)
private fun <T: Any> SerializersModule.reflectiveOrContextual(kClass: KClass<T>): KSerializer<T> {
    return kClass.serializerOrNull() ?: getContextual(kClass) ?: kClass.serializerNotRegistered()
}

@Deprecated("Deprecated during serialization 1.0 API stabilization", ReplaceWith("serializer(type)"), level = DeprecationLevel.ERROR)
public fun serializerByTypeToken(type: Type): KSerializer<Any> = serializer(type)

@Deprecated("Deprecated during serialization 1.0 API stabilization", ReplaceWith("typeOf()"), level = DeprecationLevel.ERROR)
public inline fun <reified T> typeTokenOf(): Type = error("Should not be called")
