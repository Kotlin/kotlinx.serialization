/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.conformance

import com.google.protobuf_test_messages.proto3.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.*

@Serializable
data class KTestMessagesProto3Primitive(
    @ProtoNumber(1) val optionalInt32: Int = 0,
    @ProtoNumber(2) val optionalInt64: Long = 0,
    @ProtoNumber(3) val optionalUint32: UInt = 0U,
    @ProtoNumber(4) val optionalUint64: ULong = 0UL,
    @ProtoNumber(5) @ProtoType(ProtoIntegerType.SIGNED) val optionalSint32: Int = 0,
    @ProtoNumber(6) @ProtoType(ProtoIntegerType.SIGNED) val optionalSint64: Long = 0,
    @ProtoNumber(7) @ProtoType(ProtoIntegerType.FIXED) val optionalFixed32: Int = 0,
    @ProtoNumber(8) @ProtoType(ProtoIntegerType.FIXED) val optionalFixed64: Long = 0,
    @ProtoNumber(9) @ProtoType(ProtoIntegerType.FIXED) val optionalSfixed32: Int = 0,
    @ProtoNumber(10) @ProtoType(ProtoIntegerType.FIXED) val optionalSfixed64: Long = 0,
    @ProtoNumber(11) val optionalFloat: Float = 0.0f,
    @ProtoNumber(12) val optionalDouble: Double = 0.0,
    @ProtoNumber(13) val optionalBool: Boolean = false,
    @ProtoNumber(14) val optionalString: String = "",
    @ProtoNumber(15) val optionalBytes: ByteArray = byteArrayOf(),
)

class Proto3PrimitiveTest {
    @Test
    fun default() {
        val message = KTestMessagesProto3Primitive(
            optionalInt32 = Int.MAX_VALUE,
            optionalInt64 = Long.MAX_VALUE,
            optionalUint32 = UInt.MAX_VALUE,
            optionalUint64 = ULong.MAX_VALUE,
            optionalSint32 = Int.MAX_VALUE,
            optionalSint64 = Long.MAX_VALUE,
            optionalFixed32 = Int.MAX_VALUE,
            optionalFixed64 = Long.MAX_VALUE,
            optionalSfixed32 = Int.MAX_VALUE,
            optionalSfixed64 = Long.MAX_VALUE,
            optionalFloat = Float.MAX_VALUE,
            optionalDouble = Double.MAX_VALUE,
            optionalBool = true,
            optionalString = "string",
            optionalBytes = byteArrayOf(1, 2, 3, 4, 5)
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.optionalInt32, restored.optionalInt32)
        assertEquals(message.optionalInt64, restored.optionalInt64)
        assertEquals(message.optionalUint32, restored.optionalUint32.toUInt())
        assertEquals(message.optionalUint64, restored.optionalUint64.toULong())
        assertEquals(message.optionalSint32, restored.optionalSint32)
        assertEquals(message.optionalSint64, restored.optionalSint64)
        assertEquals(message.optionalFixed32, restored.optionalFixed32)
        assertEquals(message.optionalFixed64, restored.optionalFixed64)
        assertEquals(message.optionalSfixed32, restored.optionalSfixed32)
        assertEquals(message.optionalSfixed64, restored.optionalSfixed64)
        assertEquals(message.optionalFloat, restored.optionalFloat)
        assertEquals(message.optionalDouble, restored.optionalDouble)
        assertEquals(message.optionalBool, restored.optionalBool)
        assertEquals(message.optionalString, restored.optionalString)
        assertContentEquals(message.optionalBytes, restored.optionalBytes.toByteArray())

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Primitive>(restored.toByteArray())

        // [equals] method is not implemented for [ByteArray] so we need to compare it separately.
        assertEquals(message, restoredMessage.copy(optionalBytes = message.optionalBytes))
        assertContentEquals(message.optionalBytes, restoredMessage.optionalBytes)
    }
}
