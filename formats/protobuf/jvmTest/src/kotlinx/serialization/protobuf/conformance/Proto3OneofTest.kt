/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.conformance

import com.google.protobuf_test_messages.proto3.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.*

@Serializable
data class KTestMessageProto3Oneof(
    @ProtoNumber(111) val oneofUint32: UInt? = null,
    @ProtoNumber(112) val oneofNestedMessage: KTestMessagesProto3Message.KNestedMessage? = null,
    @ProtoNumber(113) val oneofString: String? = null,
    @ProtoNumber(114) val oneofBytes: ByteArray? = null,
    @ProtoNumber(115) val oneofBool: Boolean? = null,
    @ProtoNumber(116) val oneofUint64: ULong? = null,
    @ProtoNumber(117) val oneofFloat: Float? = null,
    @ProtoNumber(118) val oneofDouble: Double? = null,
    @ProtoNumber(119) val oneofEnum: KTestMessagesProto3Enum.KNestedEnum? = null,
) {
    init {
        require(
            listOf(
                oneofUint32,
                oneofNestedMessage,
                oneofString,
                oneofBytes,
                oneofBool,
                oneofUint64,
                oneofFloat,
                oneofDouble,
                oneofEnum,
            ).count { it != null } == 1
        )
    }
}

class Proto3OneofTest {
    @Test
    fun default() {
        listOf(
            KTestMessageProto3Oneof(oneofUint32 = 150u),
            KTestMessageProto3Oneof(oneofNestedMessage = KTestMessagesProto3Message.KNestedMessage(a = 150)),
            KTestMessageProto3Oneof(oneofString = "150"),
            KTestMessageProto3Oneof(oneofBytes = "150".toByteArray()),
            KTestMessageProto3Oneof(oneofBool = true),
            KTestMessageProto3Oneof(oneofUint64 = 150uL),
            KTestMessageProto3Oneof(oneofFloat = 150f),
            KTestMessageProto3Oneof(oneofDouble = 150.0),
            KTestMessageProto3Oneof(oneofEnum = KTestMessagesProto3Enum.KNestedEnum.BAR),
        ).forEach { message ->
            val bytes = ProtoBuf.encodeToByteArray(message)
            val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)
            if (message.oneofUint32 != null) assertEquals(message.oneofUint32, restored.oneofUint32.toUInt())
            if (message.oneofNestedMessage != null) assertEquals(
                message.oneofNestedMessage.a,
                restored.oneofNestedMessage?.a
            )
            if (message.oneofString != null) assertEquals(message.oneofString, restored.oneofString)
            if (message.oneofBytes != null) assertContentEquals(message.oneofBytes, restored.oneofBytes.toByteArray())
            if (message.oneofBool != null) assertEquals(message.oneofBool, restored.oneofBool)
            if (message.oneofUint64 != null) assertEquals(message.oneofUint64, restored.oneofUint64.toULong())
            if (message.oneofFloat != null) assertEquals(message.oneofFloat, restored.oneofFloat)
            if (message.oneofDouble != null) assertEquals(message.oneofDouble, restored.oneofDouble)
            if (message.oneofEnum != null) assertEquals(message.oneofEnum.name, restored.oneofEnum?.name)

            val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessageProto3Oneof>(restored.toByteArray())
            assertEquals(message, restoredMessage.copy(oneofBytes = message.oneofBytes))
            assertContentEquals(message.oneofBytes, restoredMessage.oneofBytes)
        }
    }
}
