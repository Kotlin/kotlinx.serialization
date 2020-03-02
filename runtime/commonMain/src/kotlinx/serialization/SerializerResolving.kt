/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR")

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.builtins.*
import kotlin.reflect.*

/**
 * Creates a serializer for the provided reified type [T] with support of user-defined generic classes.
 * This method is a reified version of `serializer(KType)`
 *
 * Example of usage:
 * ```
 * val map = mapOf(1 to listOf(listOf("1")))
 * json.stringify(serializer(), map)
 * ```
 *
 * This is a computation-heavy call, so it is recommended to cache its result.
 * [typeOf] API currently does not work with user-defined generic classes on Kotlin/JS.
 */
@Suppress("NO_REFLECTION_IN_CLASS_PATH")
@ImplicitReflectionSerializer
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
 * json.stringify(serializer, map)
 * ```
 *
 * This is a computation-heavy call, so it is recommended to cache its result.
 * [typeOf] API currently does not work with user-defined generic classes on Kotlin/JS.
 */
@Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH", "UNSUPPORTED")
@OptIn(ImplicitReflectionSerializer::class)
public fun serializer(type: KType): KSerializer<Any?> {
    fun serializerByKTypeImpl(type: KType): KSerializer<Any> {
        val rootClass = when (val t = type.classifier) {
            is KClass<*> -> t
            else -> error("Only KClass supported as classifier, got $t")
        } as KClass<Any>

        val typeArguments =  type.arguments
            .map { requireNotNull(it.type) { "Star projections are not allowed, had $it instead" } }
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
                        if (isReferenceArray(type, rootClass)) {
                            return ArraySerializer<Any, Any?>(typeArguments[0].classifier as KClass<Any>, serializers[0]).cast()
                        }
                        requireNotNull(rootClass.constructSerializerForGivenTypeArgs(*serializers.toTypedArray())) {
                            "Can't find a method to construct serializer for type ${rootClass.simpleName()}. " +
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
 * Constructs KSerializer<D<T0, T1, ...>> by given KSerializer<T0>, KSerializer<T1>, ...
 * via reflection (on JVM) or compiler+plugin intrinsic `SerializerFactory` (on Native)
 *
 * Currently, unsupported on JS.
 */
@ImplicitReflectionSerializer
internal expect fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>?

/**
 * Checks whether given KType and its corresponding KClass represent a reference array
 * TODO replace this one in 1.4 where comparison against Array::class is allowed
 */
internal expect fun isReferenceArray(type: KType, rootClass: KClass<Any>): Boolean
