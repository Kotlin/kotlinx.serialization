/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf


import kotlinx.serialization.*
import kotlin.test.*

class InvalidFieldNumberTest {

    @Serializable
    data class Holder(val value: Int)

    @Serializable
    data class ListHolder(val value: List<Int>)

    @Serializable
    data class ZeroProtoNumber(@ProtoNumber(0) val value: Int)

    @Serializable
    data class NegativeProtoNumber(@ProtoNumber(-5) val value: Int)

    @Test
    fun testDeserializeZeroInput() {
        assertFailsWithMessage<SerializationException>("0 is not allowed as the protobuf field number in kotlinx.serialization.protobuf.InvalidFieldNumberTest.Holder, the input bytes may have been corrupted") {
            // first value with field number = 0
            val hexString = "000f"
            ProtoBuf.decodeFromHexString<Holder>(hexString)
        }
    }

    @Test
    fun testDeserializeZeroInputForElement() {
        assertFailsWithMessage<SerializationException>("0 is not allowed as the protobuf field number in kotlinx.serialization.protobuf.InvalidFieldNumberTest.ListHolder, the input bytes may have been corrupted") {
            // first element with field number = 0
            val hexString = "000f"
            ProtoBuf.decodeFromHexString<ListHolder>(hexString)
        }
    }

    @Test
    fun testSerializeZeroProtoNumber() {
        assertFailsWithMessage<SerializationException>("0 is not allowed in ProtoNumber for kotlinx.serialization.protobuf.InvalidFieldNumberTest.ZeroProtoNumber, because protobuf support field values in range 1..2147483647") {
            ProtoBuf.encodeToHexString(ZeroProtoNumber(42))
        }
    }

    @Test
    fun testDeserializeZeroProtoNumber() {
        assertFailsWithMessage<SerializationException>("0 is not allowed in ProtoNumber for kotlinx.serialization.protobuf.InvalidFieldNumberTest.ZeroProtoNumber, because protobuf support field values in range 1..2147483647") {
            ProtoBuf.decodeFromHexString<ZeroProtoNumber>("000f")
        }
    }

    @Test
    fun testSerializeNegativeProtoNumber() {
        assertFailsWithMessage<SerializationException>("-5 is not allowed in ProtoNumber for kotlinx.serialization.protobuf.InvalidFieldNumberTest.NegativeProtoNumber, because protobuf support field values in range 1..2147483647") {
            ProtoBuf.encodeToHexString(NegativeProtoNumber(42))
        }
    }

    @Test
    fun testDeserializeNegativeProtoNumber() {
        assertFailsWithMessage<SerializationException>("-5 is not allowed in ProtoNumber for kotlinx.serialization.protobuf.InvalidFieldNumberTest.NegativeProtoNumber, because protobuf support field values in range 1..2147483647") {
            ProtoBuf.decodeFromHexString<NegativeProtoNumber>("000f")
        }
    }
}