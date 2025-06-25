/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.reflect.*
import kotlin.time.*
import kotlin.uuid.*

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
        notRegisteredMessage() +
            "To get enum serializer on Kotlin/Native, it should be annotated with @Serializable annotation.\n" +
            "To get interface serializer on Kotlin/Native, use PolymorphicSerializer() constructor function.\n"
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
        is kotlinx.serialization.internal.SerializerFactory -> assocObject.serializer(*args) as KSerializer<T>
        else -> null
    }

@Suppress("DEPRECATION_ERROR")
internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.constructSerializerForGivenTypeArgs()

internal actual fun <T: Any> KClass<T>.isInterface(): Boolean = false // we do not know, but also PolymorphicSerializer is never returned on Native for interfaces

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

internal actual fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E> {
    val result = arrayOfAnyNulls<E>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("USELESS_CAST")
    return result as Array<E>
}

@Suppress("UNCHECKED_CAST")
private fun <T> arrayOfAnyNulls(size: Int): Array<T> = arrayOfNulls<Any>(size) as Array<T>

internal actual fun isReferenceArray(rootClass: KClass<Any>): Boolean = rootClass == Array::class

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
