/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.KSerializer
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

/*
 * By default, we use ClassValue-based caches to avoid classloader leaks,
 * but ClassValue is not available on Android, thus we attempt to check it dynamically
 * and fallback to ConcurrentHashMap-based cache.
 */
private val useClassValue = try {
    Class.forName("java.lang.ClassValue")
    true
} catch (_: Throwable) {
    false
}

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

private class ClassValueCache<T>(val compute: (KClass<*>) -> KSerializer<T>?) : SerializerCache<T> {
    private val classValue = ClassValueReferences<CacheEntry<T>>()

    override fun get(key: KClass<Any>): KSerializer<T>? {
        return classValue
            .getOrSet(key.java) { CacheEntry(compute(key)) }
            .serializer
    }
}

/**
 * A class that combines the capabilities of ClassValue and SoftReference.
 * Softly binds the calculated value to the specified class.
 *
 * [SoftReference] used to prevent class loaders from leaking,
 * since the value can transitively refer to an instance of type [Class], this may prevent the loader from
 * being collected during garbage collection.
 *
 * In the first calculation the value is cached, every time [getOrSet] is called, a pre-calculated value is returned.
 *
 * However, the value can be collected during garbage collection (thanks to [SoftReference])
 * - in this case, when trying to call the [getOrSet] function, the value will be calculated again and placed in the cache.
 *
 * An important requirement for a function generating a value is that it must be stable, so that each time it is called for the same class, the function returns similar values.
 * In the case of serializers, these should be instances of the same class filled with equivalent values.
 */
@SuppressAnimalSniffer
private class ClassValueReferences<T> : ClassValue<MutableSoftReference<T>>() {
    override fun computeValue(type: Class<*>): MutableSoftReference<T> {
        return MutableSoftReference()
    }

    inline fun getOrSet(key: Class<*>, crossinline factory: () -> T): T {
        val ref: MutableSoftReference<T> = get(key)

        ref.reference.get()?.let { return it }

        // go to the slow path and create serializer with blocking, also wrap factory block
        return ref.getOrSetWithLock { factory() }
    }

}

/**
 * Wrapper over `SoftReference`, used  to store a mutable value.
 */
private class MutableSoftReference<T> {
    // volatile because of situations like https://stackoverflow.com/a/7855774
    @JvmField
    @Volatile
    var reference: SoftReference<T> = SoftReference(null)

    /*
    It is important that the monitor for synchronized is the `MutableSoftReference` of a specific class
    This way access to reference is blocked only for one serializable class, and not for all
     */
    @Synchronized
    fun getOrSetWithLock(factory: () -> T): T {
        // exit function if another thread has already filled in the `reference` with non-null value
        reference.get()?.let { return it }

        val value = factory()
        reference = SoftReference(value)
        return value
    }
}

private class ClassValueParametrizedCache<T>(private val compute: (KClass<Any>, List<KType>) -> KSerializer<T>?) :
    ParametrizedSerializerCache<T> {
    private val classValue = ClassValueReferences<ParametrizedCacheEntry<T>>()

    override fun get(key: KClass<Any>, types: List<KType>): Result<KSerializer<T>?> {
        return classValue.getOrSet(key.java) { ParametrizedCacheEntry() }
            .computeIfAbsent(types) { compute(key, types) }
    }
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


private class ConcurrentHashMapParametrizedCache<T>(private val compute: (KClass<Any>, List<KType>) -> KSerializer<T>?) :
    ParametrizedSerializerCache<T> {
    private val cache = ConcurrentHashMap<Class<*>, ParametrizedCacheEntry<T>>()

    override fun get(key: KClass<Any>, types: List<KType>): Result<KSerializer<T>?> {
        return cache.getOrPut(key.java) { ParametrizedCacheEntry() }
            .computeIfAbsent(types) { compute(key, types) }
    }
}

/**
 * Wrapper for cacheable serializer of some type.
 * Used to store cached serializer or indicates that the serializer is not cacheable.
 *
 * If serializer for type is not cacheable then value of [serializer] is `null`.
 */
private class CacheEntry<T>(@JvmField val serializer: KSerializer<T>?)

/**
 * Workaround of https://youtrack.jetbrains.com/issue/KT-54611 and https://github.com/Kotlin/kotlinx.serialization/issues/2065
 */
private class KTypeWrapper(private val origin: KType) : KType {
    override val annotations: List<Annotation>
        get() = origin.annotations
    override val arguments: List<KTypeProjection>
        get() = origin.arguments
    override val classifier: KClassifier?
        get() = origin.classifier
    override val isMarkedNullable: Boolean
        get() = origin.isMarkedNullable

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (origin != (other as? KTypeWrapper)?.origin) return false

        val kClassifier = classifier
        if (kClassifier is KClass<*>) {
            val otherClassifier = (other as? KType)?.classifier
            if (otherClassifier == null || otherClassifier !is KClass<*>) {
                return false
            }
            return kClassifier.java == otherClassifier.java
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        return origin.hashCode()
    }

    override fun toString(): String {
        return "KTypeWrapper: $origin"
    }
}

private class ParametrizedCacheEntry<T> {
    private val serializers: ConcurrentHashMap<List<KTypeWrapper>, Result<KSerializer<T>?>> = ConcurrentHashMap()
    inline fun computeIfAbsent(types: List<KType>, producer: () -> KSerializer<T>?): Result<KSerializer<T>?> {
        val wrappedTypes = types.map { KTypeWrapper(it) }
        return serializers.getOrPut(wrappedTypes) {
            kotlin.runCatching { producer() }
        }
    }
}

