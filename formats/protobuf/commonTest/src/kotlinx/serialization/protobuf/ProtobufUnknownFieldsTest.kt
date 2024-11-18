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
        @ProtoUnknownFields val unknownFields: ProtoMessage
    )

    @Test
    fun testDecodeWithUnknownField() {
        val data = BuildData(42, "42", byteArrayOf(42, 42, 42), listOf(42, 42, 42), InnerData("42", 42, listOf("42", "42", "42")))

        /**
         * 1: 42
         * 2: {"42"}
         * 3: {"***"}
         * 4: 42
         * 4: 42
         * 4: 42
         * 5: {
         *   1: {"42"}
         *   2: 42
         *   3: {"42"}
         *   3: {"42"}
         *   3: {"42"}
         * }
         */
        val encoded = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(DataWithUnknownFields.serializer(), encoded)
        assertEquals(data.a, decoded.a)
        assertEquals(6, decoded.unknownFields.size)

        val encoded2 = ProtoBuf.encodeToHexString(DataWithUnknownFields.serializer(), decoded)
        assertEquals(encoded, encoded2)
        val data2 = ProtoBuf.decodeFromHexString(BuildData.serializer(), encoded2)
        assertEquals(data, data2)
    }

    @Test
    fun testCannotDecodeArbitraryMessage() {
        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.decodeFromHexString(ProtoMessage.serializer(), "")
        }
    }

    @Test
    fun testCannotEncodeArbitraryMessage() {
        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.encodeToHexString(ProtoMessage.serializer(), ProtoMessage.Empty)
        }
    }

    @Serializable
    data class DataWithMultipleUnknownFields(
        val a: Int,
        @ProtoUnknownFields val unknownFields: ProtoMessage,
        @ProtoUnknownFields val unknownFields2: ProtoMessage
    )

    @Test
    fun testOnlyOneUnknownFieldAllowed() {
        val encoded = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.decodeFromHexString(DataWithMultipleUnknownFields.serializer(), encoded)
        }
    }

    @Serializable
    data class DataWithStaggeredFields(
        @ProtoNumber(2)
        val b: String,
        @ProtoUnknownFields val unknownFields: ProtoMessage,
        @ProtoNumber(4)
        val d: List<Int>
    )

    @Test
    fun testUnknownFieldBeforeKnownField() {
        val data = BuildData(42, "42", byteArrayOf(42, 42, 42), listOf(42, 42, 42), InnerData("42", 42, listOf("42", "42", "42")))

        /**
         * 1: 42
         * 2: {"42"}
         * 3: {"***"}
         * 4: 42
         * 4: 42
         * 4: 42
         * 5: {
         *   1: {"42"}
         *   2: 42
         *   3: {"42"}
         *   3: {"42"}
         *   3: {"42"}
         * }
         * }
         */
        val hex = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(DataWithStaggeredFields.serializer(), hex)
        assertEquals(3, decoded.unknownFields.size)
        assertEquals("42", decoded.b)
        assertEquals(listOf(42, 42, 42), decoded.d)

        val encoded = ProtoBuf.encodeToHexString(DataWithStaggeredFields.serializer(), decoded)
        /**
         * fields are re-ordered but acceptable in protobuf wire data
         *
         * 2: {"42"}
         * 1: 42
         * 3: {"***"}
         * 5: {
         *   1: {"42"}
         *   2: 42
         *   3: {"42"}
         *   3: {"42"}
         *   3: {"42"}
         * }
         * 4: 42
         * 4: 42
         * 4: 42
         */
        assertEquals("12023432082a1a032a2a2a2a120a023432102a1a0234321a0234321a023432202a202a202a", encoded)
        val decodeOrigin = ProtoBuf.decodeFromHexString(BuildData.serializer(), encoded)
        assertEquals(data, decodeOrigin)
    }

    @Serializable
    data class TotalKnownData(@ProtoUnknownFields val fields: ProtoMessage = ProtoMessage.Empty)

    @Serializable
    data class NestedUnknownData(val a: Int, @ProtoNumber(5) val inner: TotalKnownData, @ProtoUnknownFields val unknown: ProtoMessage)

    @Test
    fun testDecodeNestedUnknownData() {
        /**
         * 1: 42
         * 2: {"42"}
         * 3: {"***"}
         * 4: 42
         * 4: 42
         * 4: 42
         * 5: {
         *   1: {"42"}
         *   2: 42
         *   3: {"42"}
         *   3: {"42"}
         *   3: {"42"}
         * }
         */
        val hex = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(NestedUnknownData.serializer(), hex)
        assertEquals(5, decoded.unknown.size)
    }

    object CustomSerializer: KSerializer<DataWithUnknownFields> {
        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor("CustomData") {
                element<Int>("a", annotations = listOf(ProtoNumber(1)))
                element<ProtoMessage>("unknownFields", annotations = listOf(ProtoUnknownFields()))
            }

        override fun deserialize(decoder: Decoder): DataWithUnknownFields {
            var a = 0
            var unknownFields = ProtoMessage.Empty
            decoder.decodeStructure(descriptor) {
                loop@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break@loop
                        0 -> a = decodeIntElement(descriptor, index)
                        1 -> unknownFields += decodeSerializableElement(descriptor, index, ProtoMessage.serializer())
                        else -> error("Unexpected index: $index")
                    }
                }
            }
            return DataWithUnknownFields(a, unknownFields)
        }

        override fun serialize(encoder: Encoder, value: DataWithUnknownFields) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.a)
                encodeSerializableElement(descriptor, 1, ProtoMessage.serializer(), value.unknownFields)
            }
        }
    }

    @Test
    fun testCustomSerializer() {
        val data = BuildData(42, "42", byteArrayOf(42, 42, 42), listOf(42, 42, 42), InnerData("42", 42, listOf("42", "42", "42")))

        /**
         * 1: 42
         * 2: {"42"}
         * 3: {"***"}
         * 4: 42
         * 4: 42
         * 4: 42
         * 5: {
         *   1: {"42"}
         *   2: 42
         *   3: {"42"}
         *   3: {"42"}
         *   3: {"42"}
         * }
         */
        val encoded = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(CustomSerializer, encoded)

        assertEquals(data.a, decoded.a)
        assertEquals(6, decoded.unknownFields.size)

        val encoded2 = ProtoBuf.encodeToHexString(CustomSerializer, decoded)
        assertEquals(encoded, encoded2)
        val data2 = ProtoBuf.decodeFromHexString(BuildData.serializer(), encoded2)
        assertEquals(data, data2)
    }

    @Serializable
    data class DataWithWrongTypeUnknownFields(
        val a: Int,
        @ProtoUnknownFields val unknownFields: Map<Int, ByteArray>,
    )

    @Test
    fun testCannotDecodeWrongTypeUnknownFields() {
        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.decodeFromHexString(DataWithWrongTypeUnknownFields.serializer(), "")
        }
    }

    @Serializable
    data class DataWithMissingUnknownFields(
        val a: Int,
        val unknownFields: ProtoMessage = ProtoMessage.Empty
    )

    @Test
    fun testCannotEncodeMissingAnnotationUnknownFields() {
        val encoded = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(DataWithMissingUnknownFields.serializer(), encoded)
        assertFailsWith<IllegalArgumentException> {
            ProtoBuf.encodeToHexString(DataWithMissingUnknownFields.serializer(), decoded)
        }
    }

    @Serializable
    data class DataWithNullableUnknownFields(
        @ProtoNumber(1) val a: Int,
        @ProtoUnknownFields val unknownFields: ProtoMessage? = null
    )
    @Test
    fun testDataWithNullableUnknownFields() {
        val encoded = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(DataWithNullableUnknownFields.serializer(), encoded)
        assertEquals(42, decoded.a)
        assertEquals(6, decoded.unknownFields?.size)

        val encoded2 = "082a"
        val decoded2 = ProtoBuf.decodeFromHexString(DataWithNullableUnknownFields.serializer(), encoded2)
        assertEquals(42, decoded2.a)
        assertNull(decoded2.unknownFields)
    }
}