/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.conformance

import com.google.protobuf_test_messages.proto3.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.*

@Serializable
data class KTestMessagesProto3Enum(
    @ProtoNumber(21) val optionalNestedEnum: KNestedEnum = KNestedEnum.FOO,
    @ProtoNumber(22) val optionalForeignEnum: KForeignEnum = KForeignEnum.FOREIGN_FOO,
    @ProtoNumber(23) val optionalAliasedEnum: KAliasedEnum = KAliasedEnum.ALIAS_FOO,
) {
    enum class KNestedEnum {
        @ProtoNumber(0)
        FOO,

        @ProtoNumber(1)
        BAR,

        @ProtoNumber(2)
        BAZ,

        @ProtoNumber(-1)
        NEG;

        fun toProto() = TestMessagesProto3.TestAllTypesProto3.NestedEnum.valueOf(this.name)
    }


    enum class KAliasedEnum {
        @ProtoNumber(0)
        ALIAS_FOO,

        @ProtoNumber(1)
        ALIAS_BAR,

        @ProtoNumber(2)
        ALIAS_BAZ,

        @ProtoNumber(2)
        MOO,

        @ProtoNumber(2)
        moo,

        @ProtoNumber(2)
        bAz;

        fun toProto() = TestMessagesProto3.TestAllTypesProto3.AliasedEnum.valueOf(this.name)
    }
}

enum class KForeignEnum {
    @ProtoNumber(0)
    FOREIGN_FOO,

    @ProtoNumber(1)
    FOREIGN_BAR,

    @ProtoNumber(2)
    FOREIGN_BAZ;

    fun toProto() = TestMessagesProto3.ForeignEnum.valueOf(this.name)
}

class Proto3EnumTest {
    @Test
    fun default() {
        val message = KTestMessagesProto3Enum(
            optionalNestedEnum = KTestMessagesProto3Enum.KNestedEnum.NEG,
            optionalForeignEnum = KForeignEnum.FOREIGN_BAR,
            optionalAliasedEnum = KTestMessagesProto3Enum.KAliasedEnum.ALIAS_BAR
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.optionalNestedEnum.toProto(), restored.optionalNestedEnum)
        assertEquals(message.optionalForeignEnum.toProto(), restored.optionalForeignEnum)
        assertEquals(message.optionalAliasedEnum.toProto(), restored.optionalAliasedEnum)

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Enum>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }
}
