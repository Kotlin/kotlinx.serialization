/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
 * Reflectively retrieves a serializer for the given [type].
 *
 * This overload is intended to be used as an interoperability layer for JVM-centric libraries,
 * that operate with Java's type tokens and cannot use Kotlin's [KType] or [typeOf].
 * For application-level serialization, it is recommended to use `serializer<T>()` or `serializer(KType)` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * Note that because [Type] does not contain any information about nullability, all created serializers
 * work only with non-nullable data.
 *
 * Not all [Type] implementations are supported.
 * [type] must be an instance of [Class], [GenericArrayType], [ParameterizedType] or [WildcardType].
 *
 * @throws SerializationException if serializer cannot be created (provided [type] or its type argument is not serializable).
 * @throws IllegalArgumentException if an unsupported subclass of [Type] is provided.
 */
public fun serializer(type: Type): KSerializer<Any> = EmptySerializersModule().serializer(type)

/**
 * Reflectively retrieves a serializer for the given [type].
 *
 * This overload is intended to be used as an interoperability layer for JVM-centric libraries,
 * that operate with Java's type tokens and cannot use Kotlin's [KType] or [typeOf].
 * For application-level serialization, it is recommended to use `serializer<T>()` or `serializer(KType)` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * Note that because [Type] does not contain any information about nullability, all created serializers
 * work only with non-nullable data.
 *
 * Not all [Type] implementations are supported.
 * [type] must be an instance of [Class], [GenericArrayType], [ParameterizedType] or [WildcardType].
 *
 * @return [KSerializer] for given [type] or `null` if serializer cannot be created (given [type] or its type argument is not serializable).
 * @throws IllegalArgumentException if an unsupported subclass of [Type] is provided.
 */
public fun serializerOrNull(type: Type): KSerializer<Any>? = EmptySerializersModule().serializerOrNull(type)

/**
 * Retrieves a serializer for the given [type] using
 * reflective construction and [contextual][SerializersModule.getContextual] lookup as a fallback for non-serializable types.
 *
 * This overload is intended to be used as an interoperability layer for JVM-centric libraries,
 * that operate with Java's type tokens and cannot use Kotlin's [KType] or [typeOf].
 * For application-level serialization, it is recommended to use `serializer<T>()` or `serializer(KType)` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * Note that because [Type] does not contain any information about nullability, all created serializers
 * work only with non-nullable data.
 *
 * Not all [Type] implementations are supported.
 * [type] must be an instance of [Class], [GenericArrayType], [ParameterizedType] or [WildcardType].
 *
 * @throws SerializationException if serializer cannot be created (provided [type] or its type argument is not serializable).
 * @throws IllegalArgumentException if an unsupported subclass of [Type] is provided.
 */
public fun SerializersModule.serializer(type: Type): KSerializer<Any> =
    serializerByJavaTypeImpl(type, failOnMissingTypeArgSerializer = true)
        ?: type.prettyClass().serializerNotRegistered()

/**
 * Retrieves a serializer for the given [type] using
 * reflective construction and [contextual][SerializersModule.getContextual] lookup as a fallback for non-serializable types.
 *
 * This overload is intended to be used as an interoperability layer for JVM-centric libraries,
 * that operate with Java's type tokens and cannot use Kotlin's [KType] or [typeOf].
 * For application-level serialization, it is recommended to use `serializer<T>()` or `serializer(KType)` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object singletons.
 *
 * Note that because [Type] does not contain any information about nullability, all created serializers
 * work only with non-nullable data.
 *
 * Not all [Type] implementations are supported.
 * [type] must be an instance of [Class], [GenericArrayType], [ParameterizedType] or [WildcardType].
 *
 * @return [KSerializer] for given [type] or `null` if serializer cannot be created (given [type] or its type argument is not serializable).
 * @throws IllegalArgumentException if an unsupported subclass of [Type] is provided.
 */
public fun SerializersModule.serializerOrNull(type: Type): KSerializer<Any>? =
    serializerByJavaTypeImpl(type, failOnMissingTypeArgSerializer = false)

private fun SerializersModule.serializerByJavaTypeImpl(
    type: Type,
    failOnMissingTypeArgSerializer: Boolean = true
): KSerializer<Any>? =
    when (type) {
        is GenericArrayType -> {
            genericArraySerializer(type, failOnMissingTypeArgSerializer)
        }
        is Class<*> -> typeSerializer(type, failOnMissingTypeArgSerializer)
        is ParameterizedType -> {
            val rootClass = (type.rawType as Class<*>)
            val args = (type.actualTypeArguments)
            val argsSerializers =
                if (failOnMissingTypeArgSerializer) args.map { serializer(it) } else args.map {
                    serializerOrNull(it) ?: return null
                }
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
                    val varargs = argsSerializers.map { it as KSerializer<Any?> }
                    reflectiveOrContextual(rootClass as Class<Any>, varargs)
                }
            }
        }
        is WildcardType -> serializerByJavaTypeImpl(type.upperBounds.first())
        else -> throw IllegalArgumentException("type should be an instance of Class<?>, GenericArrayType, ParametrizedType or WildcardType, but actual argument $type has type ${type::class}")
    }

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.typeSerializer(
    type: Class<*>,
    failOnMissingTypeArgSerializer: Boolean
): KSerializer<Any>? {
    return if (type.isArray && !type.componentType.isPrimitive) {
        val eType: Class<*> = type.componentType
        val s = if (failOnMissingTypeArgSerializer) serializer(eType) else (serializerOrNull(eType) ?: return null)
        val arraySerializer = ArraySerializer(eType.kotlin as KClass<Any>, s)
        arraySerializer as KSerializer<Any>
    } else {
        reflectiveOrContextual(type as Class<Any>, emptyList())
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun <T : Any> SerializersModule.reflectiveOrContextual(
    jClass: Class<T>,
    typeArgumentsSerializers: List<KSerializer<Any?>>
): KSerializer<T>? {
    jClass.constructSerializerForGivenTypeArgs(*typeArgumentsSerializers.toTypedArray())?.let { return it }
    val kClass = jClass.kotlin
    return kClass.builtinSerializerOrNull() ?: getContextual(kClass, typeArgumentsSerializers)
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

private fun Type.prettyClass(): Class<*> = when (val it = this) {
    is Class<*> -> it
    is ParameterizedType -> it.rawType.prettyClass()
    is WildcardType -> it.upperBounds.first().prettyClass()
    is GenericArrayType -> it.genericComponentType.prettyClass()
    else -> throw IllegalArgumentException("type should be an instance of Class<?>, GenericArrayType, ParametrizedType or WildcardType, but actual argument $it has type ${it::class}")
}
