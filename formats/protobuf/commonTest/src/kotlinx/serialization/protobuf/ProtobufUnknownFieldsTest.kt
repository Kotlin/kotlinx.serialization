/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
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
        val hex = "082a120234321a032a2a2a202a202a202a2a120a023432102a1a0234321a0234321a023432"
        val decoded = ProtoBuf.decodeFromHexString(DataWithStaggeredFields.serializer(), hex)
        assertEquals(3, decoded.unknownFields.size)
        assertEquals("42", decoded.b)
        assertEquals(listOf(42, 42, 42), decoded.d)

        val encoded = ProtoBuf.encodeToHexString(DataWithStaggeredFields.serializer(), decoded)
        val decodeOrigin = ProtoBuf.decodeFromHexString(BuildData.serializer(), encoded)
        assertEquals(data, decodeOrigin)
    }
}