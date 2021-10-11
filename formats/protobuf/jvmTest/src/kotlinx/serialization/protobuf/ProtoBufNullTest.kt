/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.protobuf

import kotlinx.serialization.*

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test that [ProtoBuf] works correctly if [ProtoBuf.encodeDefaults] is set to `false`
 * and that `null` is allowed as a default value.
 *
 * In this case `null` values should not get encoded into bytes. This allows to check if an optional
 * field was set or not (like it is possible with the *Java ProtoBuf library*).
 */
class ProtoBufNullTest {

    /** ProtoBuf instance that does **not** encode defaults. */
    private val protoBuf = ProtoBuf { encodeDefaults = false }

    @Test
    fun testReadCompareWithNulls() {
        val data = MessageWithOptionals()
        assertTrue(readCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun testDumpCompareWithNulls() {
        val data = MessageWithOptionals()
        assertTrue(dumpCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun testReadCompareWithDefaults() {
        val data = MessageWithOptionals(0, "", MessageWithOptionals.Position.FIRST, 99, listOf(1, 2, 3))
        assertTrue(readCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun testDumpCompareWithDefaults() {
        val data = MessageWithOptionals(0, "", MessageWithOptionals.Position.FIRST, 99, listOf(1, 2, 3))
        assertTrue(dumpCompare(data, protoBuf = protoBuf))
    }

    @Test
    fun testReadCompareWithValues() {
        val data = MessageWithOptionals(42, "Test", MessageWithOptionals.Position.SECOND, 24, listOf(1, 2, 3))
        assertTrue(readCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun testDumpCompareWithValues() {
        val data = MessageWithOptionals(42, "Test", MessageWithOptionals.Position.SECOND, 24, listOf(1, 2, 3))
        assertTrue(dumpCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun testThatNullValuesAreNotEncoded() {
        val data = MessageWithOptionals()
        val parsed = TestData.MessageWithOptionals.parseFrom(protoBuf.encodeToByteArray(data))

        assertFalse(parsed.hasA(), "Expected that null value for optional field `a` is not encoded.")
        assertFalse(parsed.hasB(), "Expected that null value for optional field `b` is not encoded.")
        assertFalse(parsed.hasC(), "Expected that null value for optional field `c` is not encoded.")
        assertFalse(parsed.hasD(), "Expected that null value for optional field `d` is not encoded.")

        assertEquals(0, parsed.a, "Expected default value for field `a`.")
        assertEquals("", parsed.b, "Expected default value for field `b`.")
        assertEquals(TestData.MessageWithOptionals.Position.FIRST, parsed.c, "Expected default value for field `c`.")
        assertEquals(99, parsed.d, "Expected default value for field `d`.")
        assertEquals(emptyList(), parsed.eList, "Expected default value for field `e`.")
    }

    @Test
    fun testThatDefaultValuesAreEncodedCorrectly() {
        val data = MessageWithOptionals(0, "", MessageWithOptionals.Position.FIRST, 99, emptyList())
        val parsed = TestData.MessageWithOptionals.parseFrom(protoBuf.encodeToByteArray(data))

        assertTrue(parsed.hasA(), "Expected that custom value for optional field `a` is encoded.")
        assertTrue(parsed.hasB(), "Expected that custom value for optional field `b` is encoded.")
        assertTrue(parsed.hasC(), "Expected that custom value for optional field `c` is encoded.")
        assertTrue(parsed.hasD(), "Expected that custom value for optional field `d` is encoded.")

        assertEquals(0, parsed.a, "Expected custom value for field `a`.")
        assertEquals("", parsed.b, "Expected custom value for field `b`.")
        assertEquals(TestData.MessageWithOptionals.Position.FIRST, parsed.c, "Expected custom value for field `c`.")
        assertEquals(99, parsed.d, "Expected custom value for field `d`.")
        assertEquals(emptyList(), parsed.eList, "Expected custom value for field `e`.")
    }

    @Test
    fun testThatCustomValuesAreEncodedCorrectly() {
        val data = MessageWithOptionals(42, "Test", MessageWithOptionals.Position.SECOND, 24, listOf(1, 2, 3))
        val parsed = TestData.MessageWithOptionals.parseFrom(protoBuf.encodeToByteArray(data))

        assertTrue(parsed.hasA(), "Expected that custom value for optional field `a` is encoded.")
        assertTrue(parsed.hasB(), "Expected that custom value for optional field `b` is encoded.")
        assertTrue(parsed.hasC(), "Expected that custom value for optional field `c` is encoded.")
        assertTrue(parsed.hasD(), "Expected that custom value for optional field `d` is encoded.")

        assertEquals(42, parsed.a, "Expected custom value for field `a`.")
        assertEquals("Test", parsed.b, "Expected custom value for field `b`.")
        assertEquals(TestData.MessageWithOptionals.Position.SECOND, parsed.c, "Expected custom value for field `c`.")
        assertEquals(24, parsed.d, "Expected custom value for field `d`.")
        assertEquals(listOf(1, 2, 3), parsed.eList, "Expected custom value for field `e`.")
    }

    @Test
    fun testThatNullValuesAreNotDecoded() {
        val data = TestData.MessageWithOptionals.newBuilder().build()
        val parsed = protoBuf.decodeFromByteArray<MessageWithOptionals>(data.toByteArray())

        assertFalse(parsed.hasA(), "Expected that null value for optional field `a` is not decoded.")
        assertFalse(parsed.hasB(), "Expected that null value for optional field `b` is not decoded.")
        assertFalse(parsed.hasC(), "Expected that null value for optional field `c` is not decoded.")
        assertFalse(parsed.hasD(), "Expected that null value for optional field `d` is not decoded.")

        assertEquals(0, parsed.a, "Expected default value for field `a`.")
        assertEquals("", parsed.b, "Expected default value for field `b`.")
        assertEquals(MessageWithOptionals.Position.FIRST, parsed.c, "Expected default value for field `c`.")
        assertEquals(99, parsed.d, "Expected default value for field `d`.")
        assertEquals(emptyList(), parsed.e, "Expected default value for field `e`.")
    }

    @Test
    fun testThatDefaultValuesAreDecodedCorrectly() {
        val data = TestData.MessageWithOptionals.newBuilder()
                .setA(0)
                .setB("")
                .setC(TestData.MessageWithOptionals.Position.FIRST)
                .setD(99)
                .addAllE(emptyList())
                .build()
        val parsed = protoBuf.decodeFromByteArray<MessageWithOptionals>(data.toByteArray())

        assertTrue(parsed.hasA(), "Expected that custom value for optional field `a` is decoded.")
        assertTrue(parsed.hasB(), "Expected that custom value for optional field `b` is decoded.")
        assertTrue(parsed.hasC(), "Expected that custom value for optional field `c` is decoded.")
        assertTrue(parsed.hasD(), "Expected that custom value for optional field `d` is decoded.")

        assertEquals(0, parsed.a, "Expected custom value for field `a`.")
        assertEquals("", parsed.b, "Expected custom value for field `b`.")
        assertEquals(MessageWithOptionals.Position.FIRST, parsed.c, "Expected custom value for field `c`.")
        assertEquals(99, parsed.d, "Expected custom value for field `d`.")
        assertEquals(emptyList(), parsed.e, "Expected custom value for field `e`.")
    }

    @Test
    fun testThatCustomValuesAreDecodedCorrectly() {
        val data = TestData.MessageWithOptionals.newBuilder()
                .setA(42)
                .setB("Test")
                .setC(TestData.MessageWithOptionals.Position.SECOND)
                .setD(24)
                .addAllE(listOf(1, 2, 3))
                .build()
        val parsed = protoBuf.decodeFromByteArray<MessageWithOptionals>(data.toByteArray())

        assertTrue(parsed.hasA(), "Expected that custom value for optional field `a` is decoded.")
        assertTrue(parsed.hasB(), "Expected that custom value for optional field `b` is decoded.")
        assertTrue(parsed.hasC(), "Expected that custom value for optional field `c` is decoded.")
        assertTrue(parsed.hasD(), "Expected that custom value for optional field `d` is decoded.")

        assertEquals(42, parsed.a, "Expected custom value for field `a`.")
        assertEquals("Test", parsed.b, "Expected custom value for field `b`.")
        assertEquals(MessageWithOptionals.Position.SECOND, parsed.c, "Expected custom value for field `c`.")
        assertEquals(24, parsed.d, "Expected custom value for field `d`.")
        assertEquals(listOf(1, 2, 3), parsed.e, "Expected custom value for field `e`.")
    }

    /**
     * Test [Serializable] that manually implements `TestOptional` defined in `test_data.proto`.
     *
     * Using `null` as default values allows to implement [hasA], ... according to Java ProtoBuf library.
     */
    @Serializable
    private data class MessageWithOptionals(
            private val _a: Int? = null,
            private val _b: String? = null,
            private val _c: Position? = null,
            private val _d: Int? = null,
            private val _e: List<Int>? = null
    ) : IMessage {

        val a: Int
            get() = _a ?: 0

        val b: String
            get() = _b ?: ""

        val c: Position
            get() = _c ?: Position.FIRST

        val d: Int
            get() = _d ?: 99

        val e: List<Int>
            get() = _e ?: emptyList()

        fun hasA() = _a != null

        fun hasB() = _b != null

        fun hasC() = _c != null

        fun hasD() = _d != null

        /**
         * Convert this [Serializable] object to its expected [TestData.MessageWithOptionals] ProtoBuf message.
         *
         * For this test we expect that `null` values are not encoded.
         */
        override fun toProtobufMessage(): TestData.MessageWithOptionals =
                TestData.MessageWithOptionals.newBuilder().also { builder ->
                    if (_a != null) builder.a = _a
                    if (_b != null) builder.b = _b
                    if (_c != null) builder.c = _c.toProtoBuf()
                    if (_d != null) builder.d = _d
                    if (_e != null) builder.addAllE(_e)
                }.build()

        enum class Position {
            FIRST, SECOND;

            fun toProtoBuf() = when (this) {
                FIRST -> TestData.MessageWithOptionals.Position.FIRST
                SECOND -> TestData.MessageWithOptionals.Position.SECOND
            }
        }
    }
}
