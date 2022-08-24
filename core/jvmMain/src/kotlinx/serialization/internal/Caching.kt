/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
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
internal actual fun createCache(factory: (KClass<*>) -> KSerializer<Any>?): SerializerCache {
    return if (useClassValue) ClassValueCache(factory) else ConcurrentHashMapCache(factory)
}

/**
 * Creates a **strongly referenced** cache of values associated with [Class].
 * Serializers are computed using provided [factory] function.
 *
 * `null` values are not supported, though there aren't any technical limitations.
 */
internal actual fun createParametrizedCache(factory: (KClass<Any>, List<KType>) -> KSerializer<Any>?): ParametrizedSerializerCache {
    return if (useClassValue) ClassValueParametrizedCache(factory) else ConcurrentHashMapParametrizedCache(factory)
}

@SuppressAnimalSniffer
private class ClassValueCache(private val compute: (KClass<*>) -> KSerializer<Any>?) : SerializerCache {
    private val classValue = initClassValue()

    private fun initClassValue() = object : ClassValue<CacheEntry>() {
        override fun computeValue(type: Class<*>): CacheEntry {
            val pair = SerializerPair.from(compute(type.kotlin))
            return CacheEntry(pair)
        }
    }

    override fun get(key: KClass<Any>, isNullable: Boolean): KSerializer<Any?>? =
        classValue[key.java].serializers?.get(isNullable)
}

@SuppressAnimalSniffer
private class ClassValueParametrizedCache(private val compute: (KClass<Any>, List<KType>) -> KSerializer<Any>?) : ParametrizedSerializerCache {
    private val classValue = initClassValue()

    private fun initClassValue() = object : ClassValue<ParametrizedCacheEntry>() {
        override fun computeValue(type: Class<*>): ParametrizedCacheEntry {
            return ParametrizedCacheEntry()
        }
    }

    override fun get(key: KClass<Any>, isNullable: Boolean, types: List<KType>): Result<KSerializer<Any?>?> =
        classValue[key.java]
            .computeIfAbsent(types) { compute(key, types) }
            .map { it?.get(isNullable) }
}

/**
 * We no longer support Java 6, so the only place we use this cache is Android, where there
 * are no classloader leaks issue, thus we can safely use strong references and do not bother
 * with WeakReference wrapping.
 */
private class ConcurrentHashMapCache(private val compute: (KClass<*>) -> KSerializer<Any>?) : SerializerCache {
    private val cache = ConcurrentHashMap<Class<*>, CacheEntry>()

    override fun get(key: KClass<Any>, isNullable: Boolean): KSerializer<Any?>? {
        return cache.getOrPut(key.java) {
            CacheEntry(SerializerPair.from(compute(key)))
        }.serializers?.get(isNullable)
    }
}



private class ConcurrentHashMapParametrizedCache(private val compute: (KClass<Any>, List<KType>) -> KSerializer<Any>?) : ParametrizedSerializerCache {
    private val cache = ConcurrentHashMap<Class<*>, ParametrizedCacheEntry>()

    override fun get(key: KClass<Any>, isNullable: Boolean, types: List<KType>): Result<KSerializer<Any?>?> {
        return cache.getOrPut(key.java) { ParametrizedCacheEntry() }
            .computeIfAbsent(types) { compute(key, types) }
            .map { it?.get(isNullable) }
    }
}



private class SerializerPair(private val nonNull: KSerializer<out Any>, private val nullable: KSerializer<out Any?>) {
    fun get(isNullable: Boolean): KSerializer<Any?> {
        return (if (isNullable) nullable else nonNull).cast()
    }

    companion object {
        fun from(serializer: KSerializer<out Any>?): SerializerPair? {
            return serializer?.let { SerializerPair(it, it.nullable) }
        }
    }
}

private class CacheEntry(@JvmField val serializers: SerializerPair?)

private class ParametrizedCacheEntry {
    private val serializers: ConcurrentHashMap<List<KType>, Result<SerializerPair?>> = ConcurrentHashMap()
    inline fun computeIfAbsent(types: List<KType>, producer: () -> KSerializer<out Any>?): Result<SerializerPair?> {
        return serializers.getOrPut(types) {
            kotlin.runCatching { SerializerPair.from(producer()) }
        }
    }
}

