/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufTruncatedInputTest {

    @Serializable
    private data class FixedInt(@ProtoType(ProtoIntegerType.FIXED) val value: Int)

    @Serializable
    private data class FixedLong(@ProtoType(ProtoIntegerType.FIXED) val value: Long)

    @Serializable
    private data class FloatHolder(val value: Float)

    @Serializable
    private data class DoubleHolder(val value: Double)

    @Serializable
    private data class PackedFloats(@ProtoPacked val value: List<Float>)

    @Serializable
    private data class PackedDoubles(@ProtoPacked val value: List<Double>)

    @Serializable
    private data class StringHolder(val value: String)

    @Serializable
    private data class NestedString(@ProtoNumber(1) val inner: StringHolder, @ProtoNumber(2) val tail: Int = 0)

    @Serializable
    private data class Known(@ProtoNumber(1) val known: Int = 0)

    private inline fun <reified T : Any> assertTruncated(hex: String) {
        assertFailsWith<SerializationException>("Input $hex should be rejected") {
            ProtoBuf.decodeFromHexString<T>(hex)
        }
    }

    /** All the prefixes of [payload] except [payload] itself, after the given field [header]. */
    private fun prefixes(header: String, payload: String): List<String> =
        (0 until payload.length step 2).map { header + payload.substring(0, it) }

    @Test
    fun testTruncatedFixedInt() {
        val encoded = ProtoBuf.encodeToHexString(FixedInt(0x04030201))
        assertEquals("0d01020304", encoded)
        prefixes("0d", "01020304").forEach { assertTruncated<FixedInt>(it) }
        assertEquals(FixedInt(0x04030201), ProtoBuf.decodeFromHexString(encoded))
    }

    @Test
    fun testTruncatedFixedLong() {
        val encoded = ProtoBuf.encodeToHexString(FixedLong(0x0807060504030201))
        assertEquals("090102030405060708", encoded)
        prefixes("09", "0102030405060708").forEach { assertTruncated<FixedLong>(it) }
        assertEquals(FixedLong(0x0807060504030201), ProtoBuf.decodeFromHexString(encoded))
    }

    @Test
    fun testTruncatedFloat() {
        val encoded = ProtoBuf.encodeToHexString(FloatHolder(1.5f))
        assertEquals("0d0000c03f", encoded)
        prefixes("0d", "0000c03f").forEach { assertTruncated<FloatHolder>(it) }
        assertEquals(FloatHolder(1.5f), ProtoBuf.decodeFromHexString(encoded))
    }

    @Test
    fun testTruncatedDouble() {
        val encoded = ProtoBuf.encodeToHexString(DoubleHolder(1.5))
        assertEquals("09000000000000f83f", encoded)
        prefixes("09", "000000000000f83f").forEach { assertTruncated<DoubleHolder>(it) }
        assertEquals(DoubleHolder(1.5), ProtoBuf.decodeFromHexString(encoded))
    }

    @Test
    fun testTruncatedElementOfPackedList() {
        // The length of the packed field is valid, but it doesn't fit a whole number of elements.
        assertTruncated<PackedFloats>("0a020000")
        assertTruncated<PackedFloats>("0a060000c03f0000")
        assertTruncated<PackedDoubles>("0a0400000000")
    }

    @Test
    fun testTruncatedUnknownFixedFieldIsNotSkipped() {
        // Field 2, which Known doesn't have, with the wire types i32 and i64
        prefixes("15", "01020304").forEach { assertTruncated<Known>(it) }
        prefixes("11", "0102030405060708").forEach { assertTruncated<Known>(it) }
        assertEquals(Known(), ProtoBuf.decodeFromHexString("1501020304"))
        assertEquals(Known(), ProtoBuf.decodeFromHexString("110102030405060708"))
    }

    @Test
    fun testTruncatedString() {
        // Length is 5, but there are less bytes
        assertTruncated<StringHolder>("0a05")
        assertTruncated<StringHolder>("0a056162")
        assertEquals(StringHolder("abcde"), ProtoBuf.decodeFromHexString("0a056162636465"))
    }

    @Test
    fun testStringDoesNotReadPastTheEndOfNestedMessage() {
        // The nested message is 3 bytes long and its string claims 5 bytes, which are available only in the outer one.
        assertTruncated<NestedString>("0a030a0561100110011001")
        assertEquals(NestedString(StringHolder("a"), 1), ProtoBuf.decodeFromHexString("0a030a01611001"))
    }
}
