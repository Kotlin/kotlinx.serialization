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
data class KTestMessagesProto3Unpacked(
    @ProtoNumber(89) val unpackedInt32: List<Int> = emptyList(),
    @ProtoNumber(90) val unpackedInt64: List<Long> = emptyList(),
    @ProtoNumber(91) val unpackedUint32: List<UInt> = emptyList(),
    @ProtoNumber(92) val unpackedUint64: List<ULong> = emptyList(),
    @ProtoNumber(93) val unpackedSint32: List<Int> = emptyList(),
    @ProtoNumber(94) val unpackedSint64: List<Long> = emptyList(),
    @ProtoNumber(95) val unpackedFixed32: List<Int> = emptyList(),
    @ProtoNumber(96) val unpackedFixed64: List<Long> = emptyList(),
    @ProtoNumber(97) val unpackedSfixed32: List<Int> = emptyList(),
    @ProtoNumber(98) val unpackedSfixed64: List<Long> = emptyList(),
    @ProtoNumber(99) val unpackedFloat: List<Float> = emptyList(),
    @ProtoNumber(100) val unpackedDouble: List<Double> = emptyList(),
    @ProtoNumber(101) val unpackedBool: List<Boolean> = emptyList(),
)

class Proto3UnpackedTest {
    @Test
    fun default() {
        val message = KTestMessagesProto3Unpacked(
            unpackedInt32 = Gen.list(Gen.int()).generate(),
            unpackedInt64 = Gen.list(Gen.long()).generate(),
            unpackedUint32 = Gen.list(Gen.int().map { it.toUInt() }).generate(),
            unpackedUint64 = Gen.list(Gen.long().map { it.toULong() }).generate(),
            unpackedFloat = Gen.list(Gen.float()).generate(),
            unpackedDouble = Gen.list(Gen.double()).generate(),
            unpackedBool = Gen.list(Gen.bool()).generate(),
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.unpackedInt32, restored.unpackedInt32List)
        assertEquals(message.unpackedInt64, restored.unpackedInt64List)
        assertEquals(message.unpackedUint32, restored.unpackedUint32List.map { it.toUInt() })
        assertEquals(message.unpackedUint64, restored.unpackedUint64List.map { it.toULong() })
        assertEquals(message.unpackedFloat, restored.unpackedFloatList)
        assertEquals(message.unpackedDouble, restored.unpackedDoubleList)
        assertEquals(message.unpackedBool, restored.unpackedBoolList)

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Unpacked>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }

    @Test
    @Ignore
    // Issue: https://github.com/Kotlin/kotlinx.serialization/issues/2419
    fun signedAndFixed() {
        val message = KTestMessagesProto3Unpacked(
            unpackedSint32 = Gen.list(Gen.int()).generate(),
            unpackedSint64 = Gen.list(Gen.long()).generate(),
            unpackedFixed32 = Gen.list(Gen.int()).generate(),
            unpackedFixed64 = Gen.list(Gen.long()).generate(),
            unpackedSfixed32 = Gen.list(Gen.int()).generate(),
            unpackedSfixed64 = Gen.list(Gen.long()).generate(),
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)

        assertEquals(message.unpackedSint32, restored.unpackedSint32List)
        assertEquals(message.unpackedSint64, restored.unpackedSint64List)
        assertEquals(message.unpackedFixed32, restored.unpackedFixed32List)
        assertEquals(message.unpackedFixed64, restored.unpackedFixed64List)
        assertEquals(message.unpackedSfixed32, restored.unpackedSfixed32List)
        assertEquals(message.unpackedSfixed64, restored.unpackedSfixed64List)

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Unpacked>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }
}
