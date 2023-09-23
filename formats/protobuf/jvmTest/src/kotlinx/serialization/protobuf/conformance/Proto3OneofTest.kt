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

    /**
     * Verify that the given [KTestMessageProto3Oneof] is correctly encoded and decoded as
     * [TestMessagesProto3.TestAllTypesProto3] by running the [verificationFunction]. This
     * method also verifies that the encoded and decoded message is equal to the original message.
     *
     * @param verificationFunction a function that verifies the encoded and decoded message. First parameter
     * is the original message and the second parameter is the decoded protobuf library message.
     * @receiver the [KTestMessageProto3Oneof] to verify
     */
    private fun KTestMessageProto3Oneof.verify(
        verificationFunction: (KTestMessageProto3Oneof, TestMessagesProto3.TestAllTypesProto3) -> Unit,
    ) {
        val bytes = ProtoBuf.encodeToByteArray(this)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        verificationFunction.invoke(this, restored)

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessageProto3Oneof>(restored.toByteArray())

        // [equals] method is not implemented for [ByteArray] so we need to compare it separately.
        assertEquals(this, restoredMessage.copy(oneofBytes = this.oneofBytes))
        assertContentEquals(this.oneofBytes, restoredMessage.oneofBytes)
    }

    @Test
    fun uint32() {
        KTestMessageProto3Oneof(oneofUint32 = 150u).verify { self, restored ->
            assertEquals(self.oneofUint32, restored.oneofUint32.toUInt())
        }
    }

    @Test
    fun nestedMessage() {
        KTestMessageProto3Oneof(
            oneofNestedMessage = KTestMessagesProto3Message.KNestedMessage(a = 150),
        ).verify { self, restored ->
            assertEquals(self.oneofNestedMessage?.a, restored.oneofNestedMessage.a)
        }
    }

    @Test
    fun string() {
        KTestMessageProto3Oneof(oneofString = "150").verify { self, restored ->
            assertEquals(self.oneofString, restored.oneofString)
        }
    }

    @Test
    fun bytes() {
        KTestMessageProto3Oneof(oneofBytes = "150".toByteArray()).verify { self, restored ->
            assertContentEquals(self.oneofBytes, restored.oneofBytes.toByteArray())
        }
    }

    @Test
    fun bool() {
        KTestMessageProto3Oneof(oneofBool = true).verify { self, restored ->
            assertEquals(self.oneofBool, restored.oneofBool)
        }
    }

    @Test
    fun uint64() {
        KTestMessageProto3Oneof(oneofUint64 = 150uL).verify { self, restored ->
            assertEquals(self.oneofUint64, restored.oneofUint64.toULong())
        }
    }

    @Test
    fun float() {
        KTestMessageProto3Oneof(oneofFloat = 150f).verify { self, restored ->
            assertEquals(self.oneofFloat, restored.oneofFloat)
        }
    }

    @Test
    fun double() {
        KTestMessageProto3Oneof(oneofDouble = 150.0).verify { self, restored ->
            assertEquals(self.oneofDouble, restored.oneofDouble)
        }
    }

    @Test
    fun enum() {
        KTestMessageProto3Oneof(oneofEnum = KTestMessagesProto3Enum.KNestedEnum.BAR).verify { self, restored ->
            assertEquals(self.oneofEnum?.name, restored.oneofEnum.name)
        }
    }
}
