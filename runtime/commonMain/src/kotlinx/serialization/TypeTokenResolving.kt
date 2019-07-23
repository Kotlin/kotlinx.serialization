/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * Reified version of [serializerByKType], provided for convenience.
 * [KType] is obtained via [typeOf] function, consult its documentation
 * to understand limitations.
 *
 * [typeOf] and [KType] API currently don't work on Kotlin/JS.
 */
@Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH")
@ImplicitReflectionSerializer
public inline fun <reified T> serializer(): KSerializer<T> {
    return serializerByKType(typeOf<T>()) as KSerializer<T>
}

/**
 * This method is able to construct serializer for a given [type].
 * Since [KType], in contrary to [KClass], contains information about generic type parameters,
 * it is possible to construct serializers for collections using this method.
 * However, user-defined generic classes are not supported for now.
 *
 * Keep in mind that this is a 'heavy' call, so result probably should be cached somewhere else.
 *
 * [typeOf] and [KType] API currently don't work on Kotlin/JS.
 *
 * @see typeOf
 */
@Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH", "UNSUPPORTED")
@ImplicitReflectionSerializer
public fun serializerByKType(type: KType): KSerializer<Any?> {
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
                    .map(::serializerByKType)
                // Array is not supported, see KT-32839
                when (rootClass) {
                    List::class, ArrayList::class -> ArrayListSerializer(args[0])
                    HashSet::class -> HashSetSerializer(args[0])
                    Set::class, LinkedHashSet::class -> LinkedHashSetSerializer(args[0])
                    HashMap::class -> HashMapSerializer(args[0], args[1])
                    Map::class, LinkedHashMap::class -> LinkedHashMapSerializer(args[0], args[1])
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
