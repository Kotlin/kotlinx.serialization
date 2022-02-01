/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class PackedArraySerializerTest {

    abstract class BaseFloatArrayCarrier {
        abstract val createdAt: ULong
        abstract val vector: FloatArray

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BaseFloatArrayCarrier) return false

            if (createdAt != other.createdAt) return false
            if (!vector.contentEquals(other.vector)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = createdAt.hashCode()
            result = 31 * result + vector.contentHashCode()
            return result
        }
    }

    @Serializable
    class PackedFloatArrayCarrier(
        @ProtoNumber(2)
        override val createdAt: ULong,
        @ProtoPacked
        @ProtoNumber(3) override val vector: FloatArray
    ) : BaseFloatArrayCarrier()

    @Serializable
    class NonPackedFloatArrayCarrier(
        @ProtoNumber(2)
        override val createdAt: ULong,
        @ProtoNumber(3) override val vector: FloatArray
    ) : BaseFloatArrayCarrier()

    @Serializable
    data class PackedStringCarrier(
        @ProtoNumber(0)
        @ProtoPacked
        val s: List<String>
    )

    /**
     * Test that when packing is specified the array is encoded as packed
     */
    @Test
    fun testEncodePackedFloatArrayProtobuf() {
        val obj = PackedFloatArrayCarrier(1234567890L.toULong(), floatArrayOf(1f, 2f, 3f))
        val s = ProtoBuf.encodeToHexString(PackedFloatArrayCarrier.serializer(), obj).uppercase()
        assertEquals("""10D285D8CC041A0C0000803F0000004000004040""", s)
    }

    /**
     * Test that when packing is not specified the array is not encoded as packed. Note that protobuf 3
     * should encode as packed by default. The format doesn't allow specifying versions at this point so
     * the default remains the original.
     */
    @Test
    fun testEncodeNonPackedFloatArrayProtobuf() {
        val obj = NonPackedFloatArrayCarrier(1234567890L.toULong(), floatArrayOf(1f, 2f, 3f))
        val s = ProtoBuf.encodeToHexString(NonPackedFloatArrayCarrier.serializer(), obj).uppercase()
        assertEquals("""10D285D8CC041D0000803F1D000000401D00004040""", s)
    }

    /**
     * Per the specification decoders should support both packed and repeated fields independent of whether
     * a field is specified as packed in the schema. Check that decoding works with both types (packed and non-packed)
     * if the data itself is packed.
     */
    @Test
    fun testDecodePackedFloatArrayProtobuf() {
        val obj: BaseFloatArrayCarrier = PackedFloatArrayCarrier(1234567890L.toULong(), floatArrayOf(1f, 2f, 3f))
        val s = """10D285D8CC041A0C0000803F0000004000004040"""
        val decodedPacked = ProtoBuf.decodeFromHexString(PackedFloatArrayCarrier.serializer(), s)
        assertEquals(obj, decodedPacked)
        val decodedNonPacked = ProtoBuf.decodeFromHexString(NonPackedFloatArrayCarrier.serializer(), s)
        assertEquals(obj, decodedNonPacked)
    }

    /**
     * Per the specification decoders should support both packed and repeated fields independent of whether
     * a field is specified as packed in the schema. Check that decoding works with both types (packed and non-packed)
     * if the data itself is not packed.
     */
    @Test
    fun testDecodeNonPackedFloatArrayProtobuf() {
        val obj: BaseFloatArrayCarrier = PackedFloatArrayCarrier(1234567890L.toULong(), floatArrayOf(1f, 2f, 3f))
        val s = """10D285D8CC041D0000803F1D000000401D00004040"""
        val decodedPacked = ProtoBuf.decodeFromHexString(PackedFloatArrayCarrier.serializer(), s)
        assertEquals(obj, decodedPacked)
        val decodedNonPacked = ProtoBuf.decodeFromHexString(NonPackedFloatArrayCarrier.serializer(), s)
        assertEquals(obj, decodedNonPacked)
    }

    /**
     * Test that serializing a list of strings is never packed, and deserialization ignores the packing annotation.
     */
    @Test
    fun testEncodeAnnotatedStringList() {
        val obj = PackedStringCarrier(listOf("aaa", "bbb", "ccc"))
        val expectedHex = "020361616102036262620203636363"
        val encodedHex = ProtoBuf.encodeToHexString(obj)
        assertEquals(expectedHex, encodedHex)
        assertEquals(obj, ProtoBuf.decodeFromHexString<PackedStringCarrier>(expectedHex))

        val invalidPackedHex = "020C036161610362626203636363"
        val decoded = ProtoBuf.decodeFromHexString<PackedStringCarrier>(invalidPackedHex)
        val invalidString = "\u0003aaa\u0003bbb\u0003ccc"
        assertEquals(PackedStringCarrier(listOf(invalidString)), decoded)
    }

    /**
     * Test that toplevel "packed" lists with only byte length also work.
     */
    @Test
    fun testDecodeToplevelPackedList() {
        val input = "0feffdb6f507e6cc9933ba0180feff03"
        val listData = listOf(0x7eadbeef, 0x6666666, 0xba, 0x7fff00)
        val decoded = ProtoBuf.decodeFromHexString<List<Int>>(input)

        assertEquals(listData, decoded)
    }

}
