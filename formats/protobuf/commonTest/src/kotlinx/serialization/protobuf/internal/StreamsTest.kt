/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlin.test.*

class StreamsTest {
    private val initialCapacity = 32
    private val varint32EncodedSizes = mapOf(
        0 to 1,
        127 to 1,
        128 to 2,
        16_383 to 2,
        16_384 to 3,
        2_097_152 to 4,
        268_435_456 to 5,
        Int.MAX_VALUE to 5,
        -1 to 10,
        Int.MIN_VALUE to 10,
    )

    private fun bytes(count: Int, offset: Int = 0) = ByteArray(count) { (it + offset).toByte() }

    @Test
    fun testManySmallWrites() {
        val output = ByteArrayOutput()
        repeat(100_000) { output.encodeVarint32(it and 0x7F) }
        val result = output.toByteArray()
        assertEquals(100_000, result.size)
        assertEquals(100_000, output.size())
        result.forEachIndexed { index, byte -> assertEquals((index and 0x7F).toByte(), byte) }
    }

    @Test
    fun testBulkWriteLargerThanGrownCapacity() {
        val output = ByteArrayOutput()
        output.encodeVarint32(1)
        val bulk = bytes(10_000)
        output.write(bulk)
        assertContentEquals(byteArrayOf(1) + bulk, output.toByteArray())
    }

    @Test
    fun testWritesExactlyAtCapacityBoundary() {
        val output = ByteArrayOutput()
        val full = bytes(initialCapacity)
        output.write(full)
        assertContentEquals(full, output.toByteArray())

        output.writeInt(0x01020304)
        output.writeLong(0x0506070809101112)
        assertContentEquals(
            full + byteArrayOf(1, 2, 3, 4) + byteArrayOf(5, 6, 7, 8, 9, 0x10, 0x11, 0x12),
            output.toByteArray()
        )
    }

    @Test
    fun testVarintsCrossingCapacityBoundary() {
        val output = ByteArrayOutput()
        output.write(bytes(initialCapacity - 2))
        output.encodeVarint64(Long.MAX_VALUE)
        output.encodeVarint32(Int.MIN_VALUE)

        val decoded = ByteArrayInput(output.toByteArray())
        repeat(initialCapacity - 2) { assertEquals(it, decoded.read()) }
        assertEquals(Long.MAX_VALUE, decoded.readVarint64(eofAllowed = false))
        assertEquals(Int.MIN_VALUE, decoded.readVarint64(eofAllowed = false).toInt())
        assertEquals(0, decoded.availableBytes)
    }

    @Test
    fun testVarint32EncodedSizes() {
        for ((value, expectedSize) in varint32EncodedSizes) {
            val output = ByteArrayOutput()
            output.encodeVarint32(value)
            assertEquals(expectedSize, output.size(), "Encoded size of $value")
            assertEquals(value, ByteArrayInput(output.toByteArray()).readVarint64(eofAllowed = false).toInt())
        }
    }

    @Test
    fun testNegativeVarint32IsSignExtendedToTenBytes() {
        for (value in listOf(-1, -128, Int.MIN_VALUE)) {
            val asVarint32 = ByteArrayOutput().apply { encodeVarint32(value) }.toByteArray()
            val asVarint64 = ByteArrayOutput().apply { encodeVarint64(value.toLong()) }.toByteArray()
            assertEquals(10, asVarint32.size, "Encoded size of $value")
            assertContentEquals(asVarint64, asVarint32)
        }
    }

    @Test
    fun testVarint32AtEveryPositionNearCapacityBoundary() {
        for ((value, expectedSize) in varint32EncodedSizes) {
            for (prefixSize in initialCapacity - 10..initialCapacity) {
                val output = ByteArrayOutput()
                output.write(bytes(prefixSize))
                output.encodeVarint32(value)
                assertEquals(prefixSize + expectedSize, output.size(), "Encoded size of $value after $prefixSize bytes")

                val decoded = ByteArrayInput(output.toByteArray())
                assertContentEquals(bytes(prefixSize), decoded.readExactNBytes(prefixSize))
                assertEquals(value, decoded.readVarint64(eofAllowed = false).toInt())
                assertEquals(0, decoded.availableBytes)
            }
        }
    }

    @Test
    fun testWriteNestedOutput() {
        val inner = ByteArrayOutput()
        val innerContent = bytes(10_000, offset = 7)
        inner.write(innerContent)

        val outer = ByteArrayOutput()
        outer.encodeVarint32(1)
        outer.write(inner)
        outer.encodeVarint32(2)

        assertContentEquals(byteArrayOf(1) + innerContent + 2.toByte(), outer.toByteArray())
    }
}
