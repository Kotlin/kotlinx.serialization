/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)
package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

internal object InternalHexConverter {
    private const val hexCode = "0123456789ABCDEF"

    fun parseHexBinary(s: String): ByteArray {
        val len = s.length
        require(len % 2 == 0) { "HexBinary string must be even length" }
        val bytes = ByteArray(len / 2)
        var i = 0

        while (i < len) {
            val h = hexToInt(s[i])
            val l = hexToInt(s[i + 1])
            require(!(h == -1 || l == -1)) { "Invalid hex chars: ${s[i]}${s[i + 1]}" }

            bytes[i / 2] = ((h shl 4) + l).toByte()
            i += 2
        }

        return bytes
    }

    private fun hexToInt(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'A'..'F' -> ch - 'A' + 10
        in 'a'..'f' -> ch - 'a' + 10
        else -> -1
    }

    fun printHexBinary(data: ByteArray, lowerCase: Boolean = false): String {
        val r = StringBuilder(data.size * 2)
        for (b in data) {
            r.append(hexCode[b.toInt() shr 4 and 0xF])
            r.append(hexCode[b.toInt() and 0xF])
        }
        return if (lowerCase) r.toString().lowercase() else r.toString()
    }

    fun toHexString(n: Int): String {
        val arr = ByteArray(4)
        for (i in 0 until 4) {
            arr[i] = (n shr (24 - i * 8)).toByte()
        }
        return printHexBinary(arr, true).trimStart('0').takeIf { it.isNotEmpty() } ?: "0"
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.cachedSerialNames(): Set<String> {
    if (this is CachedNames) return serialNames
    val result = HashSet<String>(elementsCount)
    for (i in 0 until elementsCount) {
        result += getElementName(i)
    }
    return result
}

private val EMPTY_DESCRIPTOR_ARRAY: Array<SerialDescriptor> = arrayOf()

/**
 * Same as [toTypedArray], but uses special empty array constant, if [this]
 * is null or empty.
 */
internal fun List<SerialDescriptor>?.compactArray(): Array<SerialDescriptor> =
    takeUnless { it.isNullOrEmpty() }?.toTypedArray() ?: EMPTY_DESCRIPTOR_ARRAY

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@PublishedApi
internal inline fun <T> KSerializer<*>.cast(): KSerializer<T> = this as KSerializer<T>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@PublishedApi
internal inline fun <T> SerializationStrategy<*>.cast(): SerializationStrategy<T> = this as SerializationStrategy<T>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@PublishedApi
internal inline fun <T> DeserializationStrategy<*>.cast(): DeserializationStrategy<T> =
    this as DeserializationStrategy<T>

internal fun KClass<*>.serializerNotRegistered(): Nothing {
    throw SerializationException(notRegisteredMessage())
}

internal fun KClass<*>.notRegisteredMessage(): String = notRegisteredMessage(simpleName ?: "<local class name not available>")

internal fun notRegisteredMessage(className: String): String = "Serializer for class '$className' is not found.\n" +
        "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.\n"

internal expect fun KClass<*>.platformSpecificSerializerNotRegistered(): Nothing

@Suppress("UNCHECKED_CAST")
internal fun KType.kclass() = when (val t = classifier) {
    is KClass<*> -> t
    is KTypeParameter -> {
        // If you are going to change this error message, please also actualize the message in the compiler intrinsics here:
        // Kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/backend/ir/SerializationJvmIrIntrinsicSupport.kt#argumentTypeOrGenerateException
        throw IllegalArgumentException(
            "Captured type parameter $t from generic non-reified function. " +
                    "Such functionality cannot be supported because $t is erased, either specify serializer explicitly or make " +
                    "calling function inline with reified $t."
        )
    }

    else ->  throw IllegalArgumentException("Only KClass supported as classifier, got $t")
} as KClass<Any>

// If you are going to change this error message, please also actualize the message in the compiler intrinsics here:
// Kotlin/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/backend/ir/SerializationJvmIrIntrinsicSupport.kt#argumentTypeOrGenerateException
internal fun KTypeProjection.typeOrThrow(): KType = requireNotNull(type) { "Star projections in type arguments are not allowed, but had $type" }

/**
 * Constructs KSerializer<D<T0, T1, ...>> by given KSerializer<T0>, KSerializer<T1>, ...
 * via reflection (on JVM) or compiler+plugin intrinsic `SerializerFactory` (on Native)
 */
internal expect fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>?

/**
 * Checks whether given KType and its corresponding KClass represent a reference array
 */
internal expect fun isReferenceArray(rootClass: KClass<Any>): Boolean

/**
 *  Array.get that checks indices on JS
 */
internal expect fun <T> Array<T>.getChecked(index: Int): T

/**
 *  Array.get that checks indices on JS
 */
internal expect fun BooleanArray.getChecked(index: Int): Boolean

internal expect fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>?

/**
 * Create serializers cache for non-parametrized and non-contextual serializers.
 * The activity and type of cache is determined for a specific platform and a specific environment.
 */
internal expect fun <T> createCache(factory: (KClass<*>) -> KSerializer<T>?): SerializerCache<T>

/**
 * Create serializers cache for parametrized and non-contextual serializers. Parameters also non-contextual.
 * The activity and type of cache is determined for a specific platform and a specific environment.
 */
internal expect fun <T> createParametrizedCache(factory: (KClass<Any>, List<KType>) -> KSerializer<T>?): ParametrizedSerializerCache<T>

internal expect fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E>

internal inline fun <T, K> Iterable<T>.elementsHashCodeBy(selector: (T) -> K): Int {
    return fold(1) { hash, element -> 31 * hash + selector(element).hashCode() }
}

/**
 * Cache class for non-parametrized and non-contextual serializers.
 */
internal interface SerializerCache<T> {
    /**
     * Returns cached serializer or `null` if serializer not found.
     */
    fun get(key: KClass<Any>): KSerializer<T>?
}

/**
 * Cache class for parametrized and non-contextual serializers.
 */
internal interface ParametrizedSerializerCache<T> {
    /**
     * Returns successful result with cached serializer or `null` if root serializer not found.
     * If no serializer was found for the parameters, then result contains an exception.
     */
    fun get(key: KClass<Any>, types: List<KType> = emptyList()): Result<KSerializer<T>?>
}
