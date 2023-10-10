/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.conformance

import com.google.protobuf_test_messages.proto3.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.*

@Serializable
data class KTestMessagesProto3Message(
    @ProtoNumber(18) val optionalNestedMessage: KNestedMessage? = null,
    @ProtoNumber(19) val optionalForeignMessage: KForeignMessage? = null,
) {
    @Serializable
    data class KNestedMessage(
        @ProtoNumber(1) val a: Int = 0,
        @ProtoNumber(2) val corecursive: KTestMessagesProto3Message? = null,
    ) {
        fun toProto(): TestMessagesProto3.TestAllTypesProto3.NestedMessage =
            TestMessagesProto3.TestAllTypesProto3.NestedMessage.parseFrom(
                ProtoBuf.encodeToByteArray(this)
            )
    }
}

@Serializable
data class KForeignMessage(
    @ProtoNumber(1) val c: Int = 0,
) {
    fun toProto(): TestMessagesProto3.ForeignMessage =
        TestMessagesProto3.ForeignMessage.parseFrom(
            ProtoBuf.encodeToByteArray(this)
        )
}

class Proto3MessageTest {
    @Test
    fun default() {
        val message = KTestMessagesProto3Message(
            optionalNestedMessage = KTestMessagesProto3Message.KNestedMessage(
                a = 150,
                corecursive = KTestMessagesProto3Message(
                    optionalNestedMessage = KTestMessagesProto3Message.KNestedMessage(
                        a = 42,
                    )
                )
            ),
            optionalForeignMessage = KForeignMessage(
                c = 150,
            )
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)
        assertEquals(message.optionalNestedMessage?.a, restored.optionalNestedMessage.a)
        assertEquals(
            message.optionalNestedMessage?.corecursive?.optionalNestedMessage?.a,
            restored.optionalNestedMessage.corecursive.optionalNestedMessage.a
        )
        assertEquals(message.optionalForeignMessage?.c, restored.optionalForeignMessage.c)
    }
}
