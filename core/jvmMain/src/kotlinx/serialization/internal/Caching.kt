/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
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
 * Values are computed using provided [factory] function.
 *
 * `null` values are not supported, though there aren't any technical limitations.
 */
internal actual fun createCachedFactoryWrapper(factory: (KClass<*>) -> KSerializer<Any>?): CachedSerializerFactory {
    return if (useClassValue) ClassValueCache(factory) else ConcurrentHashMapCache(factory)
}

@SuppressAnimalSniffer
private class ClassValueCache(private val compute: (KClass<*>) -> KSerializer<Any>?) : CachedSerializerFactory {
    private val classValue = initClassValue()

    private fun initClassValue() = object : ClassValue<CacheEntry>() {
        override fun computeValue(type: Class<*>): CacheEntry {
            val pair = compute(type.kotlin)?.let { SerializerPair(it, it.nullable) }
            return CacheEntry(pair)
        }
    }

    override fun get(key: KClass<*>, isNullable: Boolean): KSerializer<Any?>? =
        classValue[key.java].serializer(isNullable)
}

/**
 * We no longer support Java 6, so the only place we use this cache is Android, where there
 * are no classloader leaks issue, thus we can safely use strong references and do not bother
 * with WeakReference wrapping.
 */
private class ConcurrentHashMapCache(private val compute: (KClass<*>) -> KSerializer<Any>?) : CachedSerializerFactory {
    private val cache = ConcurrentHashMap<Class<*>, CacheEntry>()

    override fun get(key: KClass<*>, isNullable: Boolean): KSerializer<Any?>? {
        return cache.getOrPut(key.java) {
            val pair = compute(key)?.let { SerializerPair(it, it.nullable) }
            CacheEntry(pair)
        }.serializer(isNullable)
    }
}

private class SerializerPair(val nonNull: KSerializer<out Any>, val nullable: KSerializer<out Any?>)

private class CacheEntry(private val serializers: SerializerPair?) {
    fun serializer(isNullable: Boolean): KSerializer<Any?>? {
        return serializers?.let { if (isNullable) it.nullable else it.nonNull }?.cast()
    }
}

