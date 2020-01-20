/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import java.lang.reflect.*
import kotlin.reflect.KClass

@PublishedApi
internal open class TypeBase<T>

inline fun <reified T> typeTokenOf(): Type {
    val base = object : TypeBase<T>() {}
    val superType = base::class.java.genericSuperclass!!
    return (superType as ParameterizedType).actualTypeArguments.first()!!
}

/**
 * This method uses reflection to construct serializer for given type. However,
 * since it accepts type token, it is available only on JVM by design,
 * and it can work correctly even with generics, so
 * it is not annotated with [ImplicitReflectionSerializer].
 *
 * Keep in mind that this is a 'heavy' call, so result probably should be cached somewhere else.
 *
 * This method intended for static, format-agnostic resolving (e.g. in adapter factories) so context is not used here.
 */
@Suppress("UNCHECKED_CAST")
@UseExperimental(ImplicitReflectionSerializer::class)
fun serializerByTypeToken(type: Type): KSerializer<Any> = when (type) {
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
        ReferenceArraySerializer(kclass, serializer) as KSerializer<Any>
    }
    is Class<*> -> if (!type.isArray) {
        requireNotNull<KSerializer<out Any>>((type.kotlin as KClass<Any>).serializer<Any>()) as KSerializer<Any>
    } else {
        val eType: Class<*> = type.componentType
        val s = serializerByTypeToken(eType)
        val arraySerializer = ReferenceArraySerializer(eType.kotlin as KClass<Any>, s)
        arraySerializer as KSerializer<Any>
    }
    is ParameterizedType -> {
        val rootClass = (type.rawType as Class<*>)
        val args = (type.actualTypeArguments)
        when {
            List::class.java.isAssignableFrom(rootClass) -> ArrayListSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            Set::class.java.isAssignableFrom(rootClass) -> HashSetSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            Map::class.java.isAssignableFrom(rootClass) -> HashMapSerializer(
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
                        ?: requireNotNull<KSerializer<out Any>>((rootClass.kotlin as KClass<Any>).serializer()) as KSerializer<Any>
            }
        }
    }
    is WildcardType -> serializerByTypeToken(type.upperBounds.first())
    else -> throw IllegalArgumentException("typeToken should be an instance of Class<?>, GenericArray, ParametrizedType or WildcardType, but actual type is $type ${type::class}")
}
