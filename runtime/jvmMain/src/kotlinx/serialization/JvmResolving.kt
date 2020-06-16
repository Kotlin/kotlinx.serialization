/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import java.lang.reflect.*
import kotlin.reflect.*

@PublishedApi
internal open class TypeBase<T>

/**
 * Provides an instance of [java.lang.reflect.Type] with
 * generic arguments filled in.
 *
 * Consider using Kotlin's [typeOf] and [KType] since they can
 * also provide information about nullability.
 */
@Deprecated("Consider using Kotlin type token instead", ReplaceWith("typeOf()"), level = DeprecationLevel.WARNING)
public inline fun <reified T> typeTokenOf(): Type {
    val base = object : TypeBase<T>() {}
    val superType = base::class.java.genericSuperclass!!
    return (superType as ParameterizedType).actualTypeArguments.first()!!
}

/**
 * Constructs a serializer for the given reflective Java [type].
 * [serializerByTypeToken] is intended to be used as an interoperability layer for libraries like GSON and Retrofit,
 * that operate with reflective Java [Type] and cannot use [typeOf].
 *
 * For application-level serialization, it is recommended to use `serializer<T>()` instead as it is aware of
 * Kotlin-specific type information, such as nullability, sealed classes and object.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(UnsafeSerializationApi::class)
public fun serializerByTypeToken(type: Type): KSerializer<Any> = when (type) {
    // TODO stabilize for Spring
    is GenericArrayType -> {
        val eType = type.genericComponentType.let {
            when (it) {
                is WildcardType -> it.upperBounds.first()
                else -> it
            }
        }
        val serializer = serializerByTypeToken(eType)
        val kclass = when (eType) {
            is ParameterizedType -> (eType.rawType as Class<*>).kotlin
            is KClass<*> -> eType
            else -> throw IllegalStateException("unsupported type in GenericArray: ${eType::class}")
        } as KClass<Any>
        ArraySerializer(kclass, serializer) as KSerializer<Any>
    }
    is Class<*> -> if (!type.isArray) {
        (type.kotlin as KClass<Any>).serializer<Any>()
    } else {
        val eType: Class<*> = type.componentType
        val s = serializerByTypeToken(eType)
        val arraySerializer = ArraySerializer(eType.kotlin as KClass<Any>, s)
        arraySerializer as KSerializer<Any>
    }
    is ParameterizedType -> {
        val rootClass = (type.rawType as Class<*>)
        val args = (type.actualTypeArguments)
        when {
            List::class.java.isAssignableFrom(rootClass) -> ListSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            Set::class.java.isAssignableFrom(rootClass) -> SetSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            Map::class.java.isAssignableFrom(rootClass) -> MapSerializer(
                serializerByTypeToken(args[0]),
                serializerByTypeToken(args[1])
            ) as KSerializer<Any>
            Map.Entry::class.java.isAssignableFrom(rootClass) -> MapEntrySerializer(
                serializerByTypeToken(args[0]),
                serializerByTypeToken(args[1])
            ) as KSerializer<Any>

            else -> {
                // probably we should deprecate this method because it can't differ nullable vs non-nullable types
                // since it uses Java TypeToken, not Kotlin one
                val varargs = args.map { serializerByTypeToken(it) as KSerializer<Any?> }.toTypedArray()
                (rootClass.kotlin.constructSerializerForGivenTypeArgs(*varargs) as? KSerializer<Any>)
                        ?: (rootClass.kotlin as KClass<Any>).serializer()
            }
        }
    }
    is WildcardType -> serializerByTypeToken(type.upperBounds.first())
    else -> throw IllegalArgumentException("typeToken should be an instance of Class<?>, GenericArray, ParametrizedType or WildcardType, but actual type is $type ${type::class}")
}
