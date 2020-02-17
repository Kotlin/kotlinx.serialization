/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR")

package kotlinx.serialization.builtins

import kotlinx.serialization.*

/**
 * Returns serializer for [String] with descriptor of [PrimitiveKind.STRING] kind.
 */
public fun String.Companion.serializer(): KSerializer<String> = kotlinx.serialization.internal.StringSerializer

/**
 * Returns serializer for [Char] with descriptor of [PrimitiveKind.CHAR] kind.
 */
public fun Char.Companion.serializer(): KSerializer<Char> = kotlinx.serialization.internal.CharSerializer

/**
 * Returns serializer for [Byte] with descriptor of [PrimitiveKind.BYTE] kind.
 */
public fun Byte.Companion.serializer(): KSerializer<Byte> = kotlinx.serialization.internal.ByteSerializer

/**
 * Returns serializer for [Short] with descriptor of [PrimitiveKind.SHORT] kind.
 */
public fun Short.Companion.serializer(): KSerializer<Short> = kotlinx.serialization.internal.ShortSerializer

/**
 * Returns serializer for [Int] with descriptor of [PrimitiveKind.INT] kind.
 */
public fun Int.Companion.serializer(): KSerializer<Int> = kotlinx.serialization.internal.IntSerializer

/**
 * Returns serializer for [Long] with descriptor of [PrimitiveKind.LONG] kind.
 */
public fun Long.Companion.serializer(): KSerializer<Long> = kotlinx.serialization.internal.LongSerializer

/**
 * Returns serializer for [Float] with descriptor of [PrimitiveKind.FLOAT] kind.
 */
public fun Float.Companion.serializer(): KSerializer<Float> = kotlinx.serialization.internal.FloatSerializer

/**
 * Returns serializer for [Double] with descriptor of [PrimitiveKind.DOUBLE] kind.
 */
public fun Double.Companion.serializer(): KSerializer<Double> = kotlinx.serialization.internal.DoubleSerializer

/**
 * Returns serializer for [Boolean] with descriptor of [PrimitiveKind.BOOLEAN] kind.
 */
public fun Boolean.Companion.serializer(): KSerializer<Boolean> = kotlinx.serialization.internal.BooleanSerializer

/**
 * Returns serializer for [UnitSerializer] with descriptor of [StructureKind.OBJECT] kind.
 */
public fun UnitSerializer(): KSerializer<Unit> = kotlinx.serialization.internal.UnitSerializer
