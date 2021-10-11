/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import org.junit.Test
import kotlin.test.*

/**
 * Test that [ProtoBuf] works correctly if [ProtoBuf.encodeDefaults] is set to `false`.
 *
 * In this case default values should not get encoded into bytes.
 */
class ProtoBufOptionalTest {

    /** ProtoBuf instance that does **not** encode defaults. */
    private val protoBuf = ProtoBuf { encodeDefaults = false }

    @Test
    fun readCompareWithDefaults() {
        val data = MessageWithOptionals()
        assertTrue(readCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun dumpCompareWithDefaults() {
        val data = MessageWithOptionals()
        assertTrue(dumpCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun readCompareWithValues() {
        val data = MessageWithOptionals(42, "Test", MessageWithOptionals.Position.SECOND, 24, listOf(1, 2, 3))
        assertTrue(readCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun dumpCompareWithValues() {
        val data = MessageWithOptionals(42, "Test", MessageWithOptionals.Position.SECOND, 24, listOf(1, 2, 3))
        assertTrue(dumpCompare(data, alwaysPrint = true, protoBuf = protoBuf))
    }

    @Test
    fun testThatDefaultValuesAreNotEncoded() {
        val data = MessageWithOptionals()
        val parsed = TestData.MessageWithOptionals.parseFrom(protoBuf.encodeToByteArray(data))

        assertFalse(parsed.hasA(), "Expected that default value for optional field `a` is not encoded.")
        assertFalse(parsed.hasB(), "Expected that default value for optional field `b` is not encoded.")
        assertFalse(parsed.hasC(), "Expected that default value for optional field `c` is not encoded.")
        assertFalse(parsed.hasD(), "Expected that default value for optional field `d` is not encoded.")

        assertEquals(0, parsed.a, "Expected default value for field `a`.")
        assertEquals("", parsed.b, "Expected default value for field `b`.")
        assertEquals(TestData.MessageWithOptionals.Position.FIRST, parsed.c, "Expected default value for field `c`.")
        assertEquals(99, parsed.d, "Expected default value for field `d`.")
        assertEquals(emptyList(), parsed.eList, "Expected default value for field `e`.")
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

    /**
     * Test [Serializable] that manually implements `TestOptional` defined in `test_data.proto`.
     */
    @Serializable
    data class MessageWithOptionals(
        val a: Int = 0,
        val b: String = "",
        val c: Position = Position.FIRST,
        val d: Int = 99,
        val e: List<Int> = emptyList()
    ) : IMessage {

        /**
         * Convert this [Serializable] object to its expected [TestData.MessageWithOptionals] ProtoBuf message.
         *
         * For this test we expect that default values are not encoded.
         */
        override fun toProtobufMessage(): TestData.MessageWithOptionals =
            TestData.MessageWithOptionals.newBuilder().also { builder ->
                val defaults = MessageWithOptionals()
                if (a != defaults.a) builder.a = a
                if (b != defaults.b) builder.b = b
                if (c != defaults.c) builder.c = c.toProtoBuf()
                if (d != defaults.d) builder.d = d
                if (e != defaults.e) builder.addAllE(e)
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
