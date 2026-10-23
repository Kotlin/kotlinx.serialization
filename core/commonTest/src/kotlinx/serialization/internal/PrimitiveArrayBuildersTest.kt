/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
class PrimitiveArrayBuildersTest {
    private val count = 100_000

    @Test
    fun testByteArrayBuilderGrowth() {
        val builder = ByteArrayBuilder()
        repeat(count) { builder.append(it.toByte()) }
        assertContentEquals(ByteArray(count) { it.toByte() }, builder.build())
    }

    @Test
    fun testShortArrayBuilderGrowth() {
        val builder = ShortArrayBuilder()
        repeat(count) { builder.append(it.toShort()) }
        assertContentEquals(ShortArray(count) { it.toShort() }, builder.build())
    }

    @Test
    fun testIntArrayBuilderGrowth() {
        val builder = IntArrayBuilder()
        repeat(count) { builder.append(it) }
        assertContentEquals(IntArray(count) { it }, builder.build())
    }

    @Test
    fun testLongArrayBuilderGrowth() {
        val builder = LongArrayBuilder()
        repeat(count) { builder.append(it.toLong()) }
        assertContentEquals(LongArray(count) { it.toLong() }, builder.build())
    }

    @Test
    fun testFloatArrayBuilderGrowth() {
        val builder = FloatArrayBuilder()
        repeat(count) { builder.append(it.toFloat()) }
        assertContentEquals(FloatArray(count) { it.toFloat() }, builder.build())
    }

    @Test
    fun testDoubleArrayBuilderGrowth() {
        val builder = DoubleArrayBuilder()
        repeat(count) { builder.append(it.toDouble()) }
        assertContentEquals(DoubleArray(count) { it.toDouble() }, builder.build())
    }

    @Test
    fun testCharArrayBuilderGrowth() {
        val builder = CharArrayBuilder()
        repeat(count) { builder.append(it.toChar()) }
        assertContentEquals(CharArray(count) { it.toChar() }, builder.build())
    }

    @Test
    fun testBooleanArrayBuilderGrowth() {
        val builder = BooleanArrayBuilder()
        repeat(count) { builder.append(it % 3 == 0) }
        assertContentEquals(BooleanArray(count) { it % 3 == 0 }, builder.build())
    }

    @Test
    fun testUByteArrayBuilderGrowth() {
        val builder = UByteArrayBuilder()
        repeat(count) { builder.append(it.toUByte()) }
        assertContentEquals(UByteArray(count) { it.toUByte() }, builder.build())
    }

    @Test
    fun testUShortArrayBuilderGrowth() {
        val builder = UShortArrayBuilder()
        repeat(count) { builder.append(it.toUShort()) }
        assertContentEquals(UShortArray(count) { it.toUShort() }, builder.build())
    }

    @Test
    fun testUIntArrayBuilderGrowth() {
        val builder = UIntArrayBuilder()
        repeat(count) { builder.append(it.toUInt()) }
        assertContentEquals(UIntArray(count) { it.toUInt() }, builder.build())
    }

    @Test
    fun testULongArrayBuilderGrowth() {
        val builder = ULongArrayBuilder()
        repeat(count) { builder.append(it.toULong()) }
        assertContentEquals(ULongArray(count) { it.toULong() }, builder.build())
    }

    @Test
    fun testGrowthFromEmptyBuffer() {
        val builder = IntArrayBuilder(0)
        repeat(100) { builder.append(it) }
        assertContentEquals(IntArray(100) { it }, builder.build())
    }

    @Test
    fun testGrowthFromExistingContent() {
        val builder = LongArrayBuilder(longArrayOf(1, 2, 3))
        repeat(1_000) { builder.append(it.toLong()) }
        assertContentEquals(longArrayOf(1, 2, 3) + LongArray(1_000) { it.toLong() }, builder.build())
    }

    @Test
    fun testEnsureCapacityWithExpectedSize() {
        val builder = ByteArrayBuilder()
        builder.ensureCapacity(count)
        repeat(count) { builder.append(it.toByte()) }
        assertContentEquals(ByteArray(count) { it.toByte() }, builder.build())
    }
}
