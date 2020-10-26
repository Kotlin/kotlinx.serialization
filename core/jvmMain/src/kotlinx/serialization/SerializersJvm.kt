/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:JvmMultifileClass
@file:JvmName("SerializersKt")
@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.TripleSerializer
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import java.lang.reflect.*
import kotlin.reflect.*

/**
 * Reflectively constructs a serializer for the given reflective Java [type].
 * [serializer] is intended to be used as an interoperability layer for libraries like GSON and Retrofit,
 * that operate with reflective Java [Type] and cannot use [typeOf].
 *
 * For application-level serialization, it is recommended to use `serializer<T>()` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * @throws SerializationException if serializer cannot be created (provided [type] or its type argument is not serializable).
 */
@ExperimentalSerializationApi
public fun serializer(type: Type): KSerializer<Any> = EmptySerializersModule.serializer(type)

/**
 * Reflectively constructs a serializer for the given reflective Java [type].
 * [serializer] is intended to be used as an interoperability layer for libraries like GSON and Retrofit,
 * that operate with reflective Java [Type] and cannot use [typeOf].
 *
 * For application-level serialization, it is recommended to use `serializer<T>()` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * Returns `null` if serializer cannot be created (provided [type] or its type argument is not serializable).
 */
@ExperimentalSerializationApi
public fun serializerOrNull(type: Type): KSerializer<Any>? = EmptySerializersModule.serializerOrNull(type)

/**
 * Retrieves serializer for the given reflective Java [type] using
 * reflective construction and [contextual][SerializersModule.getContextual] lookup for non-serializable types.
 *
 * [serializer] is intended to be used as an interoperability layer for libraries like GSON and Retrofit,
 * that operate with reflective Java [Type] and cannot use [typeOf].
 *
 * For application-level serialization, it is recommended to use `serializer<T>()` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * @throws SerializationException if serializer cannot be created (provided [type] or its type argument is not serializable).
 */
@ExperimentalSerializationApi
public fun SerializersModule.serializer(type: Type): KSerializer<Any> =
    serializerByJavaTypeImpl(type, failOnMissingTypeArgSerializer = true) ?: type.kclass().serializerNotRegistered()

/**
 * Retrieves serializer for the given reflective Java [type] using
 * reflective construction and [contextual][SerializersModule.getContextual] lookup for non-serializable types.
 *
 * [serializer] is intended to be used as an interoperability layer for libraries like GSON and Retrofit,
 * that operate with reflective Java [Type] and cannot use [typeOf].
 *
 * For application-level serialization, it is recommended to use `serializer<T>()` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * Returns `null` if serializer cannot be created (provided [type] or its type argument is not serializable).
 */
@ExperimentalSerializationApi
public fun SerializersModule.serializerOrNull(type: Type): KSerializer<Any>? =
    serializerByJavaTypeImpl(type, failOnMissingTypeArgSerializer = false)

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.serializerByJavaTypeImpl(type: Type, failOnMissingTypeArgSerializer: Boolean = true): KSerializer<Any>? =
    when (type) {
        is GenericArrayType -> {
            genericArraySerializer(type, failOnMissingTypeArgSerializer)
        }
        is Class<*> -> typeSerializer(type, failOnMissingTypeArgSerializer)
        is ParameterizedType -> {
            val rootClass = (type.rawType as Class<*>)
            val args = (type.actualTypeArguments)
            val argsSerializers =
                if (failOnMissingTypeArgSerializer) args.map { serializer(it) } else args.map { serializerOrNull(it) ?: return null }
            when {
                Set::class.java.isAssignableFrom(rootClass) -> SetSerializer(argsSerializers[0]) as KSerializer<Any>
                List::class.java.isAssignableFrom(rootClass) || Collection::class.java.isAssignableFrom(rootClass) -> ListSerializer(
                    argsSerializers[0]
                ) as KSerializer<Any>
                Map::class.java.isAssignableFrom(rootClass) -> MapSerializer(
                    argsSerializers[0],
                    argsSerializers[1]
                ) as KSerializer<Any>
                Map.Entry::class.java.isAssignableFrom(rootClass) -> MapEntrySerializer(
                    argsSerializers[0],
                    argsSerializers[1]
                ) as KSerializer<Any>
                Pair::class.java.isAssignableFrom(rootClass) -> PairSerializer(
                    argsSerializers[0],
                    argsSerializers[1]
                ) as KSerializer<Any>
                Triple::class.java.isAssignableFrom(rootClass) -> TripleSerializer(
                    argsSerializers[0],
                    argsSerializers[1],
                    argsSerializers[2]
                ) as KSerializer<Any>

                else -> {
                    // probably we should deprecate this method because it can't differ nullable vs non-nullable types
                    // since it uses Java TypeToken, not Kotlin one
                    val varargs = argsSerializers.map { it as KSerializer<Any?> }.toTypedArray()
                    (rootClass.kotlin.constructSerializerForGivenTypeArgs(*varargs) as? KSerializer<Any>)
                            ?: reflectiveOrContextual(rootClass.kotlin as KClass<Any>)
                }
            }
        }
        is WildcardType -> serializerByJavaTypeImpl(type.upperBounds.first())
        else -> throw IllegalArgumentException("typeToken should be an instance of Class<?>, GenericArray, ParametrizedType or WildcardType, but actual type is $type ${type::class}")
    }

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.typeSerializer(type: Class<*>, failOnMissingTypeArgSerializer: Boolean): KSerializer<Any>? {
    return if (!type.isArray) {
        reflectiveOrContextual(type.kotlin as KClass<Any>)
    } else {
        val eType: Class<*> = type.componentType
        val s = if (failOnMissingTypeArgSerializer) serializer(eType) else (serializerOrNull(eType) ?: return null)
        val arraySerializer = ArraySerializer(eType.kotlin as KClass<Any>, s)
        arraySerializer as KSerializer<Any>
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.genericArraySerializer(
    type: GenericArrayType,
    failOnMissingTypeArgSerializer: Boolean
): KSerializer<Any>? {
    val eType = type.genericComponentType.let {
        when (it) {
            is WildcardType -> it.upperBounds.first()
            else -> it
        }
    }
    val serializer = if (failOnMissingTypeArgSerializer) serializer(eType) else (serializerOrNull(eType) ?: return null)
    val kclass = when (eType) {
        is ParameterizedType -> (eType.rawType as Class<*>).kotlin
        is KClass<*> -> eType
        else -> throw IllegalStateException("unsupported type in GenericArray: ${eType::class}")
    } as KClass<Any>
    return ArraySerializer(kclass, serializer) as KSerializer<Any>
}

@OptIn(ExperimentalSerializationApi::class)
private fun <T : Any> SerializersModule.reflectiveOrContextual(kClass: KClass<T>): KSerializer<T>? {
    return kClass.serializerOrNull() ?: getContextual(kClass)
}

private fun Type.kclass(): KClass<*> = when (val it = this) {
    is KClass<*> -> it
    is Class<*> -> it.kotlin
    is ParameterizedType -> it.rawType.kclass()
    is WildcardType -> it.upperBounds.first().kclass()
    is GenericArrayType -> it.genericComponentType.kclass()
    else -> throw IllegalArgumentException("typeToken should be an instance of Class<?>, GenericArray, ParametrizedType or WildcardType, but actual type is $it ${it::class}")
}
