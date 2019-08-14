/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * Reified version of `serializer(type)`, provided for convenience.
 * This method constructs the serializer for provided reified type [T].
 *
 * This method works with generic parameters for built-in serializable classes such as [List] and [Map].
 * User-defined generic classes are not supported for now.
 *
 * Example of usage:
 * ```
 * val map = mapOf(1 to listOf(listOf("1")))
 * val serializer = serializer<Map<Int, List<List<String>>>()
 * json.stringify(serializer, map)
 * ```
 *
 * This is a computation-heavy call, so it is recommended to cache the result.
 * [typeOf] API currently does not work on Kotlin/JS.
 */
@Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH")
@ImplicitReflectionSerializer
public inline fun <reified T> serializer(): KSerializer<T> {
    return serializer(typeOf<T>()) as KSerializer<T>
}

/**
 * Method that constructs a serializer for the given [type].
 *
 * This method works with generic parameters for built-in serializable classes such as [List] and [Map].
 * User-defined generic classes are not supported for now.
 *
 * Example of usage:
 * ```
 * val map = mapOf(1 to listOf(listOf("1")))
 * val serializer = serializer(typeOf<Map<Int, List<List<String>>>())
 * json.stringify(serializer, map)
 * ```
 *
 * This is a computation-heavy call, so it is recommended to cache the result.
 * [typeOf] API currently does not work on Kotlin/JS.
 */
@Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH", "UNSUPPORTED")
@ImplicitReflectionSerializer
public fun serializer(type: KType): KSerializer<Any?> {
    fun serializerByKTypeImpl(type: KType): KSerializer<Any> {
        val rootClass = when (val t = type.classifier) {
            is KClass<*> -> t
            else -> error("Only KClass supported as classifier, got $t")
        } as KClass<Any>

        return when {
            type.arguments.isEmpty() -> rootClass.serializer()
            else -> {
                val args = type.arguments
                    .map { requireNotNull(it.type) { "Star projections are not allowed" } }
                    .map(::serializer)
                // Array is not supported, see KT-32839
                when (rootClass) {
                    List::class, MutableList::class, ArrayList::class -> ArrayListSerializer(args[0])
                    HashSet::class -> HashSetSerializer(args[0])
                    Set::class, MutableSet::class, LinkedHashSet::class -> LinkedHashSetSerializer(args[0])
                    HashMap::class -> HashMapSerializer(args[0], args[1])
                    Map::class, MutableMap::class, LinkedHashMap::class -> LinkedHashMapSerializer(args[0], args[1])
                    Map.Entry::class -> MapEntrySerializer(args[0], args[1])
                    Pair::class -> PairSerializer(args[0], args[1])
                    Triple::class -> TripleSerializer(args[0], args[1], args[2])
                    else -> throw UnsupportedOperationException("The only generic classes supported for now are standard collections, got class $rootClass")
                }
            }
        } as KSerializer<Any>
    }

    val result = serializerByKTypeImpl(type)
    return if (type.isMarkedNullable) makeNullable(result) else result as KSerializer<Any?>
}
