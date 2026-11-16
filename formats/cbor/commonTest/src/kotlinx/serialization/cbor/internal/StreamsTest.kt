/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor.internal

import kotlin.test.*

class StreamsTest {
    private val initialCapacity = 32

    private fun bytes(count: Int, offset: Int = 0) = ByteArray(count) { (it + offset).toByte() }

    @Test
    fun testManySingleByteWrites() {
        val output = ByteArrayOutput()
        val expected = bytes(100_000)
        expected.forEach { output.write(it.toInt()) }
        assertContentEquals(expected, output.toByteArray())
    }

    @Test
    fun testBulkWriteLargerThanGrownCapacity() {
        val output = ByteArrayOutput()
        output.write(1)
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

        output.write(42)
        assertContentEquals(full + 42.toByte(), output.toByteArray())
    }

    @Test
    fun testWriteWithOffsetAndCount() {
        val output = ByteArrayOutput()
        val source = bytes(1_000)
        output.write(source, offset = 100, count = 500)
        assertContentEquals(source.copyOfRange(100, 600), output.toByteArray())
    }

    @Test
    fun testCopyFromNestedOutput() {
        val inner = ByteArrayOutput()
        val innerContent = bytes(10_000, offset = 7)
        inner.write(innerContent)

        val outer = ByteArrayOutput()
        outer.write(1)
        outer.write(2)
        outer.copyFrom(inner)
        outer.write(3)

        assertContentEquals(byteArrayOf(1, 2) + innerContent + 3.toByte(), outer.toByteArray())
    }

    @Test
    fun testInvalidWriteArgumentsThrow() {
        val output = ByteArrayOutput()
        assertFailsWith<IndexOutOfBoundsException> { output.write(bytes(10), offset = -1, count = 1) }
        assertFailsWith<IndexOutOfBoundsException> { output.write(bytes(10), offset = 5, count = 6) }
        assertFailsWith<IndexOutOfBoundsException> { output.write(bytes(10), offset = 0, count = -1) }
        assertContentEquals(ByteArray(0), output.toByteArray())
    }
}
