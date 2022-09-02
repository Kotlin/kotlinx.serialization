/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.KSerializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType

/*
 * By default, we use ClassValue-based caches to avoid classloader leaks,
 * but ClassValue is not available on Android, thus we attempt to check it dynamically
 * and fallback to ConcurrentHashMap-based cache.
 */
private val useClassValue = runCatching {
    Class.forName("java.lang.ClassValue")
}.map { true }.getOrDefault(false)

/**
 * Creates a **strongly referenced** cache of values associated with [Class].
 * Serializers are computed using provided [factory] function.
 *
 * `null` values are not supported, though there aren't any technical limitations.
 */
internal actual fun <T> createCache(factory: (KClass<*>) -> KSerializer<T>?): SerializerCache<T> {
    return if (useClassValue) ClassValueCache(factory) else ConcurrentHashMapCache(factory)
}

/**
 * Creates a **strongly referenced** cache of values associated with [Class].
 * Serializers are computed using provided [factory] function.
 *
 * `null` values are not supported, though there aren't any technical limitations.
 */
internal actual fun <T> createParametrizedCache(factory: (KClass<Any>, List<KType>) -> KSerializer<T>?): ParametrizedSerializerCache<T> {
    return if (useClassValue) ClassValueParametrizedCache(factory) else ConcurrentHashMapParametrizedCache(factory)
}

@SuppressAnimalSniffer
private class ClassValueCache<T>(private val compute: (KClass<*>) -> KSerializer<T>?) : SerializerCache<T> {
    private val classValue = initClassValue()

    private fun initClassValue() = object : ClassValue<CacheEntry<T>>() {
        /*
         * Since during the computing of the value for the `ClassValue` entry, we do not know whether a nullable
         *  serializer is needed, so we may need to differentiate nullable/non-null  caches by a level higher
         */
        override fun computeValue(type: Class<*>): CacheEntry<T> {
            return CacheEntry(compute(type.kotlin))
        }
    }

    override fun get(key: KClass<Any>): KSerializer<T>? = classValue[key.java].serializer
}

@SuppressAnimalSniffer
private class ClassValueParametrizedCache<T>(private val compute: (KClass<Any>, List<KType>) -> KSerializer<T>?) : ParametrizedSerializerCache<T> {
    private val classValue = initClassValue()

    private fun initClassValue() = object : ClassValue<ParametrizedCacheEntry<T>>() {
        /*
        * Since during the computing of the value for the `ClassValue` entry, we do not know whether a nullable
        *  serializer is needed, so we may need to differentiate nullable/non-null  caches by a level higher
        */
        override fun computeValue(type: Class<*>): ParametrizedCacheEntry<T> {
            return ParametrizedCacheEntry()
        }
    }

    override fun get(key: KClass<Any>, types: List<KType>): Result<KSerializer<T>?> =
        classValue[key.java].computeIfAbsent(types) { compute(key, types) }
}

/**
 * We no longer support Java 6, so the only place we use this cache is Android, where there
 * are no classloader leaks issue, thus we can safely use strong references and do not bother
 * with WeakReference wrapping.
 */
private class ConcurrentHashMapCache<T>(private val compute: (KClass<*>) -> KSerializer<T>?) : SerializerCache<T> {
    private val cache = ConcurrentHashMap<Class<*>, CacheEntry<T>>()

    override fun get(key: KClass<Any>): KSerializer<T>? {
        return cache.getOrPut(key.java) {
            CacheEntry(compute(key))
        }.serializer
    }
}



private class ConcurrentHashMapParametrizedCache<T>(private val compute: (KClass<Any>, List<KType>) -> KSerializer<T>?) : ParametrizedSerializerCache<T> {
    private val cache = ConcurrentHashMap<Class<*>, ParametrizedCacheEntry<T>>()

    override fun get(key: KClass<Any>, types: List<KType>): Result<KSerializer<T>?> {
        return cache.getOrPut(key.java) { ParametrizedCacheEntry() }
            .computeIfAbsent(types) { compute(key, types) }
    }
}

private class CacheEntry<T>(@JvmField val serializer: KSerializer<T>?)

private class ParametrizedCacheEntry<T> {
    private val serializers: ConcurrentHashMap<List<KType>, Result<KSerializer<T>?>> = ConcurrentHashMap()
    inline fun computeIfAbsent(types: List<KType>, producer: () -> KSerializer<T>?): Result<KSerializer<T>?> {
        return serializers.getOrPut(types) {
            kotlin.runCatching { producer() }
        }
    }
}

