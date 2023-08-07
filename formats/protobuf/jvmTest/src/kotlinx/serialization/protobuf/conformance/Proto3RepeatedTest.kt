/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.conformance

import com.google.protobuf_test_messages.proto3.*
import io.kotlintest.properties.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.*

@Serializable
data class KTestMessagesProto3Repeated(
    @ProtoNumber(31) @ProtoPacked val repeatedInt32: List<Int> = emptyList(),
    @ProtoNumber(32) @ProtoPacked val repeatedInt64: List<Long> = emptyList(),
//    @ProtoNumber(33) @ProtoPacked val repeatedUint32: List<UInt> = emptyList(),
//    @ProtoNumber(34) @ProtoPacked val repeatedUint64: List<ULong> = emptyList(),
//    @ProtoNumber(35) @ProtoPacked val repeatedSint32: List<Int> = emptyList(),
//    @ProtoNumber(36) @ProtoPacked val repeatedSint64: List<Long> = emptyList(),
//    @ProtoNumber(37) @ProtoPacked val repeatedFixed32: List<Int> = emptyList(),
//    @ProtoNumber(38) @ProtoPacked val repeatedFixed64: List<Long> = emptyList(),
//    @ProtoNumber(39) @ProtoPacked val repeatedSfixed32: List<Int> = emptyList(),
//    @ProtoNumber(40) @ProtoPacked val repeatedSfixed64: List<Long> = emptyList(),
    @ProtoNumber(41) @ProtoPacked val repeatedFloat: List<Float> = emptyList(),
    @ProtoNumber(42) @ProtoPacked val repeatedDouble: List<Double> = emptyList(),
    @ProtoNumber(43) @ProtoPacked val repeatedBool: List<Boolean> = emptyList(),
    @ProtoNumber(44) val repeatedString: List<String> = emptyList(),
//    @ProtoNumber(45) val repeatedBytes: List<ByteArray> = emptyList(),
    @ProtoNumber(48) val repeatedNestedMessages: List<KTestMessagesProto3Message.KNestedMessage> = emptyList(),
    @ProtoNumber(49) val repeatedForeignMessages: List<KForeignMessage> = emptyList(),
)

class Proto3RepeatedTest {
    @Test
    fun default() {
        val message = KTestMessagesProto3Repeated(
            repeatedInt32 = Gen.list(Gen.int()).generate(),
            repeatedInt64 = Gen.list(Gen.long()).generate(),
//            repeatedUint32 = Gen.list(Gen.int().map { it.toUInt() }).generate(),
//            repeatedUint64 = Gen.list(Gen.long().map { it.toULong() }).generate(),
//            repeatedSint32 = Gen.list(Gen.int()).generate(),
//            repeatedSint64 = Gen.list(Gen.long()).generate(),
//            repeatedFixed32 = Gen.list(Gen.int()).generate(),
//            repeatedFixed64 = Gen.list(Gen.long()).generate(),
//            repeatedSfixed32 = Gen.list(Gen.int()).generate(),
//            repeatedSfixed64 = Gen.list(Gen.long()).generate(),
            repeatedFloat = Gen.list(Gen.float()).generate(),
            repeatedDouble = Gen.list(Gen.double()).generate(),
            repeatedBool = Gen.list(Gen.bool()).generate(),
            repeatedString = Gen.list(Gen.string()).generate(),
//            repeatedBytes = Gen.list(Gen.string().map { it.toByteArray() }).generate(),
            repeatedNestedMessages = listOf(
                KTestMessagesProto3Message.KNestedMessage(
                    1,
                    null
                ),
                KTestMessagesProto3Message.KNestedMessage(
                    2,
                    KTestMessagesProto3Message(
                        KTestMessagesProto3Message.KNestedMessage(3, null),
                    )
                )
            ),
            repeatedForeignMessages = listOf(
                KForeignMessage(1),
                KForeignMessage(-12),
            )
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.repeatedInt32, restored.repeatedInt32List)
        assertEquals(message.repeatedInt64, restored.repeatedInt64List)
//        assertEquals(message.repeatedUint32, restored.repeatedUint32List.map { it.toUInt() })
//        assertEquals(message.repeatedUint64, restored.repeatedUint64List.map { it.toULong() })
//        assertEquals(message.repeatedSint32, restored.repeatedSint32List)
//        assertEquals(message.repeatedSint64, restored.repeatedSint64List)
//        assertEquals(message.repeatedFixed32, restored.repeatedFixed32List)
//        assertEquals(message.repeatedFixed64, restored.repeatedFixed64List)
//        assertEquals(message.repeatedSfixed32, restored.repeatedSfixed32List)
//        assertEquals(message.repeatedSfixed64, restored.repeatedSfixed64List)
        assertEquals(message.repeatedFloat, restored.repeatedFloatList)
        assertEquals(message.repeatedDouble, restored.repeatedDoubleList)
        assertEquals(message.repeatedBool, restored.repeatedBoolList)
        assertEquals(message.repeatedString, restored.repeatedStringList)
//        assertEquals(message.repeatedBytes.map { it.toList() }, restored.repeatedBytesList.map { it.toList() })
        assertEquals(message.repeatedNestedMessages.map { it.toProto() }, restored.repeatedNestedMessageList)
        assertEquals(message.repeatedForeignMessages.map { it.toProto() }, restored.repeatedForeignMessageList)

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Repeated>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }
}
