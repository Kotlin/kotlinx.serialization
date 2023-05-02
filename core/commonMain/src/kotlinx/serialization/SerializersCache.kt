/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.internal.cast
import kotlinx.serialization.internal.createCache
import kotlinx.serialization.internal.createParametrizedCache
import kotlinx.serialization.modules.EmptySerializersModule
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass
import kotlin.reflect.KType


/**
 * Cache for non-null non-parametrized and non-contextual serializers.
 */
@ThreadLocal
private val SERIALIZERS_CACHE = createCache { it.serializerOrNull() }

/**
 * Cache for nullable non-parametrized and non-contextual serializers.
 */
@ThreadLocal
private val SERIALIZERS_CACHE_NULLABLE = createCache<Any?> { it.serializerOrNull()?.nullable?.cast() }

/**
 * Cache for non-null parametrized and non-contextual serializers.
 */
@ThreadLocal
private val PARAMETRIZED_SERIALIZERS_CACHE = createParametrizedCache { clazz, types ->
    val serializers = EmptySerializersModule().serializersForParameters(types, true)!!
    clazz.parametrizedSerializerOrNull(serializers) { types[0].classifier }
}

/**
 * Cache for nullable parametrized and non-contextual serializers.
 */
@ThreadLocal
private val PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE = createParametrizedCache<Any?> { clazz, types ->
    val serializers = EmptySerializersModule().serializersForParameters(types, true)!!
    clazz.parametrizedSerializerOrNull(serializers) { types[0].classifier }?.nullable?.cast()
}

/**
 * Find cacheable serializer in the cache.
 * If serializer is cacheable but missed in cache - it will be created, placed into the cache and returned.
 */
internal fun findCachedSerializer(clazz: KClass<Any>, isNullable: Boolean): KSerializer<Any?>? {
    return if (!isNullable) {
        SERIALIZERS_CACHE.get(clazz)?.cast()
    } else {
        SERIALIZERS_CACHE_NULLABLE.get(clazz)
    }
}

/**
 * Find cacheable parametrized serializer in the cache.
 * If serializer is cacheable but missed in cache - it will be created, placed into the cache and returned.
 */
internal fun findParametrizedCachedSerializer(
    clazz: KClass<Any>,
    types: List<KType>,
    isNullable: Boolean
): Result<KSerializer<Any?>?> {
    return if (!isNullable) {
        @Suppress("UNCHECKED_CAST")
        PARAMETRIZED_SERIALIZERS_CACHE.get(clazz, types) as Result<KSerializer<Any?>?>
    } else {
        PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE.get(clazz, types)
    }
}
