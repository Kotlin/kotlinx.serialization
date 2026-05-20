/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class ProtobufUnknownFieldsTest {
    @Serializable
    data class InnerData(val name: String, val b: Int, val c: List<String>)

    @Serializable
    data class BuildData(val a: Int, val b: String, val c: ByteArray, val d: List<Int>, val e: InnerData) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as BuildData

            if (a != other.a) return false
            if (b != other.b) return false
            if (!c.contentEquals(other.c)) return false
            if (d != other.d) return false
            if (e != other.e) return false

            return true
        }

        override fun hashCode(): Int {
            var result = a
            result = 31 * result + b.hashCode()
            result = 31 * result + c.contentHashCode()
            result = 31 * result + d.hashCode()
            result = 31 * result + e.hashCode()
            return result
        }

    }

    @Serializable
    data class DataWithUnknownFields(
        val a: Int,
        val unknownFields: ProtoUnknownFieldHolder
    )

    @Test
    fun testDecodeFromHexString() {
        // This is the only test that verifies decoding from a known HEX constant
        val encoded = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(DataWithUnknownFields.serializer(), encoded)
        assertEquals(42, decoded.a)
        assertTrue(decoded.unknownFields.fields.isNotEmpty())
    }

    @Test
    fun testDecodeWithUnknownField() {
        val data = BuildData(
            42,
            "42",
            byteArrayOf(42, 42, 42),
            listOf(42, 42, 42),
            InnerData("42", 42, listOf("42", "42", "42"))
        )

        val encoded = ProtoBuf.encodeToByteArray(BuildData.serializer(), data)
        val decoded = ProtoBuf.decodeFromByteArray(DataWithUnknownFields.serializer(), encoded)
        assertEquals(data.a, decoded.a)
        assertTrue(decoded.unknownFields.fields.isNotEmpty())

        val reEncoded = ProtoBuf.encodeToByteArray(DataWithUnknownFields.serializer(), decoded)
        assertContentEquals(encoded, reEncoded)
        val restored = ProtoBuf.decodeFromByteArray(BuildData.serializer(), reEncoded)
        assertEquals(data, restored)
    }

    @Test
    fun testCannotDecodeArbitraryMessage() {
        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.decodeFromHexString(ProtoUnknownFieldHolder.serializer(), "")
        }
    }

    @Test
    fun testCannotEncodeArbitraryMessage() {
        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.encodeToHexString(ProtoUnknownFieldHolder.serializer(), ProtoUnknownFieldHolder.Empty)
        }
    }

    @Serializable
    data class DataWithMultipleUnknownFields(
        val a: Int,
        val unknownFields: ProtoUnknownFieldHolder,
        val unknownFields2: ProtoUnknownFieldHolder
    )

    @Test
    fun testOnlyOneUnknownFieldAllowed() {
        val data = BuildData(42, "42", byteArrayOf(42), listOf(42), InnerData("42", 42, listOf("42")))
        val encoded = ProtoBuf.encodeToHexString(BuildData.serializer(), data)
        assertFailsWithMessage<IllegalArgumentException>(
            "Only one unknown fields holder is allowed in a message, but get unknownFields2 and unknownFields"
        ) {
            ProtoBuf.decodeFromHexString(DataWithMultipleUnknownFields.serializer(), encoded)
        }
    }

    @Serializable
    data class DataWithStaggeredFields(
        @ProtoNumber(2)
        val b: String,
        val unknownFields: ProtoUnknownFieldHolder,
        @ProtoNumber(4)
        val d: List<Int>
    )

    @Test
    fun testUnknownFieldBeforeKnownField() {
        val data = BuildData(
            42,
            "42",
            byteArrayOf(42, 42, 42),
            listOf(42, 42, 42),
            InnerData("42", 42, listOf("42", "42", "42"))
        )

        val encoded = ProtoBuf.encodeToByteArray(BuildData.serializer(), data)
        val decoded = ProtoBuf.decodeFromByteArray(DataWithStaggeredFields.serializer(), encoded)
        assertTrue(decoded.unknownFields.fields.isNotEmpty())
        assertEquals("42", decoded.b)
        assertEquals(listOf(42, 42, 42), decoded.d)

        val reEncoded = ProtoBuf.encodeToByteArray(DataWithStaggeredFields.serializer(), decoded)
        val restored = ProtoBuf.decodeFromByteArray(BuildData.serializer(), reEncoded)
        assertEquals(data, restored)
    }

    @Serializable
    data class TotalKnownData(val fields: ProtoUnknownFieldHolder = ProtoUnknownFieldHolder.Empty)

    @Serializable
    data class NestedUnknownData(
        val a: Int,
        @ProtoNumber(5) val inner: TotalKnownData,
        val unknown: ProtoUnknownFieldHolder
    )

    @Test
    fun testDecodeNestedUnknownData() {
        val data = BuildData(
            42,
            "42",
            byteArrayOf(42, 42, 42),
            listOf(42, 42, 42),
            InnerData("42", 42, listOf("42", "42", "42"))
        )

        val encoded = ProtoBuf.encodeToByteArray(BuildData.serializer(), data)
        val decoded = ProtoBuf.decodeFromByteArray(NestedUnknownData.serializer(), encoded)
        assertEquals(42, decoded.a)
        assertTrue(decoded.unknown.fields.isNotEmpty())

        val reEncoded = ProtoBuf.encodeToByteArray(NestedUnknownData.serializer(), decoded)
        val restored = ProtoBuf.decodeFromByteArray(BuildData.serializer(), reEncoded)
        assertEquals(data, restored)
    }

    object CustomSerializer : KSerializer<DataWithUnknownFields> {
        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor("CustomData") {
                element<Int>("a", annotations = listOf(ProtoNumber(1)))
                element<ProtoUnknownFieldHolder>("unknownFields")
            }

        override fun deserialize(decoder: Decoder): DataWithUnknownFields {
            var a = 0
            var unknownFields = ProtoUnknownFieldHolder.Empty
            decoder.decodeStructure(descriptor) {
                loop@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break@loop
                        0 -> a = decodeIntElement(descriptor, index)
                        1 -> unknownFields += decodeSerializableElement(descriptor, index, ProtoUnknownFieldHolder.serializer())
                        else -> error("Unexpected index: $index")
                    }
                }
            }
            return DataWithUnknownFields(a, unknownFields)
        }

        override fun serialize(encoder: Encoder, value: DataWithUnknownFields) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.a)
                encodeSerializableElement(descriptor, 1, ProtoUnknownFieldHolder.serializer(), value.unknownFields)
            }
        }
    }

    @Test
    fun testCustomSerializer() {
        val data = BuildData(
            42,
            "42",
            byteArrayOf(42, 42, 42),
            listOf(42, 42, 42),
            InnerData("42", 42, listOf("42", "42", "42"))
        )

        val encoded = ProtoBuf.encodeToByteArray(BuildData.serializer(), data)
        val decoded = ProtoBuf.decodeFromByteArray(CustomSerializer, encoded)

        assertEquals(data.a, decoded.a)
        assertTrue(decoded.unknownFields.fields.isNotEmpty())

        val reEncoded = ProtoBuf.encodeToByteArray(CustomSerializer, decoded)
        assertContentEquals(encoded, reEncoded)
        val restored = ProtoBuf.decodeFromByteArray(BuildData.serializer(), reEncoded)
        assertEquals(data, restored)
    }

    @Serializable
    data class DataWithWrongTypeUnknownFields(
        val a: Int,
        val unknownFields: Map<Int, ByteArray>,
    )

    @Serializable
    data class DataWithNullableUnknownFields(
        @ProtoNumber(1) val a: Int,
        val unknownFields: ProtoUnknownFieldHolder? = null
    )

    @Test
    fun testDataWithNullableUnknownFields() {
        val data = BuildData(42, "42", byteArrayOf(42), listOf(42), InnerData("42", 42, listOf("42")))
        val encoded = ProtoBuf.encodeToByteArray(BuildData.serializer(), data)
        val decoded = ProtoBuf.decodeFromByteArray(DataWithNullableUnknownFields.serializer(), encoded)
        assertEquals(42, decoded.a)
        assertTrue(decoded.unknownFields!!.fields.isNotEmpty())

        // When there are no unknown fields, the holder should be null
        @Serializable
        data class OnlyA(@ProtoNumber(1) val a: Int)
        val encodedKnownOnly = ProtoBuf.encodeToByteArray(OnlyA.serializer(), OnlyA(42))
        val decoded2 = ProtoBuf.decodeFromByteArray(DataWithNullableUnknownFields.serializer(), encodedKnownOnly)
        assertEquals(42, decoded2.a)
        assertNull(decoded2.unknownFields)
    }

    @Serializable
    data class ToBuildOneOf(val a: String? = null, val b: Long? = null, val c: InnerData? = null)

    @Serializable
    data class TestFewerOneOf(
        @ProtoOneOf val oneOf: OneOf? = null,
        val unknownFields: ProtoUnknownFieldHolder = ProtoUnknownFieldHolder.Empty
    )

    @Serializable
    sealed interface OneOf {
        @Serializable
        data class A(
            @ProtoNumber(1) val a: String
        ) : OneOf

        @Serializable
        data class B(
            @ProtoNumber(2) val b: Long
        ) : OneOf
    }

    @Test
    fun testUnknownOneOfField() {
        val present = ToBuildOneOf(a = "test")
        val encoded = ProtoBuf.encodeToHexString(present)
        val decoded = ProtoBuf.decodeFromHexString(TestFewerOneOf.serializer(), encoded)
        assertEquals(OneOf.A("test"), decoded.oneOf)
        assertEquals(0, decoded.unknownFields.size)

        val absent = ToBuildOneOf(c = InnerData("test", 42, listOf("test")))
        val encoded2 = ProtoBuf.encodeToHexString(absent)
        val decoded2 = ProtoBuf.decodeFromHexString(TestFewerOneOf.serializer(), encoded2)
        assertNull(decoded2.oneOf)
        assertTrue(decoded2.unknownFields.fields.isNotEmpty())
    }

    @Serializable
    data class DataWithLargeNumbers(
        @ProtoNumber(1) val smallNumber: Int,
        @ProtoNumber(536870911) val hugeFieldNumber: Int  // Max field number (2^29 - 1)
    )

    @Serializable
    data class DataWithUnknownLargeNumbers(
        @ProtoNumber(1) val smallNumber: Int,
        val unknownFields: ProtoUnknownFieldHolder = ProtoUnknownFieldHolder.Empty
    )

    private fun assertRoundTripWithLargeNumbers(smallNumber: Int, hugeFieldNumber: Int) {
        val data = DataWithLargeNumbers(smallNumber, hugeFieldNumber)
        val encoded = ProtoBuf.encodeToHexString(DataWithLargeNumbers.serializer(), data)
        val decoded = ProtoBuf.decodeFromHexString(DataWithUnknownLargeNumbers.serializer(), encoded)

        assertEquals(smallNumber, decoded.smallNumber)
        assertTrue(decoded.unknownFields.fields.isNotEmpty())

        val reEncoded = ProtoBuf.encodeToHexString(DataWithUnknownLargeNumbers.serializer(), decoded)
        val finalData = ProtoBuf.decodeFromHexString(DataWithLargeNumbers.serializer(), reEncoded)

        assertEquals(data.smallNumber, finalData.smallNumber)
        assertEquals(data.hugeFieldNumber, finalData.hugeFieldNumber)
    }

    @Test
    fun testUnknownFieldsWithNumbersGreaterThan127() {
        // 1-byte varint boundary
        assertRoundTripWithLargeNumbers(smallNumber = 1, hugeFieldNumber = 127)

        // 2-byte varint: min and max
        assertRoundTripWithLargeNumbers(smallNumber = 2, hugeFieldNumber = 128)
        assertRoundTripWithLargeNumbers(smallNumber = 3, hugeFieldNumber = 16383)

        // 3-byte varint: min and max
        assertRoundTripWithLargeNumbers(smallNumber = 4, hugeFieldNumber = 16384)
        assertRoundTripWithLargeNumbers(smallNumber = 5, hugeFieldNumber = 2097151)

        // Larger values
        assertRoundTripWithLargeNumbers(smallNumber = 6, hugeFieldNumber = Int.MAX_VALUE)

        // Negative value (encoded as 10-byte varint in protobuf)
        assertRoundTripWithLargeNumbers(smallNumber = 7, hugeFieldNumber = -1)
    }

    // --- Tests for all wire types with extreme values ---

    @Serializable
    data class OnlyKnownId(@ProtoNumber(1) val id: Int = 0, val unknown: ProtoUnknownFieldHolder = ProtoUnknownFieldHolder.Empty)

    /**
     * Helper: encode [full] with its serializer, decode as [OnlyKnownId] (field 1 known, rest unknown),
     * re-encode, then decode back as [T] and assert equality.
     */
    private inline fun <reified T> assertUnknownFieldRoundTrip(full: T, serializer: KSerializer<T>) {
        val encoded = ProtoBuf.encodeToByteArray(serializer, full)
        val partial = ProtoBuf.decodeFromByteArray(OnlyKnownId.serializer(), encoded)
        val reEncoded = ProtoBuf.encodeToByteArray(OnlyKnownId.serializer(), partial)
        val restored = ProtoBuf.decodeFromByteArray(serializer, reEncoded)
        assertEquals(full, restored)
    }

    // -- VARINT wire type --

    @Serializable
    data class VarintIntData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: Int)

    @Test
    fun testUnknownVarintInt() {
        assertUnknownFieldRoundTrip(VarintIntData(1, 0), VarintIntData.serializer())
        assertUnknownFieldRoundTrip(VarintIntData(1, -1), VarintIntData.serializer())
        assertUnknownFieldRoundTrip(VarintIntData(1, Int.MIN_VALUE), VarintIntData.serializer())
        assertUnknownFieldRoundTrip(VarintIntData(1, Int.MAX_VALUE), VarintIntData.serializer())
        assertUnknownFieldRoundTrip(VarintIntData(1, 127), VarintIntData.serializer())
        assertUnknownFieldRoundTrip(VarintIntData(1, 128), VarintIntData.serializer())
    }

    @Serializable
    data class VarintLongData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: Long)

    @Test
    fun testUnknownVarintLong() {
        assertUnknownFieldRoundTrip(VarintLongData(1, 0L), VarintLongData.serializer())
        assertUnknownFieldRoundTrip(VarintLongData(1, -1L), VarintLongData.serializer())
        assertUnknownFieldRoundTrip(VarintLongData(1, Long.MIN_VALUE), VarintLongData.serializer())
        assertUnknownFieldRoundTrip(VarintLongData(1, Long.MAX_VALUE), VarintLongData.serializer())
    }

    @Serializable
    data class VarintBoolData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: Boolean)

    @Test
    fun testUnknownVarintBool() {
        assertUnknownFieldRoundTrip(VarintBoolData(1, false), VarintBoolData.serializer())
        assertUnknownFieldRoundTrip(VarintBoolData(1, true), VarintBoolData.serializer())
    }

    // -- i32 wire type (fixed32) --

    @Serializable
    data class Fixed32Data(
        @ProtoNumber(1) val id: Int,
        @ProtoNumber(2) @ProtoType(ProtoIntegerType.FIXED) val value: Int
    )

    @Test
    fun testUnknownFixed32() {
        assertUnknownFieldRoundTrip(Fixed32Data(1, 0), Fixed32Data.serializer())
        assertUnknownFieldRoundTrip(Fixed32Data(1, -1), Fixed32Data.serializer())
        assertUnknownFieldRoundTrip(Fixed32Data(1, Int.MIN_VALUE), Fixed32Data.serializer())
        assertUnknownFieldRoundTrip(Fixed32Data(1, Int.MAX_VALUE), Fixed32Data.serializer())
    }

    @Serializable
    data class FloatData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: Float)

    @Test
    fun testUnknownFloat() {
        assertUnknownFieldRoundTrip(FloatData(1, 0.0f), FloatData.serializer())
        assertUnknownFieldRoundTrip(FloatData(1, -1.0f), FloatData.serializer())
        assertUnknownFieldRoundTrip(FloatData(1, Float.MIN_VALUE), FloatData.serializer())
        assertUnknownFieldRoundTrip(FloatData(1, Float.MAX_VALUE), FloatData.serializer())
        assertUnknownFieldRoundTrip(FloatData(1, Float.NaN), FloatData.serializer())
        assertUnknownFieldRoundTrip(FloatData(1, Float.POSITIVE_INFINITY), FloatData.serializer())
        assertUnknownFieldRoundTrip(FloatData(1, Float.NEGATIVE_INFINITY), FloatData.serializer())
    }

    // -- i64 wire type (fixed64) --

    @Serializable
    data class Fixed64Data(
        @ProtoNumber(1) val id: Int,
        @ProtoNumber(2) @ProtoType(ProtoIntegerType.FIXED) val value: Long
    )

    @Test
    fun testUnknownFixed64() {
        assertUnknownFieldRoundTrip(Fixed64Data(1, 0L), Fixed64Data.serializer())
        assertUnknownFieldRoundTrip(Fixed64Data(1, -1L), Fixed64Data.serializer())
        assertUnknownFieldRoundTrip(Fixed64Data(1, Long.MIN_VALUE), Fixed64Data.serializer())
        assertUnknownFieldRoundTrip(Fixed64Data(1, Long.MAX_VALUE), Fixed64Data.serializer())
    }

    @Serializable
    data class DoubleData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: Double)

    @Test
    fun testUnknownDouble() {
        assertUnknownFieldRoundTrip(DoubleData(1, 0.0), DoubleData.serializer())
        assertUnknownFieldRoundTrip(DoubleData(1, -1.0), DoubleData.serializer())
        assertUnknownFieldRoundTrip(DoubleData(1, Double.MIN_VALUE), DoubleData.serializer())
        assertUnknownFieldRoundTrip(DoubleData(1, Double.MAX_VALUE), DoubleData.serializer())
        assertUnknownFieldRoundTrip(DoubleData(1, Double.NaN), DoubleData.serializer())
        assertUnknownFieldRoundTrip(DoubleData(1, Double.POSITIVE_INFINITY), DoubleData.serializer())
        assertUnknownFieldRoundTrip(DoubleData(1, Double.NEGATIVE_INFINITY), DoubleData.serializer())
    }

    // -- SIZE_DELIMITED wire type --

    @Serializable
    data class StringData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: String)

    @Test
    fun testUnknownString() {
        assertUnknownFieldRoundTrip(StringData(1, ""), StringData.serializer())
        assertUnknownFieldRoundTrip(StringData(1, "hello"), StringData.serializer())
        assertUnknownFieldRoundTrip(StringData(1, "a".repeat(70_000)), StringData.serializer()) // > 65kB
    }

    @Serializable
    data class ByteArrayData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as ByteArrayData
            return id == other.id && value.contentEquals(other.value)
        }
        override fun hashCode(): Int = 31 * id + value.contentHashCode()
    }

    @Test
    fun testUnknownByteArray() {
        assertUnknownFieldRoundTrip(ByteArrayData(1, ByteArray(0)), ByteArrayData.serializer())
        assertUnknownFieldRoundTrip(ByteArrayData(1, byteArrayOf(0, 1, 127, -128, -1)), ByteArrayData.serializer())
        assertUnknownFieldRoundTrip(ByteArrayData(1, ByteArray(70_000) { it.toByte() }), ByteArrayData.serializer()) // > 65kB
    }

    @Serializable
    data class EmbeddedMessageData(@ProtoNumber(1) val id: Int, @ProtoNumber(2) val value: InnerData)

    @Test
    fun testUnknownEmbeddedMessage() {
        // Empty-ish embedded message
        assertUnknownFieldRoundTrip(
            EmbeddedMessageData(1, InnerData("", 0, emptyList())),
            EmbeddedMessageData.serializer()
        )
        // Normal embedded message
        assertUnknownFieldRoundTrip(
            EmbeddedMessageData(1, InnerData("test", 42, listOf("a", "b", "c"))),
            EmbeddedMessageData.serializer()
        )
    }
}