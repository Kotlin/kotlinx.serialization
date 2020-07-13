/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR", "UNCHECKED_CAST")
@file:JvmMultifileClass
@file:JvmName("SerializersKt")

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.builtins.TripleSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.internal.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * Creates a serializer for the provided reified type [T] with support of user-defined generic classes.
 * This method is a reified version of `serializer(KType)`
 *
 * Example of usage:
 * ```
 * val map = mapOf(1 to listOf(listOf("1")))
 * json.encodeToString(serializer(), map)
 * ```
 */
public inline fun <reified T> serializer(): KSerializer<T> {
    return serializer(typeOf<T>()).cast()
}

/**
 * Creates a serializer for the given [type] with support of user-defined generic classes.
 * [type] argument can be obtained with experimental [typeOf] method.
 *
 * Example of usage:
 * ```
 * val map = mapOf(1 to listOf(listOf("1")))
 * val serializer = serializer(typeOf<Map<Int, List<List<String>>>>())
 * json.encodeToString(serializer, map)
 * ```
 */
@OptIn(UnsafeSerializationApi::class)
public fun serializer(type: KType): KSerializer<Any?> {
    fun serializerByKTypeImpl(type: KType): KSerializer<Any> {
        val rootClass = type.kclass()

        val typeArguments = type.arguments
            .map { requireNotNull(it.type) { "Star projections in type arguments are not allowed, but had $type" } }
        return when {
            typeArguments.isEmpty() -> rootClass.serializer()
            else -> {
                val serializers = typeArguments
                    .map(::serializer)
                // Array is not supported, see KT-32839
                when (rootClass) {
                    List::class, MutableList::class, ArrayList::class -> ArrayListSerializer(serializers[0])
                    HashSet::class -> HashSetSerializer(serializers[0])
                    Set::class, MutableSet::class, LinkedHashSet::class -> LinkedHashSetSerializer(serializers[0])
                    HashMap::class -> HashMapSerializer(serializers[0], serializers[1])
                    Map::class, MutableMap::class, LinkedHashMap::class -> LinkedHashMapSerializer(serializers[0], serializers[1])
                    Map.Entry::class -> MapEntrySerializer(serializers[0], serializers[1])
                    Pair::class -> PairSerializer(serializers[0], serializers[1])
                    Triple::class -> TripleSerializer(serializers[0], serializers[1], serializers[2])
                    else -> {
                        if (isReferenceArray(rootClass)) {
                            return ArraySerializer<Any, Any?>(typeArguments[0].classifier as KClass<Any>, serializers[0]).cast()
                        }
                        requireNotNull(rootClass.constructSerializerForGivenTypeArgs(*serializers.toTypedArray())) {
                            "Can't find a method to construct serializer for type ${rootClass.simpleName}. " +
                                    "Make sure this class is marked as @Serializable or provide serializer explicitly."
                        }
                    }
                }
            }
        }.cast()
    }

    val result = serializerByKTypeImpl(type)
    return if (type.isMarkedNullable) result.nullable else result.cast()
}

/**
 * Retrieves a [KSerializer] for the given [KClass].
 * The given class must be annotated with [Serializable] or be one of the built-in types.
 * It is not recommended to use this method for anything, but last-ditch resort, e.g.
 * when all type info is lost, your application has crashed and it is the final attempt to log or send some serializable data.
 *
 * The recommended way to retrieve the serializer is inline [serializer] function and [`serializer(KType)`][serializer]
 *
 * This API is not guaranteed to work consistent across different platforms or
 * to work in cases that slightly differ from "plain @Serializable class".
 *
 * @throws SerializationException if serializer can't be found.
 */
@UnsafeSerializationApi
public fun <T : Any> KClass<T>.serializer(): KSerializer<T> = serializerOrNull() ?: serializerNotRegistered()

/**
 * Retrieves a [KSerializer] for the given [KClass] or returns `null` if none is found.
 * The given class must be annotated with [Serializable] or be one of the built-in types.
 * It is not recommended to use this method for anything, but last-ditch resort, e.g.
 * when all type info is lost, your application has crashed and it is the final attempt to log or send some serializable data.
 *
 * This API is not guaranteed to work consistent across different platforms or
 * to work in cases that slightly differ from "plain @Serializable class".
 */
@UnsafeSerializationApi
public fun <T : Any> KClass<T>.serializerOrNull(): KSerializer<T>? =
    compiledSerializerImpl() ?: builtinSerializerOrNull()

