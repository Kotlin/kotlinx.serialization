/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.*

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> Array<T>.getChecked(index: Int): T {
    return get(index)
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun BooleanArray.getChecked(index: Int): Boolean {
    return get(index)
}

internal actual fun KClass<*>.platformSpecificSerializerNotRegistered(): Nothing {
    throw SerializationException(
        "${notRegisteredMessage()}\n" +
                "On Kotlin/Wasm explicitly declared serializer should be used for interfaces and enums without @Serializable annotation"
    )
}

@Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION_ERROR"
)
@OptIn(ExperimentalAssociatedObjects::class)
internal actual fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>? =
    when (val assocObject = findAssociatedObject<SerializableWith>()) {
        is KSerializer<*> -> assocObject as KSerializer<T>
        is SerializerFactory -> assocObject.serializer(*args) as KSerializer<T>
        else -> null
    }

@Suppress("DEPRECATION_ERROR")
internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.constructSerializerForGivenTypeArgs()


internal actual fun <T> createCache(factory: (KClass<*>) -> KSerializer<T>?): SerializerCache<T> {
    return object: SerializerCache<T> {
        override fun get(key: KClass<Any>): KSerializer<T>? {
            return factory(key)
        }
    }
}

internal actual fun <T> createParametrizedCache(factory: (KClass<Any>, List<KType>) -> KSerializer<T>?): ParametrizedSerializerCache<T> {
    return object: ParametrizedSerializerCache<T> {
        override fun get(key: KClass<Any>, types: List<KType>): Result<KSerializer<T>?> {
            return kotlin.runCatching { factory(key, types) }
        }
    }
}

internal actual fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E> = toTypedArray()

internal actual fun isReferenceArray(rootClass: KClass<Any>): Boolean = rootClass == Array::class