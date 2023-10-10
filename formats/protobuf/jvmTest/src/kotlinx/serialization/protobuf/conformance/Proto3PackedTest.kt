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
data class KTestMessagesProto3Packed(
    @ProtoNumber(75) @ProtoPacked val packedInt32: List<Int> = emptyList(),
    @ProtoNumber(76) @ProtoPacked val packedInt64: List<Long> = emptyList(),
    @ProtoNumber(77) @ProtoPacked val packedUint32: List<UInt> = emptyList(),
    @ProtoNumber(78) @ProtoPacked val packedUint64: List<ULong> = emptyList(),
    @ProtoNumber(79) @ProtoPacked val packedSint32: List<Int> = emptyList(),
    @ProtoNumber(80) @ProtoPacked val packedSint64: List<Long> = emptyList(),
    @ProtoNumber(81) @ProtoPacked val packedFixed32: List<Int> = emptyList(),
    @ProtoNumber(82) @ProtoPacked val packedFixed64: List<Long> = emptyList(),
    @ProtoNumber(83) @ProtoPacked val packedSfixed32: List<Int> = emptyList(),
    @ProtoNumber(84) @ProtoPacked val packedSfixed64: List<Long> = emptyList(),
    @ProtoNumber(85) @ProtoPacked val packedFloat: List<Float> = emptyList(),
    @ProtoNumber(86) @ProtoPacked val packedDouble: List<Double> = emptyList(),
    @ProtoNumber(87) @ProtoPacked val packedBool: List<Boolean> = emptyList(),
)

class Proto3PackedTest {
    @Test
    fun default() {
        val message = KTestMessagesProto3Packed(
            packedInt32 = Gen.list(Gen.int()).generate(),
            packedInt64 = Gen.list(Gen.long()).generate(),
            packedFloat = Gen.list(Gen.float()).generate(),
            packedDouble = Gen.list(Gen.double()).generate(),
            packedBool = Gen.list(Gen.bool()).generate(),
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.packedInt32, restored.packedInt32List)
        assertEquals(message.packedInt64, restored.packedInt64List)
        assertEquals(message.packedFloat, restored.packedFloatList)
        assertEquals(message.packedDouble, restored.packedDoubleList)
        assertEquals(message.packedBool, restored.packedBoolList)

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Packed>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }

    @Test
    @Ignore
    // Issue: https://github.com/Kotlin/kotlinx.serialization/issues/2419
    fun signedAndFixed() {
        val message = KTestMessagesProto3Packed(
            packedSint32 = Gen.list(Gen.int()).generate(),
            packedSint64 = Gen.list(Gen.long()).generate(),
            packedFixed32 = Gen.list(Gen.int()).generate(),
            packedFixed64 = Gen.list(Gen.long()).generate(),
            packedSfixed32 = Gen.list(Gen.int()).generate(),
            packedSfixed64 = Gen.list(Gen.long()).generate(),
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.packedSint32, restored.packedSint32List)
        assertEquals(message.packedSint64, restored.packedSint64List)
        assertEquals(message.packedFixed32, restored.packedFixed32List)
        assertEquals(message.packedFixed64, restored.packedFixed64List)
        assertEquals(message.packedSfixed32, restored.packedSfixed32List)
        assertEquals(message.packedSfixed64, restored.packedSfixed64List)

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Packed>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }

    @Test
    @Ignore
    // Issue: https://github.com/Kotlin/kotlinx.serialization/issues/2418
    fun unsigned() {
        val message = KTestMessagesProto3Packed(
            packedUint32 = Gen.list(Gen.int().map { it.toUInt() }).generate(),
            packedUint64 = Gen.list(Gen.long().map { it.toULong() }).generate(),
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.packedUint32, restored.packedUint32List.map { it.toUInt() })
        assertEquals(message.packedUint64, restored.packedUint64List.map { it.toULong() })

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Packed>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }
}
