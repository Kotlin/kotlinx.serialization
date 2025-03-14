/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.reflect.*
import kotlin.time.*
import kotlin.uuid.*

internal actual fun <T> Array<T>.getChecked(index: Int): T {
    if (index !in indices) throw IndexOutOfBoundsException("Index $index out of bounds $indices")
    return get(index)
}

internal actual fun BooleanArray.getChecked(index: Int): Boolean {
    if (index !in indices) throw IndexOutOfBoundsException("Index $index out of bounds $indices")
    return get(index)
}

internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.constructSerializerForGivenTypeArgs() ?: (
        if (this === Nothing::class) NothingSerializer // .js throws an exception for Nothing
        else this.js.asDynamic().Companion?.serializer()
        ) as? KSerializer<T>

internal actual fun <T: Any> KClass<T>.isInterface(): Boolean = isInterface

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

internal actual fun KClass<*>.platformSpecificSerializerNotRegistered(): Nothing {
    throw SerializationException(
        notRegisteredMessage() +
                "To get enum serializer on Kotlin/JS, it should be annotated with @Serializable annotation."
    )
}

@Suppress("UNCHECKED_CAST", "DEPRECATION_ERROR")
@OptIn(ExperimentalAssociatedObjects::class)
internal actual fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>? =
    try {
        val assocObject = findAssociatedObject<SerializableWith>()
        when {
            assocObject is KSerializer<*> -> assocObject as KSerializer<T>
            assocObject is SerializerFactory -> assocObject.serializer(*args) as KSerializer<T>
            else -> null
        }
    } catch (e: dynamic) {
        null
    }

internal actual fun isReferenceArray(rootClass: KClass<Any>): Boolean = rootClass == Array::class

/**
 * WARNING: may be broken in arbitrary time in the future without notice
 *
 * Should be eventually replaced with compiler intrinsics
 */
private val KClass<*>.isInterface: Boolean
    get(): Boolean {
        // .js throws an exception for Nothing
        if (this === Nothing::class) return false
        return js.asDynamic().`$metadata$`?.kind == "interface"
    }

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalUuidApi::class, ExperimentalSerializationApi::class,
    ExperimentalTime::class)
internal actual fun initBuiltins(): Map<KClass<*>, KSerializer<*>> = mapOf(
    String::class to String.serializer(),
    Char::class to Char.serializer(),
    CharArray::class to CharArraySerializer(),
    Double::class to Double.serializer(),
    DoubleArray::class to DoubleArraySerializer(),
    Float::class to Float.serializer(),
    FloatArray::class to FloatArraySerializer(),
    Long::class to Long.serializer(),
    LongArray::class to LongArraySerializer(),
    ULong::class to ULong.serializer(),
    ULongArray::class to ULongArraySerializer(),
    Int::class to Int.serializer(),
    IntArray::class to IntArraySerializer(),
    UInt::class to UInt.serializer(),
    UIntArray::class to UIntArraySerializer(),
    Short::class to Short.serializer(),
    ShortArray::class to ShortArraySerializer(),
    UShort::class to UShort.serializer(),
    UShortArray::class to UShortArraySerializer(),
    Byte::class to Byte.serializer(),
    ByteArray::class to ByteArraySerializer(),
    UByte::class to UByte.serializer(),
    UByteArray::class to UByteArraySerializer(),
    Boolean::class to Boolean.serializer(),
    BooleanArray::class to BooleanArraySerializer(),
    Unit::class to Unit.serializer(),
    Nothing::class to NothingSerializer(),
    Duration::class to Duration.serializer(),
    Instant::class to Instant.serializer(),
    Uuid::class to Uuid.serializer()
)
