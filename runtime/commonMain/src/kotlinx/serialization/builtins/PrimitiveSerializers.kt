/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR", "FunctionName")

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * Returns serializer for [Char] with [descriptor][SerialDescriptor] of [PrimitiveKind.CHAR] kind.
 */
public fun Char.Companion.serializer(): KSerializer<Char> = kotlinx.serialization.internal.CharSerializer

/**
 * Returns serializer for [CharArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Char.Companion.serializer].
 */
@Suppress("UNCHECKED_CAST")
public fun CharArraySerializer(): KSerializer<CharArray> = CharArraySerializer

/**
 * Returns serializer for [Byte] with [descriptor][SerialDescriptor] of [PrimitiveKind.BYTE] kind.
 */
public fun Byte.Companion.serializer(): KSerializer<Byte> = kotlinx.serialization.internal.ByteSerializer

/**
 * Returns serializer for [ByteArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Byte.Companion.serializer].
 */
public fun ByteArraySerializer(): KSerializer<ByteArray> = ByteArraySerializer

/**
 * Returns serializer for [Short] with [descriptor][SerialDescriptor] of [PrimitiveKind.SHORT] kind.
 */
public fun Short.Companion.serializer(): KSerializer<Short> = kotlinx.serialization.internal.ShortSerializer

/**
 * Returns serializer for [ShortArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Short.Companion.serializer].
 */
public fun ShortArraySerializer(): KSerializer<ShortArray> = ShortArraySerializer

/**
 * Returns serializer for [Int] with [descriptor][SerialDescriptor] of [PrimitiveKind.INT] kind.
 */
public fun Int.Companion.serializer(): KSerializer<Int> = kotlinx.serialization.internal.IntSerializer

/**
 * Returns serializer for [IntArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Int.Companion.serializer].
 */
public fun IntArraySerializer(): KSerializer<IntArray> = IntArraySerializer

/**
 * Returns serializer for [Long] with [descriptor][SerialDescriptor] of [PrimitiveKind.LONG] kind.
 */
public fun Long.Companion.serializer(): KSerializer<Long> = kotlinx.serialization.internal.LongSerializer

/**
 * Returns serializer for [LongArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Long.Companion.serializer].
 */
public fun LongArraySerializer(): KSerializer<LongArray> = LongArraySerializer

/**
 * Returns serializer for [Float] with [descriptor][SerialDescriptor] of [PrimitiveKind.FLOAT] kind.
 */
public fun Float.Companion.serializer(): KSerializer<Float> = kotlinx.serialization.internal.FloatSerializer

/**
 * Returns serializer for [FloatArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Float.Companion.serializer].
 */
public fun FloatArraySerializer(): KSerializer<FloatArray> = FloatArraySerializer

/**
 * Returns serializer for [Double] with [descriptor][SerialDescriptor] of [PrimitiveKind.DOUBLE] kind.
 */
public fun Double.Companion.serializer(): KSerializer<Double> = kotlinx.serialization.internal.DoubleSerializer

/**
 * Returns serializer for [DoubleArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Double.Companion.serializer].
 */
public fun DoubleArraySerializer(): KSerializer<DoubleArray> = DoubleArraySerializer

/**
 * Returns serializer for [Boolean] with [descriptor][SerialDescriptor] of [PrimitiveKind.BOOLEAN] kind.
 */
public fun Boolean.Companion.serializer(): KSerializer<Boolean> = kotlinx.serialization.internal.BooleanSerializer

/**
 * Returns serializer for [BooleanArray] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized one by one with [Boolean.Companion.serializer].
 */
public fun BooleanArraySerializer(): KSerializer<BooleanArray> = BooleanArraySerializer

/**
 * Returns serializer for [UnitSerializer] with [descriptor][SerialDescriptor] of [StructureKind.OBJECT] kind.
 */
public fun UnitSerializer(): KSerializer<Unit> = kotlinx.serialization.internal.UnitSerializer

/**
 * Returns serializer for [String] with [descriptor][SerialDescriptor] of [PrimitiveKind.STRING] kind.
 */
public fun String.Companion.serializer(): KSerializer<String> = kotlinx.serialization.internal.StringSerializer

/**
 * Returns serializer for reference [Array] of type [E] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized with the given [elementSerializer].
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : Any, reified E : T?> ArraySerializer(elementSerializer: KSerializer<E>): KSerializer<Array<E>> =
    ArraySerializer<T, E>(T::class, elementSerializer)

/**
 * Returns serializer for reference [Array] of type [E] with [descriptor][SerialDescriptor] of [StructureKind.LIST] kind.
 * Each element of the array is serialized with the given [elementSerializer].
 */
public fun <T : Any, E : T?> ArraySerializer(
    kClass: KClass<T>,
    elementSerializer: KSerializer<E>
): KSerializer<Array<E>> = ReferenceArraySerializer<T, E>(kClass, elementSerializer)
