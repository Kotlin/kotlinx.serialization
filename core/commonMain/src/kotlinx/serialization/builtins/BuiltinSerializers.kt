/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR", "FunctionName")

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*
import kotlinx.serialization.descriptors.*

/**
 * Returns a nullable serializer for the given serializer of non-null type.
 */
@OptIn(ExperimentalSerializationApi::class)
public val <T : Any> KSerializer<T>.nullable: KSerializer<T?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return if (descriptor.isNullable) (this as KSerializer<T?>) else NullableSerializer(this)
    }

/**
 * Returns built-in serializer for Kotlin [Pair].
 * Resulting serializer represents pair as a structure of two key-value pairs.
 */
public fun <K, V> PairSerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Pair<K, V>> = kotlinx.serialization.internal.PairSerializer(keySerializer, valueSerializer)

/**
 * Returns built-in serializer for [Map.Entry].
 * Resulting serializer represents entry as a structure with a single key-value pair.
 * E.g. `Pair(1, 2)` and `Map.Entry(1, 2)` will be serialized to JSON as
 * `{"first": 1, "second": 2}` and {"1": 2} respectively.
 */
public fun <K, V> MapEntrySerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Map.Entry<K, V>> = kotlinx.serialization.internal.MapEntrySerializer(keySerializer, valueSerializer)

/**
 * Returns built-in serializer for Kotlin [Triple].
 * Resulting serializer represents triple as a structure of three key-value pairs.
 */
public fun <A, B, C> TripleSerializer(
    aSerializer: KSerializer<A>,
    bSerializer: KSerializer<B>,
    cSerializer: KSerializer<C>
): KSerializer<Triple<A, B, C>> = kotlinx.serialization.internal.TripleSerializer(aSerializer, bSerializer, cSerializer)

/**
 * Returns serializer for [Char] with [descriptor][SerialDescriptor] of [PrimitiveKind.CHAR] kind.
 */
public fun Char.Companion.serializer(): KSerializer<Char> = CharSerializer

/**
 * Returns serializer for [CharArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Char.Companion.serializer].
 */
@Suppress("UNCHECKED_CAST")
public fun CharArraySerializer(): KSerializer<CharArray> = CharArraySerializer

/**
 * Returns serializer for [Byte] with [descriptor][SerialDescriptor] of [PrimitiveKind.BYTE] kind.
 */
public fun Byte.Companion.serializer(): KSerializer<Byte> = ByteSerializer

/**
 * Returns serializer for [ByteArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Byte.Companion.serializer].
 */
public fun ByteArraySerializer(): KSerializer<ByteArray> = ByteArraySerializer

/**
 * Returns serializer for [Short] with [descriptor][SerialDescriptor] of [PrimitiveKind.SHORT] kind.
 */
public fun Short.Companion.serializer(): KSerializer<Short> = ShortSerializer

/**
 * Returns serializer for [ShortArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Short.Companion.serializer].
 */
public fun ShortArraySerializer(): KSerializer<ShortArray> = ShortArraySerializer

/**
 * Returns serializer for [Int] with [descriptor][SerialDescriptor] of [PrimitiveKind.INT] kind.
 */
public fun Int.Companion.serializer(): KSerializer<Int> = IntSerializer

/**
 * Returns serializer for [IntArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Int.Companion.serializer].
 */
public fun IntArraySerializer(): KSerializer<IntArray> = IntArraySerializer

/**
 * Returns serializer for [Long] with [descriptor][SerialDescriptor] of [PrimitiveKind.LONG] kind.
 */
public fun Long.Companion.serializer(): KSerializer<Long> = LongSerializer

/**
 * Returns serializer for [LongArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Long.Companion.serializer].
 */
public fun LongArraySerializer(): KSerializer<LongArray> = LongArraySerializer

/**
 * Returns serializer for [Float] with [descriptor][SerialDescriptor] of [PrimitiveKind.FLOAT] kind.
 */
public fun Float.Companion.serializer(): KSerializer<Float> = FloatSerializer

/**
 * Returns serializer for [FloatArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Float.Companion.serializer].
 */
public fun FloatArraySerializer(): KSerializer<FloatArray> = FloatArraySerializer

/**
 * Returns serializer for [Double] with [descriptor][SerialDescriptor] of [PrimitiveKind.DOUBLE] kind.
 */
public fun Double.Companion.serializer(): KSerializer<Double> = DoubleSerializer

/**
 * Returns serializer for [DoubleArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Double.Companion.serializer].
 */
public fun DoubleArraySerializer(): KSerializer<DoubleArray> = DoubleArraySerializer

/**
 * Returns serializer for [Boolean] with [descriptor][SerialDescriptor] of [PrimitiveKind.BOOLEAN] kind.
 */
public fun Boolean.Companion.serializer(): KSerializer<Boolean> = BooleanSerializer

/**
 * Returns serializer for [BooleanArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Boolean.Companion.serializer].
 */
public fun BooleanArraySerializer(): KSerializer<BooleanArray> = BooleanArraySerializer

/**
 * Returns serializer for [Unit] with [descriptor][SerialDescriptor] of [StructureKind.OBJECT] kind.
 */
@Suppress("unused")
public fun Unit.serializer(): KSerializer<Unit> = UnitSerializer

/**
 * Returns serializer for [String] with [descriptor][SerialDescriptor] of [PrimitiveKind.STRING] kind.
 */
public fun String.Companion.serializer(): KSerializer<String> = StringSerializer

/**
 * Returns serializer for reference [Array] of type [E] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized with the given [elementSerializer].
 */
@Suppress("UNCHECKED_CAST")
@ExperimentalSerializationApi
public inline fun <reified T : Any, reified E : T?> ArraySerializer(elementSerializer: KSerializer<E>): KSerializer<Array<E>> =
    ArraySerializer<T, E>(T::class, elementSerializer)

/**
 * Returns serializer for reference [Array] of type [E] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized with the given [elementSerializer].
 */
@ExperimentalSerializationApi
public fun <T : Any, E : T?> ArraySerializer(
    kClass: KClass<T>,
    elementSerializer: KSerializer<E>
): KSerializer<Array<E>> = ReferenceArraySerializer<T, E>(kClass, elementSerializer)

/**
 * Creates a serializer for [`List<T>`][List] for the given serializer of type [T].
 */
public fun <T> ListSerializer(elementSerializer: KSerializer<T>): KSerializer<List<T>> =
    ArrayListSerializer(elementSerializer)

/**
 * Creates a serializer for [`Set<T>`][Set] for the given serializer of type [T].
 */
public fun <T> SetSerializer(elementSerializer: KSerializer<T>): KSerializer<Set<T>> =
    LinkedHashSetSerializer(elementSerializer)

/**
 * Creates a serializer for [`Map<K, V>`][Map] for the given serializers for
 * its ket type [K] and value type [V].
 */
public fun <K, V> MapSerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Map<K, V>> = LinkedHashMapSerializer(keySerializer, valueSerializer)

/**
 * Returns serializer for [UInt].
 */
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
public fun UInt.Companion.serializer(): KSerializer<UInt> = UIntSerializer

/**
 * Returns serializer for [ULong].
 */
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
public fun ULong.Companion.serializer(): KSerializer<ULong> = ULongSerializer

/**
 * Returns serializer for [UByte].
 */
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
public fun UByte.Companion.serializer(): KSerializer<UByte> = UByteSerializer

/**
 * Returns serializer for [UShort].
 */
@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
public fun UShort.Companion.serializer(): KSerializer<UShort> = UShortSerializer
